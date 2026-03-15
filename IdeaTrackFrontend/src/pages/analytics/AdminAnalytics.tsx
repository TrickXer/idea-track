// src/pages/analytics/AdminAnalytics.tsx
// Admin Analytics Dashboard – all Employee + Reviewer graphs plus admin-only
// charts: Project Approval Metrics, Approved Idea Distribution, Department Statistics.
import React, { useEffect, useState } from "react";
import {
  LineChart, Line, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from "recharts";
import {
  Lightbulb, Trophy,
  RefreshCw, TrendingUp,
  Building2, BarChart2, PieChart as PieIcon,
} from "lucide-react";
import { fetchMyProfile } from "../../utils/profileApi";
import EngagementChart from "../../components/analytics/EngagementChart";
import {
  getPerformanceEmployee,
  getIdeaDistribution,
  getLeaderboard,
  getProjectApprovalMetrics,
  getApprovedIdeaDistribution,
  getDepartmentStatistics,
  getTotalIdeasSubmitted,
  type EmployeePerformanceDTO,
  type CategoryCountDTO,
  type LeaderboardDTO,
  type ProjectApprovalMetricsDTO,
  type DepartmentMetricsDTO,
} from "../../utils/analyticsApi";

// ─── Colour palette ──────────────────────────────────────────────
const PRIMARY   = "#4318FF";
const SECONDARY = "#868CFF";
const ACCENT    = "#21C6DE";
const WARN      = "#FFCE20";
const SUCCESS   = "#05CD99";
const DANGER    = "#FF6B6B";
const PIE_COLORS = [PRIMARY, SECONDARY, ACCENT, WARN, SUCCESS, DANGER, "#FF9F43", "#A29BFE", "#FD79A8", "#00B894"];
const CURRENT_YEAR = new Date().getFullYear();

// ─── Shared styles ───────────────────────────────────────────────
const inputStyle: React.CSSProperties = {
  padding: "6px 10px", border: "1px solid #E2E8F0", borderRadius: 8,
  fontSize: 13, width: 80, color: "#1B254B",
};
const btnSmall: React.CSSProperties = {
  padding: "6px 14px", background: PRIMARY, color: "white",
  border: "none", borderRadius: 8, fontSize: 13, cursor: "pointer", fontWeight: 600,
};
const tdStyle: React.CSSProperties = { padding: "10px 16px", fontSize: 14, color: "#1B254B" };

// ─── Sub-components ──────────────────────────────────────────────
const StatCard: React.FC<{ icon: React.ReactNode; label: string; value: number | string; color: string }> = (
  { icon, label, value, color }
) => (
  <div style={{
    background: "white", borderRadius: 16, padding: "20px 24px",
    boxShadow: "0 4px 24px rgba(67,24,255,0.07)",
    display: "flex", alignItems: "center", gap: 16, flex: 1, minWidth: 160,
  }}>
    <div style={{
      width: 48, height: 48, borderRadius: 12,
      background: `${color}1A`, display: "flex", alignItems: "center", justifyContent: "center",
    }}>
      <span style={{ color }}>{icon}</span>
    </div>
    <div>
      <div style={{ fontSize: 22, fontWeight: 800, color: "#1B254B" }}>{value}</div>
      <div style={{ fontSize: 13, color: "#A3AED0" }}>{label}</div>
    </div>
  </div>
);

const ChartCard: React.FC<{ title: string; subtitle?: string; children: React.ReactNode }> = (
  { title, subtitle, children }
) => (
  <div style={{
    background: "white", borderRadius: 20, padding: 24,
    boxShadow: "0 4px 24px rgba(67,24,255,0.07)",
  }}>
    <div style={{ marginBottom: 20 }}>
      <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: "#1B254B" }}>{title}</h3>
      {subtitle && <p style={{ margin: "4px 0 0", fontSize: 13, color: "#A3AED0" }}>{subtitle}</p>}
    </div>
    {children}
  </div>
);

const SectionDivider: React.FC<{ title: string; subtitle: string; color?: string }> = (
  { title, subtitle, color = "#868CFF" }
) => (
  <div style={{ display: "flex", alignItems: "center", gap: 14, margin: "32px 0 16px" }}>
    <div style={{ width: 4, height: 36, borderRadius: 4, background: color }} />
    <div>
      <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "#1B254B" }}>{title}</h2>
      <p style={{ margin: 0, fontSize: 13, color: "#A3AED0" }}>{subtitle}</p>
    </div>
  </div>
);

