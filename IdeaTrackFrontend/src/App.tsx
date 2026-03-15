import { BrowserRouter, Routes, Route, Navigate, Outlet } from "react-router-dom";
import { AuthProvider, useAuth } from "./utils/authContext";
import { NotificationProvider } from "./context/NotificationContext";
import { ToastProvider } from "./context/ToastContext";
import ProtectedRoute from "./components/auth/ProtectedRoute";
import Layout from "./components/layout/Layout";
import LandingPage from "./pages/landing/LandingPage";

// ─── Auth ─────────────────────────────────────────────────────
import Login from "./pages/auth/Login";
import Signup from "./pages/auth/Signup";

// ─── Idea Submission ──────────────────────────────────────────
import EmployeeDashboard from "./pages/employee/EmployeeDashboard";
import IdeaForm from "./pages/idea/IdeaForm";
import IdeaWall from "./pages/idea/IdeaWall";
import OwnerIdeaDetail from "./components/idea/OwnerIdeaDetail";
import NonOwnerIdeaDetail from "./components/idea/NonOnwerIdeaDetail";
import EditDraftForm from "./pages/idea/editDraftForm";

// ─── Profile ──────────────────────────────────────────────────
import ProfileHub from "./pages/profile/ProfileHub";
import IdeaHierarchy from "./pages/idea/IdeaHierarchy";

// ─── Notifications ────────────────────────────────────────────
import AllNotifications from "./pages/notifications/AllNotifications";

// ─── Employee – Proposals ─────────────────────────────────────
import AcceptedIdeasPage from "./pages/employee/AcceptedIdeasPage";
import ProposalCreatePage from "./pages/employee/ProposalCreatePage";
import DraftUpdatePage from "./pages/employee/DraftUpdatePage";

// ─── Admin – Dashboard & User Management ─────────────────────
import AdminDashboard from "./pages/admin/AdminDashboard";
import AdminConsole from "./pages/admin/AdminConsole";
import SuperAdminConsole from "./pages/admin/SuperAdminConsole";

// ─── Admin – Category & Bulk ──────────────────────────────────
import CategoryManagement from "./components/admin/CategoryManagement";
import BulkIdeaConsole from "./components/admin/BulkIdeaConsole";

// ─── Admin – Proposals ───────────────────────────────────────
import ProposalsPage from "./pages/proposal/ProposalsPage";
import PendingProposals from "./pages/proposal/PendingProposals";
import ProposalReview from "./pages/proposal/ProposalReview";
import ReviewersOverdue from "./pages/proposal/ReviewersOverdue";
import HealthSummary from "./pages/proposal/HealthSummary";

// ─── Admin – Analytics & Reports ─────────────────────────
import ReportCreationAndDisplay from "./pages/profile/ReportCreationAndDisplay";

// ─── Analytics Dashboards ────────────────────────────────────
import EmployeeAnalytics from "./pages/analytics/EmployeeAnalytics";
import ReviewerAnalytics from "./pages/analytics/ReviewerAnalytics";
import AdminAnalytics from "./pages/analytics/AdminAnalytics";

// ─── Reviewer Module ──────────────────────────────────────────
import ReviewerDashboard from "./pages/reviewer/ReviewerDashboard";
import { ReviewerIdeaPage } from "./pages/reviewer/ReviewerIdeaPage";
import { useAxiosErrorHandler } from "./hooks/useAxiosErrorHandler";

/** Redirects /analytics to the right dashboard based on the user's highest role. */
const AnalyticsRedirect: React.FC = () => {
    const { roles } = useAuth();
    if (roles.includes("ADMIN") || roles.includes("SUPERADMIN")) {
        return <Navigate to="/analytics/admin" replace />;
    }
    if (roles.includes("REVIEWER")) {
        return <Navigate to="/analytics/reviewer" replace />;
    }
    return <Navigate to="/analytics/employee" replace />;
};

/** Renders the matched child route inside the Layout shell. */
const AppOutlet: React.FC = () => <Outlet />;

