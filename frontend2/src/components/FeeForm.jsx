import React, { useState } from "react";
import { feeApi } from "../api/services";
import { Alert, Field, Modal, SaveButton, SelectInput, TextInput } from "./UI";
import { feeBody, validateFee } from "../utils/supplement";
export default function FeeForm({ value, onClose, onSaved }) {
  const [f, setF] = useState({
    code: "",
    name: "",
    feeKind: "MANDATORY",
    calculationMethod: "PER_AREA",
    unitPrice: "",
    billingCycle: "MONTHLY",
    dueDay: "",
    ...value,
    description: value?.description || "",
  });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const set = (k, v) => {
    setF((x) => ({ ...x, [k]: v }));
    setErrors((x) => ({ ...x, [k]: undefined }));
  };
  const input = (k, label, type = "text", required = true, locked = false) => (
    <Field name={k} label={label} required={required} error={errors[k]}>
      <TextInput
        name={k}
        type={type}
        step={k === "unitPrice" ? "0.01" : undefined}
        value={f[k] ?? ""}
        disabled={locked}
        onChange={(e) => set(k, e.target.value)}
      />
    </Field>
  );
  const select = (k, label, options, locked = false) => (
    <Field name={k} label={label} required error={errors[k]}>
      <SelectInput
        name={k}
        value={f[k]}
        disabled={locked}
        onChange={(e) => set(k, e.target.value)}
      >
        {options.map(([v, l]) => (
          <option key={v} value={v}>
            {l}
          </option>
        ))}
      </SelectInput>
    </Field>
  );
  async function submit(e) {
    e.preventDefault();
    if (busy) return;
    const invalid = validateFee(f);
    setErrors(invalid);
    if (Object.keys(invalid).length) return;
    setBusy(true);
    setError(null);
    try {
      const data = feeBody(f);
      const result = value
        ? await feeApi.update(value.id, data)
        : await feeApi.create(data);
      onSaved(result);
    } catch (e) {
      setError(e);
      setErrors(e.fields || {});
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={value ? "Chỉnh sửa khoản thu" : "Thêm khoản thu"}
      busy={busy}
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" disabled={busy} onClick={onClose}>
            Hủy
          </button>
          <SaveButton form="fee-form" busy={busy} />
        </>
      }
    >
      <form id="fee-form" noValidate onSubmit={submit}>
        <Alert>
          {error?.message}
          {error?.details?.suggestedAction === "CREATE_NEW" &&
            " Hãy tạo khoản thu mới để thay đổi loại hoặc cách tính."}
        </Alert>
        {value?.inUse && (
          <p className="info-inline">
            Khoản đã được sử dụng: không đổi mã, loại, cách tính hoặc chu kỳ.
            Đơn giá mới chỉ áp dụng từ kỳ lập tiếp theo; công nợ cũ giữ nguyên.
          </p>
        )}
        <fieldset className="form-fields" disabled={busy}>
          <div className="form-grid">
            {input("code", "Mã khoản thu", "text", true, value?.inUse)}
            {input("name", "Tên khoản thu")}
            {select(
              "feeKind",
              "Loại khoản thu",
              [
                ["MANDATORY", "Bắt buộc"],
                ["VOLUNTARY", "Tự nguyện"],
              ],
              value?.inUse,
            )}
            {f.feeKind === "MANDATORY" ? (
              <>
                {select(
                  "calculationMethod",
                  "Cách tính",
                  [
                    ["PER_AREA", "Theo diện tích"],
                    ["FIXED", "Cố định mỗi hộ"],
                  ],
                  value?.inUse,
                )}
                {input("unitPrice", "Đơn giá (đ)", "number")}
                {select(
                  "billingCycle",
                  "Chu kỳ thu",
                  [
                    ["MONTHLY", "Hằng tháng"],
                    ["ONE_TIME", "Một lần"],
                  ],
                  value?.inUse,
                )}
                {input("dueDay", "Hạn nộp (ngày trong tháng)", "number")}
              </>
            ) : (
              <p className="info-inline">
                Khoản tự nguyện không sinh công nợ. Hộ không đóng sẽ không bị
                tính nợ.
              </p>
            )}
            {input("description", "Mô tả", "text", false)}
          </div>
        </fieldset>
      </form>
    </Modal>
  );
}
