// src/pages/analytics/ReviewerAnalytics.tsx
// Reviewer Analytics Dashboard – Employee graphs + additional Reviewer-specific
// charts: performance, acceptance count, decision breakdown.
import React, { useEffect, useState } from "react";
import {
  LineChart, Line, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
  RadarChart, PolarGrid, PolarAngleAxis, Radar,
} from "recharts";
import {
  TrendingUp, Lightbulb, ThumbsUp,
  Trophy, RefreshCw, ClipboardCheck, CheckCircle,
} from "lucide-react";
import { fetchMyProfile } from "../../utils/profileApi";
import EngagementChart from "../../components/analytics/EngagementChart";
import {
  getPerformanceEmployee,
  getIdeaDistribution,
  getLeaderboard,
  getPerformanceReviewer,
  getReviewerAcceptanceCount,
  getReviewerDecisionBreakdown,
  getTotalIdeasSubmitted,
  type EmployeePerformanceDTO,
  type CategoryCountDTO,
  type LeaderboardDTO,
  type ReviewerPerformanceDTO,
  type AcceptApproveCountDTO,
  type DecisionBreakdownDTO,
} from "../../utils/analyticsApi";

// ─── Colour palette ──────────────────────────────────────────────
const PRIMARY   = "#4318FF";
const SECONDARY = "#868CFF";
const ACCENT    = "#21C6DE";
const WARN      = "#FFCE20";
const SUCCESS   = "#05CD99";
const DANGER    = "#FF6B6B";
const PIE_COLORS = [PRIMARY, SECONDARY, ACCENT, WARN, SUCCESS, DANGER, "#FF9F43", "#A29BFE"];
const CURRENT_YEAR = new Date().getFullYear();

// ─── Shared tiny styles ──────────────────────────────────────────
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

const SectionDivider: React.FC<{ title: string; subtitle: string; color?: string }> = (
  { title, subtitle, color = "#868CFF" }
) => (
  <div style={{
    display: "flex", alignItems: "center", gap: 14, margin: "32px 0 16px",
  }}>
    <div style={{ width: 4, height: 36, borderRadius: 4, background: color }} />
    <div>
      <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "#1B254B" }}>{title}</h2>
      <p style={{ margin: 0, fontSize: 13, color: "#A3AED0" }}>{subtitle}</p>
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

const renderPieLabel = ({ name, percent }: { name?: string; percent?: number }) =>
  name && percent !== undefined ? `${name} (${(percent * 100).toFixed(0)}%)` : "";// ─── Main Component ──────────────────────────────────────────────
