import React from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { useAuth, homeForRoles } from "./auth/AuthContext";
import Layout from "./components/Layout";
import LoginPage from "./pages/LoginPage";
import HomePage from "./pages/HomePage";
import HouseholdsPage from "./pages/HouseholdsPage";
import HouseholdDetailPage from "./pages/HouseholdDetailPage";
import ResidentsPage from "./pages/ResidentsPage";
import ResidentDetailPage from "./pages/ResidentDetailPage";
import UsersPage from "./pages/UsersPage";
import ProfilePage from "./pages/ProfilePage";
function Protected({ roles, children }) {
  const { token, user, loading } = useAuth();
  const location = useLocation();
  if (!token)
    return <Navigate to="/dang-nhap" state={{ from: location }} replace />;
  if (loading || !user)
    return <div className="boot-loading">Đang xác thực phiên đăng nhập...</div>;
  if (roles && !roles.some((r) => user.roles?.includes(r)))
    return (
      <div className="forbidden">
        <h1>Không có quyền truy cập</h1>
        <p>Tài khoản của bạn không có quyền sử dụng chức năng này.</p>
        <a href="/">Quay về trang chủ</a>
      </div>
    );
  return children;
}
export default function App() {
  const { token, user } = useAuth();
  return (
    <Routes>
      <Route
        path="/dang-nhap"
        element={
          token && user ? (
            <Navigate replace to={homeForRoles(user.roles)} />
          ) : (
            <LoginPage />
          )
        }
      />
      <Route
        element={
          <Protected>
            <Layout />
          </Protected>
        }
      >
        <Route index element={<HomePage />} />
        <Route path="ho-so" element={<ProfilePage />} />
        <Route
          path="ho-gia-dinh"
          element={
            <Protected roles={["BQT", "ACCOUNTANT"]}>
              <HouseholdsPage />
            </Protected>
          }
        />
        <Route
          path="ho-gia-dinh/:id"
          element={
            <Protected roles={["BQT", "ACCOUNTANT"]}>
              <HouseholdDetailPage />
            </Protected>
          }
        />
        <Route
          path="nhan-khau"
          element={
            <Protected roles={["BQT", "ACCOUNTANT"]}>
              <ResidentsPage />
            </Protected>
          }
        />
        <Route
          path="nhan-khau/:id"
          element={
            <Protected roles={["BQT", "ACCOUNTANT"]}>
              <ResidentDetailPage />
            </Protected>
          }
        />
        <Route
          path="tai-khoan"
          element={
            <Protected roles={["BQT"]}>
              <UsersPage />
            </Protected>
          }
        />
      </Route>
      <Route
        path="*"
        element={<Navigate replace to={token ? "/" : "/dang-nhap"} />}
      />
    </Routes>
  );
}
