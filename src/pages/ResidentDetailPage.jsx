import React, { useCallback, useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Edit3, Trash2 } from "lucide-react";
import { residentApi } from "../api/services";
import { useAuth } from "../auth/AuthContext";
import ResidentForm from "../components/ResidentForm";
import {
  Alert,
  Confirm,
  DataLine,
  LoadState,
  PageError,
  PageHeader,
  Panel,
  PillStatus,
  StatusBadge,
} from "../components/UI";
import { formatDate, genderLabel, residenceLabel } from "../utils/format";
export default function ResidentDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const editAllowed = hasRole("BQT");
  const [r, setR] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [edit, setEdit] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [notice, setNotice] = useState("");
  const loadVersion = useRef(0);
  const load = useCallback(async () => {
    const version = ++loadVersion.current;
    setLoading(true);
    setError(null);
    try {
      const value = await residentApi.get(id);
      if (version === loadVersion.current) setR(value);
    } catch (e) {
      if (version === loadVersion.current) setError(e);
    } finally {
      if (version === loadVersion.current) setLoading(false);
    }
  }, [id]);
  useEffect(() => {
    load();
    return () => {
      loadVersion.current += 1;
    };
  }, [load]);
  const remove = async () => {
    if (busy) return;
    setBusy(true);
    setActionError(null);
    try {
      await residentApi.remove(id);
      navigate(`/ho-gia-dinh/${r.householdId}`, { replace: true });
    } catch (err) {
      setDeleting(false);
      setActionError(err);
    } finally {
      setBusy(false);
    }
  };
  if (error) return <PageError error={error} retry={load} />;
  if (loading || !r || String(r.id) !== id) return <LoadState />;
  return (
    <>
      <div className="back-link">
        <Link to="/nhan-khau">
          <ArrowLeft size={16} /> Danh sách nhân khẩu
        </Link>
      </div>
      <PageHeader
        eyebrow="US-02 · HỒ SƠ NHÂN KHẨU"
        title={r.fullName}
        description={`Thuộc hộ ${r.householdCode} · Căn hộ ${r.apartmentNo}`}
        actions={
          editAllowed && (
            <div className="action-row">
              <button
                className="btn btn-secondary"
                onClick={() => setEdit(true)}
              >
                <Edit3 size={16} /> Chỉnh sửa
              </button>
              <button
                className="btn btn-outline-danger"
                onClick={() => setDeleting(true)}
              >
                <Trash2 size={16} /> Xóa nhập nhầm
              </button>
            </div>
          )
        }
      />
      {notice && <Alert tone="success">{notice}</Alert>}
      {actionError && (
        <Alert>
          {actionError.message}
          {actionError.code === "HAS_HISTORY" &&
            " Vui lòng liên hệ người phụ trách biến động cư trú."}
          {actionError.code === "HAS_ACCOUNT" &&
            " Cần xử lý tài khoản liên kết trước."}
          {actionError.code === "INVALID_NEW_HEAD" && " Cần đổi chủ hộ trước."}
        </Alert>
      )}
      <div className="two-col">
        <Panel title="Thông tin cá nhân">
          <dl className="data-list">
            <DataLine label="Họ và tên">{r.fullName}</DataLine>
            <DataLine label="Ngày sinh">{formatDate(r.dateOfBirth)}</DataLine>
            <DataLine label="Giới tính">
              {genderLabel[r.gender] || "Chưa xác định"}
            </DataLine>
            <DataLine label="CCCD/CMND">{r.identityNumber || "—"}</DataLine>
            <DataLine label="Số điện thoại">{r.phone || "—"}</DataLine>
          </dl>
        </Panel>
        <Panel title="Thông tin cư trú">
          <dl className="data-list">
            <DataLine label="Hộ gia đình">
              <Link className="table-link" to={`/ho-gia-dinh/${r.householdId}`}>
                {r.householdCode} — {r.apartmentNo}
              </Link>
            </DataLine>
            <DataLine label="Quan hệ với chủ hộ">
              {r.relationshipToHead}
            </DataLine>
            <DataLine label="Chủ hộ">{r.isHead ? "Có" : "Không"}</DataLine>
            <DataLine label="Trạng thái">
              <PillStatus
                status={r.residencyStatus}
                label={residenceLabel[r.residencyStatus]}
              />
              {r.statusExpired && <StatusBadge tone="red">Quá hạn</StatusBadge>}
            </DataLine>
            <DataLine label="Hạn trạng thái">
              {formatDate(r.statusUntil)}
            </DataLine>
            <DataLine label="Ngày vào hộ">{formatDate(r.moveInDate)}</DataLine>
            <DataLine label="Ngày chuyển đi">
              {formatDate(r.moveOutDate)}
            </DataLine>
          </dl>
        </Panel>
      </div>
      <p className="info-inline">
        Xóa chỉ dùng cho hồ sơ nhập nhầm. Khi cư dân chuyển đi hoặc thay đổi
        trạng thái, hãy liên hệ người phụ trách biến động cư trú.
      </p>
      {edit && (
        <ResidentForm
          value={r}
          onClose={() => setEdit(false)}
          onSaved={() => {
            setEdit(false);
            setNotice("Đã cập nhật thông tin nhân khẩu.");
            load();
          }}
        />
      )}
      {deleting && (
        <Confirm
          title="Xóa bản ghi nhân khẩu?"
          action="Xóa nhân khẩu"
          danger
          busy={busy}
          onClose={() => setDeleting(false)}
          onConfirm={remove}
          description="Chỉ dành cho hồ sơ nhập nhầm chưa có lịch sử biến động bổ sung, chưa gắn tài khoản và không phải chủ hộ. Không dùng thao tác xóa để ghi nhận chuyển đi hoặc mất."
        />
      )}
    </>
  );
}
