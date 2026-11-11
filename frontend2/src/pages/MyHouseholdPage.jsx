import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { meApi } from "../api/services";
import ResidentRequestForm from "../components/ResidentRequestForm";
import {
  Alert,
  DataLine,
  EmptyState,
  LoadState,
  PageError,
  PageHeader,
  Panel,
  PillStatus,
} from "../components/UI";
import {
  formatArea,
  formatDate,
  genderLabel,
  householdLabel,
  residenceLabel,
} from "../utils/format";

export default function MyHouseholdPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [revision, setRevision] = useState(0);
  const [form, setForm] = useState(null);
  const refresh = () => setRevision((v) => v + 1);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    meApi
      .household({ signal: controller.signal })
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
  }, [revision]);
  const pending = (id, type) =>
    data?.pendingRequests?.some(
      (r) => String(r.residentId) === String(id) && r.type === type,
    );
  const active = data?.household?.status === "ACTIVE";
  return (
    <>
      <PageHeader
        title="Hộ của tôi"
        description="Thông tin căn hộ và các thành viên trong hộ gia đình của bạn."
        actions={
          <>
            <Link className="btn btn-secondary" to="/de-nghi">
              Đề nghị của tôi
            </Link>
            <button
              className="btn btn-secondary"
              onClick={refresh}
              disabled={loading}
            >
              Tải lại
            </button>
          </>
        }
      />
      {loading ? (
        <LoadState />
      ) : error ? (
        <PageError
          error={
            error.code === "RESIDENT_LINK_REQUIRED"
              ? {
                  message:
                    "Tài khoản chưa liên kết nhân khẩu. Vui lòng liên hệ Ban quản trị để được hỗ trợ.",
                }
              : error
          }
          retry={refresh}
        />
      ) : (
        data && (
          <>
            {!active && (
              <Alert>
                Hộ đã ngừng hoạt động. Bạn có thể xem thông tin nhưng không thể
                gửi đề nghị mới.
              </Alert>
            )}
            <Panel title={`Căn hộ ${data.household.apartmentNo}`}>
              <dl>
                <DataLine label="Mã hộ">
                  {data.household.householdCode}
                </DataLine>
                <DataLine label="Tầng">{data.household.floor}</DataLine>
                <DataLine label="Diện tích">
                  {formatArea(data.household.area)}
                </DataLine>
                <DataLine label="Trạng thái">
                  {householdLabel[data.household.status]}
                </DataLine>
              </dl>
            </Panel>
            <Panel
              title="Thành viên trong hộ"
              extra={
                <button
                  className="btn btn-primary"
                  disabled={!active}
                  onClick={() => setForm({ type: "TEMPORARY_RESIDENCE" })}
                >
                  Khai báo tạm trú
                </button>
              }
            >
              {!data.residents.length ? (
                <EmptyState title="Chưa có thành viên" />
              ) : (
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>Thành viên</th>
                        <th>Thông tin cá nhân</th>
                        <th>Cư trú</th>
                        <th>Đề nghị</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.residents.map((r) => (
                        <tr key={r.id}>
                          <td>
                            <strong>{r.fullName}</strong>
                            {r.id === data.myResidentId && <span> (Bạn)</span>}
                            <div>
                              {r.isHead ? "Chủ hộ" : r.relationshipToHead}
                            </div>
                          </td>
                          <td>
                            <div>Ngày sinh: {formatDate(r.dateOfBirth)}</div>
                            <div>Giới tính: {genderLabel[r.gender] || "—"}</div>
                            <div>CCCD/CMND: {r.identityNumber || "—"}</div>
                            <div>Điện thoại: {r.phone || "—"}</div>
                          </td>
                          <td>
                            <PillStatus
                              status={r.residencyStatus}
                              label={residenceLabel[r.residencyStatus]}
                            />
                            {r.statusUntil && (
                              <div>
                                Đến {formatDate(r.statusUntil)}
                                {r.statusExpired && " · Đã hết hạn"}
                              </div>
                            )}
                          </td>
                          <td>
                            <div className="request-actions">
                              <button
                                className="btn btn-secondary"
                                disabled={
                                  !active || pending(r.id, "PROFILE_UPDATE")
                                }
                                onClick={() =>
                                  setForm({
                                    type: "PROFILE_UPDATE",
                                    resident: r,
                                  })
                                }
                              >
                                {pending(r.id, "PROFILE_UPDATE")
                                  ? "Sửa thông tin: đang chờ duyệt"
                                  : "Đề nghị sửa"}
                              </button>
                              <button
                                className="btn btn-secondary"
                                disabled={
                                  !active ||
                                  r.residencyStatus !== "RESIDING" ||
                                  pending(r.id, "TEMPORARY_ABSENCE")
                                }
                                onClick={() =>
                                  setForm({
                                    type: "TEMPORARY_ABSENCE",
                                    resident: r,
                                  })
                                }
                              >
                                {pending(r.id, "TEMPORARY_ABSENCE")
                                  ? "Tạm vắng: đang chờ duyệt"
                                  : "Khai báo tạm vắng"}
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <p className="field-note">
                Đề nghị đang chờ của mọi thành viên trong hộ đều được tính. Chỉ
                khai tạm vắng cho người đang cư trú.
              </p>
            </Panel>
          </>
        )
      )}
      {form && (
        <ResidentRequestForm
          {...form}
          canSubmit={
            !loading &&
            !error &&
            active &&
            (form.type === "TEMPORARY_RESIDENCE" ||
              (data.residents.some(
                (r) =>
                  r.id === form.resident.id &&
                  (form.type !== "TEMPORARY_ABSENCE" ||
                    r.residencyStatus === "RESIDING"),
              ) &&
                !pending(form.resident.id, form.type)))
          }
          onClose={() => setForm(null)}
          onRefresh={refresh}
        />
      )}
    </>
  );
}