const AppRoutes = () => {
    // Setup axios error interceptor with toast notifications
    useAxiosErrorHandler();

    return (
        <Routes>
            {/* ── Public (no Layout) ───────────────────────────────── */}
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />

            {/* ── All authenticated pages share Layout ─────────────── */}
            <Route element={<Layout><AppOutlet /></Layout>}>
                {/* ── General ──────────────────────────────────────── */}
                <Route
                    path="/dashboard"
                    element={
                        <ProtectedRoute>
                            <EmployeeDashboard />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/create-idea"
                    element={
                        <ProtectedRoute>
                            <IdeaForm />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/explore"
                    element={
                        <ProtectedRoute>
                            <IdeaWall />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/my-idea/:id"
                    element={
                        <ProtectedRoute>
                            <OwnerIdeaDetail />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/idea/:id"
                    element={
                        <ProtectedRoute>
                            <NonOwnerIdeaDetail />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/edit-idea/:id"
                    element={
                        <ProtectedRoute>
                            <EditDraftForm />
                        </ProtectedRoute>
                    }
                />

                {/* ── Profile ──────────────────────────────────────── */}
                <Route
                    path="/profile"
                    element={
                        <ProtectedRoute>
                            <ProfileHub />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/hierarchy/idea/:ideaId"
                    element={
                        <ProtectedRoute>
                            <IdeaHierarchy />
                        </ProtectedRoute>
                    }
                />

                {/* ── Notifications ─────────────────────────────────── */}
                <Route
                    path="/notifications"
                    element={
                        <ProtectedRoute>
                            <AllNotifications />
                        </ProtectedRoute>
                    }
                />

                {/* ── Employee – Proposals ──────────────────────────── */}
                <Route
                    path="/employee/accepted-ideas"
                    element={
                        <ProtectedRoute>
                            <AcceptedIdeasPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/employee/proposals/new/:ideaId"
                    element={
                        <ProtectedRoute>
                            <ProposalCreatePage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/employee/proposals/:proposalId/edit"
                    element={
                        <ProtectedRoute>
                            <DraftUpdatePage />
                        </ProtectedRoute>
                    }
                />

                {/* ── Admin – Dashboard ─────────────────────────────── */}
                <Route
                    path="/admin"
                    element={
                        <ProtectedRoute allow={["ADMIN", "SUPERADMIN"]}>
                            <AdminDashboard />
                        </ProtectedRoute>
                    }
                />

                {/* ── Admin – User Management ───────────────────────── */}
                <Route
                    path="/admin/users"
                    element={
                        <ProtectedRoute allow={["ADMIN", "SUPERADMIN"]}>
                            <AdminConsole />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/super-admin"
                    element={
                        <ProtectedRoute allow={["SUPERADMIN"]}>
                            <SuperAdminConsole />
                        </ProtectedRoute>
                    }
                />

                {/* ── Admin – Category & Bulk ───────────────────────── */}
                <Route
                    path="/admin/categories"
                    element={
                        <ProtectedRoute allow={["ADMIN", "SUPERADMIN"]}>
                            <CategoryManagement />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/admin/bulk-ideas"
                    element={
                        <ProtectedRoute allow={["ADMIN", "SUPERADMIN"]}>
                            <BulkIdeaConsole />
                        </ProtectedRoute>
                    }
                />

                {/* ── Admin – Proposals ─────────────────────────────── */}
                <Route
                    path="/admin/proposals"
                    element={
                        <ProtectedRoute allow={["ADMIN", "SUPERADMIN"]}>
                            <ProposalsPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/admin/proposals/pending"
                    element={
                        <ProtectedRoute allow={["ADMIN", "SUPERADMIN"]}>
                            <PendingProposals />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/admin/proposals/:proposalId/review"
                    element={
                        <ProtectedRoute allow={["ADMIN", "SUPERADMIN"]}>
                            <ProposalReview />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/admin/reviewers-overdue"
                    element={
                        <ProtectedRoute allow={["ADMIN", "SUPERADMIN"]}>
                            <ReviewersOverdue />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/admin/health"
                    element={
                        <ProtectedRoute allow={["ADMIN", "SUPERADMIN"]}>
                            <HealthSummary />
                        </ProtectedRoute>
                    }
                />

                {/* ── Admin – Reviewer Assignment (redirects to User Management tab) ── */}
                <Route
                    path="/admin/reviewer-assignment"
                    element={<Navigate to="/admin/users" replace />}
                />
                <Route
                    path="/admin/reports"
                    element={
                        <ProtectedRoute allow={["ADMIN", "SUPERADMIN"]}>
                            <ReportCreationAndDisplay />
                        </ProtectedRoute>
                    }
                />

                {/* ── Analytics Dashboards ──────────────────────────── */}
                <Route
                    path="/analytics"
                    element={
                        <ProtectedRoute>
                            <AnalyticsRedirect />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/analytics/employee"
                    element={
                        <ProtectedRoute>
                            <EmployeeAnalytics />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/analytics/reviewer"
                    element={
                        <ProtectedRoute allow={["REVIEWER", "ADMIN", "SUPERADMIN"]}>
                            <ReviewerAnalytics />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/analytics/admin"
                    element={
                        <ProtectedRoute allow={["ADMIN", "SUPERADMIN"]}>
                            <AdminAnalytics />
                        </ProtectedRoute>
                    }
                />

                {/* ── Reviewer Module ───────────────────────────────── */}
                <Route
                    path="/reviewer/dashboard"
                    element={
                        <ProtectedRoute allow={["REVIEWER", "ADMIN", "SUPERADMIN"]}>
                            <ReviewerDashboard />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/reviewer/ideas/:ideaId"
                    element={
                        <ProtectedRoute allow={["REVIEWER", "ADMIN", "SUPERADMIN"]}>
                            <ReviewerIdeaPage />
                        </ProtectedRoute>
                    }
                />

                {/* ── Fallback ──────────────────────────────────────── */}
                <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Route>
        </Routes>
    );
};

const App = () => {
  return (
    <AuthProvider>
      <NotificationProvider>
        <ToastProvider>
          <BrowserRouter>
            <AppRoutes />
          </BrowserRouter>
        </ToastProvider>
      </NotificationProvider>
    </AuthProvider>
  );
};

export default App;