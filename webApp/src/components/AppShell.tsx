import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../state/AuthContext";

export function AppShell() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  return <>
    <header className="site-header">
      <NavLink to="/destinations" className="brand">TourVerse</NavLink>
      <nav aria-label="Primary navigation">
        <NavLink to="/destinations">Destinations</NavLink>
        {user ? <>
          <NavLink to="/profile">Profile</NavLink>
          <NavLink to="/favorites">Favorites</NavLink>
          <NavLink to="/trips">My trips</NavLink>
          {user.role === "ADMIN" && <NavLink to="/admin/destination-imports">Catalogue admin</NavLink>}
          <button className="nav-button" onClick={() => logout().finally(() => navigate("/login"))}>Logout</button>
        </> : <>
          <NavLink to="/login">Login</NavLink>
          <NavLink to="/register">Register</NavLink>
        </>}
      </nav>
    </header>
    <main><Outlet /></main>
    <footer><p>TourVerse · Kotlin, Ktor, React and TypeScript</p></footer>
  </>;
}
