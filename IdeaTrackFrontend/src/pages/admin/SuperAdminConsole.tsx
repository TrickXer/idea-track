// src/pages/SuperAdminConsole.tsx
import React, { useEffect, useMemo, useState } from "react";
import UserCard from "../../components/user/UserCard";
import RegisterUserModal from "../../components/user/RegisterUserModal";
import UserModal from "../../components/user/UserModal";
import { listUsers, createUser } from "../../utils/adminApi";
import type { CreateUserPayload } from "../../utils/types";
import "../../components/admin/console.css";
import ReviewerStageAssignment from "../reviewer/ReviewerStageAssignment";
import { Search } from "lucide-react";

type Tab = "ADMIN" | "REVIEWER" | "EMPLOYEE" | "STAGE_ASSIGN";

const SuperAdminConsole: React.FC = () => {
  const [users, setUsers] = useState<any[]>([]);
  const [tab, setTab] = useState<Tab>("ADMIN");
  const [search, setSearch] = useState("");
  const [showRegister, setShowRegister] = useState(false);
  const [activeUser, setActiveUser] = useState<any | null>(null);

  const load = async () => {
    const data = await listUsers();
    setUsers(data);
  };

  useEffect(() => { load(); }, []);

  const filtered = useMemo(() => {
    if (tab === "STAGE_ASSIGN") return [];
    const t = search.toLowerCase();
    return users
      .filter((u) => u.role === tab)
      .filter((u) =>
        [u.name, u.email, u.userId, u.deptName].some((f) =>
          String(f ?? "").toLowerCase().includes(t)
        )
      );
  }, [users, tab, search]);

  return (
    <>
      <main className="page">
        <div className="header-flex">
          <div>
            <h1>Superadmin Control Center</h1>
            <p style={{ color: "var(--muted)" }}>Manage all system users</p>
          </div>
          <button className="btn" style={{ background: "var(--brand)", color: "#fff", padding: "12px 20px" }} onClick={() => setShowRegister(true)}>
            + Register User
          </button>
        </div>

        <div className="tabs">
          <div className={`tab ${tab === "ADMIN" ? "active" : ""}`} onClick={() => setTab("ADMIN")}>
            Admins ({users.filter((u) => u.role === "ADMIN").length})
          </div>
          <div className={`tab ${tab === "REVIEWER" ? "active" : ""}`} onClick={() => setTab("REVIEWER")}>
            Reviewers ({users.filter((u) => u.role === "REVIEWER").length})
          </div>
          <div className={`tab ${tab === "EMPLOYEE" ? "active" : ""}`} onClick={() => setTab("EMPLOYEE")}>
            Employees ({users.filter((u) => u.role === "EMPLOYEE").length})
          </div>
          <div className={`tab ${tab === "STAGE_ASSIGN" ? "active" : ""}`} onClick={() => setTab("STAGE_ASSIGN")}>
            Stage Assign
          </div>
        </div>

        {tab === "STAGE_ASSIGN" ? (
          <ReviewerStageAssignment />
        ) : (
          <>
            <div className="search-container">
              <span className="search-icon"><Search size={16} /></span>
              <input className="search-bar" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search..." />
            </div>
            <div className="user-list">
              {filtered.map((u) => (
                <UserCard key={u.userId} user={u} onManage={() => setActiveUser(u)} />
              ))}
            </div>
          </>
        )}
      </main>

      <RegisterUserModal
        open={showRegister}
        onClose={() => setShowRegister(false)}
        allowedRoles={["ADMIN", "REVIEWER", "EMPLOYEE"]}
        onSubmit={async (payload: CreateUserPayload) => { const result = await createUser(payload); await load(); return result; }}
      />

      <UserModal
        open={!!activeUser}
        user={activeUser}
        onClose={() => setActiveUser(null)}
        onChanged={load}
        allowRoles={["ADMIN", "REVIEWER", "EMPLOYEE"]}
      />
    </>
  );
};

export default SuperAdminConsole;
