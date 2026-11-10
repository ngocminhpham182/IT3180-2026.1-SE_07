import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { movementApi, residentApi } from "../api/services";
import {
  Alert,
  EmptyState,
  Field,
  LoadState,
  Modal,
  PageError,
  Panel,
  SaveButton,
  SelectInput,
  TextInput,
} from "./UI";
import HouseholdPicker from "./HouseholdPicker";
import {
  allowedMovements,
  movementBody,
  movementLabels,
  validateMovement,
  vietnamToday,
} from "../utils/supplement";
import { formatDate, formatDateTime } from "../utils/format";

function MovementForm({ resident, history, onClose, onSaved, onResident }) {
  const [f, setF] = useState({
    movementType: allowedMovements(resident.residencyStatus)[0] || "",
    effectiveDate: vietnamToday(),
    startDate: vietnamToday(),
    endDate: "",
    newHouseholdId: "",
    relationshipToHead: "",
    newHeadResidentId: "",
    note: "",
  });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [candidates, setCandidates] = useState([]);
  const set = (k, v) => {
    setF((x) => ({ ...x, [k]: v }));
    setErrors((x) => ({ ...x, [k]: undefined }));
  };
  const input = (key, label, type = "text", required = true) => (
    <Field name={key} label={label} required={required} error={errors[key]}>
      <TextInput
        name={key}
        type={type}
        value={f[key]}
        onChange={(e) => set(key, e.target.value)}
      />
    </Field>
  );
  async function submit(e) {
    e.preventDefault();
    if (busy) return;
    const invalid = validateMovement(f, resident, history);
    if (candidates.length && !f.newHeadResidentId)
      invalid.newHeadResidentId = "Chọn chủ hộ mới trước khi ghi nhận.";
    setErrors(invalid);
    if (Object.keys(invalid).length) return;
    setBusy(true);
    setError(null);
    try {
      const result = await movementApi.create(resident.id, movementBody(f));
      onSaved(result.resident);
    } catch (err) {
      setError(err);
      setErrors(err.fields || {});
      if (err.code === "HEAD_REQUIRED")
        setCandidates(err.details?.candidates || []);
      if (["INVALID_TRANSITION", "RESIDENT_NOT_RESIDING"].includes(err.code)) {
        try {
          onResident(await residentApi.get(resident.id));
        } catch {
          /* Keep the original actionable error. */
        }
      }
    } finally {
      setBusy(false);
    }
  }
  const temporary = f.movementType.startsWith("TEMPORARY_");
  return (
    <Modal
      title="Ghi nhận biến động"
      onClose={onClose}
      busy={busy}
      footer={
        <>
          <button className="btn btn-ghost" disabled={busy} onClick={onClose}>
            Hủy
          </button>
          <SaveButton busy={busy} form="movement-form">
            Ghi nhận
          </SaveButton>
        </>
      }
    >
      <form id="movement-form" onSubmit={submit} noValidate>
        <Alert>{error?.message}</Alert>
        <fieldset disabled={busy} className="form-fields">
          <div className="form-grid">
            <Field
              name="movementType"
              label="Loại biến động"
              error={errors.movementType}
              required
            >
              <SelectInput
                name="movementType"
                value={f.movementType}
                onChange={(e) => {
                  set("movementType", e.target.value);
                  set("newHeadResidentId", "");
                  setCandidates([]);
                }}
              >
                <option value="">Chọn loại biến động</option>
                {allowedMovements(resident.residencyStatus).map((t) => (
                  <option key={t} value={t}>
                    {movementLabels[t]}
                  </option>
                ))}
              </SelectInput>
            </Field>
            {temporary ? (
              <>
                {input("startDate", "Ngày bắt đầu", "date")}
                {input("endDate", "Ngày kết thúc", "date", false)}
              </>
            ) : (
              input("effectiveDate", "Ngày hiệu lực", "date")
            )}
            {["TRANSFER", "MOVE_IN"].includes(f.movementType) && (
              <>
                <HouseholdPicker
                  name="newHouseholdId"
                  value={f.newHouseholdId}
                  onChange={(v) => set("newHouseholdId", v)}
                  error={errors.newHouseholdId}
                  excludeId={
                    f.movementType === "TRANSFER"
                      ? resident.householdId
                      : undefined
                  }
                />
                {input("relationshipToHead", "Quan hệ với chủ hộ mới")}
              </>
            )}
            {candidates.length > 0 && (
              <Field
                name="newHeadResidentId"
                label="Chủ hộ mới của hộ cũ"
                error={errors.newHeadResidentId}
                required
              >
                <SelectInput
                  name="newHeadResidentId"
                  value={f.newHeadResidentId}
                  onChange={(e) => set("newHeadResidentId", e.target.value)}
                >
                  <option value="">Chọn chủ hộ mới</option>
                  {candidates.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.fullName} — {c.relationshipToHead}
                    </option>
                  ))}
                </SelectInput>
              </Field>
            )}
            {input("note", "Ghi chú", "text", false)}
          </div>
        </fieldset>
        {["MOVE_OUT", "DEATH"].includes(f.movementType) && (
          <p className="info-inline">
            Tài khoản cư dân liên kết sẽ bị khóa khi ghi nhận. Lịch sử đã lưu
            không thể sửa hoặc xóa.
          </p>
        )}
      </form>
    </Modal>
  );
}
export default function MovementPanel({ resident, canEdit, onResident }) {
  const [history, setHistory] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [revision, setRevision] = useState(0);
  const [notice, setNotice] = useState("");
  useEffect(() => {
    const ctrl = new AbortController();
    setLoading(true);
    setError(null);
    movementApi
      .list(resident.id, { signal: ctrl.signal })
      .then((v) => {
        if (!ctrl.signal.aborted) setHistory(v);
      })
      .catch((e) => {
        if (!ctrl.signal.aborted) setError(e);
      })
      .finally(() => {
        if (!ctrl.signal.aborted) setLoading(false);
      });
    return () => ctrl.abort();
  }, [resident.id, revision]);
  return (
    <Panel
      title="Lịch sử biến động"
      extra={
        canEdit && (
          <button
            className="btn btn-primary"
            disabled={
              !allowedMovements(resident.residencyStatus).length ||
              loading ||
              !!error
            }
            onClick={() => setEditing(true)}
          >
            Ghi nhận biến động
          </button>
        )
      }
    >
      <Alert tone="success">{notice}</Alert>
      {canEdit && (
        <p className="info-inline">
          Khai sinh hoặc thêm người mới:{" "}
          <Link to={`/ho-gia-dinh/${resident.householdId}`}>
            mở hộ gia đình và thêm nhân khẩu
          </Link>
          . Lịch sử chỉ đọc, không sửa/xóa.
        </p>
      )}
      {error ? (
        <PageError error={error} retry={() => setRevision((x) => x + 1)} />
      ) : loading ? (
        <LoadState />
      ) : !history.length ? (
        <EmptyState title="Chưa có biến động" />
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Hiệu lực</th>
                <th>Biến động</th>
                <th>Khoảng thời gian</th>
                <th>Hộ cũ → hộ mới</th>
                <th>Ghi chú</th>
                <th>Người ghi nhận</th>
              </tr>
            </thead>
            <tbody>
              {[...history]
                .sort(
                  (a, b) =>
                    String(b.effectiveDate).localeCompare(
                      String(a.effectiveDate),
                    ) ||
                    String(b.recordedAt).localeCompare(String(a.recordedAt)) ||
                    b.id - a.id,
                )
                .map((m) => (
                  <tr key={m.id}>
                    <td>{formatDate(m.effectiveDate)}</td>
                    <td>
                      {movementLabels[m.movementType] || m.movementType}
                      {m.isInitial && (
                        <small className="cell-note">
                          Tự tạo khi thêm hồ sơ
                        </small>
                      )}
                    </td>
                    <td>
                      {formatDate(m.startDate)} — {formatDate(m.endDate)}
                    </td>
                    <td>
                      {m.oldHousehold?.householdCode || "—"} →{" "}
                      {m.newHousehold?.householdCode || "—"}
                    </td>
                    <td>{m.note || "—"}</td>
                    <td>
                      {m.recordedBy?.fullName || "—"}
                      <small className="cell-note">
                        {formatDateTime(m.recordedAt)}
                      </small>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      )}
      {editing && (
        <MovementForm
          resident={resident}
          history={history}
          onResident={onResident}
          onClose={() => setEditing(false)}
          onSaved={(r) => {
            onResident(r);
            setEditing(false);
            setRevision((x) => x + 1);
            setNotice("Đã ghi nhận biến động và cập nhật tình trạng cư trú.");
          }}
        />
      )}
    </Panel>
  );
}
