import React, { useState, useEffect } from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../../utils/authContext";
import { ProfileAvatar, useProfileImage } from "../../utils/profileImageHandler";
import { fetchMyProfile } from "../../utils/profileApi";
import {
  Menu,
  X,
  LayoutDashboard,
  Lightbulb,
  FileText,
  CheckSquare,
  Users,
  ClipboardList,
  BarChart2,
  Settings,
  LogOut,
} from "lucide-react";
import NotificationBell from "../notifications/NotificationBell";
import "./Layout.css";

interface LayoutProps {
  children: React.ReactNode;
}

const Sidebar = ({ isOpen, mobileOpen, closeMobile }: { isOpen: boolean, mobileOpen: boolean, closeMobile: () => void }) => {
  const { roles } = useAuth();
  const location = useLocation();

  const isAdmin = roles.includes("ADMIN") || roles.includes("SUPERADMIN");
  const isSuperAdmin = roles.includes("SUPERADMIN");
  const isReviewer = roles.includes("REVIEWER");

  const isActive = (path: string) => {
    // Exact match or nested path (but not partial matches)
    if (location.pathname === path) return true;
    // For nested paths, check if it's a proper child route
    // e.g., /admin should not match /admin/users, only /admin/nested/path should match /admin
    if (path === "/admin") {
      // Only /admin dashboard itself, not any /admin/* subpages
      return location.pathname === path;
    }
    // For other paths, allow startsWith for true nested routes
    return location.pathname.startsWith(`${path}/`);
  };

  const NavItem = ({ to, icon: Icon, label }: { to: string, icon: any, label: string }) => (
    <Link
      to={to}
      className={`nav-item ${isActive(to) ? "active" : ""}`}
      onClick={closeMobile}
    >
      <Icon className="icon" size={20} />
      <span>{label}</span>
    </Link>
  );

  return (
    <div className={`sidebar ${!isOpen ? "closed" : ""} ${mobileOpen ? "open" : ""}`}>
      <div className="sidebar-header">
        <div style={{ display: "flex", alignItems: "center", color: "var(--primary-color)" }}>
          <div style={{ 
            width: 36, height: 36, background: "var(--primary-color)", 
            borderRadius: 10, display: "flex", alignItems: "center", justifyContent: "center", color: "white" 
          }}>
            <Lightbulb size={20} className="fill-current" />
          </div>
          <span className="logo-text">IdeaTrack</span>
        </div>
        <button className="toggle-btn d-lg-none ms-auto" onClick={closeMobile}>
          <X size={24} />
        </button>
      </div>

      <div className="sidebar-content">
        <div className="nav-section-label">Main</div>
        <NavItem to="/dashboard" icon={LayoutDashboard} label="Dashboard" />
        <NavItem to="/explore" icon={Lightbulb} label="Idea Wall" />
        <NavItem to="/employee/accepted-ideas" icon={FileText} label="My Proposals" />

        {(isReviewer || isAdmin) && (
          <>
            <div className="nav-section-label">Review</div>
            <NavItem to="/reviewer/dashboard" icon={CheckSquare} label="Reviewer Dashboard" />
          </>
        )}

        {/* Analytics – single entry point; server redirects to the right dashboard */}
        <div className="nav-section-label">Analytics</div>
        <NavItem to="/analytics" icon={BarChart2} label="Analytics" />

        {isAdmin && (
          <>
            <div className="nav-section-label">Administration</div>
            <NavItem to="/admin" icon={LayoutDashboard} label="Admin Dashboard" />
            <NavItem to="/admin/users" icon={Users} label="User Management" />
            <NavItem to="/admin/proposals" icon={ClipboardList} label="Proposals" />
            <NavItem to="/admin/reports" icon={BarChart2} label="Reports" />
            {isSuperAdmin && (
              <NavItem to="/super-admin" icon={Users} label="Super Admin" />
            )}
            
            <div className="nav-section-label">Configuration</div>
            <NavItem to="/admin/categories" icon={Settings} label="Categories" />
          </>
        )}
      </div>

      <div style={{ padding: 20, borderTop: "1px solid var(--gray-100)" }}>
        <div className="card-shine" style={{ padding: "16px", background: "linear-gradient(135deg, #868CFF 0%, #4318FF 100%)", color: "white" }}>
          <div style={{ width: 36, height: 36, background: "rgba(255,255,255,0.2)", borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", marginBottom: 12 }}>
            <Lightbulb size={20} />
          </div>
          <h6 style={{ margin: 0, fontSize: 14 }}>Got a new idea?</h6>
          <p style={{ margin: "4px 0 12px", fontSize: 12, opacity: 0.8 }}>Share your innovation with us</p>
          <Link to="/create-idea" className="btn btn-sm btn-light w-100 text-primary border-0 fw-bold">
            Create Idea
          </Link>
        </div>
      </div>
    </div>
  );
};

const Header = ({ toggleSidebar, toggleMobile }: { toggleSidebar: () => void, toggleMobile: () => void }) => {
  const { token, payload, logout } = useAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [userProfile, setUserProfile] = useState<{ name: string; profileUrl: string | null } | null>(null);
  const [loading, setLoading] = useState(true);
  const dropdownRef = React.useRef<HTMLDivElement>(null);

  const displayName = payload?.sub ?? "User";
  const profileData = userProfile || { name: displayName, profileUrl: null };
  const { imageUrl, initials, shouldShowImage } = useProfileImage(profileData.profileUrl, profileData.name);
  const [showInitials, setShowInitials] = useState(!shouldShowImage);

  // Fetch user profile to get name and profile URL
  useEffect(() => {
    const loadProfile = async () => {
      try {
        setLoading(true);
        const profile = await fetchMyProfile();
        console.log("Profile fetched:", profile);
        setUserProfile({
          name: profile.name || displayName,
          profileUrl: profile.profileUrl || null
        });
        setShowInitials(!profile.profileUrl); // Reset showInitials based on new profile
      } catch (err) {
        console.error("Failed to fetch user profile:", err);
        setUserProfile({
          name: displayName,
          profileUrl: null
        });
        setShowInitials(true);
      } finally {
        setLoading(false);
      }
    };
    loadProfile();
  }, [token]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleImageError = () => {
    setShowInitials(true);
  };

  return (
    <header className="top-header">
      <div className="header-left">
        <button className="toggle-btn d-none d-lg-block" onClick={toggleSidebar}>
          <Menu size={24} />
        </button>
        <button className="toggle-btn d-lg-none" onClick={toggleMobile}>
          <Menu size={24} />
        </button>
        <div className="ms-3 d-none d-md-block text-muted">
          <span style={{ fontSize: 14 }}>Pages / Dashboard</span>
          <h5 style={{ margin: 0, color: "var(--gray-500)", fontWeight: 700 }}>Overview</h5>
        </div>
      </div>

      <div className="header-right">
        <NotificationBell />

        <div ref={dropdownRef} style={{ position: "relative" }}>
          <button
            onClick={() => setDropdownOpen(prev => !prev)}
            style={{
              background: "none", border: "none", padding: 0,
              cursor: "pointer", borderRadius: "50%", display: "flex",
              alignItems: "center", overflow: "hidden"
            }}
          >
            {!showInitials && imageUrl ? (
              <img
                src={imageUrl}
                alt={profileData.name}
                onError={handleImageError}
                style={{
                  width: "40px",
                  height: "40px",
                  borderRadius: "50%",
                  objectFit: "cover",
                  border: "2px solid #e2e8f0"
                }}
              />
            ) : (
              <div
                style={{
                  width: "40px",
                  height: "40px",
                  borderRadius: "50%",
                  backgroundColor: "var(--bs-primary)",
                  color: "white",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontWeight: "bold",
                  fontSize: "16px",
                  border: "2px solid #e2e8f0"
                }}
              >
                {initials}
              </div>
            )}
          </button>

          {dropdownOpen && (
            <div style={{
              position: "absolute", right: 0, top: "calc(100% + 10px)",
              background: "white", borderRadius: 14, minWidth: 200,
              boxShadow: "0 10px 40px rgba(0,0,0,0.12)",
              zIndex: 9999, overflow: "hidden",
              animation: "authFadeIn 0.15s ease-out",
            }}>
              {/* User info */}
              <div style={{ padding: "14px 16px", borderBottom: "1px solid #F4F7FE" }}>
                <div style={{ display: "flex", gap: 10, flexDirection: "column", alignItems: "flex-start" }}>
                  <div>
                    <div style={{ fontWeight: 700, fontSize: 14, color: "#1B254B" }}>{profileData.name}</div>
                    <div style={{ fontSize: 11, color: "#A3AED0" }}>{payload?.roles?.[0] ?? "User"}</div>
                  </div>
                </div>
              </div>

              {/* Profile link */}
              <Link
                to="/profile"
                onClick={() => setDropdownOpen(false)}
                style={{
                  display: "flex", alignItems: "center", gap: 10,
                  padding: "12px 16px", fontSize: 14, color: "#1B254B",
                  textDecoration: "none", transition: "background 0.15s",
                }}
                onMouseEnter={e => (e.currentTarget.style.background = "#F4F7FE")}
                onMouseLeave={e => (e.currentTarget.style.background = "transparent")}
              >
                <Users size={16} color="#4318FF" /> My Profile
              </Link>

              <div style={{ height: 1, background: "#F4F7FE" }} />

              {/* Logout */}
              <button
                onClick={() => { setDropdownOpen(false); logout(); }}
                style={{
                  display: "flex", alignItems: "center", gap: 10,
                  padding: "12px 16px", fontSize: 14, color: "#EE5D50",
                  background: "none", border: "none", width: "100%",
                  cursor: "pointer", transition: "background 0.15s", textAlign: "left",
                }}
                onMouseEnter={e => (e.currentTarget.style.background = "#FFF0F0")}
                onMouseLeave={e => (e.currentTarget.style.background = "transparent")}
              >
                <LogOut size={16} /> Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [mobileOpen, setMobileOpen] = useState(false);

  // Auto-close sidebar on small screens
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 992) {
        setSidebarOpen(false);
      } else {
        setSidebarOpen(true);
      }
    };
    
    // Initial check
    handleResize();

    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  return (
    <div className="main-layout">
      <Sidebar 
        isOpen={sidebarOpen}
        mobileOpen={mobileOpen}
        closeMobile={() => setMobileOpen(false)}
      />
      
      <div className={`main-content ${!sidebarOpen ? "full-width" : ""}`}>
        <Header 
          toggleSidebar={() => setSidebarOpen(!sidebarOpen)}
          toggleMobile={() => setMobileOpen(!mobileOpen)}
        />
        <div className="page-container">
          {children}
        </div>
      </div>

      {/* Mobile overlay */}
      {mobileOpen && (
        <div 
          className="d-lg-none position-fixed top-0 start-0 w-100 h-100 bg-dark bg-opacity-50" 
          style={{ zIndex: 999 }}
          onClick={() => setMobileOpen(false)}
        />
      )}
    </div>
  );
};

export default Layout;
