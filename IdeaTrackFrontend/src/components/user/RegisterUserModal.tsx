// src/components/RegisterUserModal.tsx
import React, { useEffect, useState } from "react";
import type { CreateUserPayload } from "../../utils/types";
import { getDepartments } from "../../utils/adminApi";

type Props = {
  open: boolean;
  onClose: () => void;
  onSubmit: (payload: CreateUserPayload) => Promise<any> | any;
  // Which roles are allowed by this console (e.g., ["REVIEWER","EMPLOYEE"] for Admin; add "ADMIN" for Super Admin)
  allowedRoles: Array<"ADMIN" | "REVIEWER" | "EMPLOYEE">;
};

// ─── Temp-password popup shown after a successful user creation ───
const TempPasswordPopup: React.FC<{
  name: string;
  email: string;
  tempPassword: string;
  onClose: () => void;
}> = ({ name, email, tempPassword, onClose }) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(tempPassword).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  return (
    <div className="modal-overlay show" onClick={onClose}>
      <div
        className="modal-card"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: 420, textAlign: "center" }}
      >
        {/* Icon */}
        <div style={{
          width: 64, height: 64, borderRadius: "50%",
          background: "#EFF3FF", display: "flex", alignItems: "center",
          justifyContent: "center", margin: "0 auto 18px",
        }}>
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none"
            stroke="var(--brand, #4318FF)" strokeWidth="2"
            strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
          </svg>
        </div>

        <h2 style={{ margin: "0 0 6px", fontSize: 20 }}>User Created!</h2>
        <p style={{ color: "var(--muted)", fontSize: 14, margin: "0 0 22px" }}>
          <strong>{name}</strong> ({email}) has been registered.<br />
          Share the temporary password below — it cannot be retrieved later.
        </p>

        {/* Password display */}
        <div style={{
          background: "#F4F7FE", borderRadius: 10,
          padding: "14px 16px", marginBottom: 20,
          display: "flex", alignItems: "center", gap: 10,
        }}>
          <code style={{
            flex: 1, fontSize: 17, fontFamily: "monospace",
            letterSpacing: "0.08em", color: "#1B254B",
            wordBreak: "break-all", textAlign: "left",
          }}>
            {tempPassword}
          </code>
          <button
            onClick={handleCopy}
            title="Copy to clipboard"
            style={{
              flexShrink: 0,
              padding: "7px 14px",
              background: copied ? "#05CD99" : "var(--brand, #4318FF)",
              color: "#fff",
              border: "none",
              borderRadius: 8,
              cursor: "pointer",
              fontSize: 13,
              fontWeight: 600,
              transition: "background 0.2s",
              minWidth: 70,
            }}
          >
            {copied ? "✓ Copied" : "Copy"}
          </button>
        </div>

        <button
          className="btn"
          style={{
            width: "100%",
            background: "var(--brand, #4318FF)",
            color: "#fff",
            padding: "11px 0",
          }}
          onClick={onClose}
        >
          Done
        </button>
      </div>
    </div>
  );
};

