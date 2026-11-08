import React, { useState } from "react";
import { householdApi, residentApi } from "../api/services";
import { todayLocal } from "../utils/format";
import { validateResident } from "../utils/validation";
import { Alert, Field, Modal, SaveButton, SelectInput, TextInput } from "./UI";
const blank = {
  fullName: "",
  dateOfBirth: "",
  gender: "",
  identityNumber: "",
  phone: "",
  relationshipToHead: "",
  entryType: "MOVE_IN",
  entryDate: todayLocal(),
  startDate: "",
  endDate: "",
};
export default function ResidentForm({
  householdId,
  householdStatus,
  value,
  onClose,
  onSaved,
}) {
  const creating = !value;
  const [f, setF] = useState(() =>
    value
      ? {
          ...blank,
          ...Object.fromEntries(
            [
              "fullName",
              "dateOfBirth",
              "gender",
              "identityNumber",
              "phone",
              "relationshipToHead",
            ].map((k) => [k, value[k] ?? ""]),
          ),
        }
      : { ...blank, entryDate: todayLocal() },
  );
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const set = (key, v) => {
    setF((p) => ({ ...p, [key]: v }));
    setErrors((p) => ({ ...p, [key]: undefined }));
  };
  const submit = async (e) => {
    e.preventDefault();
    if (busy) return;
    if (creating && householdStatus === "INACTIVE") {
      setMessage("Hộ không còn hoạt động.");
      return;
    }
    const validation = validateResident(f, creating);
    setErrors(validation);
    setMessage("");
    if (Object.keys(validation).length) return;
    const payload = {
      fullName: f.fullName.trim(),
      dateOfBirth: f.dateOfBirth,
      gender: f.gender || null,
      identityNumber: f.identityNumber.trim() || null,
      phone: f.phone.trim() || null,
      relationshipToHead: f.relationshipToHead.trim(),
    };
    if (creating) {
      payload.entryType = f.entryType;
      if (f.entryType === "BIRTH") payload.entryDate = f.dateOfBirth;
      else if (f.entryDate) payload.entryDate = f.entryDate;
      if (f.entryType === "TEMPORARY_RESIDENCE") {
        delete payload.entryDate;
        payload.startDate = f.startDate;
        payload.endDate = f.endDate || null;
      }
    }
    setBusy(true);
    try {
      const saved = creating
        ? await householdApi.addResident(householdId, payload)
        : await residentApi.update(value.id, payload);
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
      title={creating ? "Thêm nhân khẩu" : `Sửa thông tin: ${value.fullName}`}
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" disabled={busy} onClick={onClose}>
            Hủy
          </button>
          <SaveButton busy={busy} form="resident-form" />
        </>
      }
    >
      <form id="resident-form" noValidate onSubmit={submit}>
        {message && <Alert>{message}</Alert>}
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
              placeholder="Nguyễn Văn A"
            />
          </Field>
          <Field
            label="Ngày sinh"
            name="dateOfBirth"
            error={errors.dateOfBirth}
            required
          >
            <TextInput
              name="dateOfBirth"
              type="date"
              value={f.dateOfBirth}
              max={todayLocal()}
              onChange={(e) => set("dateOfBirth", e.target.value)}
            />
          </Field>
          <Field label="Giới tính" name="gender" error={errors.gender}>
            <SelectInput
              name="gender"
              value={f.gender}
              onChange={(e) => set("gender", e.target.value)}
            >
              <option value="">Không xác định</option>
              <option value="MALE">Nam</option>
              <option value="FEMALE">Nữ</option>
              <option value="OTHER">Khác</option>
            </SelectInput>
          </Field>
          <Field
            label="CCCD/CMND"
            name="identityNumber"
            error={errors.identityNumber}
          >
            <TextInput
              name="identityNumber"
              value={f.identityNumber}
              onChange={(e) => set("identityNumber", e.target.value)}
              placeholder="9 hoặc 12 chữ số"
            />
          </Field>
          <Field label="Số điện thoại" name="phone" error={errors.phone}>
            <TextInput
              name="phone"
              value={f.phone}
              onChange={(e) => set("phone", e.target.value)}
              placeholder="0901234567"
            />
          </Field>
          <Field
            label="Quan hệ với chủ hộ"
            name="relationshipToHead"
            error={errors.relationshipToHead}
            required
            wide
          >
            <TextInput
              name="relationshipToHead"
              value={f.relationshipToHead}
              onChange={(e) => set("relationshipToHead", e.target.value)}
              placeholder="VD: Con, Vợ, Bố, Mẹ..."
            />
          </Field>
          {creating && (
            <>
              <Field
                label="Hình thức vào hộ"
                name="entryType"
                error={errors.entryType}
                required
              >
                <SelectInput
                  name="entryType"
                  value={f.entryType}
                  onChange={(e) => set("entryType", e.target.value)}
                >
                  <option value="MOVE_IN">Chuyển đến</option>
                  <option value="BIRTH">Khai sinh</option>
                  <option value="TEMPORARY_RESIDENCE">Tạm trú</option>
                  <option value="EXISTING">Nhập dữ liệu có sẵn</option>
                </SelectInput>
              </Field>
              {f.entryType !== "TEMPORARY_RESIDENCE" &&
                f.entryType !== "BIRTH" && (
                  <Field
                    label="Ngày vào hộ"
                    name="entryDate"
                    error={errors.entryDate}
                  >
                    <TextInput
                      name="entryDate"
                      type="date"
                      max={todayLocal()}
                      value={f.entryDate}
                      onChange={(e) => set("entryDate", e.target.value)}
                    />
                  </Field>
                )}
              {f.entryType === "TEMPORARY_RESIDENCE" && (
                <>
                  <Field
                    label="Bắt đầu tạm trú"
                    name="startDate"
                    error={errors.startDate}
                    required
                  >
                    <TextInput
                      name="startDate"
                      type="date"
                      value={f.startDate}
                      onChange={(e) => set("startDate", e.target.value)}
                    />
                  </Field>
                  <Field
                    label="Kết thúc tạm trú"
                    name="endDate"
                    error={errors.endDate}
                  >
                    <TextInput
                      name="endDate"
                      type="date"
                      value={f.endDate}
                      min={f.startDate || undefined}
                      onChange={(e) => set("endDate", e.target.value)}
                    />
                  </Field>
                </>
              )}
            </>
          )}
        </div>
        {!creating && (
          <p className="info-inline">
            Biểu mẫu này cập nhật thông tin cá nhân. Để đổi chủ hộ, hãy mở hồ sơ
            hộ gia đình.
          </p>
        )}
      </form>
    </Modal>
  );
}
