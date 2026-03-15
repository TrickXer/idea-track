// src/pages/analytics/EmployeeAnalytics.tsx
// Employee Analytics Dashboard – shows personal engagement, performance,
// idea distribution and leaderboard.
import React, { useEffect, useState } from "react";
import {
  BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from "recharts";
import { Lightbulb, Trophy, RefreshCw } from "lucide-react";
import { fetchMyProfile } from "../../utils/profileApi";
import EngagementChart from "../../components/analytics/EngagementChart";
import {
  getPerformanceEmployee,
  getIdeaDistribution,
  getLeaderboard,
  getTotalIdeasSubmitted,
  type EmployeePerformanceDTO,
  type CategoryCountDTO,
  type LeaderboardDTO,
} from "../../utils/analyticsApi";

// ─── Colour palette ──────────────────────────────────────────────
const PRIMARY = "#4318FF";
const SECONDARY = "#868CFF";
const ACCENT = "#21C6DE";
const WARN = "#FFCE20";
const SUCCESS = "#05CD99";

const PIE_COLORS = [PRIMARY, SECONDARY, ACCENT, WARN, SUCCESS, "#FF6B6B", "#FF9F43", "#A29BFE"];
const CURRENT_YEAR = new Date().getFullYear();

// ─── Small stat card ─────────────────────────────────────────────
interface StatCardProps {
  icon: React.ReactNode;
  label: string;
  value: number | string;
  color: string;
}
const StatCard: React.FC<StatCardProps> = ({ icon, label, value, color }) => (
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

// ─── Chart card wrapper ──────────────────────────────────────────
const ChartCard: React.FC<{ title: string; subtitle?: string; children: React.ReactNode }> = ({
  title, subtitle, children,
}) => (
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

const tdStyle: React.CSSProperties = { padding: "10px 16px", fontSize: 14, color: "#1B254B" };

// ─── Main Component ──────────────────────────────────────────────
const EmployeeAnalytics: React.FC = () => {
  const [userId, setUserId] = useState<number | null>(null);

  const [perfData, setPerfData]       = useState<EmployeePerformanceDTO[]>([]);
  const [distData, setDistData]       = useState<CategoryCountDTO[]>([]);
  const [leaderboard, setLeaderboard] = useState<LeaderboardDTO[]>([]);
  const [totalIdeasSubmitted, setTotalIdeasSubmitted] = useState<number | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // ── Load user profile first, then fetch analytics ──────────────
  useEffect(() => {
    fetchMyProfile()
      .then((profile) => setUserId(profile.userId))
      .catch(() => setError("Failed to load user profile."));
  }, []);

  // ── Fetch all employee analytics once userId is known ──────────
  useEffect(() => {
    if (!userId) return;
    setLoading(true);
    setError(null);

    Promise.all([
      getPerformanceEmployee(userId),
      getIdeaDistribution(userId),
      getLeaderboard(userId),
      getTotalIdeasSubmitted(CURRENT_YEAR),
    ])
      .then(([perf, dist, lb, totalIdeas]) => {
        setPerfData(perf.data);
        setDistData(dist.data);
        setLeaderboard(lb.data);
        setTotalIdeasSubmitted(totalIdeas.data);
      })
      .catch(() => setError("Failed to load analytics data. Please try again."))
      .finally(() => setLoading(false));
  }, [userId]);

  // ── Summary stats derived from fetched data ────────────────────
  const totalIdeas = perfData.reduce((s, d) => s + d.count, 0);
  const myRank = leaderboard.find((l) => l.userId === userId)?.rank ?? "–";

  // ── Leaderboard top 10 + current user row if outside top 10 ───
  const topTen   = leaderboard.slice(0, 10);
  const myEntry  = leaderboard.find((l) => l.userId === userId);
  const iAmOutside = myEntry !== undefined && myEntry.rank > 10;

  // ── Custom pie label ───────────────────────────────────────────────
  const renderPieLabel = ({ name, percent }: { name?: string; percent?: number }) =>
    name && percent !== undefined ? `${name} (${(percent * 100).toFixed(0)}%)` : "";

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
        <p style={{ color: "#FF6B6B", fontSize: 16 }}>{error}</p>
        <button
          onClick={() => window.location.reload()}
          style={{
            marginTop: 12, padding: "10px 24px", background: PRIMARY, color: "white",
            border: "none", borderRadius: 10, cursor: "pointer", fontWeight: 600,
          }}
        >Retry</button>
      </div>
    );
  }

  return (
    <div style={{ padding: "0 0 40px" }}>
      {/* ── Hero ── */}
      <div style={{
        background: "linear-gradient(135deg, #1B254B 0%, #4318FF 100%)",
        borderRadius: 20, padding: "36px 40px", marginBottom: 28, color: "white",
      }}>
        <h1 style={{ margin: 0, fontSize: 28, fontWeight: 800 }}>My Analytics</h1>
        <p style={{ margin: "8px 0 0", opacity: 0.75, fontSize: 15 }}>
          Personal engagement, idea performance and leaderboard standings
        </p>
      </div>

      {/* ── Platform-wide ideas banner ── */}
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
              Total ideas submitted platform-wide in {CURRENT_YEAR} — keep the innovation going!
            </div>
          </div>
        </div>
      )}

      {/* ── Stat Cards ── */}
      <div style={{ display: "flex", gap: 16, flexWrap: "wrap", marginBottom: 28 }}>
        <StatCard icon={<Lightbulb size={22} />}   label="Total Ideas Submitted" value={totalIdeas}   color={PRIMARY} />
        <StatCard icon={<Trophy size={22} />}       label="Leaderboard Rank"       value={`#${myRank}`} color={WARN}    />
      </div>

      {/* ── Unified Engagement Chart ── */}
      <div style={{ marginBottom: 20 }}>
        {userId !== null && <EngagementChart userId={userId} />}
      </div>

      {/* ── Row 2: Monthly Performance (Bar) + Idea Distribution (Pie) ── */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20, marginBottom: 20 }}>
        {/* Monthly Performance */}
        <ChartCard
          title="Idea Submission Performance"
          subtitle="Number of ideas submitted each month (all time)"
        >
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={perfData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F4F7FE" />
              <XAxis dataKey="month" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" name="Ideas Submitted" fill={SECONDARY} radius={[6, 6, 0, 0]}>
                {perfData.map((_, i) => (
                  <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        {/* Idea Distribution Pie */}
        <ChartCard
          title="Idea Category Distribution"
          subtitle="Breakdown of your ideas by category"
        >
          {distData.length === 0 ? (
            <div style={{ textAlign: "center", padding: 60, color: "#A3AED0" }}>No ideas submitted yet.</div>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie
                  data={distData}
                  dataKey="ideaCount"
                  nameKey="categoryName"
                  cx="50%"
                  cy="50%"
                  outerRadius={100}
                  label={renderPieLabel}
                  labelLine
                >
                  {distData.map((_, i) => (
                    <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          )}
        </ChartCard>
      </div>

      {/* ── Row 3: Leaderboard ── */}
      <ChartCard
        title="Leaderboard"
        subtitle="Top contributors ranked by XP – your position is highlighted"
      >
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
                    <tr key={entry.userId} style={{
                      background: isMe ? "#EFF3FF" : "transparent",
                      borderRadius: 10,
                    }}>
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
                        }}>
                          {entry.rank}
                        </span>
                      </td>
                      <td style={tdStyle}>
                        <span style={{ fontWeight: isMe ? 700 : 500, color: isMe ? PRIMARY : "#1B254B" }}>
                          {entry.userName} {isMe && <span style={{ fontSize: 11, color: PRIMARY }}>(You)</span>}
                        </span>
                      </td>
                      <td style={tdStyle}>
                        <span style={{
                          padding: "2px 10px", borderRadius: 20,
                          fontSize: 11, fontWeight: 600,
                          background: "#F4F7FE", color: "#A3AED0",
                        }}>
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

      <style>{`
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
};

export default EmployeeAnalytics;
