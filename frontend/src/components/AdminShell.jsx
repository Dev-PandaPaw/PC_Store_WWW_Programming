import React from "react";
import { Link, Navigate, NavLink, Outlet, useLocation } from "react-router-dom";
import { ArrowUpRight, Box, LogOut } from "lucide-react";
import { useAuth } from "../contexts/authContextStore";
import useAdminAccess from "../hooks/useAdminAccess";
import { ADMIN_HOME, hasAdminRole } from "../utils/authorization";
import "../styles/admin-lists.css";

export default function AdminShell() {
  const { user, loading, logout } = useAuth();
  const location = useLocation();
  const access = useAdminAccess(
    hasAdminRole(user) || (!user && import.meta.env.DEV),
  );

  if (loading || access.loading)
    return (
      <main className="orders-page" role="status">
        Đang kiểm tra quyền quản trị…
      </main>
    );
  if (!access.account) {
    if (!user)
      return (
        <Navigate
          to="/login"
          replace
          state={{ from: location.pathname + location.search }}
        />
      );
    return (
      <main className="orders-page">
        <h1>Quản trị cửa hàng</h1>
        <p role="alert">
          {access.error || "Tài khoản của bạn không có quyền quản trị."}
        </p>
        <p>
          <Link to="/">Về cửa hàng</Link>
        </p>
      </main>
    );
  }

  return (
    <div className="admin-shell">
      <header className="admin-shell__header">
        <div className="admin-shell__inner">
          <Link to={ADMIN_HOME} className="admin-shell__brand">
            <span className="admin-shell__brand-mark">
              <Box size={18} />
            </span>
            <span>
              PixelCore <small>ADMIN</small>
            </span>
          </Link>
          <nav className="admin-shell__nav" aria-label="Điều hướng quản trị">
            <NavLink
              to={ADMIN_HOME}
              className={({ isActive }) => (isActive ? "is-active" : "")}
            >
              Sản phẩm
            </NavLink>
            <NavLink
              to="/admin/orders"
              className={({ isActive }) => (isActive ? "is-active" : "")}
            >
              Đơn hàng
            </NavLink>
            <NavLink
              to="/admin/users"
              className={({ isActive }) => (isActive ? "is-active" : "")}
            >
              Tài khoản
            </NavLink>
          </nav>
          <div className="admin-shell__account">
            <span className="admin-shell__user">
              <i />
              {user?.fullName || user?.username || access.account.name || "Quản trị viên"}
            </span>
            <Link to="/" className="admin-shell__store-link">
              Cửa hàng <ArrowUpRight size={14} />
            </Link>
            <button type="button" onClick={logout} aria-label="Đăng xuất">
              <LogOut size={16} />
            </button>
          </div>
        </div>
      </header>
      <main className="admin-shell__main">
        <Outlet />
      </main>
    </div>
  );
}
