import React, { useEffect, useState } from 'react';
import { getAllProposals as listProposals } from '../../utils/proposalApi';
import { useNavigate } from 'react-router-dom';

type RawProposal = {
  id?: number | string;          // sometimes backend uses 'proposalId' instead
  proposalId?: number | string;  // handle this too
  title?: string;
  status?: string;               // e.g., SUBMITTED / PROJECT_PROPOSAL / PROJECTPROPOSAL
  stageId?: number;
  stageName?: string;
  createdAt?: string;
  updatedAt?: string;
  [key: string]: any;
};

type PageLike<T> = {
  content?: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
  [key: string]: any;
};

// 🧠 Normalizers
const toKey = (v?: string) => (v || '').replace(/[\s_-]/g, '').toUpperCase();
const normalizeId = (p: RawProposal): number | string | undefined =>
  p.id ?? p.proposalId;

// 🔒 If your BE uses a different string, set it here:
const SUBMITTED_VALUE = 'PROJECTPROPOSAL'; // change to 'SUBMITTED' or 'PROJECT_PROPOSAL' if needed
// You can also list accepted variants if your data varies:
const SUBMITTED_VARIANTS = new Set([
  toKey('PROJECTPROPOSAL'),
  toKey('PROJECT_PROPOSAL'),
  toKey('SUBMITTED'),
  toKey('ProjectProposal'),
]);

const PendingProposals: React.FC = () => {
  const navigate = useNavigate();

  const [loading, setLoading] = useState(false);
  const [error, setError]       = useState<string | null>(null);
  const [rawItems, setRawItems] = useState<RawProposal[]>([]);
  const [rows, setRows] = useState<Array<RawProposal & { idNorm: number | string }>>([]);
  const [debug, setDebug] = useState({ rawCount: 0, submittedCount: 0 });

  useEffect(() => {
    let cancel = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        // ⚠️ CHANGE 'status' to 'state' here if your BE expects a different param key.
        const params: Record<string, any> = {
          page: 0,
          size: 20,
          sort: 'createdAt,desc',
          status: SUBMITTED_VALUE, // server-side filter attempt
        };

        const resp = await listProposals(params);
        if (cancel) return;

        // Support Page-like or array
        const pageish = resp as PageLike<RawProposal>;
        const content: RawProposal[] = Array.isArray(pageish?.content)
          ? pageish.content!
          : Array.isArray(resp)
          ? (resp as RawProposal[])
          : [];

        // Save raw
        setRawItems(content);

        // Client-side check: normalize id and filter submitted variants
        const submitted = content.filter(p => SUBMITTED_VARIANTS.has(toKey(p.status)));
        const withId = submitted
          .map(p => ({ ...p, idNorm: normalizeId(p) }))
          .filter(p => p.idNorm !== undefined && p.idNorm !== null) as Array<RawProposal & { idNorm: number | string }>;

        setRows(withId);
        setDebug({ rawCount: content.length, submittedCount: withId.length });
      } catch (e: any) {
        if (!cancel) setError(e?.message || 'Failed to load proposals');
      } finally {
        if (!cancel) setLoading(false);
      }
    })();
    return () => { cancel = true; };
  }, []);

  const goToProposalReview = (proposalId: number | string) => {
    const idStr = String(proposalId);
    navigate(`/admin/proposals/${idStr}/review`);
  };

  return (
    <div className="container my-3">
      {/* Banner confirms page renders */}
      <div className="alert alert-info">
        <strong>PendingProposals</strong> mounted •
        Raw: {debug.rawCount} • Submitted (with id): {debug.submittedCount} •
        Expect status like <code>{SUBMITTED_VALUE}</code>
      </div>

      {loading && <div className="text-muted">Loading…</div>}
      {error && <div className="alert alert-danger">{error}</div>}

      {/* If you want to see exactly what we got (keep while debugging) */}
      <details className="mb-3">
        <summary>Show first 5 raw items (debug)</summary>
        <pre style={{ maxHeight: 240, overflow: 'auto' }}>
{JSON.stringify(rawItems.slice(0, 5), null, 2)}
        </pre>
      </details>

      {/* Table of ONLY submitted proposals with a usable id */}
      <div className="table-responsive border rounded">
        <table className="table table-hover mb-0">
          <thead className="table-light">
            <tr>
              <th style={{ width: 80 }}>ID</th>
              <th style={{ minWidth: 240 }}>Title</th>
              <th style={{ width: 160 }}>Status</th>
              <th style={{ width: 180, textAlign: 'right' }}>Action</th>
            </tr>
          </thead>
          <tbody>
            {!loading && rows.length === 0 && (
              <tr>
                <td colSpan={4} className="text-center text-muted py-4">
                  We got {rawItems.length} raw items, but none matched “submitted with an ID”.
                  <div className="small text-muted mt-2">
                    Check if status equals one of: {Array.from(SUBMITTED_VARIANTS).join(', ')}<br />
                    Also check if the item has <code>id</code> or <code>proposalId</code>.
                  </div>
                </td>
              </tr>
            )}

            {rows.map((row) => {
              const idStr = String(row.idNorm);
              return (
                <tr key={idStr}>
                  <td>{row.idNorm}</td>
                  <td className="text-truncate" style={{ maxWidth: 420 }}>
                    {row.title ?? '—'}
                  </td>
                  <td><span className="badge bg-light text-dark">{row.status ?? '—'}</span></td>
                  <td className="text-end">
                    <button
                      className="btn btn-sm btn-primary"
                      onClick={() => goToProposalReview(row.idNorm)}
                    >
                      Start Reviewing
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default PendingProposals;