import React from "react";
import { Link } from "react-router-dom";
import {
  ArrowUpRight,
  Building2,
  CircleUserRound,
  ShieldCheck,
  UsersRound,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { PageHeader, Panel } from "../components/UI";
export default function HomePage() {
  const { user, hasRole } = useAuth();
  const managed = hasRole("BQT") || hasRole("ACCOUNTANT");
  const links = [
    ...(managed
      ? [
          {
            to: "/ho-gia-dinh",
            title: "Hộ gia đình",
            desc: "Tra cứu, xem thông tin hộ và chủ hộ.",
            icon: Building2,
          },
          {
            to: "/nhan-khau",
            title: "Nhân khẩu",
            desc: "Tra cứu trạng thái cư trú và thông tin thành viên.",
            icon: UsersRound,
          },
        ]
      : []),
    ...(hasRole("BQT")
      ? [
          {
            to: "/tai-khoan",
            title: "Tài khoản & phân quyền",
            desc: "Cấp và quản lý quyền truy cập hệ thống.",
            icon: ShieldCheck,
          },
        ]
      : []),
    {
      to: "/ho-so",
      title: "Hồ sơ cá nhân",
      desc: "Cập nhật thông tin và đổi mật khẩu.",
      icon: CircleUserRound,
    },
  ];
  return (
    <>
      <PageHeader
        eyebrow="TỔNG QUAN"
        title={`Xin chào, ${user?.fullName || user?.username}!`}
        description="Chọn chức năng phù hợp với vai trò của bạn để bắt đầu làm việc."
      />
      <div className="hero-strip">
        <span>BLUE MOON · SPRINT 1</span>
        <h2>Mọi thông tin cần thiết, trong một giao diện thống nhất.</h2>
        <p>Tra cứu thông tin cư dân và cập nhật hồ sơ theo quyền được cấp.</p>
      </div>
      <Panel title="Truy cập nhanh">
        <div className="quick-grid">
          {links.map(({ to, title, desc, icon: Icon }) => (
            <Link key={to} to={to} className="quick-card">
              <div className="quick-icon">
                <Icon size={23} />
              </div>
              <strong>{title}</strong>
              <p>{desc}</p>
              <ArrowUpRight size={18} className="quick-arrow" />
            </Link>
          ))}
        </div>
      </Panel>
      {!managed && (
        <p className="info-inline">
          Bạn có thể xem, cập nhật hồ sơ cá nhân và đổi mật khẩu tại mục Hồ sơ
          cá nhân.
        </p>
      )}
    </>
  );
}
