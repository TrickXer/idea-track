import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ClipboardList, CheckCircle2, XCircle, Link } from 'lucide-react';
import {
  processProposalDecision,
  startProposalReview,
  type ObjectivesResponse,
  type Paged,
  type ProposalDecisionRequest,
} from '../../utils/proposalApi';
import restApi from '../../utils/restApi';

const DEFAULT_SORT = 'objectiveSeq,asc';
const DEFAULT_PAGE = 1;          // backend expects 1-based
const DEFAULT_PAGE_SIZE = 20;    // controller default is 20

// --------------------
// Optional meta shapes
// --------------------
type ProposalMeta = {
  proposalId: number;
  ideaId: number;
  userId: number;
  budget: number;
  timeLineStart?: string;
  timeLineEnd?: string;
  ideaStatus: string;
  createdAt?: string;
  updatedAt?: string;
};

type IdeaMeta = {
  ideaId: number;
  title?: string;
  problemStatement?: string;
  description?: string;
  categoryName?: string;
  authorName?: string;
  tags?: string[];
  thumbnailURL?: string;
  createdAt?: string;
  upvotes?: number;
  downvotes?: number;
};

// --------------------
// Helper: Proof cell
// --------------------
function ProofFileCell({ row }: { row: ObjectivesResponse }) {
  const name = row.proofFileName;
  const type = row.proofContentType;
  const path = row.proofFilePath;
  const size = row.proofSizeBytes;

  if (!name && !path) return <>—</>;

  return (
    <div className="small">
      <div><strong>{name ?? '(unnamed proof)'}</strong></div>
      {type && <div className="text-muted">Type: {type}</div>}
      {typeof size === 'number' && <div className="text-muted">Size: {(size / 1024).toFixed(1)} KB</div>}
      {/* Add a download link if you expose one:
      <div className="mt-1">
        <a className="btn btn-sm btn-outline-secondary" href={`/api/files/download?path=${encodeURIComponent(path!)}`} target="_blank" rel="noreferrer">Download</a>
      </div> */}
    </div>
  );
}