const renderPieLabel = ({ name, percent }: { name?: string; percent?: number }) =>
  name && percent !== undefined ? `${name} (${(percent * 100).toFixed(0)}%)` : "";

// ─── Main Component ──────────────────────────────────────────────
const AdminAnalytics: React.FC = () => {
  const [userId, setUserId] = useState<number | null>(null);

  // Selector state
  const [adminYear, setAdminYear]           = useState(CURRENT_YEAR);
  const [inputAdminYearRaw, setInputAdminYearRaw]   = useState(String(CURRENT_YEAR));
  const [adminYearErr, setAdminYearErr]     = useState<string | null>(null);
  const [deptYear, setDeptYear]             = useState(CURRENT_YEAR);
  const [deptMonth, setDeptMonth]           = useState(new Date().getMonth() + 1);

  // Employee data
  const [perfData, setPerfData]         = useState<EmployeePerformanceDTO[]>([]);
  const [distData, setDistData]         = useState<CategoryCountDTO[]>([]);
  const [leaderboard, setLeaderboard]   = useState<LeaderboardDTO[]>([]);

  // Admin data
  const [approvalMetrics, setApprovalMetrics]     = useState<ProjectApprovalMetricsDTO[]>([]);
  const [approvedDist, setApprovedDist]           = useState<CategoryCountDTO[]>([]);
  const [deptStats, setDeptStats]                 = useState<DepartmentMetricsDTO[]>([]);
  const [totalIdeasSubmitted, setTotalIdeasSubmitted] = useState<number | null>(null);

  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState<string | null>(null);

  // Load profile
  useEffect(() => {
    fetchMyProfile()
      .then((p) => setUserId(p.userId))
      .catch(() => setError("Failed to load user profile."));
  }, []);

  // Fetch everything once userId ready
  useEffect(() => {
    if (!userId) return;
    setLoading(true);
    setError(null);

    Promise.all([
      // Employee
      getPerformanceEmployee(userId),
      getIdeaDistribution(userId),
      getLeaderboard(userId),
      // Admin
      getProjectApprovalMetrics(adminYear),
      getApprovedIdeaDistribution(adminYear),
      getDepartmentStatistics(deptYear, deptMonth),
      getTotalIdeasSubmitted(adminYear),
    ])
      .then(([perf, dist, lb, appMetrics, appDist, deptStat, totalIdeas]) => {
        setPerfData(perf.data);
        setDistData(dist.data);
        setLeaderboard(lb.data);
        setApprovalMetrics(appMetrics.data);
        setApprovedDist(appDist.data);
        setDeptStats(deptStat.data);
        setTotalIdeasSubmitted(totalIdeas.data);
      })
      .catch(() => setError("Failed to load analytics data. Please try again."))
      .finally(() => setLoading(false));
  }, [userId]); // eslint-disable-line react-hooks/exhaustive-deps

  const refreshAdminYearData = () => {
    Promise.all([
      getProjectApprovalMetrics(adminYear),
      getApprovedIdeaDistribution(adminYear),
      getTotalIdeasSubmitted(adminYear),
    ]).then(([m, d, total]) => {
      setApprovalMetrics(m.data);
      setApprovedDist(d.data);
      setTotalIdeasSubmitted(total.data);
    }).catch(console.error);
  };
  const refreshDeptStats = () => {
    getDepartmentStatistics(deptYear, deptMonth)
      .then((r) => setDeptStats(r.data))
      .catch(console.error);
  };

  // Summary stats
  const totalIdeas    = perfData.reduce((s, d) => s + d.count, 0);
  const myRank        = leaderboard.find((l) => l.userId === userId)?.rank ?? "–";
  const totalApproved = approvalMetrics.reduce((s, d) => s + d.totalApprovedIdeaCount, 0);
  const topTen        = leaderboard.slice(0, 10);
  const myEntry        = leaderboard.find((l) => l.userId === userId);
  const iAmOutside     = myEntry !== undefined && myEntry.rank > 10;

  const MONTHS = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];

  function validateYear(val: string): string | null {
    const n = Number(val);
    if (!val || isNaN(n) || !Number.isInteger(n)) return "Year must be a whole number.";
    if (n < 2020)             return "Year must be 2020 or later.";
    if (n > CURRENT_YEAR)    return `Year cannot be after ${CURRENT_YEAR}.`;
    return null;
  }
  function onAdminYearChange(val: string) {
    setInputAdminYearRaw(val);
    setAdminYearErr(validateYear(val));
  }
  function applyAdminYear() {
    const err = validateYear(inputAdminYearRaw);
    setAdminYearErr(err);
    if (err) return;
    setAdminYear(Number(inputAdminYearRaw));
    refreshAdminYearData();
  }

  if (loading) {
    return (
      <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: 400 }}>
        <RefreshCw size={32} color={PRIMARY} style={{ animation: "spin 1s linear infinite" }} />
        <span style={{ marginLeft: 12, color: "#A3AED0", fontSize: 16 }}>Loading analytics…</span>
      </div>
    );
  }
  if (error) {
    return (
      <div style={{ textAlign: "center", padding: 60 }}>
        <p style={{ color: DANGER, fontSize: 16 }}>{error}</p>
        <button onClick={() => window.location.reload()}
          style={{ marginTop: 12, padding: "10px 24px", background: PRIMARY, color: "white", border: "none", borderRadius: 10, cursor: "pointer", fontWeight: 600 }}>
          Retry
        </button>
      </div>
    );
  }

  return (
    <div style={{ padding: "0 0 40px" }}>
      {/* ── Hero ── */}
      <div style={{
        background: "linear-gradient(135deg, #1B254B 0%, #4318FF 60%, #21C6DE 100%)",
        borderRadius: 20, padding: "36px 40px", marginBottom: 28, color: "white",
      }}>
        <h1 style={{ margin: 0, fontSize: 28, fontWeight: 800 }}>Platform Analytics</h1>
        <p style={{ margin: "8px 0 0", opacity: 0.75, fontSize: 15 }}>
          Organisation-wide overview — your engagement as an employee plus platform-wide metrics
        </p>
      </div>

      {/* Platform-wide total ideas banner */}
      {totalIdeasSubmitted !== null && (
        <div style={{
          background: "linear-gradient(135deg, #4318FF 0%, #21C6DE 100%)",
          borderRadius: 16, padding: "20px 28px", marginBottom: 24,
          display: "flex", alignItems: "center", gap: 20, color: "white",
          boxShadow: "0 4px 24px rgba(67,24,255,0.18)",
        }}>
          <div style={{
            width: 52, height: 52, borderRadius: 14, background: "rgba(255,255,255,0.18)",
            display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0,
          }}>
            <Lightbulb size={26} color="white" />
          </div>
          <div>
            <div style={{ fontSize: 28, fontWeight: 800, lineHeight: 1 }}>{totalIdeasSubmitted.toLocaleString()}</div>
            <div style={{ fontSize: 14, opacity: 0.85, marginTop: 4 }}>
              Total ideas submitted platform-wide in {adminYear} — keep the innovation going!
            </div>
          </div>
        </div>
      )}

      {/* ═══════════════════════════════════════════════════════
          SECTION 1: Employee Analytics
      ═══════════════════════════════════════════════════════ */}
      <SectionDivider title="Employee Engagement" subtitle="Personal idea submission and participation stats" color={PRIMARY} />

      <div style={{ display: "flex", gap: 16, flexWrap: "wrap", marginBottom: 24 }}>
        <StatCard icon={<Lightbulb size={22} />}    label="Ideas Submitted"  value={totalIdeas}    color={PRIMARY} />
        <StatCard icon={<Trophy size={22} />}        label="Leaderboard Rank" value={`#${myRank}`}  color={WARN}    />
      </div>

      {/* Unified Engagement Chart */}
      <div style={{ marginBottom: 20 }}>
        {userId !== null && <EngagementChart userId={userId} />}
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20, marginBottom: 20 }}>
        <ChartCard title="Idea Submission Performance" subtitle="Monthly submissions (all time)">
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={perfData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F4F7FE" />
              <XAxis dataKey="month" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" name="Ideas Submitted" radius={[6, 6, 0, 0]}>
                {perfData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="My Idea Category Distribution" subtitle="Your ideas by category">
          {distData.length === 0
            ? <div style={{ textAlign: "center", padding: 50, color: "#A3AED0" }}>No ideas submitted yet.</div>
            : (
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie data={distData} dataKey="ideaCount" nameKey="categoryName"
                    cx="50%" cy="50%" outerRadius={80} label={renderPieLabel} labelLine>
                    {distData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            )}
        </ChartCard>
      </div>

      {/* ═══════════════════════════════════════════════════════
          SECTION 2: Platform Overview (Admin)
      ═══════════════════════════════════════════════════════ */}
      <SectionDivider title="Platform Overview" subtitle="Organisation-wide approval metrics and department engagement" color={ACCENT} />

      <div style={{ display: "flex", gap: 16, flexWrap: "wrap", marginBottom: 24 }}>
        <StatCard icon={<BarChart2 size={22} />}    label="Total Approved (Year)" value={totalApproved} color={PRIMARY} />
        <StatCard icon={<PieIcon size={22} />}      label="Approved Categories"   value={approvedDist.length} color={ACCENT}   />
        <StatCard icon={<Building2 size={22} />}    label="Active Departments"    value={deptStats.length}    color={SUCCESS}  />
      </div>

      {/* Year picker for admin charts */}
      <div style={{ display: "flex", gap: 12, alignItems: "flex-start", marginBottom: 20, flexWrap: "wrap" }}>
        <div style={{ display: "flex", flexDirection: "column" }}>
          <label style={{ fontSize: 12, color: "#A3AED0", marginBottom: 4, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.04em" }}>Admin charts year</label>
          <input type="number" value={inputAdminYearRaw} min={2020} max={CURRENT_YEAR}
            onChange={(e) => onAdminYearChange(e.target.value)}
            style={{ ...inputStyle, border: adminYearErr ? "1.5px solid #FF6B6B" : "1.5px solid #E2E8F0" }} />
          {adminYearErr && <span style={{ fontSize: 11, color: "#FF6B6B", marginTop: 3 }}>{adminYearErr}</span>}
        </div>
        <button onClick={applyAdminYear} disabled={!!adminYearErr}
          style={{ ...btnSmall, marginTop: 20, opacity: adminYearErr ? 0.5 : 1, cursor: adminYearErr ? "not-allowed" : "pointer" }}>Apply</button>
      </div>

      {/* Project Approval Metrics + Approved Distribution */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20, marginBottom: 20 }}>
        <ChartCard title="Project Approval Metrics" subtitle={`Accepted vs Approved monthly – ${adminYear}`}>
          <ResponsiveContainer width="100%" height={260}>
            <LineChart data={approvalMetrics}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F4F7FE" />
              <XAxis dataKey="month" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip /><Legend />
              <Line type="monotone" dataKey="totalAcceptedIdeaCount" name="Accepted" stroke={ACCENT}  strokeWidth={2} dot={{ r: 4 }} />
              <Line type="monotone" dataKey="totalApprovedIdeaCount" name="Approved" stroke={SUCCESS} strokeWidth={2} dot={{ r: 4 }} />
            </LineChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Approved Idea Distribution" subtitle={`Category breakdown of approved ideas – ${adminYear}`}>
          {approvedDist.length === 0
            ? <div style={{ textAlign: "center", padding: 50, color: "#A3AED0" }}>No approved ideas data.</div>
            : (
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie data={approvedDist} dataKey="ideaCount" nameKey="categoryName"
                    cx="50%" cy="50%" outerRadius={90} label={renderPieLabel} labelLine>
                    {approvedDist.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            )}
        </ChartCard>
      </div>

      {/* Department Statistics */}
      <ChartCard
        title="Department Statistics"
        subtitle={`Ideas, votes and comments per department – ${MONTHS[deptMonth - 1]} ${deptYear}`}
      >
        <div style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 20 }}>
          <label style={{ fontSize: 13, color: "#A3AED0" }}>Year</label>
          <input type="number" value={deptYear} min={2020} max={CURRENT_YEAR}
            onChange={(e) => setDeptYear(Number(e.target.value))} style={inputStyle} />
          <label style={{ fontSize: 13, color: "#A3AED0" }}>Month</label>
          <select value={deptMonth} onChange={(e) => setDeptMonth(Number(e.target.value))}
            style={{ ...inputStyle, width: 90 }}>
            {MONTHS.map((m, i) => <option key={m} value={i + 1}>{m}</option>)}
          </select>
          <button onClick={refreshDeptStats} style={btnSmall}>Apply</button>
        </div>

        {deptStats.length === 0
          ? <div style={{ textAlign: "center", padding: 40, color: "#A3AED0" }}>No department data for this period.</div>
          : (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={deptStats} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="#F4F7FE" />
                <XAxis type="number" tick={{ fontSize: 11 }} />
                <YAxis dataKey="deptName" type="category" tick={{ fontSize: 12 }} width={120} />
                <Tooltip /><Legend />
                <Bar dataKey="ideaCount"    name="Ideas"    fill={PRIMARY}  radius={[0, 4, 4, 0]} />
                <Bar dataKey="voteCount"    name="Votes"    fill={SUCCESS}  radius={[0, 4, 4, 0]} />
                <Bar dataKey="commentCount" name="Comments" fill={ACCENT}   radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
      </ChartCard>

      {/* ═══════════════════════════════════════════════════════
          SECTION 3: Leaderboard
      ═══════════════════════════════════════════════════════ */}
      <div style={{ marginTop: 24 }}>
        <ChartCard title="Leaderboard" subtitle="Top 10 contributors ranked by XP">
          {leaderboard.length === 0
            ? <div style={{ textAlign: "center", padding: 40, color: "#A3AED0" }}>No leaderboard data available.</div>
            : (
              <div style={{ overflowX: "auto" }}>
                <table style={{ width: "100%", borderCollapse: "separate", borderSpacing: "0 6px" }}>
                  <thead>
                    <tr>
                      {["Rank", "Name", "Role", "XP"].map((h) => (
                        <th key={h} style={{
                          padding: "10px 16px", textAlign: "left",
                          fontSize: 11, fontWeight: 700, color: "#A3AED0",
                          textTransform: "uppercase", letterSpacing: "0.06em",
                        }}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {topTen.map((entry) => {
                      const isMe = entry.userId === userId;
                      return (
                        <tr key={entry.userId} style={{ background: isMe ? "#EFF3FF" : "transparent" }}>
                          <td style={tdStyle}>
                            <span style={{
                              width: 28, height: 28, borderRadius: "50%",
                              background: entry.rank === 1 ? WARN
                                        : entry.rank === 2 ? "#C0C0C0"
                                        : entry.rank === 3 ? "#CD7F32"
                                        : "#F4F7FE",
                              color: entry.rank <= 3 ? "#1B254B" : "#A3AED0",
                              display: "inline-flex", alignItems: "center", justifyContent: "center",
                              fontWeight: 700, fontSize: 13,
                            }}>{entry.rank}</span>
                          </td>
                          <td style={tdStyle}>
                            <span style={{ fontWeight: isMe ? 700 : 500, color: isMe ? PRIMARY : "#1B254B" }}>
                              {entry.userName} {isMe && <span style={{ fontSize: 11, color: PRIMARY }}>(You)</span>}
                            </span>
                          </td>
                          <td style={tdStyle}>
                            <span style={{ padding: "2px 10px", borderRadius: 20, fontSize: 11, fontWeight: 600, background: "#F4F7FE", color: "#A3AED0" }}>
                              {entry.role}
                            </span>
                          </td>
                          <td style={{ ...tdStyle, fontWeight: 700, color: PRIMARY }}>{entry.xp} XP</td>
                        </tr>
                      );
                    })}
                    {iAmOutside && myEntry && (
                      <>
                        <tr>
                          <td colSpan={4} style={{ padding: "4px 16px", textAlign: "center", color: "#A3AED0", fontSize: 18, letterSpacing: 4 }}>
                            · · ·
                          </td>
                        </tr>
                        <tr style={{ background: "#EFF3FF", outline: `2px solid ${PRIMARY}`, outlineOffset: -2, borderRadius: 10 }}>
                          <td style={tdStyle}>
                            <span style={{
                              width: 28, height: 28, borderRadius: "50%",
                              background: "#E0E7FF", color: PRIMARY,
                              display: "inline-flex", alignItems: "center", justifyContent: "center",
                              fontWeight: 700, fontSize: 13,
                            }}>
                              {myEntry.rank}
                            </span>
                          </td>
                          <td style={tdStyle}>
                            <span style={{ fontWeight: 700, color: PRIMARY }}>
                              {myEntry.userName} <span style={{ fontSize: 11, color: PRIMARY }}>(You)</span>
                            </span>
                          </td>
                          <td style={tdStyle}>
                            <span style={{ padding: "2px 10px", borderRadius: 20, fontSize: 11, fontWeight: 600, background: "#F4F7FE", color: "#A3AED0" }}>
                              {myEntry.role}
                            </span>
                          </td>
                          <td style={{ ...tdStyle, fontWeight: 700, color: PRIMARY }}>{myEntry.xp} XP</td>
                        </tr>
                      </>
                    )}
                  </tbody>
                </table>
              </div>
            )}
        </ChartCard>
      </div>

      <style>{`
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
};

export default AdminAnalytics;
