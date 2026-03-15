import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../utils/authContext";

interface Props {
  /** Allowed roles, e.g. ["ADMIN", "SUPERADMIN"]. Empty = any authenticated user. */
  allow?: string[];
  children: React.ReactNode;
}

const ProtectedRoute: React.FC<Props> = ({ allow = [], children }) => {
  const { token, roles } = useAuth();
  const location = useLocation();

  if (!token) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allow.length > 0) {
    const ok = allow.some((r) => roles.includes(r.toUpperCase()));
    if (!ok) {
      return <Navigate to="/login" replace />;
    }
  }

  return <>{children}</>;
};

export default ProtectedRoute;
