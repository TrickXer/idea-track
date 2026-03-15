// src/components/UserCard.tsx
import React from "react";

const roleClass = (role: string) =>
  role === "ADMIN"
    ? "rp-admin"
    : role === "REVIEWER"
    ? "rp-reviewer"
    : "rp-employee";

const UserCard: React.FC<{ user: any; onManage: () => void }> = ({ user, onManage }) => {
  return (
    <div className="user-item" onClick={onManage}>
      <div className="avatar-md">{user.name?.charAt(0)}</div>

      <div className="user-info">
        <h4>
          {user.name}
          <span className={`role-pill ${roleClass(user.role)}`}>{user.role}</span>
        </h4>
        <p>
          #{user.userId} • {user.email}
        </p>
      </div>

      <div className="actions">
        <button className="btn btn-outline" onClick={onManage}>
          Manage
        </button>
      </div>
    </div>
  );
};

export default UserCard;