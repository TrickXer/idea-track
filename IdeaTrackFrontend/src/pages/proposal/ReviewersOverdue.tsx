import React, { useEffect, useState } from 'react';
import { getOverdueReviewers } from '../../utils/proposalApi';
import { AlertCircle, AlertTriangle, Info, CheckCircle2 } from 'lucide-react';

type Reviewer = { id: number; pendingTasks: number; overdueByDays: number };

const ReviewersOverdue: React.FC = () => {
  const [reviewers, setReviewers] = useState<Reviewer[]>([]);
  const [_rawResponse, setRawResponse] = useState<any>(null);
  const [_rawContent, setRawContent] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetch = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getOverdueReviewers({ page: 0, size: 50 });
      console.debug('getOverdueReviewers raw response:', data);
      setRawResponse(data);
      const content: any[] = data?.content ?? data ?? [];
      console.debug('resolved content array:', content);
      setRawContent(content);

      // Normalize items to expected shape { id, name, overdueCount, lastAssigned }
      const normalized = content.map(item => {
        const id = item.id ?? item.reviewerId ?? item.userId ?? item.reviewer?.id ?? item.user?.id ?? null;

        // const nameCandidates: any[] = [];
        // if (item.name) nameCandidates.push(item.name);
        // if (item.fullName) nameCandidates.push(item.fullName);
        // if (item.reviewerName) nameCandidates.push(item.reviewerName);
        // if (item.reviewer && typeof item.reviewer === 'object') {
        //   if (item.reviewer.name) nameCandidates.push(item.reviewer.name);
        //   if (item.reviewer.fullName) nameCandidates.push(item.reviewer.fullName);
        //   if (item.reviewer.firstName || item.reviewer.lastName) nameCandidates.push([item.reviewer.firstName, item.reviewer.lastName].filter(Boolean).join(' '));
        // }
        // if (item.user && typeof item.user === 'object') {
        //   if (item.user.name) nameCandidates.push(item.user.name);
        //   if (item.user.fullName) nameCandidates.push(item.user.fullName);
        //   if (item.user.firstName || item.user.lastName) nameCandidates.push([item.user.firstName, item.user.lastName].filter(Boolean).join(' '));
        // }
        // if (item.firstName || item.lastName) nameCandidates.push([item.firstName, item.lastName].filter(Boolean).join(' '));

        // const name = nameCandidates.find(v => v && String(v).trim()) ?? 'Unknown';

        const pendingTasks = item.pendingTasks ?? item.overdue ?? item.count ?? 0;
        
        const overdueByDays = item.overdueByDays ?? item.assignedAt ?? item.lastAssignedAt ?? item.reviewer?.lastAssignedAt ?? item.assigned_at ?? null;

        return { id, pendingTasks, overdueByDays };
      });
      setReviewers(normalized as any);
    } catch (err: any) {
      setError(err?.message || 'Failed to load overdue reviewers');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetch(); }, []);

  const getPriorityBadge = (count: number) => {
    if (count >= 10) return <span className="badge bg-danger d-inline-flex align-items-center gap-1"><AlertCircle size={11} /> Critical</span>;
    if (count >= 3) return <span className="badge bg-warning d-inline-flex align-items-center gap-1"><AlertTriangle size={11} /> High</span>;
    return <span className="badge bg-info d-inline-flex align-items-center gap-1"><Info size={11} /> Medium</span>;
  };

  const [copiedId, setCopiedId] = useState<number | null>(null);

  const copyId = async (id: number) => {
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(String(id));
      } else {
        const el = document.createElement('textarea');
        el.value = String(id);
        document.body.appendChild(el);
        el.select();
        document.execCommand('copy');
        document.body.removeChild(el);
      }
      setCopiedId(id);
      setTimeout(() => setCopiedId(null), 1500);
    } catch (e) {
      console.error('copy failed', e);
    }
  };

  return (
    <div>
      {/* Debug toggle and dump - remove in production */}
      {/* <div className="mb-3">
        <button className="btn btn-sm btn-outline-secondary me-2" onClick={() => { console.debug('rawResponse', rawResponse); alert('Check console for rawResponse'); }}>
          Debug: Dump Raw Response
        </button>
        <button className="btn btn-sm btn-outline-secondary" onClick={() => { console.debug('normalized', reviewers); alert('Check console for normalized array'); }}>
          Debug: Dump Normalized
        </button>
      </div> */}
      <div className="card border-0 shadow-sm">
        <div className="card-header bg-white border-bottom">
          <div className="row align-items-center">
            <div className="col">
              <h5 className="mb-0 d-flex align-items-center gap-2"><AlertTriangle size={17} className="text-warning" /> Overdue Reviewers</h5>
              <small className="text-muted">Reviewers with pending tasks</small>
            </div>
            <div className="col-auto">
              <button className="btn btn-sm btn-outline-secondary" onClick={fetch} disabled={loading}>
                {loading ? 'Loading...' : 'Refresh'}
              </button>
            </div>
          </div>
        </div>

        {error && <div className="alert alert-danger m-3 mb-0">{error}</div>}

        <div className="card-body p-0">
          {loading && <div className="text-center p-4 text-muted">Loading reviewers...</div>}
          
          {!loading && reviewers.length === 0 && (
            <div className="text-center p-4 text-muted d-flex align-items-center justify-content-center gap-2"><CheckCircle2 size={15} className="text-success" /> All reviewers are on track</div>
          )}

          {!loading && reviewers.length > 0 && (
            <div className="table-responsive">
              <table className="table table-hover mb-0">
                <thead className="table-light">
                  <tr>
                    <th style={{width: '80px'}}>ID</th>
                    {/* <th>Reviewer</th> */}
                    <th style={{width: '120px'}}>Priority</th>
                    <th style={{width: '150px'}}>Pending Tasks</th>
                    <th style={{width: '150px'}}>Overdue By Days</th>
                    {/* <th style={{width: '120px'}}>Action</th> */}
                  </tr>
                </thead>
                <tbody>
                  {reviewers.map(r => (
                    <tr key={r.id} className={r.pendingTasks >= 10 ? 'table-danger table-opacity-50' : r.pendingTasks >= 3 ? 'table-warning table-opacity-50' : ''}>
                      <td>
                        <button className="btn btn-sm btn-outline-secondary" onClick={() => copyId(r.id)}>
                          {r.id}
                        </button>
                        {copiedId === r.id && <div className="small text-success mt-1">Copied!</div>}
                      </td>
                      <td>{getPriorityBadge(r.pendingTasks)}</td>
                      <td>
                        <span className="badge bg-light text-dark">{r.pendingTasks} tasks</span>
                      </td>
                      <td><small>{r.overdueByDays} days</small></td>
                      {/* <td>
                        <button className="btn btn-sm btn-outline-primary">
                          📧 Remind
                        </button>
                      </td> */}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {reviewers.length > 0 && (
          <div className="card-footer bg-light border-top">
            <small className="text-muted">Showing {reviewers.length} overdue reviewers</small>
          </div>
        )}
      </div>
    </div>
  );
};

export default ReviewersOverdue;
