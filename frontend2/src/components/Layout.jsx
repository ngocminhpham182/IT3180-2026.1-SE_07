import React, { useEffect, useState } from "react";
import {
  Link,
  NavLink,
  Outlet,
  useLocation,
  useNavigate,
} from "react-router-dom";
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
  Search,
  Receipt,
  CalendarDays,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { firstRoleName } from "../utils/format";
import { requestApi } from "../api/services";
export default function Layout() {
  const { user, signOut, hasRole } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const [pendingCount, setPendingCount] = useState(null);
  const isManager = hasRole("BQT");
  useEffect(() => {
    if (!isManager) {
      setPendingCount(null);
      return;
    }
    let controller;
    const refresh = () => {
      controller?.abort();
      const current = new AbortController();
      controller = current;
      requestApi
        .list({ status: "PENDING", size: 1 }, { signal: current.signal })
        .then((result) => {
          if (!current.signal.aborted) setPendingCount(result.totalElements);
        })
        .catch(() => {
          if (!current.signal.aborted) setPendingCount(null);
        });
    };
    refresh();
    window.addEventListener("bluemoon:requests-changed", refresh);
    return () => {
      controller?.abort();
      window.removeEventListener("bluemoon:requests-changed", refresh);
    };
  }, [isManager, user?.id, location.pathname]);
  const managed = hasRole("BQT") || hasRole("ACCOUNTANT");
  const items = [
    { to: "/", label: "Trang chủ", icon: LayoutDashboard },
    ...(hasRole("RESIDENT")
      ? [
          { to: "/ho-cua-toi", label: "Hộ của tôi", icon: House },
          { to: "/de-nghi", label: "Đề nghị của tôi", icon: Receipt },
        ]
      : []),
    ...(isManager
      ? [
          {
            to: "/duyet-de-nghi",
            label: "Duyệt đề nghị",
            icon: ShieldCheck,
            count: pendingCount,
          },
        ]
      : []),
    ...(managed
      ? [
          { to: "/ho-gia-dinh", label: "Hộ gia đình", icon: Building2 },
          { to: "/nhan-khau", label: "Nhân khẩu", icon: UsersRound },
          { to: "/tim-kiem", label: "Tìm kiếm nhanh", icon: Search },
          { to: "/khoan-thu", label: "Danh mục khoản thu", icon: Receipt },
          { to: "/ky-thu", label: "Kỳ thu", icon: CalendarDays },
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
          {items.map(({ to, label, icon: Icon, count }) => (
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
              {count != null && (
                <span
                  className="badge badge-amber"
                  aria-label={`${count} đề nghị chờ duyệt`}
                >
                  {count}
                </span>
              )}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-spacer" />
        <div className="sidebar-bottom">
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
