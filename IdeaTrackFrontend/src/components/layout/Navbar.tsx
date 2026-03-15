import { useState, useRef, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { LogOut, Settings, User, LayoutDashboard, Lightbulb, BarChart2, Users, FileText, ClipboardList, CheckSquare } from "lucide-react";
import NotificationBell from "../notifications/NotificationBell";
import { useAuth } from "../../utils/authContext";
import "./Navbar.css";

const Navbar = () => {
  const { payload, roles, logout } = useAuth();
  const navigate = useNavigate();
  const [profileOpen, setProfileOpen] = useState(false);
  const profileRef = useRef<HTMLDivElement>(null);

  const isAdmin = roles.includes("ADMIN") || roles.includes("SUPERADMIN");
  const isSuperAdmin = roles.includes("SUPERADMIN");

  const isReviewer = roles.includes("REVIEWER");

  const displayName = payload?.sub ?? "User";
  const avatar = `https://ui-avatars.com/api/?name=${encodeURIComponent(displayName)}&background=6c63ff&color=fff`;

  // Close profile dropdown on outside click
  useEffect(() => {
    if (!profileOpen) return;
    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as Node;
      if (profileRef.current && !profileRef.current.contains(target)) {
        setProfileOpen(false);
      }
    };
    document.addEventListener("click", handleClickOutside);
    return () => document.removeEventListener("click", handleClickOutside);
  }, [profileOpen]);

  const handleLogout = () => {
    setProfileOpen(false);
    logout();
    navigate("/login", { replace: true });
  };

  const handleProfileClick = () => {
    setProfileOpen(false);
    navigate("/profile");
  };

  return (
    <nav className="navbar-custom">
      <div className="navbar-container">
        {/* Logo and App Name */}
        <div className="navbar-brand">
          <Link to="/dashboard" style={{ display: "flex", alignItems: "center", gap: "8px", textDecoration: "none" }}>
            <div className="app-logo">
              <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect width="32" height="32" rx="8" fill="#6c63ff" />
                <path d="M16 8L20 14H12L16 8Z" fill="white" />
                <path d="M12 16L16 22L20 16H12Z" fill="white" opacity="0.7" />
              </svg>
            </div>
            <span className="app-name">IdeaTrack</span>
          </Link>
        </div>

        {/* Navigation Links (Center) */}
        <div className="navbar-nav-center">
          <Link to="/dashboard" className="nav-link">
            <LayoutDashboard size={16} />
            Dashboard
          </Link>
          <Link to="/explore" className="nav-link">
            <Lightbulb size={16} />
            IdeaWall
          </Link>

          {/* My Proposals — visible to all roles (any user's idea can be accepted) */}
          <Link to="/employee/accepted-ideas" className="nav-link">
            <FileText size={16} />
            My Proposals
          </Link>

          {/* Reviewer-specific links */}
          {(isReviewer || isAdmin) && (
            <Link to="/reviewer/dashboard" className="nav-link">
              <CheckSquare size={16} />
              My Reviews
            </Link>
          )}

          {/* Admin-specific links */}
          {isAdmin && (
            <>
              <Link to="/admin" className="nav-link">
                <LayoutDashboard size={16} />
                Admin
              </Link>
              <Link to="/admin/users" className="nav-link">
                <Users size={16} />
                Users
              </Link>
              <Link to="/admin/proposals" className="nav-link">
                <ClipboardList size={16} />
                Proposals
              </Link>
              <Link to="/admin/reports" className="nav-link">
                <BarChart2 size={16} />
                Reports
              </Link>
            </>
          )}
        </div>

        {/* Right Side: Notifications and Profile */}
        <div className="navbar-right">
          {/* Notification Bell */}
          <div className="navbar-notification">
            <NotificationBell />
          </div>

          {/* User Profile Dropdown */}
          <div className="navbar-profile" ref={profileRef}>
            <div className="profile-trigger" onClick={() => setProfileOpen(!profileOpen)}>
              <img src={avatar} alt={displayName} className="profile-avatar" />
              <span className="profile-name">{displayName}</span>
            </div>

            {profileOpen && (
              <div className="profile-dropdown">
                <button className="profile-dropdown-item" onClick={handleProfileClick}>
                  <User size={18} />
                  <span>Profile</span>
                </button>

                {isAdmin && (
                  <>
                    <Link to="/admin/categories" className="profile-dropdown-item" onClick={() => setProfileOpen(false)}>
                      <Settings size={18} />
                      <span>Category Management</span>
                    </Link>
                    <Link to="/admin/bulk-ideas" className="profile-dropdown-item" onClick={() => setProfileOpen(false)}>
                      <ClipboardList size={18} />
                      <span>Bulk Idea Console</span>
                    </Link>
                    <Link to="/admin/users?tab=STAGE_ASSIGN" className="profile-dropdown-item" onClick={() => setProfileOpen(false)}>
                      <Users size={18} />
                      <span>Reviewer Assignment</span>
                    </Link>
                    <Link to="/admin/health" className="profile-dropdown-item" onClick={() => setProfileOpen(false)}>
                      <BarChart2 size={18} />
                      <span>Health Summary</span>
                    </Link>
                  </>
                )}

                {isSuperAdmin && (
                  <Link to="/super-admin" className="profile-dropdown-item" onClick={() => setProfileOpen(false)}>
                    <Users size={18} />
                    <span>Super Admin Console</span>
                  </Link>
                )}

                <hr className="profile-dropdown-divider" />
                <button className="profile-dropdown-item logout" onClick={handleLogout}>
                  <LogOut size={18} />
                  <span>Logout</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
