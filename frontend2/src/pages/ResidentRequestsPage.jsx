import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { meApi, requestApi } from "../api/services";
import {
  EmptyState,
  Field,
  LoadState,
  PageError,
  PageHeader,
  Pagination,
  Panel,
  SelectInput,
  StatusBadge,
  TextInput,
} from "../components/UI";
import { formatDateTime } from "../utils/format";
import { requestStatuses, requestTypes } from "../utils/residentRequests";

export function RequestStatus({ status }) {
  return (
    <StatusBadge
      tone={
        status === "APPROVED"
          ? "green"
          : status === "PENDING"
            ? "amber"
            : "slate"
      }
    >
      {requestStatuses[status] || status}
    </StatusBadge>
  );
}
export default function ResidentRequestsPage({ management = false }) {
  const [status, setStatus] = useState(management ? "PENDING" : "ALL");
  const [type, setType] = useState("");
  const [keyword, setKeyword] = useState("");
  const [householdId, setHouseholdId] = useState("");
  const [page, setPage] = useState(1);
  const [revision, setRevision] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    (management ? requestApi.list : meApi.requests)(
      {
        status,
        type,
        page,
        size: 20,
        ...(management ? { keyword: keyword.trim(), householdId } : {}),
      },
      { signal: controller.signal },
    )
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
  }, [management, status, type, keyword, householdId, page, revision]);
  const filter = (setter) => (e) => {
    setter(e.target.value);
    setPage(1);
  };
  return (
    <>
      <PageHeader
        title={management ? "Duyệt đề nghị" : "Đề nghị của tôi"}
        description={
          management
            ? "Xem xét đề nghị sửa thông tin và khai báo cư trú. Hàng chờ được xếp theo thời điểm gửi cũ nhất trước."
            : "Theo dõi các đề nghị do bạn gửi và xem kết quả từ Ban quản trị."
        }
        actions={
          <>
            <button
              className="btn btn-secondary"
              disabled={loading}
              onClick={() => setRevision((v) => v + 1)}
            >
              Tải lại
            </button>
            {!management && (
              <Link className="btn btn-primary" to="/ho-cua-toi">
                Gửi đề nghị mới
              </Link>
            )}
          </>
        }
      />
      <Panel title="Bộ lọc">
        <div className="form-grid">
          <Field name="request-status" label="Trạng thái">
            <SelectInput
              name="request-status"
              value={status}
              onChange={filter(setStatus)}
            >
              <option value="ALL">Tất cả</option>
              {Object.entries(requestStatuses).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </SelectInput>
          </Field>
          <Field name="request-type" label="Loại đề nghị">
            <SelectInput
              name="request-type"
              value={type}
              onChange={filter(setType)}
            >
              <option value="">Tất cả</option>
              {Object.entries(requestTypes).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </SelectInput>
          </Field>
          {management && (
            <>
              <Field name="request-keyword" label="Mã hộ hoặc căn hộ">
                <TextInput
                  name="request-keyword"
                  value={keyword}
                  onChange={filter(setKeyword)}
                />
              </Field>
              <Field name="request-household" label="ID hộ">
                <TextInput
                  name="request-household"
                  type="number"
                  min="1"
                  step="1"
                  value={householdId}
                  onChange={filter(setHouseholdId)}
                />
              </Field>
            </>
          )}
        </div>
      </Panel>
      {loading ? (
        <LoadState />
      ) : error ? (
        <PageError error={error} retry={() => setRevision((v) => v + 1)} />
      ) : (
        data && (
          <Panel title={`Danh sách đề nghị (${data.totalElements})`}>
            {!data.content.length ? (
              <EmptyState title="Không có đề nghị phù hợp" />
            ) : (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Đề nghị</th>
                      <th>Người liên quan</th>
                      {management && <th>Hộ / Người gửi</th>}
                      <th>Ngày gửi</th>
                      <th>Trạng thái</th>
                      <th>Chi tiết</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.content.map((r) => (
                      <tr key={r.id}>
                        <td>
                          #{r.id}
                          <div>{requestTypes[r.type]}</div>
                        </td>
                        <td>
                          {r.resident?.fullName ||
                            r.proposed?.person?.fullName ||
                            "—"}
                        </td>
                        {management && (
                          <td>
                            {r.household?.householdCode} ·{" "}
                            {r.household?.apartmentNo}
                            <div>{r.submittedBy?.fullName}</div>
                          </td>
                        )}
                        <td>{formatDateTime(r.submittedAt)}</td>
                        <td>
                          <RequestStatus status={r.status} />
                          {r.decision?.reason && <div>{r.decision.reason}</div>}
                        </td>
                        <td>
                          <Link
                            className="table-link"
                            to={`${management ? "/duyet-de-nghi" : "/de-nghi"}/${r.id}`}
                          >
                            Xem #{r.id}
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <Pagination
              page={page}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              onChange={setPage}
            />
          </Panel>
        )
      )}
    </>
  );
}
