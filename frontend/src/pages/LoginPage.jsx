import React, { useEffect, useState } from "react";
import {
  Eye,
  EyeOff,
  KeyRound,
  LockKeyhole,
  MoonStar,
  ShieldCheck,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuth, homeForRoles } from "../auth/AuthContext";
import { Alert, Field, TextInput } from "../components/UI";
import { ApiError } from "../api/http";
export default function LoginPage() {
  const { login, notice, setNotice } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: "", password: "" });
  const [showPass, setShowPass] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [seconds, setSeconds] = useState(0);
  const [lockedUsername, setLockedUsername] = useState("");
  const locked =
    seconds > 0 && form.username.trim().toLowerCase() === lockedUsername;
  useEffect(() => {
    if (!seconds) return;
    const timer = setInterval(
      () => setSeconds((s) => Math.max(s - 1, 0)),
      1000,
    );
    return () => clearInterval(timer);
  }, [seconds > 0]);
  const submit = async (e) => {
    e.preventDefault();
    if (busy || locked) return;
    setError(null);
    if (!form.username.trim() || !form.password) {
      setError({
        message: "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.",
        fields: {
          username: !form.username.trim() ? "Nhập tên đăng nhập." : undefined,
          password: !form.password ? "Nhập mật khẩu." : undefined,
        },
      });
      return;
    }
    setBusy(true);
    try {
      const user = await login({
        username: form.username.trim(),
        password: form.password,
      });
      navigate(homeForRoles(user.roles), { replace: true });
    } catch (err) {
      const data = err instanceof ApiError ? err : { message: err.message };
      setError(data);
      if (data.code === "ACCOUNT_LOCKED") {
        setLockedUsername(form.username.trim().toLowerCase());
        setSeconds(
          Math.max(
            0,
            Number(data.details?.remainingSeconds) ||
              Math.ceil(
                (Date.parse(data.details?.lockedUntil) - Date.now()) / 1000,
              ) ||
              0,
          ),
        );
      }
    } finally {
      setBusy(false);
    }
  };
  const timeText = `${Math.floor(seconds / 60)
    .toString()
    .padStart(2, "0")}:${(seconds % 60).toString().padStart(2, "0")}`;
  return (
    <div className="login-shell">
      <section className="login-intro">
        <div className="login-logo">
          <MoonStar size={26} /> BlueMoon
        </div>
        <div className="login-welcome">
          <span className="login-kicker">CỔNG QUẢN TRỊ CHUNG CƯ</span>
          <h1>
            Quản lý cư dân
            <br />
            <em>rõ ràng hơn.</em>
          </h1>
          <p>
            Tập trung thông tin hộ gia đình, nhân khẩu và tài khoản trong một
            không gian làm việc bảo mật.
          </p>
        </div>
        <div className="login-footnote">
          <ShieldCheck size={18} /> Phân quyền theo vai trò · Dữ liệu được bảo
          vệ
        </div>
      </section>
      <section className="login-card-wrap">
        <div className="login-card">
          <div className="login-card-icon">
            <LockKeyhole size={23} />
          </div>
          <h2>Đăng nhập</h2>
          <p className="subtle">Sử dụng tài khoản do Ban quản trị cấp.</p>
          {notice && (
            <Alert tone="info" onClose={() => setNotice("")}>
              {notice}
            </Alert>
          )}
          {error && (
            <Alert>
              {error.message}
              {locked && <span> · Thử lại sau {timeText}</span>}
            </Alert>
          )}
          <form onSubmit={submit} noValidate>
            <Field
              label="Tên đăng nhập"
              name="username"
              error={error?.fields?.username}
              required
            >
              <TextInput
                name="username"
                autoComplete="username"
                placeholder="VD: bqt01"
                value={form.username}
                maxLength={100}
                disabled={busy}
                onChange={(e) => {
                  setError(null);
                  setForm((f) => ({ ...f, username: e.target.value }));
                }}
              />
            </Field>
            <Field
              label="Mật khẩu"
              name="password"
              error={error?.fields?.password}
              required
            >
              <div className="password-wrap">
                <TextInput
                  error={error?.fields?.password}
                  aria-describedby={
                    error?.fields?.password ? "password-error" : undefined
                  }
                  name="password"
                  autoComplete="current-password"
                  maxLength={128}
                  disabled={busy}
                  placeholder="Nhập mật khẩu"
                  type={showPass ? "text" : "password"}
                  value={form.password}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, password: e.target.value }))
                  }
                />
                <button
                  type="button"
                  aria-label={showPass ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                  onClick={() => setShowPass((s) => !s)}
                >
                  {showPass ? <EyeOff size={19} /> : <Eye size={19} />}
                </button>
              </div>
            </Field>
            <button
              disabled={busy || locked}
              type="submit"
              className="btn btn-primary btn-block"
            >
              {busy ? "Đang đăng nhập..." : "Đăng nhập"} <KeyRound size={17} />
            </button>
          </form>
          <div className="login-support">
            Chưa có tài khoản? Liên hệ Ban quản trị chung cư.
          </div>
        </div>
        <div className="login-copyright">
          © BlueMoon · Hệ thống quản lý chung cư
        </div>
      </section>
    </div>
  );
}
