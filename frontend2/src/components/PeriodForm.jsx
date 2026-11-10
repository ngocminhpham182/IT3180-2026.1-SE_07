import React, { useState } from "react";
import { periodApi } from "../api/services";
import { Alert, Field, Modal, SaveButton, TextInput } from "./UI";
import { validatePeriod } from "../utils/supplement";
export default function PeriodForm({ value, onClose, onSaved }) {
  const [f, setF] = useState({
    code: "",
    name: "",
    startDate: "",
    endDate: "",
    ...value,
  });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  function month(v) {
    if (!/^\d{4}-\d{2}$/.test(v)) return;
    const [y, m] = v.split("-").map(Number);
    setF({
      code: v,
      name: `Tháng ${m}/${y}`,
      startDate: `${v}-01`,
      endDate: `${v}-${new Date(y, m, 0).getDate()}`,
    });
    setErrors({});
  }
  async function submit(e) {
    e.preventDefault();
    if (busy) return;
    const invalid = validatePeriod(f);
    setErrors(invalid);
    if (Object.keys(invalid).length) return;
    setBusy(true);
    setError(null);
    try {
      const body = {
        code: f.code.trim(),
        name: f.name.trim(),
        startDate: f.startDate,
        endDate: f.endDate,
      };
      onSaved(
        value
          ? await periodApi.update(value.id, body)
          : await periodApi.create(body),
      );
    } catch (e) {
      setError(e);
      setErrors(e.fields || {});
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={value ? "Chỉnh sửa kỳ thu" : "Tạo kỳ thu"}
      onClose={onClose}
      busy={busy}
      footer={<SaveButton busy={busy} form="period-form" />}
    >
      <form id="period-form" noValidate onSubmit={submit}>
        <Alert>
          {error?.message}
          {error?.code === "PERIOD_OVERLAP" &&
            ` Kỳ bị trùng: ${error.details?.overlapping?.code || ""}.`}
        </Alert>
        <fieldset className="form-fields" disabled={busy}>
          <Field name="periodMonth" label="Điền nhanh theo tháng">
            <TextInput
              name="periodMonth"
              type="month"
              onChange={(e) => month(e.target.value)}
            />
          </Field>
          <div className="form-grid">
            {[
              ["code", "Mã kỳ thu", "text"],
              ["name", "Tên kỳ thu", "text"],
              ["startDate", "Ngày bắt đầu", "date"],
              ["endDate", "Ngày kết thúc", "date"],
            ].map(([k, label, type]) => (
              <Field key={k} name={k} label={label} required error={errors[k]}>
                <TextInput
                  name={k}
                  type={type}
                  value={f[k]}
                  onChange={(e) => {
                    setF({ ...f, [k]: e.target.value });
                    setErrors({ ...errors, [k]: undefined });
                  }}
                />
              </Field>
            ))}
          </div>
        </fieldset>
      </form>
    </Modal>
  );
}
