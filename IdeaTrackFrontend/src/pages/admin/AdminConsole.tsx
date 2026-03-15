// src/pages/AdminConsole.tsx
import React, { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import UserCard from "../../components/user/UserCard";
import RegisterUserModal from "../../components/user/RegisterUserModal";
import UserModal from "../../components/user/UserModal";
import { listUsers, createUser } from "../../utils/adminApi";
import type { CreateUserPayload } from "../../utils/types";
import "../../components/admin/console.css";
import ReviewerStageAssignment from "../reviewer/ReviewerStageAssignment";
import { Search } from "lucide-react";

type Tab = "REVIEWER" | "EMPLOYEE" | "STAGE_ASSIGN";

const VALID_TABS: Tab[] = ["REVIEWER", "EMPLOYEE", "STAGE_ASSIGN"];

const AdminConsole: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [users, setUsers] = useState<any[]>([]);
  const rawTab = searchParams.get("tab")?.toUpperCase() as Tab | null;
  const [tab, setTab] = useState<Tab>(rawTab && VALID_TABS.includes(rawTab) ? rawTab : "REVIEWER");
  const [search, setSearch] = useState("");
  const [showRegister, setShowRegister] = useState(false);
  const [activeUser, setActiveUser] = useState<any | null>(null);

  // Sync tab with URL query param
  const switchTab = (t: Tab) => {
    setTab(t);
    setSearchParams(t === "REVIEWER" ? {} : { tab: t }, { replace: true });
  };

  const load = async () => {
    const data = await listUsers();
    setUsers(data.filter((u: any) => u.role !== "ADMIN" && u.role !== "SUPERADMIN"));
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
            <h1>Admin Control Center</h1>
            <p style={{ color: "var(--muted)" }}>Manage system access</p>
          </div>
          <button className="btn" style={{ background: "var(--brand)", color: "#fff", padding: "12px 20px" }} onClick={() => setShowRegister(true)}>
            + Register New User
          </button>
        </div>

        <div className="tabs">
          <div className={`tab ${tab === "REVIEWER" ? "active" : ""}`} onClick={() => switchTab("REVIEWER")}>
            Reviewers ({users.filter((u) => u.role === "REVIEWER").length})
          </div>
          <div className={`tab ${tab === "EMPLOYEE" ? "active" : ""}`} onClick={() => switchTab("EMPLOYEE")}>
            Employees ({users.filter((u) => u.role === "EMPLOYEE").length})
          </div>
          <div className={`tab ${tab === "STAGE_ASSIGN" ? "active" : ""}`} onClick={() => switchTab("STAGE_ASSIGN")}>
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
        allowedRoles={["REVIEWER", "EMPLOYEE"]}
        onSubmit={async (payload: CreateUserPayload) => { const result = await createUser(payload); await load(); return result; }}
      />

      <UserModal
        open={!!activeUser}
        user={activeUser}
        onClose={() => setActiveUser(null)}
        onChanged={load}
        allowRoles={["REVIEWER", "EMPLOYEE"]}
      />
    </>
  );
};

export default AdminConsole;
