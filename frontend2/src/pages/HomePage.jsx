import React from "react";
import { Link } from "react-router-dom";
import {
  ArrowUpRight,
  Building2,
  CircleUserRound,
  ShieldCheck,
  UsersRound,
  Search,
  Receipt,
  CalendarDays,
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
          {
            to: "/tim-kiem",
            title: "Tìm kiếm nhanh",
            desc: "Tìm hộ và cư dân theo tên, mã hộ hoặc căn hộ.",
            icon: Search,
          },
          {
            to: "/khoan-thu",
            title: "Danh mục khoản thu",
            desc: "Thiết lập đơn giá và tính thử phí từng hộ.",
            icon: Receipt,
          },
          {
            to: "/ky-thu",
            title: "Kỳ thu",
            desc: "Lập và kiểm tra danh sách khoản phải thu đầu kỳ.",
            icon: CalendarDays,
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
      <PageHeader title={`Xin chào, ${user?.fullName || user?.username}!`} />
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
