import { Navigate, Outlet, useLocation } from "react-router-dom";
import { LoadingState } from "../components/StatusStates";
import { useAuth } from "../state/AuthContext";

export function ProtectedRoute() {
  const { user, initializing } = useAuth();
  const location = useLocation();
  if (initializing) return <LoadingState label="Restoring your session..." />;
  return user ? <Outlet /> : <Navigate to="/login" replace state={{ from: location.pathname }} />;
}
