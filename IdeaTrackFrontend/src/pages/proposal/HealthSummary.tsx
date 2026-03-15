import React, { useEffect, useState } from 'react';
import { getHealthSummary, type AdminHealthSummaryDto } from '../../utils/proposalApi';
import { Activity, RefreshCw, Database, BarChart2, TrendingUp, Info, CheckCircle2, AlertTriangle, XCircle } from 'lucide-react';

const HealthSummary: React.FC = () => {
  const [data, setData] = useState<AdminHealthSummaryDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetch = async () => {
    setLoading(true);
    setError(null);
    try {
      const resp = await getHealthSummary();
      setData(resp);
    } catch (err: any) {
      setError(err?.message || 'Failed to load health summary');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetch(); }, []);

  if (loading) {
    return <div className="text-center p-5"><span className="spinner-border" role="status"></span></div>;
  }

  if (error) {
    return <div className="alert alert-danger">{error}</div>;
  }

  if (!data) {
    return <div className="alert alert-info">No data available</div>;
  }

  const getStatusBadge = (status: string) => {
    if (status === 'healthy') return <span className="badge bg-success d-inline-flex align-items-center gap-1"><CheckCircle2 size={12} /> Healthy</span>;
    if (status === 'degraded') return <span className="badge bg-warning d-inline-flex align-items-center gap-1"><AlertTriangle size={12} /> Degraded</span>;
    return <span className="badge bg-danger d-inline-flex align-items-center gap-1"><XCircle size={12} /> Down</span>;
  };

  const StatCard: React.FC<{icon: React.ReactNode; label: string; value: string | number; color: string}> = ({icon, label, value, color}) => (
    <div className="col-md-6 col-lg-3 mb-3">
      <div className={`card border-0 shadow-sm h-100 bg-${color} bg-opacity-10`}>
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start">
            <div>
              <small className="text-muted d-block mb-2">{label}</small>
              <h3 className="mb-0 fw-bold">{value}</h3>
            </div>
            <div style={{fontSize: '2rem'}}>{icon}</div>
          </div>
        </div>
      </div>
    </div>
  );

  const StatusCard: React.FC<{icon: React.ReactNode; label: string; badge: React.ReactNode; color: string}> = ({icon, label, badge, color}) => (
    <div className="col-md-6 col-lg-3 mb-3">
      <div className={`card border-0 shadow-sm h-100 bg-${color} bg-opacity-10`}>
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start">
            <div>
              <small className="text-muted d-block mb-2">{label}</small>
              <div>{badge}</div>
            </div>
            <div style={{fontSize: '2rem'}}>{icon}</div>
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <>
      <div className="row mb-4">
        <div className="col">
          <h5 className="fw-bold d-flex align-items-center gap-2"><Activity size={18} className="text-success" /> System Health Summary</h5>
          <small className="text-muted">Real-time overview of system status and performance</small>
        </div>
        <div className="col-auto">
          <button className="btn btn-sm btn-outline-secondary d-inline-flex align-items-center gap-1" onClick={fetch}><RefreshCw size={13} /> Refresh</button>
        </div>
      </div>

      <div className="row">
        <StatusCard icon={<Database size={28} />} label="Database Status" badge={getStatusBadge(data.database)} color="primary" />
        <StatCard icon={<BarChart2 size={28} />} label="Queue Backlog" value={data.queueBacklog} color="info" />
        <StatCard icon={<Activity size={28} />} label="Service Uptime" value={data.serviceUptime} color="success" />
        <StatCard icon={<TrendingUp size={28} />} label="Pending Jobs" value={data.pendingJobs} color="warning" />
      </div>

      <div className="row mt-3">
        <div className="col-12">
          <div className="card border-0 shadow-sm">
            <div className="card-header bg-white">
              <h6 className="mb-0 d-flex align-items-center gap-2"><Info size={15} /> System Information</h6>
            </div>
            <div className="card-body">
              <div className="row">
                <div className="col-md-6">
                  <div className="mb-3">
                    <label className="form-label small text-muted">Application Version</label>
                    <div>
                      <strong>{data.version}</strong>
                    </div>
                  </div>
                </div>
                <div className="col-md-6">
                  <div className="mb-3">
                    <label className="form-label small text-muted">Last Updated</label>
                    <div>
                      <small>{new Date(data.timestamp).toLocaleString()}</small>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default HealthSummary;
