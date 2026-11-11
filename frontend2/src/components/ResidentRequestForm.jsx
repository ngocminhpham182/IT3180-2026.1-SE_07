import React, { useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { meApi } from "../api/services";
import { Alert, Field, Modal, SelectInput, TextInput } from "./UI";
import {
  profileFields,
  profilePayload,
  requestTypes,
  requestsChanged,
  todayVietnam,
  validateRequest,
} from "../utils/residentRequests";

export default function ResidentRequestForm({
  type,
  resident,
  onClose,
  onRefresh,
  canSubmit = true,
}) {
  const navigate = useNavigate();
  const [person, setPerson] = useState(
    profilePayload(type === "PROFILE_UPDATE" ? resident : {}),
  );
  const [dates, setDates] = useState({ startDate: "", endDate: "" });
  const [note, setNote] = useState("");
  const [errors, setErrors] = useState({});
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const lock = useRef(false);
  const prefix = type === "PROFILE_UPDATE" ? "profile" : "person";
  const submit = async (event) => {
    event.preventDefault();
    if (lock.current || !canSubmit) return;
    const body = { type, note: note.trim() || null };
    if (type !== "TEMPORARY_RESIDENCE") body.residentId = resident.id;
    if (type !== "TEMPORARY_ABSENCE") body[prefix] = profilePayload(person);
    if (type !== "PROFILE_UPDATE") Object.assign(body, dates);
    const validation = validateRequest(body, resident);
    setErrors(validation);
    setError(null);
    if (Object.keys(validation).length) return;
    lock.current = true;
    setBusy(true);
    try {
      const result = await meApi.submit(body);
      requestsChanged();
      navigate(`/de-nghi/${result.id}`, {
        state: {
          notice:
            "Đã gửi đề nghị, đang chờ Ban quản trị duyệt. Dữ liệu nhân khẩu chưa thay đổi.",
        },
      });
    } catch (e) {
      setError(e);
      setErrors(e.fields || {});
      if (
        [
          "RESIDENT_NOT_IN_HOUSEHOLD",
          "RESIDENT_NOT_RESIDING",
          "INVALID_TRANSITION",
          "HOUSEHOLD_INACTIVE",
          "PENDING_REQUEST_EXISTS",
          "NO_CHANGES",
        ].includes(e.code)
      )
        onRefresh();
    } finally {
      lock.current = false;
      setBusy(false);
    }
  };
  return (
    <Modal
      title={requestTypes[type]}
      onClose={onClose}
      busy={busy}
      footer={
        <>
          <button
            className="btn btn-secondary"
            disabled={busy}
            onClick={onClose}
          >
            Đóng
          </button>
          <button
            className="btn btn-primary"
            type="submit"
            form="resident-request"
            disabled={busy || !canSubmit}
          >
            {busy ? "Đang gửi..." : "Gửi đề nghị"}
          </button>
        </>
      }
    >
      <p className="info-inline">
        {resident
          ? `Thành viên: ${resident.fullName}. `
          : "Khai báo người khách mới ở tạm trong căn hộ. "}
        Thông tin chỉ được cập nhật sau khi Ban quản trị duyệt.
      </p>
      <Alert>{errors.form || error?.message}</Alert>
      {!canSubmit && !busy && (
        <Alert>
          Thông tin hộ đang được tải lại hoặc không còn đủ điều kiện gửi đề nghị
          này. Hãy đóng biểu mẫu và kiểm tra thông tin mới nhất.
        </Alert>
      )}
      {error?.code === "PENDING_REQUEST_EXISTS" && error.details?.requestId && (
        <p>
          <Link to={`/de-nghi/${error.details.requestId}`}>
            Xem đề nghị đang chờ
          </Link>{" "}
          · Chỉ người gửi được xem và hủy đề nghị của mình. Nếu do thành viên
          khác gửi, hãy liên hệ người đó.
        </p>
      )}
      <form id="resident-request" onSubmit={submit} noValidate>
        <fieldset disabled={busy} className="request-fieldset">
          <div className="form-grid">
            {type !== "TEMPORARY_ABSENCE" &&
              Object.entries(profileFields).map(([key, label]) => (
                <Field
                  key={key}
                  name={`${prefix}.${key}`}
                  label={label}
                  required={[
                    "fullName",
                    "dateOfBirth",
                    "relationshipToHead",
                  ].includes(key)}
                  error={errors[`${prefix}.${key}`]}
                >
                  {key === "gender" ? (
                    <SelectInput
                      name={`${prefix}.${key}`}
                      value={person[key] || ""}
                      onChange={(e) =>
                        setPerson({ ...person, [key]: e.target.value })
                      }
                    >
                      <option value="">Chưa cung cấp</option>
                      <option value="MALE">Nam</option>
                      <option value="FEMALE">Nữ</option>
                    </SelectInput>
                  ) : (
                    <TextInput
                      name={`${prefix}.${key}`}
                      value={person[key] || ""}
                      onChange={(e) =>
                        setPerson({ ...person, [key]: e.target.value })
                      }
                      type={key === "dateOfBirth" ? "date" : "text"}
                      min={key === "dateOfBirth" ? "1900-01-01" : undefined}
                      max={key === "dateOfBirth" ? todayVietnam() : undefined}
                      maxLength={
                        key === "fullName"
                          ? 150
                          : key === "relationshipToHead"
                            ? 50
                            : undefined
                      }
                    />
                  )}
                </Field>
              ))}
            {type !== "PROFILE_UPDATE" &&
              ["startDate", "endDate"].map((key) => (
                <Field
                  key={key}
                  name={key}
                  label={key === "startDate" ? "Ngày bắt đầu" : "Ngày kết thúc"}
                  required
                  error={errors[key]}
                >
                  <TextInput
                    name={key}
                    type="date"
                    value={dates[key]}
                    max={key === "startDate" ? todayVietnam() : undefined}
                    min={
                      key === "endDate"
                        ? dates.startDate || undefined
                        : (type === "TEMPORARY_ABSENCE"
                            ? resident.dateOfBirth
                            : person.dateOfBirth) || undefined
                    }
                    onChange={(e) =>
                      setDates({ ...dates, [key]: e.target.value })
                    }
                  />
                </Field>
              ))}
            <Field name="note" label="Ghi chú" error={errors.note} wide>
              <TextInput
                name="note"
                value={note}
                maxLength={500}
                onChange={(e) => setNote(e.target.value)}
              />
            </Field>
          </div>
        </fieldset>
        <Alert>
          {errors.residentId || errors.type || errors.profile || errors.person}
        </Alert>
        {type !== "PROFILE_UPDATE" && (
          <p className="field-note">
            Phải nhập đủ hai ngày. Ngày bắt đầu không được ở tương lai; ngày kết
            thúc không trước ngày bắt đầu.
          </p>
        )}
      </form>
    </Modal>
  );
}
