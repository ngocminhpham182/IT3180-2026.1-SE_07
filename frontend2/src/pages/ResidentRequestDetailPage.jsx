import React, { useEffect, useRef, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { meApi, requestApi } from "../api/services";
import {
  Alert,
  Confirm,
  DataLine,
  Field,
  LoadState,
  PageError,
  PageHeader,
  Panel,
  TextInput,
} from "../components/UI";
import { formatDate, formatDateTime, genderLabel } from "../utils/format";
import {
  profileFields,
  requestsChanged,
  requestTypes,
} from "../utils/residentRequests";
import { RequestStatus } from "./ResidentRequestsPage";

const display = (key, value) =>
  key === "dateOfBirth"
    ? formatDate(value)
    : key === "gender"
      ? genderLabel[value] || "—"
      : value || "—";
export default function ResidentRequestDetailPage({ management = false }) {
  const { id } = useParams();
  const location = useLocation();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [revision, setRevision] = useState(0);
  const [busy, setBusy] = useState(false);
  const [confirm, setConfirm] = useState(null);
  const [note, setNote] = useState("");
  const [reason, setReason] = useState("");
  const [notice, setNotice] = useState(location.state?.notice || "");
  const lock = useRef(false);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    setData(null);
    (management ? requestApi.get : meApi.request)(id, {
      signal: controller.signal,
    })
      .then((value) => {
        if (!controller.signal.aborted) setData(value);
      })
      .catch((e) => {
        if (!controller.signal.aborted) setError(e);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [id, management, revision]);
  const decide = async () => {
    if (lock.current) return;
    if (confirm === "reject" && !reason.trim()) return;
    lock.current = true;
    setBusy(true);
    setActionError(null);
    setNotice("");
    try {
      const updated = await (confirm === "approve"
        ? requestApi.approve(id, note.trim())
        : confirm === "reject"
          ? requestApi.reject(id, reason.trim())
          : meApi.cancel(id));
      setData(updated);
      setNotice("Đã xử lý đề nghị thành công.");
      requestsChanged();
    } catch (e) {
      setActionError(e);
      if (e.code === "REQUEST_NOT_PENDING") {
        setRevision((v) => v + 1);
        requestsChanged();
      }
    } finally {
      lock.current = false;
      setBusy(false);
      setConfirm(null);
    }
  };
  return (
    <>
      <PageHeader
        title={`Chi tiết đề nghị #${id}`}
        actions={
          <Link
            className="btn btn-secondary"
            to={management ? "/duyet-de-nghi" : "/de-nghi"}
          >
            Về danh sách
          </Link>
        }
      />
      <Alert tone="success">{notice}</Alert>
      <Alert>{actionError?.message}</Alert>
      {actionError?.code === "REQUEST_OUTDATED" && (
        <Alert>
          Thông tin đã thay đổi kể từ khi cư dân gửi. Các trường liên quan:{" "}
          {(actionError.details?.fields || [])
            .map((f) => profileFields[f] || f)
            .join(", ") ||
            "Nhân khẩu không còn tồn tại hoặc dữ liệu không còn phù hợp"}
          . Hãy từ chối kèm lý do và đề nghị cư dân gửi lại.
        </Alert>
      )}
      {actionError &&
        Object.entries(actionError.fields || {})
          .filter(([key]) => !["reason", "note"].includes(key))
          .map(([key, message]) => (
            <Alert key={key}>
              {profileFields[key.split(".").at(-1)] || key}: {message}
            </Alert>
          ))}
      {loading ? (
        <LoadState />
      ) : error ? (
        <PageError
          error={
            error.code === "NOT_FOUND" && !management
              ? {
                  message:
                    "Không tìm thấy đề nghị hoặc đề nghị không do bạn gửi. Chỉ người gửi được xem và hủy đề nghị của mình.",
                }
              : error
          }
          retry={() => setRevision((v) => v + 1)}
        />
      ) : (
        data && (
          <>
            <Panel
              title={requestTypes[data.type]}
              extra={<RequestStatus status={data.status} />}
            >
              <dl>
                <DataLine label="Hộ / Căn hộ">
                  {data.household?.householdCode} /{" "}
                  {data.household?.apartmentNo}
                </DataLine>
                <DataLine label="Người gửi">
                  {data.submittedBy?.fullName}
                </DataLine>
                <DataLine label="Ngày gửi">
                  {formatDateTime(data.submittedAt)}
                </DataLine>
                <DataLine label="Người liên quan">
                  {data.resident?.fullName || data.proposed?.person?.fullName}
                </DataLine>
                <DataLine label="Ghi chú">{data.note || "—"}</DataLine>
              </dl>
            </Panel>
            {data.type === "PROFILE_UPDATE" ? (
              <Panel title="So sánh thông tin">
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>Thông tin</th>
                        <th>Trước khi gửi</th>
                        <th>Đề nghị mới</th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.entries(profileFields).map(([key, label]) => (
                        <tr
                          key={key}
                          className={
                            data.changes?.includes(key) ? "request-changed" : ""
                          }
                        >
                          <th>
                            {label}
                            {data.changes?.includes(key) && " · Thay đổi"}
                          </th>
                          <td>{display(key, data.before?.[key])}</td>
                          <td>{display(key, data.proposed?.[key])}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Panel>
            ) : (
              <Panel title="Nội dung khai báo">
                <dl>
                  <DataLine label="Ngày bắt đầu">
                    {formatDate(data.proposed?.startDate)}
                  </DataLine>
                  <DataLine label="Ngày kết thúc">
                    {formatDate(data.proposed?.endDate)}
                  </DataLine>
                  {data.type === "TEMPORARY_RESIDENCE" &&
                    Object.entries(profileFields).map(([key, label]) => (
                      <DataLine key={key} label={label}>
                        {display(key, data.proposed?.person?.[key])}
                      </DataLine>
                    ))}
                </dl>
              </Panel>
            )}
            {data.decision && (
              <Panel title="Kết quả xử lý">
                <dl>
                  <DataLine label="Người xử lý">
                    {data.decision.decidedBy?.fullName}
                  </DataLine>
                  <DataLine label="Thời điểm">
                    {formatDateTime(data.decision.decidedAt)}
                  </DataLine>
                  <DataLine label="Lý do từ chối">
                    {data.decision.reason || "—"}
                  </DataLine>
                  <DataLine label="Ghi chú duyệt">
                    {data.decision.note || "—"}
                  </DataLine>
                </dl>
                {management && data.result?.residentId && (
                  <Link to={`/nhan-khau/${data.result.residentId}`}>
                    Xem hồ sơ và lịch sử biến động nhân khẩu
                  </Link>
                )}
                {!management && data.status === "APPROVED" && (
                  <Link to="/ho-cua-toi">Xem thông tin hộ đã cập nhật</Link>
                )}
              </Panel>
            )}
            {data.status === "PENDING" && (
              <Panel title={management ? "Xử lý đề nghị" : "Rút lại đề nghị"}>
                {management ? (
                  <>
                    <div className="form-grid">
                      <Field
                        name="approval-note"
                        label="Ghi chú duyệt"
                        error={actionError?.fields?.note}
                      >
                        <TextInput
                          name="approval-note"
                          maxLength={500}
                          value={note}
                          disabled={busy}
                          onChange={(e) => setNote(e.target.value)}
                        />
                      </Field>
                      <Field
                        name="rejection-reason"
                        label="Lý do từ chối"
                        required
                        error={actionError?.fields?.reason}
                      >
                        <TextInput
                          name="rejection-reason"
                          maxLength={500}
                          value={reason}
                          disabled={busy}
                          onChange={(e) => setReason(e.target.value)}
                        />
                      </Field>
                    </div>
                    <div className="request-actions">
                      <button
                        className="btn btn-primary"
                        disabled={
                          busy || actionError?.code === "REQUEST_OUTDATED"
                        }
                        onClick={() => setConfirm("approve")}
                      >
                        Duyệt đề nghị
                      </button>
                      <button
                        className="btn btn-danger"
                        disabled={busy || !reason.trim()}
                        onClick={() => setConfirm("reject")}
                      >
                        Từ chối đề nghị
                      </button>
                    </div>
                  </>
                ) : (
                  <>
                    <p>
                      Hủy đề nghị không làm thay đổi dữ liệu nhân khẩu. Sau khi
                      hủy, bạn có thể gửi lại.
                    </p>
                    <button
                      className="btn btn-danger"
                      disabled={busy}
                      onClick={() => setConfirm("cancel")}
                    >
                      Hủy đề nghị
                    </button>
                  </>
                )}
              </Panel>
            )}
          </>
        )
      )}
      {confirm && (
        <Confirm
          title={
            confirm === "approve"
              ? "Duyệt đề nghị?"
              : confirm === "reject"
                ? "Từ chối đề nghị?"
                : "Hủy đề nghị?"
          }
          description={
            confirm === "approve"
              ? "Thông tin hoặc trạng thái cư trú sẽ được cập nhật khi duyệt thành công."
              : confirm === "reject"
                ? `Lý do: ${reason.trim()}`
                : "Bạn sẽ cần tạo đề nghị mới nếu muốn gửi lại."
          }
          action="Xác nhận"
          danger={confirm !== "approve"}
          busy={busy}
          onClose={() => setConfirm(null)}
          onConfirm={decide}
        />
      )}
    </>
  );
}
