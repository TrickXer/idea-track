import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom'; // ⬅️ add useLocation
import { getAcceptedIdeas, type AcceptedIdeaDashboardDTO } from '../../utils/proposalApi';

type Props = {
  userId: number;
  className?: string;
};

function formatDate(d?: string | null): string {
  if (!d) return '—';
  const dt = new Date(d);
  if (Number.isNaN(dt.getTime())) return d || '—';
  return dt.toLocaleString(); // local datetime
}

function formatLocalDate(d?: string | null): string {
  if (!d) return '—';
  try {
    const dt = new Date(d);
    if (!Number.isNaN(dt.getTime())) {
      return dt.toLocaleDateString();
    }
  } catch {}
  return d;
}

function statusBadge(status?: string | null) {
  if (!status) return null;
  const s = status.toUpperCase();
  let cls = 'bg-secondary';
  if (s.includes('DRAFT')) cls = 'bg-warning text-dark';
  else if (s.includes('PROJECTPROPOSAL') || s.includes('SUBMITTED')) cls = 'bg-info text-dark';
  else if (s.includes('APPROVED') || s.includes('ACCEPTED')) cls = 'bg-success';
  else if (s.includes('REJECT')) cls = 'bg-danger';
  return <span className={`badge ${cls}`}>{status}</span>;
}

export default function AcceptedIdeasList({ userId, className = '' }: Props) {
  const navigate = useNavigate();
  const location = useLocation() as { state?: { updated?: { ideaId: number; proposalId: number; proposalStatus: string } } }; // ⬅️ optional: receive patch
  const [rows, setRows] = useState<AcceptedIdeaDashboardDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState('');
  const [q, setQ] = useState('');

  useEffect(() => {
    let mounted = true;
    (async () => {
      setLoading(true);
      setErr('');
      try {
        const data = await getAcceptedIdeas(userId);
        if (mounted) setRows(data ?? []);
      } catch (e: any) {
        if (mounted) setErr(e?.response?.data?.message || e?.message || 'Failed to load accepted ideas');
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, [userId]);

  // ⬅️ Optional: apply optimistic patch if submit page navigated back with updated status
  useEffect(() => {
    const patch = location.state?.updated;
    if (!patch) return;
    setRows(prev =>
      prev.map(r =>
        r.ideaId === patch.ideaId
          ? { ...r, proposalId: patch.proposalId, proposalStatus: patch.proposalStatus }
          : r
      )
    );
  }, [location.state]);

  const filtered = useMemo(() => {
    const needle = q.trim().toLowerCase();
    if (!needle) return rows;
    return rows.filter(r => {
      const hay =
        `${r.ideaTitle ?? ''} ${r.ideaDescription ?? ''} ${r.ideaStatus ?? ''} ${r.proposalStatus ?? ''}`
          .toLowerCase();
      return hay.includes(needle);
    });
  }, [rows, q]);

  const toCreate = (ideaId: number) => {
    navigate(`/employee/proposals/new/${ideaId}`);
  };
  const toEdit = (proposalId: number) => {
    navigate(`/employee/proposals/${proposalId}/edit`);
  };

  return (
    <div className={`accepted-ideas-list ${className}`}>
      <div className="d-flex align-items-center justify-content-between mb-3">
        <h5 className="mb-0">Accepted Ideas ({rows.length})</h5>
        <div className="input-group" style={{ maxWidth: 380 }}>
          <span className="input-group-text">Search</span>
          <input
            className="form-control"
            placeholder="Title, description, status…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
      </div>

      {loading && <div className="alert alert-info">Loading…</div>}
      {!loading && err && <div className="alert alert-danger">{err}</div>}
      {!loading && !err && filtered.length === 0 && (
        <div className="alert alert-secondary">No accepted ideas found.</div>
      )}

      <div className="row">
        {filtered.map((r) => {
          const hasProposal = !!r.proposalId;
          const isDraft = (r.proposalStatus || '').toUpperCase() === 'DRAFT'; // ⬅️ compute here

          return (
            <div className="col-md-6 col-lg-4 mb-3" key={r.ideaId}>
              <div className="card h-100">
                <div className="card-body d-flex flex-column">
                  <div className="d-flex justify-content-between align-items-start">
                    <h6 className="card-title mb-1">{r.ideaTitle}</h6>
                    {statusBadge(r.ideaStatus)}
                  </div>

                  {r.ideaDescription && (
                    <p className="card-text text-truncate" title={r.ideaDescription}>
                      {r.ideaDescription}
                    </p>
                  )}

                  <dl className="row small mb-2">
                    <dt className="col-5">Idea Created</dt>
                    <dd className="col-7">{formatDate(r.ideaCreatedAt)}</dd>

                    <dt className="col-5">Proposal</dt>
                    <dd className="col-7">
                      {hasProposal ? (
                        <>
                          <span className="me-2">#{r.proposalId}</span>
                          {statusBadge(r.proposalStatus)}
                        </>
                      ) : (
                        <span className="text-muted">Not created</span>
                      )}
                    </dd>

                    {hasProposal && (
                      <>
                        <dt className="col-5">Budget</dt>
                        <dd className="col-7">{r.budget ?? '—'}</dd>

                        <dt className="col-5">Timeline</dt>
                        <dd className="col-7">
                          {formatLocalDate(r.timeLineStart)} → {formatLocalDate(r.timeLineEnd)}
                        </dd>

                        <dt className="col-5">Proposal Created</dt>
                        <dd className="col-7">{formatDate(r.proposalCreatedAt)}</dd>
                      </>
                    )}
                  </dl>

                  <div className="mt-auto d-flex gap-2">
                    {!hasProposal ? (
                      <button className="btn btn-primary btn-sm" onClick={() => toCreate(r.ideaId)}>
                        Create Proposal
                      </button>
                    ) : isDraft ? (
                      <button
                        className="btn btn-outline-primary btn-sm"
                        onClick={() => toEdit(r.proposalId!)}
                      >
                        Edit Draft
                      </button>
                    ) : (
                      <button
                        className="btn btn-secondary btn-sm"
                        disabled
                        style={{ cursor: 'not-allowed' }}
                        title="Submitted"
                      >
                        Submitted
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}