const RegisterUserModal: React.FC<Props> = ({ open, onClose, onSubmit, allowedRoles }) => {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [deptName, setDeptName] = useState("");
  const [role, setRole] = useState<"" | "ADMIN" | "REVIEWER" | "EMPLOYEE">("");
  const [departments, setDepartments] = useState<string[]>([]);
  const [loadingDepts, setLoadingDepts] = useState(false);
  const [deptErr, setDeptErr] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Holds the created user info to show the temp-password popup
  const [createdInfo, setCreatedInfo] = useState<{
    name: string;
    email: string;
    tempPassword: string;
  } | null>(null);

  // Load departments when modal opens
  useEffect(() => {
    if (!open) return;
    let active = true;
    (async () => {
      setLoadingDepts(true);
      setDeptErr("");
      try {
        const res = await getDepartments(); // -> { deptNames: string[] }
        if (active) setDepartments(res.deptNames ?? []);
      } catch {
        if (active) {
          setDepartments([]);
          setDeptErr("Failed to load departments");
        }
      } finally {
        active && setLoadingDepts(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [open]);

  const submit = async () => {
    // Basic guard
    if (!name || !email || !role) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const result = await onSubmit({ name, email, deptName: deptName || undefined, role });
      const tempPassword: string = result?.tempPassword ?? result?.data?.tempPassword ?? "";
      const savedName: string = result?.user?.name ?? result?.data?.user?.name ?? name;
      const savedEmail: string = result?.user?.email ?? result?.data?.user?.email ?? email;
      // Reset form fields
      setName(""); setEmail(""); setDeptName(""); setRole("");
      // Show temp-password popup (do NOT call onClose yet)
      setCreatedInfo({ name: savedName, email: savedEmail, tempPassword });
    } catch (err: any) {
      // Extract the most useful message from the Axios error
      const msg: string =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        (typeof err?.response?.data === "string" ? err.response.data : null) ||
        err?.message ||
        "Failed to create user. Please try again.";
      setSubmitError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  if (!open) return null;

  // Show the temp-password popup after a successful creation
  if (createdInfo) {
    return (
      <TempPasswordPopup
        name={createdInfo.name}
        email={createdInfo.email}
        tempPassword={createdInfo.tempPassword}
        onClose={() => {
          setCreatedInfo(null);
          onClose();
        }}
      />
    );
  }

  return (
    <div className="modal-overlay show" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <h2 style={{ marginTop: 0 }}>Register New User</h2>
        <p style={{ color: "var(--muted)", fontSize: 14, marginBottom: 20 }}>
          Fill in the details to create a new system account.
        </p>

        {/* Submission error banner */}
        {submitError && (
          <div style={{
            background: "#FEF2F2", border: "1px solid #FECACA",
            borderRadius: 8, padding: "10px 14px",
            marginBottom: 16, display: "flex", alignItems: "flex-start", gap: 10,
          }}>
            <span style={{ fontSize: 16, lineHeight: 1.2 }}>⚠️</span>
            <span style={{ color: "#B91C1C", fontSize: 13, lineHeight: 1.5 }}>{submitError}</span>
          </div>
        )}

        {/* Full Name */}
        <div className="form-group">
          <label>Full Name</label>
          <input
            className="form-input"
            placeholder="e.g. Jane Doe"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>

        {/* Email */}
        <div className="form-group">
          <label>Email</label>
          <input
            className="form-input"
            type="email"
            placeholder="jane@corp.com"
            value={email}
            onChange={(e) => { setEmail(e.target.value); setSubmitError(null); }}
          />
        </div>

        {/* Department (Dropdown) */}
        <div className="form-group">
          <label>Department</label>
          <select
            className="form-input"
            value={deptName}
            onChange={(e) => setDeptName(e.target.value)}
            disabled={loadingDepts || !!deptErr}
          >
            <option value="">{loadingDepts ? "Loading..." : "-- Select Department --"}</option>
            {departments.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
          {deptErr && (
            <div style={{ color: "#b91c1c", fontSize: 12, marginTop: 6 }}>
              {deptErr}
            </div>
          )}
        </div>

        {/* Role */}
        <div className="form-group">
          <label>Role</label>
          <select
            className="form-input"
            value={role}
            onChange={(e) => setRole(e.target.value as "ADMIN" | "REVIEWER" | "EMPLOYEE" | "")}
          >
            <option value="">{`-- Select Role --`}</option>
            {allowedRoles.map((r) => (
              <option key={r} value={r}>
                {r.charAt(0) + r.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </div>

        {/* Actions */}
        <div style={{ marginTop: 25, display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
          <button className="btn btn-outline" onClick={onClose}>Cancel</button>
          <button
            className="btn"
            style={{ background: "var(--brand)", color: "#fff" }}
            onClick={submit}
            disabled={submitting}
          >
            {submitting ? "Creating…" : "Create"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default RegisterUserModal;