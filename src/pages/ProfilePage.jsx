import React, { useEffect, useState } from "react";
import { Eye, EyeOff, KeyRound, Save, UserRound } from "lucide-react";
import { authApi } from "../api/services";
import { useAuth } from "../auth/AuthContext";
import {
  Alert,
  DataLine,
  Field,
  LoadState,
  PageHeader,
  Panel,
  StatusBadge,
  TextInput,
} from "../components/UI";
import { validatePassword, validateProfile } from "../utils/validation";
import { firstRoleName, formatDateTime } from "../utils/format";
export default function ProfilePage() {
  const { user, updateUser, signOut } = useAuth();
  const [tab, setTab] = useState("info");
  const [f, setF] = useState({ fullName: "", email: "", phone: "" });
  const [password, setPassword] = useState({
    oldPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState("");
  const [busy, setBusy] = useState(false);
  const [show, setShow] = useState(false);
  useEffect(() => {
    if (user)
      setF({
        fullName: user.fullName || "",
        email: user.email || "",
        phone: user.phone || "",
      });
  }, [user]);
  const changeTab = (k) => {
    if (busy) return;
    setTab(k);
    setErrors({});
    setMessage("");
    setSuccess("");
  };
  const submitProfile = async (e) => {
    e.preventDefault();
    if (busy) return;
    const v = validateProfile(f);
    setErrors(v);
    setMessage("");
    setSuccess("");
    if (Object.keys(v).length) return;
    setBusy(true);
    try {
      const u = await authApi.updateMe({
        fullName: f.fullName.trim(),
        email: f.email.trim() || null,
        phone: f.phone.trim() || null,
      });
      updateUser(u);
      setSuccess("Đã cập nhật hồ sơ cá nhân thành công.");
    } catch (err) {
      setErrors(err.fields || {});
      setMessage(err.message);
    } finally {
      setBusy(false);
    }
  };
  const submitPass = async (e) => {
    e.preventDefault();
    if (busy) return;
    const v = validatePassword(password, user?.username || "");
    setErrors(v);
    setMessage("");
    setSuccess("");
    if (Object.keys(v).length) return;
    setBusy(true);
    try {
      await authApi.changePassword(password);
      setPassword({ oldPassword: "", newPassword: "", confirmPassword: "" });
      signOut(
        "Đổi mật khẩu thành công. Vui lòng đăng nhập lại bằng mật khẩu mới.",
      );
    } catch (err) {
      setErrors(err.fields || {});
      setMessage(err.message);
    } finally {
      setBusy(false);
    }
  };
  if (!user) return <LoadState />;
  return (
    <>
      <PageHeader
        eyebrow="US-10 · TÀI KHOẢN CỦA TÔI"
        title="Hồ sơ cá nhân"
        description="Xem và cập nhật thông tin cá nhân, bảo vệ tài khoản của bạn."
      />
      <div className="profile-top">
        <div className="profile-avatar">
          {(user.fullName || user.username || "?").charAt(0).toUpperCase()}
        </div>
        <div>
          <h2>{user.fullName}</h2>
          <p>
            @{user.username} · {firstRoleName(user.roles)}
          </p>
        </div>
        <div className="profile-top-status">
          <StatusBadge tone="green">
            Tài khoản {user.isActive ? "đang hoạt động" : "đã khóa"}
          </StatusBadge>
        </div>
      </div>
      <div className="profile-layout">
        <div className="profile-side">
          <Panel title="Thông tin tài khoản">
            <dl className="data-list">
              <DataLine label="Tên đăng nhập">{user.username}</DataLine>
              <DataLine label="Phân quyền">
                {firstRoleName(user.roles)}
              </DataLine>
              <DataLine label="Lần đăng nhập gần nhất">
                {formatDateTime(user.lastLoginAt)}
              </DataLine>
              <DataLine label="Ngày tạo tài khoản">
                {formatDateTime(user.createdAt)}
              </DataLine>
            </dl>
            <p className="info-inline">
              Tên đăng nhập và vai trò do Ban quản trị quản lý, không được tự
              sửa.
            </p>
          </Panel>
        </div>
        <div className="profile-main">
          <div className="tabs">
            <button
              disabled={busy}
              className={tab === "info" ? "tab-active" : ""}
              onClick={() => changeTab("info")}
            >
              <UserRound size={17} /> Thông tin cá nhân
            </button>
            <button
              disabled={busy}
              className={tab === "password" ? "tab-active" : ""}
              onClick={() => changeTab("password")}
            >
              <KeyRound size={17} /> Đổi mật khẩu
            </button>
          </div>
          <section className="panel profile-edit">
            {message && <Alert>{message}</Alert>}
            {success && <Alert tone="success">{success}</Alert>}
            {tab === "info" ? (
              <form onSubmit={submitProfile} noValidate>
                <h2>Chỉnh sửa hồ sơ</h2>
                <p className="subtle">
                  Chỉ những trường bên dưới được phép chỉnh sửa.
                </p>
                <div className="form-grid">
                  <Field
                    label="Họ và tên"
                    name="fullName"
                    error={errors.fullName}
                    wide
                    required
                  >
                    <TextInput
                      name="fullName"
                      value={f.fullName}
                      onChange={(e) =>
                        setF((p) => ({ ...p, fullName: e.target.value }))
                      }
                    />
                  </Field>
                  <Field label="Email" name="email" error={errors.email}>
                    <TextInput
                      name="email"
                      type="email"
                      value={f.email}
                      onChange={(e) =>
                        setF((p) => ({ ...p, email: e.target.value }))
                      }
                      placeholder="Chưa cập nhật"
                    />
                  </Field>
                  <Field
                    label="Số điện thoại"
                    name="phone"
                    error={errors.phone}
                  >
                    <TextInput
                      name="phone"
                      value={f.phone}
                      onChange={(e) =>
                        setF((p) => ({ ...p, phone: e.target.value }))
                      }
                      placeholder="Chưa cập nhật"
                    />
                  </Field>
                </div>
                <button
                  disabled={busy}
                  type="submit"
                  className="btn btn-primary"
                >
                  <Save size={17} />
                  {busy ? "Đang lưu..." : "Lưu thay đổi"}
                </button>
              </form>
            ) : (
              <form onSubmit={submitPass} noValidate>
                <h2>Đổi mật khẩu</h2>
                <p className="subtle">
                  Sau khi đổi thành công, hệ thống tự đăng xuất để bảo vệ phiên
                  truy cập.
                </p>
                {["oldPassword", "newPassword", "confirmPassword"].map(
                  (k, i) => (
                    <Field
                      key={k}
                      label={
                        [
                          "Mật khẩu hiện tại",
                          "Mật khẩu mới",
                          "Xác nhận mật khẩu mới",
                        ][i]
                      }
                      name={k}
                      error={errors[k]}
                      required
                    >
                      <div className="password-wrap">
                        <TextInput
                          error={errors[k]}
                          aria-describedby={
                            errors[k] ? `${k}-error` : undefined
                          }
                          name={k}
                          autoComplete={
                            i === 0 ? "current-password" : "new-password"
                          }
                          type={show ? "text" : "password"}
                          value={password[k]}
                          onChange={(e) =>
                            setPassword((p) => ({ ...p, [k]: e.target.value }))
                          }
                        />
                        <button
                          type="button"
                          aria-label={show ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                          onClick={() => setShow((p) => !p)}
                        >
                          {show ? <EyeOff size={18} /> : <Eye size={18} />}
                        </button>
                      </div>
                    </Field>
                  ),
                )}
                <button
                  disabled={busy}
                  type="submit"
                  className="btn btn-primary"
                >
                  <KeyRound size={17} />
                  {busy ? "Đang đổi..." : "Xác nhận đổi mật khẩu"}
                </button>
              </form>
            )}
          </section>
        </div>
      </div>
    </>
  );
}
