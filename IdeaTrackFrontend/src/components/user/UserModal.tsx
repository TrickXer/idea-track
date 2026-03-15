// src/components/UserModal.tsx
import React, { useEffect, useMemo, useState } from "react";
import { updateUser, deleteUser, getDepartments } from "../../utils/adminApi";
import ConfirmationModal from "../ConfirmationModal/ConfirmationModal";
import { useShowToast } from "../../hooks/useShowToast";

type Props = {
  open: boolean;
  user: any | null;
  onClose: () => void;
  onChanged: () => void; // refresh list after save/delete
  allowRoles: Array<"ADMIN" | "REVIEWER" | "EMPLOYEE">;
};

type UpdateUserPayload = Partial<{
  name: string;
  email: string;
  role: "ADMIN" | "REVIEWER" | "EMPLOYEE";
  deptName: string;
  phoneNo: string;
  bio: string;
  profileUrl: string;
}>;

const UserModal: React.FC<Props> = ({ open, user, onClose, onChanged, allowRoles }) => {
  const toast = useShowToast();
  // form state
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<"" | "ADMIN" | "REVIEWER" | "EMPLOYEE">("");
  const [deptName, setDeptName] = useState("");
  const [phoneNo, setPhoneNo] = useState("");
  const [bio, setBio] = useState("");
  const [profileUrl, setProfileUrl] = useState("");

  // departments
  const [departments, setDepartments] = useState<string[]>([]);
  const [loadingDepts, setLoadingDepts] = useState(false);

  // UX
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);

  // hydrate when opening / user changes
  useEffect(() => {
    if (!open || !user) return;
    setName(user.name ?? "");
    setEmail(user.email ?? "");
    setRole((user.role as any) ?? "");
    setDeptName(user.deptName ?? "");
    setPhoneNo(user.phoneNo ?? "");
    setBio(user.bio ?? "");
    setProfileUrl(user.profileUrl ?? "");
  }, [open, user]);

  // load departments when open
  useEffect(() => {
    if (!open) return;
    let active = true;
    (async () => {
      try {
        setLoadingDepts(true);
        const res = await getDepartments(); // -> { deptNames: string[] }
        if (active) setDepartments(res.deptNames ?? []);
      } finally {
        active && setLoadingDepts(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [open]);

  // only send changed fields
  const diffPayload: UpdateUserPayload = useMemo(() => {
    if (!user) return {};
    const p: UpdateUserPayload = {};
    const trim = (v?: string) => (v ?? "").trim();

    if (trim(name) && trim(name) !== (user.name ?? "")) p.name = trim(name);
    if (trim(email) && trim(email) !== (user.email ?? "")) p.email = trim(email);
    if (role && role !== user.role) p.role = role;
    if ((deptName ?? "") !== (user.deptName ?? "")) p.deptName = deptName || "";
    if ((phoneNo ?? "") !== (user.phoneNo ?? "")) p.phoneNo = phoneNo || "";
    if ((bio ?? "") !== (user.bio ?? "")) p.bio = bio || "";
    if ((profileUrl ?? "") !== (user.profileUrl ?? "")) p.profileUrl = profileUrl || "";
    return p;
  }, [user, name, email, role, deptName, phoneNo, bio, profileUrl]);

  const hasChanges = Object.keys(diffPayload).length > 0;

  const saveAll = async () => {
    if (!user || !hasChanges || saving) return;
    try {
      setSaving(true);
      await updateUser(user.userId, diffPayload);
      toast.success('User updated successfully.');
      await onChanged();
      onClose();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || err?.message || 'Failed to update user.');
    } finally {
      setSaving(false);
    }
  };

  const doDelete = async () => {
    if (!user || deleting) return;
    try {
      setDeleting(true);
      await deleteUser(user.userId);
      toast.success('User deleted successfully.');
      await onChanged();
      onClose();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || err?.message || 'Failed to delete user.');
    } finally {
      setDeleting(false);
    }
  };

  if (!open || !user) return null;

  const stop = (e: React.MouseEvent) => e.stopPropagation();

  return (
    <>
    <div className="modal-overlay show" onClick={onClose}>
      <div className="modal-card modal-lg" onClick={stop} role="dialog" aria-modal="true">
        {/* --- HEADER (avatar left, text stack right: Name -> Email -> Role) --- */}
        <div className="modal-header modal-header--row">
          <div className="avatar-lg" aria-hidden="true">
            {user.name?.charAt(0) ?? "U"}
          </div>

          <div className="modal-header-meta">
            <h2 className="modal-title">{user.name}</h2>
            <p className="modal-subtitle">{user.email}</p>
            {user.role && (
              <span
                className={`role-pill badge-role ${
                  user.role === "ADMIN"
                    ? "rp-admin"
                    : user.role === "REVIEWER"
                    ? "rp-reviewer"
                    : "rp-employee"
                }`}
              >
                {user.role}
              </span>
            )}
          </div>
        </div>

        {/* --- FORM PANEL --- */}
        <div className="soft-panel">
          {/* NAME */}
          <div className="form-group">
            <label className="form-label">Edit Name</label>
            <input
              className="form-input"
              placeholder="Full name"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>

          {/* EMAIL */}
          <div className="form-group">
            <label className="form-label">Email</label>
            <input
              className="form-input"
              type="email"
              placeholder="Email address"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          {/* ROLE */}
          <div className="form-group">
            <label className="form-label">Change Role</label>
            <select
              className="form-input"
              value={role}
              onChange={(e) => setRole(e.target.value as any)}
            >
              <option value="">-- Select Role --</option>
              {allowRoles.map((r) => (
                <option key={r} value={r}>
                  {r.charAt(0) + r.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </div>

          {/* DEPARTMENT */}
          <div className="form-group">
            <label className="form-label">Department</label>
            <select
              className="form-input"
              value={deptName}
              onChange={(e) => setDeptName(e.target.value)}
              disabled={loadingDepts}
            >
              <option value="">
                {loadingDepts ? "Loading..." : "-- Select Department --"}
              </option>
              {departments.map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
            </select>
          </div>

          {/* PHONE */}
          <div className="form-group">
            <label className="form-label">Phone No</label>
            <input
              className="form-input"
              placeholder="Phone number"
              value={phoneNo}
              onChange={(e) => setPhoneNo(e.target.value)}
            />
          </div>

          {/* BIO */}
          <div className="form-group">
            <label className="form-label">Bio</label>
            <textarea
              className="form-input form-textarea"
              placeholder="Bio"
              value={bio}
              onChange={(e) => setBio(e.target.value)}
            />
          </div>

          {/* PROFILE URL */}
          <div className="form-group">
            <label className="form-label">Profile URL</label>
            <input
              className="form-input"
              placeholder="https://…"
              value={profileUrl}
              onChange={(e) => setProfileUrl(e.target.value)}
            />
          </div>

          {/* ACTIONS */}
          <div className="full-actions">
            <button className="btn btn-outline" onClick={onClose} disabled={saving || deleting}>
              Close
            </button>
            <button
              className="btn"
              style={{ background: hasChanges ? "var(--brand)" : "#cbd5e1", color: "#fff" }}
              onClick={saveAll}
              disabled={!hasChanges || saving}
            >
              {saving ? "Saving..." : hasChanges ? "Save Changes" : "No Changes"}
            </button>
          </div>

          <div style={{ marginTop: 10 }}>
            <button className="btn btn-danger-light" onClick={() => setConfirmDeleteOpen(true)} disabled={saving || deleting}>
              {deleting ? "Deleting..." : "Delete User"}
            </button>
          </div>
        </div>
      </div>
    </div>

    <ConfirmationModal
      isOpen={confirmDeleteOpen}
      title="Delete User"
      message={`Are you sure you want to delete ${user.name}? This action cannot be undone.`}
      confirmText="Delete"
      cancelText="Cancel"
      isDangerous
      isLoading={deleting}
      onConfirm={async () => { setConfirmDeleteOpen(false); await doDelete(); }}
      onCancel={() => setConfirmDeleteOpen(false)}
    />
    </>
  );
};

export default UserModal;