import React, { useCallback, useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  Edit3,
  Plus,
  RefreshCcw,
  Trash2,
  UserRoundCheck,
} from "lucide-react";
import { householdApi } from "../api/services";
import { useAuth } from "../auth/AuthContext";
import HouseholdForm from "../components/HouseholdForm";
import ResidentForm from "../components/ResidentForm";
import {
  Alert,
  Confirm,
  DataLine,
  EmptyState,
  Field,
  LoadState,
  Modal,
  PageError,
  PageHeader,
  Panel,
  PillStatus,
  SelectInput,
  StatusBadge,
} from "../components/UI";
import {
  formatArea,
  formatDate,
  householdLabel,
  residenceLabel,
} from "../utils/format";
export default function HouseholdDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const canEdit = hasRole("BQT");
  const [house, setHouse] = useState(null);
  const [residents, setResidents] = useState([]);
  const [showAll, setShowAll] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState("");
  const [edit, setEdit] = useState(false);
  const [add, setAdd] = useState(false);
  const [changeHead, setChangeHead] = useState(false);
  const [headId, setHeadId] = useState("");
  const [confirm, setConfirm] = useState(null);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);
  const loadVersion = useRef(0);
  const load = useCallback(async () => {
    const version = ++loadVersion.current;
    setLoading(true);
    setError(null);
    try {
      const [h, r] = await Promise.all([
        householdApi.get(id),
        householdApi.residents(id, showAll ? "ALL" : undefined),
      ]);
      if (version !== loadVersion.current) return;
      setHouse(h);
      setResidents(r || []);
    } catch (err) {
      if (version === loadVersion.current) setError(err);
    } finally {
      if (version === loadVersion.current) setLoading(false);
    }
  }, [id, showAll]);
  useEffect(() => {
    load();
    return () => {
      loadVersion.current += 1;
    };
  }, [load]);
  const onSaved = async (message) => {
    setEdit(false);
    setAdd(false);
    setChangeHead(false);
    setConfirm(null);
    setNotice(message);
    setActionError(null);
    await load();
  };
  const perform = async () => {
    if (!confirm || busy) return;
    setBusy(true);
    setActionError(null);
    try {
      if (confirm === "DELETE") {
        await householdApi.remove(id);
        navigate("/ho-gia-dinh", { replace: true });
        return;
      }
      if (confirm === "SET_INACTIVE")
        await householdApi.updateStatus(id, "INACTIVE");
      if (confirm === "SET_ACTIVE")
        await householdApi.updateStatus(id, "ACTIVE");
      await onSaved("Đã cập nhật hộ gia đình thành công.");
    } catch (err) {
      if (err.code === "HAS_TRANSACTIONS") {
        setConfirm("DELETE_BLOCKED");
        setActionError(err);
      } else {
        setActionError(err);
        setConfirm(null);
      }
    } finally {
      setBusy(false);
    }
  };
  const eligible = residents.filter(
    (r) => r.residencyStatus === "RESIDING" && !r.isHead,
  );
  const change = async () => {
    if (!headId || busy) return;
    setBusy(true);
    setActionError(null);
    try {
      await householdApi.updateHead(id, Number(headId));
      setHeadId("");
      await onSaved("Đã đổi chủ hộ thành công.");
    } catch (err) {
      setActionError(err);
    } finally {
      setBusy(false);
    }
  };
  if (error) return <PageError error={error} retry={load} />;
  if (loading || !house || String(house.id) !== id) return <LoadState />;
  return (
    <>
      <div className="back-link">
        <Link to="/ho-gia-dinh">
          <ArrowLeft size={16} /> Danh sách hộ gia đình
        </Link>
      </div>
      <PageHeader
        eyebrow={`US-01 · HỒ SƠ HỘ ${house.householdCode}`}
        title={`Căn hộ ${house.apartmentNo}`}
        description={`Mã hộ khẩu: ${house.householdCode}`}
        actions={
          canEdit && (
            <div className="action-row">
              <button
                className="btn btn-secondary"
                onClick={() => setEdit(true)}
              >
                <Edit3 size={16} /> Chỉnh sửa
              </button>
              <button
                className="btn btn-primary"
                onClick={() => setAdd(true)}
                disabled={house.status === "INACTIVE"}
              >
                <Plus size={17} /> Thêm nhân khẩu
              </button>
            </div>
          )
        }
      />
      {notice && (
        <Alert tone="success" onClose={() => setNotice("")}>
          {notice}
        </Alert>
      )}
      {actionError && (
        <Alert onClose={() => setActionError(null)}>
          {actionError.message}
        </Alert>
      )}
      <div className="two-col">
        <Panel title="Thông tin hộ">
          <dl className="data-list">
            <DataLine label="Mã hộ khẩu">{house.householdCode}</DataLine>
            <DataLine label="Căn hộ">{house.apartmentNo}</DataLine>
            <DataLine label="Tầng">{house.floor ?? "—"}</DataLine>
            <DataLine label="Diện tích">{formatArea(house.area)}</DataLine>
            <DataLine label="Trạng thái">
              <PillStatus
                status={house.status}
                label={householdLabel[house.status]}
              />
            </DataLine>
            <DataLine label="Ngày tạo">{formatDate(house.createdAt)}</DataLine>
          </dl>
        </Panel>
        <Panel title="Thông tin cư trú">
          <dl className="data-list">
            <DataLine label="Chủ hộ">
              {house.head?.fullName || "Chưa có chủ hộ"}
            </DataLine>
            <DataLine label="Số người thuộc hộ">{house.residentCount}</DataLine>
            <DataLine label="Số người thực tế đang ở">
              {house.presentCount}
            </DataLine>
          </dl>
          <div className="panel-bottom">
            {canEdit && (
              <button
                className="btn btn-secondary btn-small"
                disabled={house.status === "INACTIVE" || eligible.length === 0}
                onClick={() => {
                  setHeadId("");
                  setChangeHead(true);
                }}
              >
                <UserRoundCheck size={16} /> Đổi chủ hộ
              </button>
            )}
            {house.head === null && (
              <p className="info-inline">
                Hộ chưa có chủ hộ. Có thể chọn một nhân khẩu đang cư trú.
              </p>
            )}
          </div>
        </Panel>
      </div>
      <section className="panel">
        <div className="panel-header">
          <div>
            <h2>Danh sách thành viên</h2>
            <p>Gồm những nhân khẩu thuộc hộ, với trạng thái cư trú hiện tại.</p>
          </div>
          <label className="checkline">
            <input
              type="checkbox"
              checked={showAll}
              onChange={(e) => setShowAll(e.target.checked)}
            />{" "}
            Hiển thị cả người đã rời hộ / đã mất
          </label>
        </div>
        {loading ? (
          <LoadState />
        ) : !residents.length ? (
          <EmptyState
            title="Hộ chưa có nhân khẩu phù hợp"
            description="Thêm nhân khẩu hoặc thay đổi bộ lọc hiển thị."
          />
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Họ và tên</th>
                  <th>Ngày sinh</th>
                  <th>Quan hệ</th>
                  <th>Trạng thái</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {residents.map((r) => (
                  <tr key={r.id}>
                    <td>
                      <Link className="table-link" to={`/nhan-khau/${r.id}`}>
                        {r.fullName}
                      </Link>{" "}
                      {r.isHead && (
                        <StatusBadge tone="blue">Chủ hộ</StatusBadge>
                      )}
                    </td>
                    <td>{formatDate(r.dateOfBirth)}</td>
                    <td>{r.relationshipToHead || "—"}</td>
                    <td>
                      <PillStatus
                        status={r.residencyStatus}
                        label={residenceLabel[r.residencyStatus]}
                      />
                      {r.statusExpired && (
                        <StatusBadge tone="red">Quá hạn</StatusBadge>
                      )}
                    </td>
                    <td>
                      <Link className="table-action" to={`/nhan-khau/${r.id}`}>
                        Chi tiết
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      {canEdit && (
        <section className="panel danger-panel">
          <div>
            <h2>Quản lý trạng thái hộ</h2>
            <p>
              Ngừng hoạt động nếu hộ không còn được quản lý. Xóa mềm chỉ được
              phép với hộ chưa có giao dịch.
            </p>
          </div>
          <div className="action-row">
            {house.status === "ACTIVE" ? (
              <button
                className="btn btn-secondary"
                onClick={() => setConfirm("SET_INACTIVE")}
              >
                <RefreshCcw size={16} /> Ngừng hoạt động
              </button>
            ) : (
              <button
                className="btn btn-secondary"
                onClick={() => setConfirm("SET_ACTIVE")}
              >
                Kích hoạt lại
              </button>
            )}
            <button
              className="btn btn-outline-danger"
              onClick={() => setConfirm("DELETE")}
            >
              <Trash2 size={16} /> Xóa hộ
            </button>
          </div>
        </section>
      )}
      {edit && (
        <HouseholdForm
          value={house}
          onClose={() => setEdit(false)}
          onSaved={() => onSaved("Đã lưu thông tin hộ gia đình.")}
        />
      )}
      {add && (
        <ResidentForm
          householdId={id}
          householdStatus={house.status}
          onClose={() => setAdd(false)}
          onSaved={() => onSaved("Đã thêm nhân khẩu thành công.")}
        />
      )}
      {changeHead && (
        <Modal
          busy={busy}
          title="Đổi chủ hộ"
          onClose={() => {
            setChangeHead(false);
            setActionError(null);
          }}
          footer={
            <>
              <button
                className="btn btn-ghost"
                disabled={busy}
                onClick={() => setChangeHead(false)}
              >
                Hủy
              </button>
              <button
                className="btn btn-primary"
                disabled={!headId || busy}
                onClick={change}
              >
                {busy ? "Đang lưu..." : "Xác nhận đổi chủ hộ"}
              </button>
            </>
          }
        >
          <p className="subtle">
            Chỉ có thể chọn thành viên đang cư trú và không phải chủ hộ hiện
            tại.
          </p>
          {actionError && <Alert>{actionError.message}</Alert>}
          <Field label="Chủ hộ mới" name="headId" required>
            <SelectInput
              name="headId"
              value={headId}
              onChange={(e) => setHeadId(e.target.value)}
            >
              <option value="">Chọn thành viên</option>
              {eligible.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.fullName} — {r.relationshipToHead}
                </option>
              ))}
            </SelectInput>
          </Field>
        </Modal>
      )}
      {["DELETE", "SET_INACTIVE", "SET_ACTIVE"].includes(confirm) && (
        <Confirm
          busy={busy}
          title={
            confirm === "DELETE" ? "Xóa hộ gia đình" : "Thay đổi trạng thái hộ"
          }
          danger={confirm === "DELETE"}
          action={
            confirm === "DELETE"
              ? "Xóa hộ"
              : confirm === "SET_INACTIVE"
                ? "Ngừng hoạt động"
                : "Kích hoạt lại"
          }
          description={
            confirm === "DELETE"
              ? `Xóa mềm hộ ${house.householdCode} và tất cả nhân khẩu nếu hộ chưa có giao dịch. Không thể hoàn tác qua giao diện.`
              : confirm === "SET_INACTIVE"
                ? "Hộ ngừng hoạt động sẽ không thể thêm nhân khẩu và không được tính vào kỳ thu mới."
                : "Cho phép hộ tiếp tục hoạt động."
          }
          onClose={() => setConfirm(null)}
          onConfirm={perform}
        />
      )}
      {confirm === "DELETE_BLOCKED" && (
        <Modal
          title="Không thể xóa hộ"
          onClose={() => setConfirm(null)}
          footer={
            <>
              <button
                className="btn btn-ghost"
                onClick={() => setConfirm(null)}
              >
                Đóng
              </button>
              <button
                className="btn btn-primary"
                onClick={() => {
                  setConfirm("SET_INACTIVE");
                  setActionError(null);
                }}
              >
                Chuyển sang ngừng hoạt động
              </button>
            </>
          }
        >
          <Alert>{actionError?.message || "Hộ đã có giao dịch."}</Alert>
          <p>
            Đã phát sinh: {actionError?.details?.receivableCount ?? 0} khoản
            phải thu, {actionError?.details?.paymentCount ?? 0} phiếu thu,{" "}
            {actionError?.details?.contributionCount ?? 0} khoản đóng góp.
          </p>
        </Modal>
      )}
    </>
  );
}
