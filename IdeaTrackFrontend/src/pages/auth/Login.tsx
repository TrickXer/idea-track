// src/pages/Login.tsx
import React, { useState } from "react";
import { useAuth, decodeJwt } from "../../utils/authContext";
import { getToken } from "../../utils/storage";
import { Link, useLocation, useNavigate } from "react-router-dom";
import "../../components/auth/auth.css";
import { Lightbulb, Rocket, Search, BarChart2 } from "lucide-react";

const Login: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");

  const submit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setBusy(true);
    setErr("");
    try {
      await login(email, password);

      // Decode roles directly from the freshly saved token (avoids stale closure)
      const freshRoles = (decodeJwt(getToken())?.roles ?? []).map((r) => String(r).toUpperCase());

      // Navigate based on role after successful login
      const from = (location.state as any)?.from?.pathname as string | undefined;
      if (from) {
        navigate(from, { replace: true });
      } else {
        const r = freshRoles[0] ?? "";
        if (r === "ADMIN" || r === "SUPERADMIN") navigate("/admin", { replace: true });
        else if (r === "REVIEWER") navigate("/reviewer/dashboard", { replace: true });
        else navigate("/dashboard", { replace: true });
      }
    } catch (e: any) {
      setErr(e?.response?.data?.message ?? "Login failed");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-page">
      {/* Left branding panel */}
      <div className="auth-panel-left">
        <div className="auth-brand-content">
          <div className="auth-brand-logo"><Lightbulb size={36} /></div>
          <div className="auth-brand-name">IdeaTrack</div>
          <p className="auth-brand-tagline">
            Your organisation's innovation engine. Capture, collaborate, and turn great ideas into real projects.
          </p>
          <ul className="auth-feature-list">
            <li>
              <span className="auth-feature-icon"><Rocket size={18} /></span>
              Submit and track your ideas in real time
            </li>
            <li>
              <span className="auth-feature-icon"><Search size={18} /></span>
              Transparent review and approval workflow
            </li>
            <li>
              <span className="auth-feature-icon"><BarChart2 size={18} /></span>
              Insights and analytics on innovation
            </li>
          </ul>
        </div>
      </div>

      {/* Right form panel */}
      <div className="auth-panel-right">
        <div className="auth-card">
          <div className="auth-logo-small">
            <span className="logo-icon"><Lightbulb size={18} /></span>
            IdeaTrack
          </div>

          <h2 className="auth-title">Welcome back</h2>
          <p className="auth-subtitle">Sign in to your account to continue</p>

          <form onSubmit={submit}>
            <label className="auth-label">Email address</label>
            <input
              className="auth-input"
              type="email"
              placeholder="you@company.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <label className="auth-label">Password</label>
            <input
              className="auth-input"
              type="password"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />

            {err && <div className="auth-error">{err}</div>}

            <button className="auth-btn" type="submit" disabled={busy}>
              {busy ? "Signing in…" : "Sign in →"}
            </button>
          </form>

          <div className="auth-link-row">
            New to IdeaTrack? <Link to="/signup">Create an account</Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
