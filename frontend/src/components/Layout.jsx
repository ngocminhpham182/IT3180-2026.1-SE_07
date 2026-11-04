import React, { useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  Building2,
  DoorOpen,
  House,
  KeyRound,
  LayoutDashboard,
  Menu,
  ShieldCheck,
  UserRound,
  UsersRound,
  X,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { firstRoleName } from "../utils/format";
export default function Layout() {
  const { user, signOut, hasRole } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const navigate = useNavigate();
  const managed = hasRole("BQT") || hasRole("ACCOUNTANT");
  const items = [
    { to: "/", label: "Trang chủ", icon: LayoutDashboard },
    ...(managed
      ? [
          { to: "/ho-gia-dinh", label: "Hộ gia đình", icon: Building2 },
          { to: "/nhan-khau", label: "Nhân khẩu", icon: UsersRound },
        ]
      : []),
    ...(hasRole("BQT")
      ? [
          {
            to: "/tai-khoan",
            label: "Tài khoản & phân quyền",
            icon: ShieldCheck,
          },
        ]
      : []),
    { to: "/ho-so", label: "Hồ sơ cá nhân", icon: UserRound },
  ];
  const logout = () => {
    signOut();
    navigate("/dang-nhap", { replace: true });
  };
  return (
    <div className="app-frame">
      <aside className={`sidebar ${mobileOpen ? "sidebar-open" : ""}`}>
        <div className="brand-row">
          <div className="brand-icon">
            <House size={21} />
          </div>
          <div>
            <strong>BlueMoon</strong>
            <span>QUẢN LÝ CHUNG CƯ</span>
          </div>
          <button
            className="icon-btn sidebar-close"
            onClick={() => setMobileOpen(false)}
            aria-label="Đóng menu"
          >
            <X />
          </button>
        </div>
        <div className="side-group-label">KHÔNG GIAN LÀM VIỆC</div>
        <nav aria-label="Điều hướng chính">
          {items.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === "/"}
              onClick={() => setMobileOpen(false)}
              className={({ isActive }) =>
                `nav-item ${isActive ? "nav-active" : ""}`
              }
            >
              <Icon size={19} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-spacer" />
        <div className="sidebar-bottom">
          <div className="sidebar-note">
            <KeyRound size={17} />
            <span>Sử dụng tài khoản đúng quyền được cấp.</span>
          </div>
          <button onClick={logout} className="nav-item signout">
            <DoorOpen size={19} /> Đăng xuất
          </button>
        </div>
      </aside>
      {mobileOpen && (
        <button
          className="overlay-sidebar"
          onClick={() => setMobileOpen(false)}
          aria-label="Đóng menu"
        />
      )}
      <main className="main-area">
        <header className="topbar">
          <div className="topbar-left">
            <button
              className="icon-btn menu-toggle"
              onClick={() => setMobileOpen(true)}
              aria-label="Mở menu"
            >
              <Menu />
            </button>
            <span className="crumb">Hệ thống quản lý cư dân</span>
          </div>
          <Link className="current-user" to="/ho-so">
            <span className="avatar">
              {(user?.fullName || "?").trim().slice(0, 1).toUpperCase()}
            </span>
            <span>
              <strong>{user?.fullName || user?.username}</strong>
              <small>{firstRoleName(user?.roles)}</small>
            </span>
          </Link>
        </header>
        <div className="content-wrap">
          <Outlet />
        </div>
        <footer className="footer">BlueMoon · Quản lý chung cư</footer>
      </main>
    </div>
  );
}
