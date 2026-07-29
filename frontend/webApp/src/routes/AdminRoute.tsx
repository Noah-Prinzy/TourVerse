import { Navigate, Outlet } from "react-router-dom";
import { LoadingState } from "../components/StatusStates";
import { useAuth } from "../state/AuthContext";

export function AdminRoute() {
  const { user, initializing } = useAuth();
  if (initializing) return <LoadingState label="Restoring your session..." />;
  return user?.role === "ADMIN" ? <Outlet /> : <Navigate to="/destinations" replace />;
}