const ReviewerAnalytics: React.FC = () => {
  const [userId, setUserId] = useState<number | null>(null);

  const [reviewYear, setReviewYear] = useState(CURRENT_YEAR);
  const [inputYearRaw, setInputYearRaw] = useState(String(CURRENT_YEAR));
  const [yearErr, setYearErr]           = useState<string | null>(null);

  // Employee data
  const [perfData, setPerfData]       = useState<EmployeePerformanceDTO[]>([]);
  const [distData, setDistData]       = useState<CategoryCountDTO[]>([]);
  const [leaderboard, setLeaderboard] = useState<LeaderboardDTO[]>([]);

  // Reviewer data
  const [reviewerPerf, setReviewerPerf]           = useState<ReviewerPerformanceDTO[]>([]);
  const [acceptanceData, setAcceptanceData]       = useState<AcceptApproveCountDTO[]>([]);
  const [decisionBreakdown, setDecisionBreakdown] = useState<DecisionBreakdownDTO[]>([]);
  const [totalIdeasSubmitted, setTotalIdeasSubmitted] = useState<number | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);

  // Load profile
  useEffect(() => {
    fetchMyProfile()
      .then((p) => setUserId(p.userId))
      .catch(() => setError("Failed to load user profile."));
  }, []);

  // Fetch all data once userId ready
  useEffect(() => {
    if (!userId) return;
    setLoading(true);
    setError(null);

    Promise.all([
      getPerformanceEmployee(userId),
      getIdeaDistribution(userId),
      getLeaderboard(userId),
      getPerformanceReviewer(userId, reviewYear),
      getReviewerAcceptanceCount(userId, reviewYear),
      getReviewerDecisionBreakdown(userId, reviewYear),
      getTotalIdeasSubmitted(reviewYear),
    ])
      .then(([perf, dist, lb, rvPerf, rvAcc, rvDec, totalIdeas]) => {
        setPerfData(perf.data);
        setDistData(dist.data);
        setLeaderboard(lb.data);
        setReviewerPerf(rvPerf.data);
        setAcceptanceData(rvAcc.data);
        setDecisionBreakdown(rvDec.data);
        setTotalIdeasSubmitted(totalIdeas.data);
      })
      .catch(() => setError("Failed to load analytics data. Please try again."))
      .finally(() => setLoading(false));
  }, [userId, reviewYear]);

  const refreshReviewerData = () => {
    if (!userId) return;
    Promise.all([
      getPerformanceReviewer(userId, reviewYear),
      getReviewerAcceptanceCount(userId, reviewYear),
      getReviewerDecisionBreakdown(userId, reviewYear),
      getTotalIdeasSubmitted(reviewYear),
    ]).then(([p, a, d, total]) => {
      setReviewerPerf(p.data);
      setAcceptanceData(a.data);
      setDecisionBreakdown(d.data);
      setTotalIdeasSubmitted(total.data);
    }).catch(console.error);
  };

  // Inline year validation helper
  function onYearInputChange(val: string) {
    setInputYearRaw(val);
    const n = Number(val);
    if (!val || isNaN(n) || !Number.isInteger(n))    setYearErr("Year must be a whole number.");
    else if (n < 2020)                               setYearErr("Year must be 2020 or later.");
    else if (n > CURRENT_YEAR)                       setYearErr(`Year cannot be after ${CURRENT_YEAR}.`);
    else                                             setYearErr(null);
  }

  function applyYear() {
    const n = Number(inputYearRaw);
    if (yearErr || !inputYearRaw || isNaN(n)) return;
    setReviewYear(n);
  }

  // Summary stats
  const totalIdeas    = perfData.reduce((s, d) => s + d.count, 0);
  const myRank        = leaderboard.find((l) => l.userId === userId)?.rank ?? "–";
  const totalAssigned = reviewerPerf.reduce((s, d) => s + d.assignedIdeaCount, 0);
  const totalOnTime   = reviewerPerf.reduce((s, d) => s + d.reviewedOnTimeCount, 0);
  const totalAccepted = acceptanceData.reduce((s, d) => s + d.count, 0);
  const topTen        = leaderboard.slice(0, 10);
  const myEntry        = leaderboard.find((l) => l.userId === userId);
  const iAmOutside     = myEntry !== undefined && myEntry.rank > 10;

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
      {/* Hero */}
      <div style={{
        background: "linear-gradient(135deg, #1B254B 0%, #868CFF 100%)",
        borderRadius: 20, padding: "36px 40px", marginBottom: 28, color: "white",
      }}>
        <h1 style={{ margin: 0, fontSize: 28, fontWeight: 800 }}>Reviewer Analytics</h1>
        <p style={{ margin: "8px 0 0", opacity: 0.75, fontSize: 15 }}>
          Your engagement as an employee and your review performance metrics
        </p>
      </div>

      {/* Platform-wide ideas banner */}
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
              Total ideas submitted platform-wide in {reviewYear} — keep the innovation going!
            </div>
          </div>
        </div>
      )}

      {/* ═══════════════════════════════════════
          SECTION 1: Employee Analytics
      ═══════════════════════════════════════ */}
      <SectionDivider title="Employee Engagement" subtitle="Your idea submission and participation stats" color={PRIMARY} />

      {/* Stat Cards – Employee */}
      <div style={{ display: "flex", gap: 16, flexWrap: "wrap", marginBottom: 24 }}>
        <StatCard icon={<Lightbulb size={22} />}     label="Total Ideas Submitted"  value={totalIdeas}       color={PRIMARY}    />
        <StatCard icon={<Trophy size={22} />}         label="Leaderboard Rank"       value={`#${myRank}`}     color={WARN}       />
      </div>

      {/* Unified Engagement Chart */}
      <div style={{ marginBottom: 20 }}>
        {userId !== null && <EngagementChart userId={userId} />}
      </div>

      {/* Performance + Distribution */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20, marginBottom: 20 }}>
        <ChartCard title="Idea Submission Performance" subtitle="Ideas submitted each month (all time)">
          <ResponsiveContainer width="100%" height={260}>
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

        <ChartCard title="Idea Category Distribution" subtitle="Your ideas broken down by category">
          {distData.length === 0 ? (
            <div style={{ textAlign: "center", padding: 60, color: "#A3AED0" }}>No ideas submitted yet.</div>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie data={distData} dataKey="ideaCount" nameKey="categoryName"
                  cx="50%" cy="50%" outerRadius={90} label={renderPieLabel} labelLine>
                  {distData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          )}
        </ChartCard>
      </div>

      {/* ═══════════════════════════════════════
          SECTION 2: Reviewer Analytics
      ═══════════════════════════════════════ */}
      <SectionDivider title="Reviewer Performance" subtitle="Your review metrics, acceptance rate and decision breakdown" color={SECONDARY} />

      {/* Stat Cards – Reviewer */}
      <div style={{ display: "flex", gap: 16, flexWrap: "wrap", marginBottom: 24 }}>
        <StatCard icon={<ClipboardCheck size={22} />} label="Ideas Assigned"        value={totalAssigned} color={PRIMARY}  />
        <StatCard icon={<CheckCircle size={22} />}    label="Reviewed On Time"      value={totalOnTime}   color={SUCCESS}  />
        <StatCard icon={<ThumbsUp size={22} />}       label="Total Accepted"        value={totalAccepted} color={ACCENT}   />
        <StatCard icon={<TrendingUp size={22} />}     label="On-Time Rate"
          value={totalAssigned > 0 ? `${Math.round((totalOnTime / totalAssigned) * 100)}%` : "–"}
          color={WARN}
        />
      </div>

      {/* Year picker for reviewer data */}
      <div style={{ display: "flex", gap: 12, alignItems: "flex-start", marginBottom: 20, flexWrap: "wrap" }}>
        <div style={{ display: "flex", flexDirection: "column" }}>
          <label style={{ fontSize: 12, color: "#A3AED0", marginBottom: 4, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.04em" }}>Reviewer data year</label>
          <input
            type="number"
            value={inputYearRaw}
            min={2020}
            max={CURRENT_YEAR}
            onChange={(e) => onYearInputChange(e.target.value)}
            style={{ ...inputStyle, border: yearErr ? "1.5px solid #FF6B6B" : "1.5px solid #E2E8F0" }}
          />
          {yearErr && <span style={{ fontSize: 11, color: "#FF6B6B", marginTop: 3 }}>{yearErr}</span>}
        </div>
        <button onClick={applyYear} disabled={!!yearErr}
          style={{ ...btnSmall, marginTop: 20, opacity: yearErr ? 0.5 : 1, cursor: yearErr ? "not-allowed" : "pointer" }}>Apply</button>
      </div>

      {/* Reviewer Performance + Acceptance Count */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20, marginBottom: 20 }}>
        <ChartCard title="Review Performance" subtitle={`Assigned vs reviewed on time – ${reviewYear}`}>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={reviewerPerf}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F4F7FE" />
              <XAxis dataKey="month" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Bar dataKey="assignedIdeaCount"    name="Assigned"       fill={SECONDARY} radius={[4, 4, 0, 0]} />
              <Bar dataKey="reviewedOnTimeCount"  name="Reviewed On Time" fill={SUCCESS}  radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Acceptance Count" subtitle={`Ideas accepted monthly – ${reviewYear}`}>
          <ResponsiveContainer width="100%" height={260}>
            <LineChart data={acceptanceData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F4F7FE" />
              <XAxis dataKey="month" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip />
              <Line type="monotone" dataKey="count" name="Accepted" stroke={SUCCESS} strokeWidth={2} dot={{ r: 4 }} />
            </LineChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>

      {/* Decision Breakdown – polar area chart */}
      <ChartCard title="Decision Breakdown" subtitle={`Accepted / Rejected / Reassigned totals – ${reviewYear}`}>
        {decisionBreakdown.length === 0 ? (
          <div style={{ textAlign: "center", padding: 60, color: "#A3AED0" }}>No decision data for this year.</div>
        ) : (() => {
          const totalAccepted2  = decisionBreakdown.reduce((s, d) => s + d.acceptedCount, 0);
          const totalRejected   = decisionBreakdown.reduce((s, d) => s + d.rejectedCount, 0);
          const totalReassigned = decisionBreakdown.reduce((s, d) => s + d.reassignCount, 0);
          const polarData = [
            { subject: "Accepted",   value: totalAccepted2,  fill: SUCCESS },
            { subject: "Rejected",   value: totalRejected,   fill: DANGER  },
            { subject: "Reassigned", value: totalReassigned, fill: WARN    },
          ];
          return (
            <div style={{ display: "flex", alignItems: "center", gap: 32, flexWrap: "wrap" }}>
              <ResponsiveContainer width="100%" height={300} style={{ flex: "1 1 260px", minWidth: 220 }}>
                <RadarChart cx="50%" cy="50%" outerRadius="70%" data={polarData}>
                  <PolarGrid />
                  <PolarAngleAxis dataKey="subject" tick={{ fontSize: 13, fontWeight: 600, fill: "#1B254B" }} />
                  <Radar name="Decisions" dataKey="value" strokeWidth={2}
                    stroke={PRIMARY} fill={PRIMARY} fillOpacity={0.15}>
                    {polarData.map((d, i) => (
                      <Cell key={i} fill={d.fill} stroke={d.fill} />
                    ))}
                  </Radar>
                  <Tooltip formatter={(value: number, name: string) => [value, name]} />
                </RadarChart>
              </ResponsiveContainer>
              <div style={{ display: "flex", flexDirection: "column", gap: 12, minWidth: 160 }}>
                {polarData.map((d) => (
                  <div key={d.subject} style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <div style={{ width: 14, height: 14, borderRadius: "50%", background: d.fill, flexShrink: 0 }} />
                    <span style={{ fontSize: 14, color: "#1B254B", fontWeight: 600 }}>{d.subject}:</span>
                    <span style={{ fontSize: 14, color: d.fill, fontWeight: 700 }}>{d.value}</span>
                  </div>
                ))}
              </div>
            </div>
          );
        })()}
      </ChartCard>

      {/* ═══════════════════════════════════════
          SECTION 3: Leaderboard
      ═══════════════════════════════════════ */}
      <div style={{ marginTop: 24 }}>
        <ChartCard title="Leaderboard" subtitle="Top contributors ranked by XP – your position is highlighted">
          {leaderboard.length === 0 ? (
            <div style={{ textAlign: "center", padding: 40, color: "#A3AED0" }}>No leaderboard data available.</div>
          ) : (
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

export default ReviewerAnalytics;
