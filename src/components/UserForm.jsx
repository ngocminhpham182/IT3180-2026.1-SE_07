import React, { useEffect, useState } from "react";
import { residentApi, userApi } from "../api/services";
import { validateUser } from "../utils/validation";
import { roleLabel } from "../utils/format";
import { Alert, Field, Modal, SaveButton, TextInput } from "./UI";
const blank = {
  username: "",
  password: "",
  fullName: "",
  email: "",
  phone: "",
  roles: [],
  residentId: null,
};
export function RoleChoices({
  roles,
  value,
  onChange,
  error,
  disabledCodes = [],
}) {
  return (
    <div className="field field-wide">
      <span className="field-title">
        Vai trò <span className="required">*</span>
      </span>
      <div className="role-choices">
        {roles.map((role) => (
          <label
            key={role.code}
            className={`role-option ${value.includes(role.code) ? "role-selected" : ""}`}
          >
            <input
              type="checkbox"
              disabled={disabledCodes.includes(role.code)}
              checked={value.includes(role.code)}
              onChange={(e) =>
                onChange(
                  e.target.checked
                    ? [...value, role.code]
                    : value.filter((r) => r !== role.code),
                )
              }
            />
            <span>{role.name || roleLabel[role.code] || role.code}</span>
          </label>
        ))}
      </div>
      {error && (
        <span className="field-error" role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
export default function UserForm({ roles, onClose, onSaved }) {
  const [f, setF] = useState({ ...blank });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [searchError, setSearchError] = useState(null);
  const [search, setSearch] = useState("");
  const [candidates, setCandidates] = useState([]);
  const [searching, setSearching] = useState(false);
  const [selectedResident, setSelectedResident] = useState(null);
  const set = (k, v) => {
    setF((p) => ({ ...p, [k]: v }));
    setErrors((p) => ({ ...p, [k]: undefined }));
  };
  useEffect(() => {
    if (!f.roles.includes("RESIDENT")) {
      setCandidates([]);
      setSearching(false);
      return;
    }
    if (search.trim().length < 2) {
      setCandidates([]);
      setSearching(false);
      return;
    }
    let cancelled = false;
    setSearching(true);
    setSearchError(null);
    const timer = setTimeout(
      () =>
        residentApi
          .list({
            keyword: search.trim(),
            status: "RESIDING",
            page: 1,
            size: 10,
          })
          .then((d) => {
            if (!cancelled) setCandidates(d.content || []);
          })
          .catch((err) => {
            if (!cancelled) {
              setCandidates([]);
              setSearchError(err);
            }
          })
          .finally(() => {
            if (!cancelled) setSearching(false);
          }),
      300,
    );
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [search, f.roles.includes("RESIDENT")]);
  const submit = async (e) => {
    e.preventDefault();
    if (busy) return;
    const validation = validateUser(f);
    setErrors(validation);
    setMessage("");
    if (Object.keys(validation).length) return;
    setBusy(true);
    try {
      const saved = await userApi.create({
        username: f.username.trim(),
        password: f.password,
        fullName: f.fullName.trim(),
        email: f.email.trim() || null,
        phone: f.phone.trim() || null,
        roles: f.roles,
        residentId: f.roles.includes("RESIDENT") ? Number(f.residentId) : null,
      });
      onSaved(saved);
    } catch (err) {
      setErrors(err.fields || {});
      setMessage(err.message);
    } finally {
      setBusy(false);
    }
  };
  return (
    <Modal
      busy={busy}
      title="Cấp tài khoản mới"
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose} disabled={busy}>
            Hủy
          </button>
          <SaveButton form="user-form" busy={busy}>
            Tạo tài khoản
          </SaveButton>
        </>
      }
    >
      <form id="user-form" noValidate onSubmit={submit}>
        {message && <Alert>{message}</Alert>}
        <p className="form-caption">Thông tin đăng nhập</p>
        <div className="form-grid">
          <Field
            label="Tên đăng nhập"
            name="username"
            error={errors.username}
            required
          >
            <TextInput
              name="username"
              autoComplete="off"
              value={f.username}
              onChange={(e) => set("username", e.target.value)}
              placeholder="VD: kt01"
            />
          </Field>
          <Field
            label="Mật khẩu ban đầu"
            name="password"
            error={errors.password}
            required
          >
            <TextInput
              name="password"
              type="password"
              autoComplete="new-password"
              value={f.password}
              onChange={(e) => set("password", e.target.value)}
              placeholder="Ít nhất 8 ký tự"
            />
          </Field>
        </div>
        <p className="form-caption form-section">Thông tin người dùng</p>
        <div className="form-grid">
          <Field
            label="Họ và tên"
            name="fullName"
            error={errors.fullName}
            required
            wide
          >
            <TextInput
              name="fullName"
              value={f.fullName}
              onChange={(e) => set("fullName", e.target.value)}
              placeholder="Họ tên đầy đủ"
            />
          </Field>
          <Field label="Email" name="email" error={errors.email}>
            <TextInput
              name="email"
              type="email"
              value={f.email}
              onChange={(e) => set("email", e.target.value)}
              placeholder="email@example.com"
            />
          </Field>
          <Field label="Điện thoại" name="phone" error={errors.phone}>
            <TextInput
              name="phone"
              value={f.phone}
              onChange={(e) => set("phone", e.target.value)}
              placeholder="0901234567"
            />
          </Field>
          <RoleChoices
            roles={roles}
            value={f.roles}
            onChange={(v) => set("roles", v)}
            error={errors.roles}
          />
          {f.roles.includes("RESIDENT") && (
            <div className="field field-wide">
              <Field
                label="Liên kết nhân khẩu đang cư trú"
                name="residentSearch"
                error={errors.residentId}
                required
                note="Tìm nhân khẩu đang cư trú theo họ tên."
              >
                <TextInput
                  name="residentSearch"
                  placeholder="Nhập ít nhất 2 ký tự để tìm"
                  value={search}
                  onChange={(e) => {
                    setSearch(e.target.value);
                    setSelectedResident(null);
                    set("residentId", null);
                  }}
                />
              </Field>
              {searchError && <Alert>{searchError.message}</Alert>}
              {selectedResident && (
                <div className="selected-person">
                  Đã chọn: <strong>{selectedResident.fullName}</strong> ·{" "}
                  {selectedResident.apartmentNo}
                </div>
              )}
              {!selectedResident && search.length >= 2 && (
                <div className="suggestions" aria-label="Kết quả tìm nhân khẩu">
                  {searching ? (
                    <span>Đang tìm...</span>
                  ) : candidates.length ? (
                    candidates.map((r) => (
                      <button
                        type="button"
                        key={r.id}
                        onClick={() => {
                          set("residentId", r.id);
                          setSelectedResident(r);
                          setSearch(r.fullName);
                          setCandidates([]);
                        }}
                      >
                        {r.fullName}{" "}
                        <small>
                          {r.householdCode} · {r.apartmentNo}
                        </small>
                      </button>
                    ))
                  ) : (
                    <span>Không tìm thấy nhân khẩu đang cư trú phù hợp.</span>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
        <p className="info-inline">
          Mật khẩu ban đầu không được hiển thị lại sau khi tạo tài khoản. Cấp
          quyền cần đúng phạm vi công việc của người dùng.
        </p>
      </form>
    </Modal>
  );
}
