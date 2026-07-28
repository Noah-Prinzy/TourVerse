import { Navigate, Route, Routes } from "react-router-dom";
import App from "./App";
import { AppShell } from "./components/AppShell";
import { LoginPage, RegisterPage } from "./features/auth/AuthPages";
import { ProfilePage } from "./features/profile/ProfilePage";
import { DestinationDetailsPage } from "./features/destinations/DestinationDetailsPage";
import { FavoritesPage } from "./features/favorites/FavoritesPage";
import { TripDetailsPage, TripsPage } from "./features/trips/TripPages";
import { NotFoundPage } from "./pages/PlaceholderPages";
import { ProtectedRoute } from "./routes/ProtectedRoute";

export default function RouterApp() {
  return <Routes>
    <Route element={<AppShell />}>
      <Route index element={<Navigate to="/destinations" replace />} />
      <Route path="destinations" element={<App />} />
      <Route path="destinations/:destinationId" element={<DestinationDetailsPage />} />
      <Route path="login" element={<LoginPage />} />
      <Route path="register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="profile" element={<ProfilePage />} />
        <Route path="favorites" element={<FavoritesPage />} />
        <Route path="trips" element={<TripsPage />} />
        <Route path="trips/:tripId" element={<TripDetailsPage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Route>
  </Routes>;
}
