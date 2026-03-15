// src/components/analytics/EngagementChart.tsx
// Unified Engagement chart card.
// A dropdown lets the user pick "Monthly" (default) or "Yearly".
// Each mode shows its own validated filter fields; Apply fetches fresh data.
import React, { useEffect, useState } from "react";
import {
  BarChart, Bar, LineChart, Line,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from "recharts";
import {
  getMonthWiseEngagement,
  getYearWiseEngagement,
  type MonthlyEngagementDTO,
  type YearlyEngagementDTO,
} from "../../utils/analyticsApi";

// ─── Palette (matches the rest of the app) ──────────────────────
const PRIMARY  = "#4318FF";
const SUCCESS  = "#05CD99";
const ACCENT   = "#21C6DE";

const CURRENT_YEAR = new Date().getFullYear();
const MIN_YEAR     = 2020;

// ─── Validation helpers ──────────────────────────────────────────
function validateMonthYear(raw: string): string | null {
  const n = Number(raw);
  if (!raw || isNaN(n) || !Number.isInteger(n))  return "Year must be a whole number.";
  if (n < MIN_YEAR)                               return `Year must be ${MIN_YEAR} or later.`;
  if (n > CURRENT_YEAR)                          return `Year cannot be after ${CURRENT_YEAR}.`;
  return null;
}

function validateYearRange(rawStart: string, rawEnd: string): { startErr: string | null; endErr: string | null } {
  const s = Number(rawStart);
  const e = Number(rawEnd);
  let startErr: string | null = null;
  let endErr:   string | null = null;

  if (!rawStart || isNaN(s) || !Number.isInteger(s)) {
    startErr = "Start year must be a whole number.";
  } else if (s < MIN_YEAR) {
    startErr = `Start year must be ${MIN_YEAR} or later.`;
  } else if (s > CURRENT_YEAR) {
    startErr = `Start year cannot be after ${CURRENT_YEAR}.`;
  }

  if (!rawEnd || isNaN(e) || !Number.isInteger(e)) {
    endErr = "End year must be a whole number.";
  } else if (e < MIN_YEAR) {
    endErr = `End year must be ${MIN_YEAR} or later.`;
  } else if (e > CURRENT_YEAR) {
    endErr = `End year cannot be after ${CURRENT_YEAR}.`;
  } else if (!startErr && e <= s) {
    endErr = "End year must be greater than Start year.";
  }

  return { startErr, endErr };
}

// ─── Shared micro-styles ─────────────────────────────────────────
const inputBase: React.CSSProperties = {
  padding: "7px 11px", borderRadius: 9, fontSize: 13,
  color: "#1B254B", outline: "none", width: 88, transition: "border-color 0.15s",
};
const inputOk:  React.CSSProperties = { ...inputBase, border: "1.5px solid #E2E8F0" };
const inputErr: React.CSSProperties = { ...inputBase, border: "1.5px solid #FF6B6B" };
const errMsg: React.CSSProperties  = { fontSize: 11, color: "#FF6B6B", marginTop: 3 };
const applyBtn: React.CSSProperties = {
  padding: "7px 16px", background: PRIMARY, color: "white",
  border: "none", borderRadius: 9, fontSize: 13, cursor: "pointer",
  fontWeight: 600, alignSelf: "flex-start", marginTop: 1,
};
const selectStyle: React.CSSProperties = {
  padding: "7px 12px", border: "1.5px solid #E2E8F0", borderRadius: 9,
  fontSize: 13, color: "#1B254B", background: "white", cursor: "pointer",
  fontWeight: 600, outline: "none",
};

// ─── Props ───────────────────────────────────────────────────────
interface EngagementChartProps {
  userId: number;
}

// ─── Component ───────────────────────────────────────────────────
const EngagementChart: React.FC<EngagementChartProps> = ({ userId }) => {
  type Mode = "monthly" | "yearly";
  const [mode, setMode] = useState<Mode>("monthly");

  // ── Monthly state ────────────────────────────────────────────
  const [monthYearRaw, setMonthYearRaw]   = useState(String(CURRENT_YEAR));
  const [monthYearErr, setMonthYearErr]   = useState<string | null>(null);
  const [monthlyData, setMonthlyData]     = useState<MonthlyEngagementDTO[]>([]);
  const [monthlyLoading, setMonthlyLoading] = useState(false);

  // ── Yearly state ─────────────────────────────────────────────
  const [startYearRaw, setStartYearRaw]   = useState(String(CURRENT_YEAR - 3));
  const [endYearRaw, setEndYearRaw]       = useState(String(CURRENT_YEAR));
  const [startYearErr, setStartYearErr]   = useState<string | null>(null);
  const [endYearErr, setEndYearErr]       = useState<string | null>(null);
  const [yearlyData, setYearlyData]       = useState<YearlyEngagementDTO[]>([]);
  const [yearlyLoading, setYearlyLoading] = useState(false);

  // ── Fetch monthly on mount (default mode) ────────────────────
  useEffect(() => {
    fetchMonthly();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  // ── When mode switches, auto-fetch if data is empty ──────────
  useEffect(() => {
    if (mode === "yearly" && yearlyData.length === 0) fetchYearly();
    if (mode === "monthly" && monthlyData.length === 0) fetchMonthly();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mode]);

  // ── Fetch helpers ────────────────────────────────────────────
  function fetchMonthly() {
    const err = validateMonthYear(monthYearRaw);
    setMonthYearErr(err);
    if (err) return;
    setMonthlyLoading(true);
    getMonthWiseEngagement(userId, Number(monthYearRaw))
      .then((r) => setMonthlyData(r.data))
      .catch(console.error)
      .finally(() => setMonthlyLoading(false));
  }

  function fetchYearly() {
    const { startErr, endErr } = validateYearRange(startYearRaw, endYearRaw);
    setStartYearErr(startErr);
    setEndYearErr(endErr);
    if (startErr || endErr) return;
    setYearlyLoading(true);
    getYearWiseEngagement(userId, Number(startYearRaw), Number(endYearRaw))
      .then((r) => setYearlyData(r.data))
      .catch(console.error)
      .finally(() => setYearlyLoading(false));
  }

  // ── Inline validation on change ──────────────────────────────
  function onMonthYearChange(val: string) {
    setMonthYearRaw(val);
    setMonthYearErr(validateMonthYear(val));
  }

  function onStartYearChange(val: string) {
    setStartYearRaw(val);
    const { startErr, endErr } = validateYearRange(val, endYearRaw);
    setStartYearErr(startErr);
    setEndYearErr(endErr);
  }

  function onEndYearChange(val: string) {
    setEndYearRaw(val);
    const { startErr, endErr } = validateYearRange(startYearRaw, val);
    setStartYearErr(startErr);
    setEndYearErr(endErr);
  }

  const isLoading = mode === "monthly" ? monthlyLoading : yearlyLoading;

  return (
    <div style={{
      background: "white", borderRadius: 20, padding: 24,
      boxShadow: "0 4px 24px rgba(67,24,255,0.07)",
    }}>
      {/* ── Card header: title + mode dropdown ── */}
      <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: 20, flexWrap: "wrap", gap: 12 }}>
        <div>
          <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: "#1B254B" }}>Engagement</h3>
          <p style={{ margin: "4px 0 0", fontSize: 13, color: "#A3AED0" }}>
            {mode === "monthly"
              ? `Ideas, votes & comments by month for ${monthYearRaw}`
              : `Ideas, votes & comments by year (${startYearRaw} – ${endYearRaw})`}
          </p>
        </div>

        {/* Mode selector dropdown */}
        <select
          value={mode}
          onChange={(e) => setMode(e.target.value as Mode)}
          style={selectStyle}
        >
          <option value="monthly">Monthly</option>
          <option value="yearly">Yearly</option>
        </select>
      </div>

      {/* ── Filter row ── */}
      {mode === "monthly" ? (
        <div style={{ display: "flex", alignItems: "flex-start", gap: 14, marginBottom: 20, flexWrap: "wrap" }}>
          <div style={{ display: "flex", flexDirection: "column" }}>
            <label style={{ fontSize: 12, color: "#A3AED0", marginBottom: 4, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.04em" }}>
              Year
            </label>
            <input
              type="number"
              value={monthYearRaw}
              min={MIN_YEAR}
              max={CURRENT_YEAR}
              onChange={(e) => onMonthYearChange(e.target.value)}
              style={monthYearErr ? inputErr : inputOk}
            />
            {monthYearErr && <span style={errMsg}>{monthYearErr}</span>}
          </div>
          <button onClick={fetchMonthly} disabled={!!monthYearErr}
            style={{ ...applyBtn, marginTop: 20, opacity: monthYearErr ? 0.5 : 1, cursor: monthYearErr ? "not-allowed" : "pointer" }}>Apply</button>
        </div>
      ) : (
        <div style={{ display: "flex", alignItems: "flex-start", gap: 14, marginBottom: 20, flexWrap: "wrap" }}>
          <div style={{ display: "flex", flexDirection: "column" }}>
            <label style={{ fontSize: 12, color: "#A3AED0", marginBottom: 4, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.04em" }}>
              Start Year
            </label>
            <input
              type="number"
              value={startYearRaw}
              min={MIN_YEAR}
              max={CURRENT_YEAR}
              onChange={(e) => onStartYearChange(e.target.value)}
              style={startYearErr ? inputErr : inputOk}
            />
            {startYearErr && <span style={errMsg}>{startYearErr}</span>}
          </div>
          <div style={{ display: "flex", flexDirection: "column" }}>
            <label style={{ fontSize: 12, color: "#A3AED0", marginBottom: 4, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.04em" }}>
              End Year
            </label>
            <input
              type="number"
              value={endYearRaw}
              min={MIN_YEAR}
              max={CURRENT_YEAR}
              onChange={(e) => onEndYearChange(e.target.value)}
              style={endYearErr ? inputErr : inputOk}
            />
            {endYearErr && <span style={errMsg}>{endYearErr}</span>}
          </div>
          <button onClick={fetchYearly} disabled={!!(startYearErr || endYearErr)}
            style={{ ...applyBtn, marginTop: 20, opacity: (startYearErr || endYearErr) ? 0.5 : 1, cursor: (startYearErr || endYearErr) ? "not-allowed" : "pointer" }}>Apply</button>
        </div>
      )}

      {/* ── Chart ── */}
      {isLoading ? (
        <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: 260 }}>
          <span style={{ color: "#A3AED0", fontSize: 14 }}>Loading…</span>
        </div>
      ) : mode === "monthly" ? (
        monthlyData.length === 0 ? (
          <div style={{ textAlign: "center", padding: "48px 0", color: "#A3AED0" }}>No data for this period.</div>
        ) : (
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={monthlyData} barCategoryGap="30%">
              <CartesianGrid strokeDasharray="3 3" stroke="#F4F7FE" />
              <XAxis dataKey="month" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Bar dataKey="ideaCount"    name="Ideas"    fill={PRIMARY}  radius={[4, 4, 0, 0]} />
              <Bar dataKey="voteCount"    name="Votes"    fill={SUCCESS}  radius={[4, 4, 0, 0]} />
              <Bar dataKey="commentCount" name="Comments" fill={ACCENT}   radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )
      ) : (
        yearlyData.length === 0 ? (
          <div style={{ textAlign: "center", padding: "48px 0", color: "#A3AED0" }}>No data for this range.</div>
        ) : (
          <ResponsiveContainer width="100%" height={260}>
            <LineChart data={yearlyData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F4F7FE" />
              <XAxis dataKey="year" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="ideaCount"    name="Ideas"    stroke={PRIMARY}  strokeWidth={2} dot={{ r: 4 }} />
              <Line type="monotone" dataKey="voteCount"    name="Votes"    stroke={SUCCESS}  strokeWidth={2} dot={{ r: 4 }} />
              <Line type="monotone" dataKey="commentCount" name="Comments" stroke={ACCENT}   strokeWidth={2} dot={{ r: 4 }} />
            </LineChart>
          </ResponsiveContainer>
        )
      )}
    </div>
  );
};

export default EngagementChart;