const ProposalReview: React.FC = () => {
  const { proposalId: proposalIdParam } = useParams<{ proposalId: string }>();
  const proposalId = proposalIdParam ? Number(proposalIdParam) : null;
  const navigate = useNavigate();

  const canQuery = useMemo(
    () => typeof proposalId === 'number' && !Number.isNaN(proposalId) && proposalId > 0,
    [proposalId]
  );

  // ---------
  // Filters
  // ---------
  const [hasProof, setHasProof] = useState<'any' | 'yes' | 'no'>('any');
  const [proofType, setProofType] = useState<'any' | 'pdf' | 'jpg' | 'png'>('any'); // pdf|jpg|png
  const [mandatory, setMandatory] = useState<'any' | 'yes' | 'no'>('any');
  const [search, setSearch] = useState<string>('');

  // -----------
  // Paging/sort
  // -----------
  const [page, setPage] = useState<number>(DEFAULT_PAGE);            // 1-based
  const [pageSize, setPageSize] = useState<number>(DEFAULT_PAGE_SIZE);
  const [sort, setSort] = useState<string>(DEFAULT_SORT);

  // ----------
  // Data state
  // ----------
  const [data, setData] = useState<Paged<ObjectivesResponse> | null>(null);
  const [loading, setLoading] = useState(false);

  // Primary call error surfaces as alert
  const [error, setError] = useState<string | null>(null);
  const [serverError, setServerError] = useState<any>(null);

  const [toast, setToast] = useState<string | null>(null);

  // Review state
  const [busy, setBusy] = useState(false);
  const [decision, setDecision] = useState<'APPROVED' | 'REJECTED' | null>(null);
  const [reviewStarted, setReviewStarted] = useState(false);

  // Optional meta sections
  const [proposalMeta, setProposalMeta] = useState<ProposalMeta | null>(null);
  const [ideaMeta, setIdeaMeta] = useState<IdeaMeta | null>(null);

  // --------------------------------
  // Auto-start review (PROJECTPROPOSAL → UNDERREVIEW)
  // --------------------------------
  useEffect(() => {
    if (!canQuery) return;
    startProposalReview(proposalId as number)
      .then(() => setReviewStarted(true))
      .catch(() => setReviewStarted(true)); // ignore if already UNDERREVIEW / forbidden
  }, [canQuery, proposalId]);

  // --------------------------------
  // Fetch Proposal meta (non-blocking, do not surface alerts)
  // --------------------------------
  useEffect(() => {
    let cancel = false;
    (async () => {
      if (!canQuery) return;
      try {
        // Align with your restApi baseURL:
        // - If baseURL = http://localhost:8091/api → use '/proposal/{id}'
        // - If baseURL = http://localhost:8091     → use '/api/proposal/{id}'
        const resp = await restApi.get<ProposalMeta>(`/api/proposal/${proposalId}`);
        if (!cancel) setProposalMeta(resp.data);
      } catch (e: any) {
        // Ignore 401/403 (not critical for the grid)
        if (e?.response?.status !== 401 && e?.response?.status !== 403) {
          // optional: console.warn('Proposal meta fetch failed', e);
        }
      }
    })();
    return () => { cancel = true; };
  }, [canQuery, proposalId]);

  // --------------------------------
  // Fetch Idea meta (best-effort, do not surface alerts)
  // --------------------------------
  useEffect(() => {
    let cancel = false;
    (async () => {
      if (!proposalMeta?.ideaId) return;
      const ideaId = proposalMeta.ideaId;
      const candidates = [
        `/api/idea/${ideaId}`,
        `/api/ideas/${ideaId}`,
        `/api/ideas/by-id/${ideaId}`,
      ];
      for (const url of candidates) {
        try {
          const r = await restApi.get<any>(url);
          const d = r?.data || {};
          const mapped: IdeaMeta = {
            ideaId,
            title: d?.title ?? d?.ideaTitle ?? '',
            problemStatement: d?.problemStatement ?? d?.problem ?? '',
            description: d?.description ?? '',
            categoryName: d?.category?.name ?? d?.categoryName ?? '',
            authorName: d?.author?.displayName ?? d?.authorName ?? '',
            tags: (typeof d?.tag === 'string'
              ? d.tag.split(',').map((t: string) => t.trim()).filter(Boolean)
              : Array.isArray(d?.tags) ? d.tags : []),
            thumbnailURL: d?.thumbnailURL ?? d?.attachmentUrl ?? '',
            createdAt: d?.createdAt ?? '',
            upvotes: d?.votes?.upvotes ?? d?.upvotes ?? 0,
            downvotes: d?.votes?.downvotes ?? d?.downvotes ?? 0,
          };
          if (!cancel) setIdeaMeta(mapped);
          break;
        } catch (e: any) {
          // Ignore auth errors and try the next candidate
          if (e?.response?.status === 401 || e?.response?.status === 403) continue;
        }
      }
    })();
    return () => { cancel = true; };
  }, [proposalMeta?.ideaId]);

  // -----------------
  // Build query params
  // -----------------
  const buildParams = () => {
    const params: Record<string, any> = {
      page,        // 1-based per controller
      pageSize,
      sort,
    };
    if (hasProof !== 'any') params.hasProof = hasProof === 'yes';
    if (proofType !== 'any') params.proofType = proofType; // pdf|jpg|png
    if (mandatory !== 'any') params.mandatory = mandatory === 'yes';
    if (search?.trim()) params.search = search.trim();
    return params;
  };

  // -------------
  // Fetch content (PRIMARY call — errors show in banner)
  // -------------
  const fetchObjectives = async () => {
    if (!canQuery) return;
    setLoading(true);
    setError(null);
    setServerError(null);
    try {
      const res = await restApi.get(`/api/adminReview/proposal/${proposalId}/review`, {
        params: buildParams(),
      });
      setData(res.data);
    } catch (e: any) {
      // Surface only this call's failures
      const msg = e?.response?.data?.message || e?.message || 'Failed to load objectives';
      setError(msg);
      setServerError(e?.response?.data ?? null);
    } finally {
      setLoading(false);
    }
  };

  // Initial + whenever filters/paging/sort change
  useEffect(() => {
    if (!canQuery) return;
    fetchObjectives();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [canQuery, page, pageSize, sort, hasProof, proofType, mandatory, search]);

  // ------------------------
  // Approve / Reject actions
  // ------------------------
  const onDecision = async (d: ProposalDecisionRequest['decision']) => {
    if (!canQuery || busy) return;
    setDecision(d);
    setBusy(true);
    setError(null);
    setServerError(null);
    try {
      // Ensure UNDERREVIEW (safe no-op if already)
      if (!reviewStarted) {
        await startProposalReview(proposalId as number).catch(() => {});
        setReviewStarted(true);
      }
      const comments = prompt(`Enter optional comments for ${d.toLowerCase()}:`) || undefined;
      const msg = await processProposalDecision(proposalId as number, { decision: d, comments });
      setToast(msg || `Proposal ${d.toLowerCase()} successfully`);

      // Reload list after decision
      await fetchObjectives();
    } catch (e: any) {
      const status = e?.response?.status;
      if (status === 401) {
        window.location.href = '/login';
        return;
      }
      const msg = e?.response?.data?.message || e?.message || 'Failed to submit decision';
      setDecision(null);
      setError(msg);
      setServerError(e?.response?.data ?? null);
    } finally {
      setBusy(false);
    }
  };

  // ----------------------
  // Render
  // ----------------------
  if (!canQuery) {
    return (
      <div className="container my-4">
        <div className="alert alert-danger">Invalid proposal id.</div>
      </div>
    );
  }

  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;
  // Spring Page "number" is zero-based; we convert to 1-based for display
  const serverPageNumberZeroBased = (data as any)?.number;
  const currentPageDisplay = serverPageNumberZeroBased != null ? Number(serverPageNumberZeroBased) + 1 : page;

  return (
    <div className="container-fluid py-3">
      <div className="d-flex align-items-center justify-content-between mb-3">
        <div>
          <h4 className="mb-0 d-flex align-items-center gap-2"><ClipboardList size={20} /> Proposal Review</h4>
          <small className="text-muted">
            Proposal #{proposalId} • Review objectives, then approve or reject
          </small>
        </div>
        <div className="d-flex gap-2">
          <button
            className="btn btn-success"
            disabled={busy || decision === 'REJECTED'}
            onClick={() => onDecision('APPROVED')}
          >
            {busy && decision === 'APPROVED' ? 'Approving…' : <><CheckCircle2 size={15} className="me-1" />Approve</>}
          </button>
          <button
            className="btn btn-danger"
            disabled={busy || decision === 'APPROVED'}
            onClick={() => onDecision('REJECTED')}
          >
            {busy && decision === 'REJECTED' ? 'Rejecting…' : <><XCircle size={15} className="me-1" />Reject</>}
          </button>
        </div>
      </div>

      {/* Toast & primary-call errors */}
      {toast && (
        <div className="alert alert-success py-2 d-flex align-items-center justify-content-between">
          <span>{toast}</span>
          <button className="btn-close" onClick={() => setToast(null)} />
        </div>
      )}
      {error && <div className="alert alert-danger">{error}</div>}
      {serverError && (
        <div className="alert alert-warning">
          <div className="fw-bold">Server error details</div>
          <pre className="mb-0" style={{ maxHeight: 240, overflow: 'auto', whiteSpace: 'pre-wrap' }}>
            {typeof serverError === 'string' ? serverError : JSON.stringify(serverError, null, 2)}
          </pre>
        </div>
      )}

      {/* Proposal meta (non-blocking) */}
      {proposalMeta && (
        <div className="card mb-3">
          <div className="card-header">Proposal Details</div>
          <div className="card-body">
            <div className="row g-3">
              <div className="col-md-2">
                <div className="small text-muted">Proposal ID</div>
                <div className="fw-semibold">{proposalMeta.proposalId}</div>
              </div>
              <div className="col-md-2">
                <div className="small text-muted">Idea ID</div>
                <div className="fw-semibold">{proposalMeta.ideaId}</div>
              </div>
              <div className="col-md-2">
                <div className="small text-muted">User ID</div>
                <div className="fw-semibold">{proposalMeta.userId}</div>
              </div>
              <div className="col-md-2">
                <div className="small text-muted">Budget</div>
                <div className="fw-semibold">{proposalMeta.budget}</div>
              </div>
              <div className="col-md-2">
                <div className="small text-muted">Start</div>
                <div className="fw-semibold">{proposalMeta.timeLineStart || '—'}</div>
              </div>
              <div className="col-md-2">
                <div className="small text-muted">End</div>
                <div className="fw-semibold">{proposalMeta.timeLineEnd || '—'}</div>
              </div>
            </div>
            <div className="row g-3 mt-1">
              <div className="col-md-2">
                <div className="small text-muted">Status</div>
                <div className="fw-semibold">{proposalMeta.ideaStatus}</div>
              </div>
              <div className="col-md-3">
                <div className="small text-muted">Created</div>
                <div className="fw-semibold">
                  {proposalMeta.createdAt ? new Date(proposalMeta.createdAt).toLocaleString() : '—'}
                </div>
              </div>
              <div className="col-md-3">
                <div className="small text-muted">Updated</div>
                <div className="fw-semibold">
                  {proposalMeta.updatedAt ? new Date(proposalMeta.updatedAt).toLocaleString() : '—'}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Idea meta (best-effort) */}
      {ideaMeta && (
        <div className="card mb-3">
          <div className="card-header">Idea Details</div>
          <div className="card-body">
            <h5 className="fw-bold mb-1">{ideaMeta.title || '—'}</h5>
            <div className="text-muted small mb-2">
              {ideaMeta.categoryName ? <>Category: <strong>{ideaMeta.categoryName}</strong> · </> : null}
              {ideaMeta.authorName ? <>By <strong>{ideaMeta.authorName}</strong></> : null}
            </div>

            {(ideaMeta.tags?.length ?? 0) > 0 && (
              <div className="mb-2">
                {ideaMeta.tags!.map((t, i) => (
                  <span key={i} className="badge bg-light text-primary border border-primary me-1">{t}</span>
                ))}
              </div>
            )}

            <div className="row g-3">
              <div className="col-md-4">
                <div className="small text-muted">Created</div>
                <div className="fw-semibold">
                  {ideaMeta.createdAt ? new Date(ideaMeta.createdAt).toLocaleString() : '—'}
                </div>
              </div>
              <div className="col-md-4">
                <div className="small text-muted">Upvotes / Downvotes</div>
                <div className="fw-semibold">
                  {ideaMeta.upvotes ?? 0} / {ideaMeta.downvotes ?? 0}
                </div>
              </div>
              <div className="col-md-4">
                {ideaMeta.thumbnailURL && (
                  <>
                    <div className="small text-muted">Attachment</div>
                    <a
                      href={ideaMeta.thumbnailURL}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="btn btn-sm btn-outline-primary rounded-pill"
                    >
                        <Link size={13} className="me-1" /> View Attachment
                    </a>
                  </>
                )}
              </div>
            </div>

            <hr />

            <div className="mb-2">
              <div className="small text-muted">Problem Statement</div>
              <div className="text-secondary" style={{ whiteSpace: 'pre-wrap' }}>
                {ideaMeta.problemStatement || '—'}
              </div>
            </div>

            <div>
              <div className="small text-muted">Description</div>
              <div className="text-secondary" style={{ whiteSpace: 'pre-wrap' }}>
                {ideaMeta.description || '—'}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Filters */}
      {/* <div className="card mb-3">
        <div className="card-header">Filters</div>
        <div className="card-body">
          <div className="row g-3 align-items-end">
            <div className="col-md-2">
              <label className="form-label">Has Proof</label>
              <select className="form-select" value={hasProof} onChange={(e) => { setPage(1); setHasProof(e.target.value as any); }}>
                <option value="any">Any</option>
                <option value="yes">Yes</option>
                <option value="no">No</option>
              </select>
            </div>
            <div className="col-md-2">
              <label className="form-label">Proof Type</label>
              <select className="form-select" value={proofType} onChange={(e) => { setPage(1); setProofType(e.target.value as any); }}>
                <option value="any">Any</option>
                <option value="pdf">PDF</option>
                <option value="jpg">JPG</option>
                <option value="png">PNG</option>
              </select>
            </div>
            <div className="col-md-2">
              <label className="form-label">Mandatory</label>
              <select className="form-select" value={mandatory} onChange={(e) => { setPage(1); setMandatory(e.target.value as any); }}>
                <option value="any">Any</option>
                <option value="yes">Yes</option>
                <option value="no">No</option>
              </select>
            </div>
            <div className="col-md-4">
              <label className="form-label">Search (title/description)</label>
              <input
                type="text"
                className="form-control"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') { setPage(1); fetchObjectives(); } }}
                placeholder="Type & press Enter to search"
              />
            </div>
            <div className="col-md-2 d-flex gap-2">
              <button className="btn btn-primary w-100" onClick={() => { setPage(1); fetchObjectives(); }} disabled={loading}>
                Apply
              </button>
              <button
                className="btn btn-outline-secondary w-100"
                onClick={() => {
                  setHasProof('any');
                  setProofType('any');
                  setMandatory('any');
                  setSearch('');
                  setSort(DEFAULT_SORT);
                  setPage(1);
                  setPageSize(DEFAULT_PAGE_SIZE);
                }}
                disabled={loading}
              >
                Reset
              </button>
            </div>
          </div>
        </div>
      </div> */}

      {/* Objectives table */}
      {/* Objectives */}
<div className="card border-0 shadow-sm">
  <div className="card-header bg-white d-flex align-items-center justify-content-between">
    <div>
      <h6 className="mb-0">Objectives</h6>
      <small className="text-muted">
        {totalElements} total • Page {currentPageDisplay} / {totalPages || 1}
      </small>
    </div>
    <div className="d-flex align-items-center gap-2">
      <label className="form-label mb-0 me-1">Sort</label>
      <select
        className="form-select form-select-sm"
        style={{ width: 220 }}
        value={sort}
        onChange={(e) => { setPage(1); setSort(e.target.value); }}
      >
        <option value="objectiveSeq,asc">Seq ↑</option>
        <option value="objectiveSeq,desc">Seq ↓</option>
        <option value="updatedAt,desc">Updated ↓</option>
        <option value="updatedAt,asc">Updated ↑</option>
        <option value="title,asc">Title A→Z</option>
        <option value="title,desc">Title Z→A</option>
      </select>

      <label className="form-label mb-0 ms-3 me-1">Page size</label>
      <select
        className="form-select form-select-sm"
        style={{ width: 100 }}
        value={pageSize}
        onChange={(e) => { setPage(1); setPageSize(Number(e.target.value)); }}
      >
        <option value={10}>10</option>
        <option value={20}>20</option>
        <option value={50}>50</option>
        <option value={100}>100</option>
      </select>

      <div className="btn-group ms-2">
        <button className="btn btn-outline-secondary btn-sm"
          onClick={() => setPage((p) => Math.max(1, p - 1))}
          disabled={loading || currentPageDisplay <= 1}
        >
          ‹ Prev
        </button>
        <button className="btn btn-outline-secondary btn-sm"
          onClick={() => setPage((p) => p + 1)}
          disabled={loading || (totalPages > 0 && currentPageDisplay >= totalPages)}
        >
          Next ›
        </button>
      </div>
    </div>
  </div>

  {loading && <div className="text-center py-3 text-muted">Loading objectives…</div>}

  <div className="card-body">
    {(data?.content?.length ?? 0) === 0 ? (
      <div className="text-center text-muted py-4 border rounded">
        {canQuery ? 'No objectives found for this proposal' : 'Open this screen with a proposalId in the URL'}
      </div>
    ) : (
      <div className="d-flex flex-column gap-3">
        {data!.content!.map((row, idx) => {
          const id = row.id ?? '—';
          const seq = row.objectiveSeq ?? '—';
          const title = row.title || '—';
          const description = row.description || '—';
          const updated = row.updatedAt ? new Date(row.updatedAt).toLocaleString() : '—';
          const isMandatory = !!row.mandatory;

          return (
            <div key={String(row.id ?? `${idx}-${row.objectiveSeq}`)} className="border rounded-3">
              {/* Header */}
              <div className="px-3 py-2 d-flex align-items-center justify-content-between" style={{ background: '#fafafa' }}>
                <div className="d-flex align-items-center gap-3">
                  <div>
                    <div className="small text-muted">Objective Seq</div>
                    <div className="fw-semibold">{seq}</div>
                  </div>
                  <div className="vr" />
                  <div>
                    <div className="small text-muted">ID</div>
                    <div className="fw-semibold">{id}</div>
                  </div>
                </div>
                <div>
                  <span className={`badge rounded-pill ${isMandatory ? 'bg-danger-subtle text-danger border border-danger' : 'bg-success-subtle text-success border border-success'}`}>
                    {isMandatory ? 'Mandatory' : 'Optional'}
                  </span>
                </div>
              </div>

              {/* Body */}
              <div className="px-3 py-3">
                <div className="mb-2">
                  <div className="small text-muted">Title</div>
                  <div className="fw-bold">{title}</div>
                </div>
                <div>
                  <div className="small text-muted">Description</div>
                  <div className="text-secondary" style={{ whiteSpace: 'pre-wrap' }}>
                    {description}
                  </div>
                </div>
              </div>

              {/* Footer */}
              <div className="px-3 py-2 border-top d-flex justify-content-end">
                <small className="text-muted">
                  Updated: <span className="fw-semibold">{updated}</span>
                </small>
              </div>
            </div>
          );
        })}
      </div>
    )}
  </div>
</div>
    </div>
  );
};

export default ProposalReview;