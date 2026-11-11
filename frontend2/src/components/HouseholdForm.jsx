import React, { useState } from "react";
import { householdApi } from "../api/services";
import { validateHousehold } from "../utils/validation";
import { todayLocal } from "../utils/format";
import { Alert, Field, Modal, SaveButton, SelectInput, TextInput } from "./UI";
const blank = {
  householdCode: "",
  apartmentNo: "",
  floor: "",
  area: "",
  head: {
    fullName: "",
    dateOfBirth: "",
    gender: "",
    identityNumber: "",
    phone: "",
  },
};
export default function HouseholdForm({ value, onClose, onSaved }) {
  const creating = !value;
  const [f, setF] = useState(() =>
    value
      ? {
          householdCode: value.householdCode || "",
          apartmentNo: value.apartmentNo || "",
          floor: value.floor ?? "",
          area: value.area ?? "",
          head: blank.head,
        }
      : structuredClone(blank),
  );
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const set = (key, val) => {
    setF((p) => ({ ...p, [key]: val }));
    setErrors((p) => ({ ...p, [key]: undefined }));
  };
  const setHead = (key, val) => {
    setF((p) => ({ ...p, head: { ...p.head, [key]: val } }));
    setErrors((p) => ({ ...p, [`head.${key}`]: undefined }));
  };
  const submit = async (e) => {
    e.preventDefault();
    if (busy) return;
    const validation = validateHousehold(f, creating);
    setErrors(validation);
    setMessage("");
    if (Object.keys(validation).length) return;
    setBusy(true);
    const common = {
      householdCode: f.householdCode.trim().toUpperCase(),
      apartmentNo: f.apartmentNo.trim().toUpperCase(),
      floor: f.floor === "" ? null : Number(f.floor),
      area: Number(f.area),
    };
    try {
      const data = creating
        ? await householdApi.create({
            ...common,
            head: {
              fullName: f.head.fullName.trim(),
              dateOfBirth: f.head.dateOfBirth,
              gender: f.head.gender || null,
              identityNumber: f.head.identityNumber.trim() || null,
              phone: f.head.phone.trim() || null,
            },
          })
        : await householdApi.update(value.id, common);
      onSaved(data);
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
      title={
        creating ? "Thêm hộ gia đình" : `Chỉnh sửa hộ ${value.householdCode}`
      }
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose} disabled={busy}>
            Hủy
          </button>
          <SaveButton busy={busy} form="household-form" />
        </>
      }
    >
      <form id="household-form" onSubmit={submit} noValidate>
        <p className="form-caption">Thông tin hộ gia đình</p>
        {message && <Alert>{message}</Alert>}
        <div className="form-grid">
          <Field
            label="Mã hộ khẩu"
            name="householdCode"
            error={errors.householdCode}
            required
          >
            <TextInput
              name="householdCode"
              value={f.householdCode}
              onChange={(e) => set("householdCode", e.target.value)}
              placeholder="H001"
            />
          </Field>
          <Field
            label="Số căn hộ"
            name="apartmentNo"
            error={errors.apartmentNo}
            required
          >
            <TextInput
              name="apartmentNo"
              value={f.apartmentNo}
              onChange={(e) => set("apartmentNo", e.target.value)}
              placeholder="A101"
            />
          </Field>
          <Field label="Tầng" name="floor" error={errors.floor}>
            <TextInput
              name="floor"
              type="number"
              min="1"
              max="60"
              value={f.floor}
              onChange={(e) => set("floor", e.target.value)}
              placeholder="1"
            />
          </Field>
          <Field
            label="Diện tích (m²)"
            name="area"
            error={errors.area}
            required
          >
            <TextInput
              name="area"
              type="number"
              min="0.01"
              max="1000"
              step="0.01"
              value={f.area}
              onChange={(e) => set("area", e.target.value)}
              placeholder="75.50"
            />
          </Field>
        </div>
        {creating && (
          <>
            <p className="form-caption form-section">
              Thông tin chủ hộ{" "}
              <span className="caption-helper">
                Tạo cùng hộ trong một lần lưu
              </span>
            </p>
            <div className="form-grid">
              <Field
                label="Họ và tên chủ hộ"
                name="head.fullName"
                error={errors["head.fullName"]}
                required
                wide
              >
                <TextInput
                  name="head.fullName"
                  value={f.head.fullName}
                  onChange={(e) => setHead("fullName", e.target.value)}
                  placeholder="Nguyễn Văn An"
                />
              </Field>
              <Field
                label="Ngày sinh"
                name="head.dateOfBirth"
                error={errors["head.dateOfBirth"]}
                required
              >
                <TextInput
                  name="head.dateOfBirth"
                  type="date"
                  value={f.head.dateOfBirth}
                  max={todayLocal()}
                  onChange={(e) => setHead("dateOfBirth", e.target.value)}
                />
              </Field>
              <Field
                label="Giới tính"
                name="head.gender"
                error={errors["head.gender"]}
              >
                <SelectInput
                  name="head.gender"
                  value={f.head.gender}
                  onChange={(e) => setHead("gender", e.target.value)}
                >
                  <option value="">Không xác định</option>
                  <option value="MALE">Nam</option>
                  <option value="FEMALE">Nữ</option>
                  <option value="OTHER">Khác</option>
                </SelectInput>
              </Field>
              <Field
                label="CCCD/CMND"
                name="head.identityNumber"
                error={errors["head.identityNumber"]}
              >
                <TextInput
                  name="head.identityNumber"
                  value={f.head.identityNumber}
                  onChange={(e) => setHead("identityNumber", e.target.value)}
                  placeholder="9 hoặc 12 chữ số"
                />
              </Field>
              <Field
                label="Số điện thoại"
                name="head.phone"
                error={errors["head.phone"]}
              >
                <TextInput
                  name="head.phone"
                  value={f.head.phone}
                  onChange={(e) => setHead("phone", e.target.value)}
                  placeholder="0901234567"
                />
              </Field>
            </div>
          </>
        )}
      </form>
    </Modal>
  );
}
