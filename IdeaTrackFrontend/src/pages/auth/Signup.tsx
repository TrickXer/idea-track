// src/pages/Signup.tsx
import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import restApi from "../../utils/restApi";
import "../../components/auth/auth.css";
import { Lightbulb, Sparkles, Handshake, Trophy } from "lucide-react";

type RoleOption = "ADMIN" | "REVIEWER" | "EMPLOYEE";

const Signup: React.FC = () => {
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [deptName, setDeptName] = useState("");
  const [role, setRole] = useState<RoleOption | "">("");
  const [departments, setDepartments] = useState<string[]>([]);
  const [msg, setMsg] = useState("");
  const [err, setErr] = useState("");

  useEffect(() => {
    restApi
      .get<{ deptNames: string[] }>("/api/profile/departments")
      .then((r) => setDepartments(r.data.deptNames ?? []))
      .catch(() => {});
  }, []);

  const submit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErr("");
    setMsg("");
    if (!role) { setErr("Please select a role"); return; }
    try {
      const res = await restApi.post<string>("/api/auth/signup", { name, email, password, deptName, role });
      setMsg(res.data);
      setTimeout(() => navigate("/login"), 1500);
    } catch (e: any) {
      setErr(e?.response?.data?.message ?? "Signup failed");
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
            Join your team's innovation hub. Share your ideas, collaborate on proposals, and help shape the future.
          </p>
          <ul className="auth-feature-list">
            <li>
              <span className="auth-feature-icon"><Sparkles size={18} /></span>
              Share ideas with your entire organisation
            </li>
            <li>
              <span className="auth-feature-icon"><Handshake size={18} /></span>
              Vote and collaborate on proposals
            </li>
            <li>
              <span className="auth-feature-icon"><Trophy size={18} /></span>
              Get recognition for impactful ideas
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

          <h2 className="auth-title">Create account</h2>
          <p className="auth-subtitle">Join IdeaTrack and start sharing ideas</p>

          <form onSubmit={submit}>
            <label className="auth-label">Full name</label>
            <input className="auth-input" placeholder="Your full name" value={name} onChange={(e) => setName(e.target.value)} required />

            <label className="auth-label">Email address</label>
            <input className="auth-input" type="email" placeholder="you@company.com" value={email} onChange={(e) => setEmail(e.target.value)} required />

            <label className="auth-label">Password</label>
            <input className="auth-input" type="password" placeholder="Create a password" value={password} onChange={(e) => setPassword(e.target.value)} required />

            <label className="auth-label">Role</label>
            <select className="auth-input" value={role} onChange={(e) => setRole(e.target.value as RoleOption | "")} required>
              <option value="">— Select your role —</option>
              <option value="EMPLOYEE">Employee</option>
              <option value="REVIEWER">Reviewer</option>
            </select>

            <label className="auth-label">Department</label>
            <select className="auth-input" value={deptName} onChange={(e) => setDeptName(e.target.value)}>
              <option value="">— Select department —</option>
              {departments.map((d) => (<option key={d} value={d}>{d}</option>))}
            </select>

            {msg && <div style={{ color: "#22c55e", background: "#f0fdf4", borderRadius: 10, padding: "10px 14px", fontSize: 13, marginBottom: 12 }}>{msg}</div>}
            {err && <div className="auth-error">{err}</div>}

            <button className="auth-btn" type="submit">Create account →</button>
          </form>

          <div className="auth-link-row">
            Already have an account? <Link to="/login">Sign in</Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Signup;
