import React, { useEffect, useState } from "react";
import { feeApi } from "../api/services";
import {
  Alert,
  Confirm,
  EmptyState,
  LoadState,
  PageError,
  PageHeader,
  StatusBadge,
} from "../components/UI";
import FeeForm from "../components/FeeForm";
import FeePreview from "../components/FeePreview";
import { cycleLabels, methodLabels, money } from "../utils/supplement";
export default function FeesPage() {
  const [filters, setFilters] = useState({ keyword: "", kind: "", active: "" });
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reload, setReload] = useState(0);
  const [form, setForm] = useState(null);
  const [preview, setPreview] = useState(null);
  const [action, setAction] = useState(null);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [notice, setNotice] = useState("");
  const [blockedFee, setBlockedFee] = useState(null);
  useEffect(() => {
    const ctrl = new AbortController();
    setLoading(true);
    setError(null);
    const t = setTimeout(
      () =>
        feeApi
          .list(filters, { signal: ctrl.signal })
          .then((v) => {
            if (!ctrl.signal.aborted) setData(v);
          })
          .catch((e) => {
            if (!ctrl.signal.aborted) setError(e);
          })
          .finally(() => {
            if (!ctrl.signal.aborted) setLoading(false);
          }),
      250,
    );
    return () => {
      clearTimeout(t);
      ctrl.abort();
    };
  }, [filters, reload]);
  async function perform() {
    if (busy) return;
    setBusy(true);
    setActionError(null);
    try {
      if (action.kind === "delete") await feeApi.remove(action.fee.id);
      else await feeApi.updateStatus(action.fee.id, !action.fee.active);
      setNotice("Đã cập nhật danh mục khoản thu.");
      setReload((x) => x + 1);
      setAction(null);
    } catch (e) {
      setActionError(e);
      if (e.code === "HAS_TRANSACTIONS") {
        setBlockedFee(action.fee);
        setReload((x) => x + 1);
      }
      setAction(null);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <PageHeader
        eyebrow="US-12 · US-13 · QUẢN LÝ THU PHÍ"
        title="Danh mục khoản thu"
        description="Thiết lập khoản bắt buộc, đóng góp tự nguyện và tính thử phí theo diện tích."
        actions={
          <button className="btn btn-primary" onClick={() => setForm({})}>
            Thêm khoản thu
          </button>
        }
      />
      <Alert tone="success">{notice}</Alert>
      <Alert>
        {actionError?.message}
        {blockedFee && (
          <>
            {" "}
            Khoản đã được sử dụng, hãy ngừng sử dụng thay vì xóa.{" "}
            <button
              className="btn btn-secondary"
              onClick={() => {
                setAction({
                  kind: "status",
                  fee: { ...blockedFee, active: true },
                });
                setBlockedFee(null);
              }}
            >
              Ngừng sử dụng khoản thu
            </button>
          </>
        )}
      </Alert>
      <section className="panel">
        <div className="filter-bar">
          <input
            aria-label="Tìm khoản thu"
            placeholder="Tìm theo mã hoặc tên..."
            value={filters.keyword}
            onChange={(e) =>
              setFilters({ ...filters, keyword: e.target.value })
            }
          />
          <select
            aria-label="Lọc loại khoản thu"
            value={filters.kind}
            onChange={(e) => setFilters({ ...filters, kind: e.target.value })}
          >
            <option value="">Tất cả loại</option>
            <option value="MANDATORY">Bắt buộc</option>
            <option value="VOLUNTARY">Tự nguyện</option>
          </select>
          <select
            aria-label="Lọc hiệu lực"
            value={filters.active}
            onChange={(e) => setFilters({ ...filters, active: e.target.value })}
          >
            <option value="">Tất cả hiệu lực</option>
            <option value="true">Đang sử dụng</option>
            <option value="false">Đã ngừng</option>
          </select>
        </div>
        {error ? (
          <PageError error={error} retry={() => setReload((x) => x + 1)} />
        ) : loading ? (
          <LoadState />
        ) : !data.length ? (
          <EmptyState title="Chưa có khoản thu phù hợp" />
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Khoản thu</th>
                  <th>Loại / cách tính</th>
                  <th>Đơn giá</th>
                  <th>Chu kỳ / hạn nộp</th>
                  <th>Hiệu lực</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {data.map((f) => (
                  <tr key={f.id}>
                    <td>
                      <strong>{f.name}</strong>
                      <small className="cell-note">{f.code}</small>
                      {f.description && (
                        <small className="cell-note">{f.description}</small>
                      )}
                    </td>
                    <td>
                      {f.feeKind === "MANDATORY" ? "Bắt buộc" : "Tự nguyện"}
                      <small className="cell-note">
                        {methodLabels[f.calculationMethod]}
                      </small>
                    </td>
                    <td>
                      {f.feeKind === "VOLUNTARY"
                        ? "Không tính nợ"
                        : money(f.unitPrice)}
                    </td>
                    <td>
                      {cycleLabels[f.billingCycle]}
                      <small className="cell-note">
                        {f.dueDay ? `Ngày ${f.dueDay}` : "Không bắt buộc"}
                      </small>
                    </td>
                    <td>
                      <StatusBadge tone={f.active ? "green" : "slate"}>
                        {f.active ? "Đang sử dụng" : "Đã ngừng"}
                      </StatusBadge>
                    </td>
                    <td>
                      <div className="action-row">
                        <button
                          className="btn btn-secondary"
                          onClick={() => setForm(f)}
                        >
                          Sửa
                        </button>
                        <button
                          className="btn btn-secondary"
                          disabled={!f.active || f.feeKind === "VOLUNTARY"}
                          onClick={() => setPreview(f)}
                        >
                          Tính thử
                        </button>
                        <button
                          className="btn btn-secondary"
                          onClick={() => setAction({ kind: "status", fee: f })}
                        >
                          {f.active ? "Ngừng" : "Bật lại"}
                        </button>
                        <button
                          className="btn btn-outline-danger"
                          disabled={f.inUse}
                          title={
                            f.inUse
                              ? "Đã có công nợ, chỉ có thể ngừng sử dụng"
                              : undefined
                          }
                          onClick={() => setAction({ kind: "delete", fee: f })}
                        >
                          Xóa
                        </button>
                      </div>
                      {f.inUse && (
                        <small className="cell-note">
                          Đã được sử dụng; không thể xóa.
                        </small>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      {form && (
        <FeeForm
          value={form.id ? form : undefined}
          onClose={() => setForm(null)}
          onSaved={() => {
            setForm(null);
            setReload((x) => x + 1);
            setNotice(
              "Đã lưu khoản thu. Đơn giá mới áp dụng cho các kỳ lập sau đó.",
            );
          }}
        />
      )}
      {preview && <FeePreview fee={preview} onClose={() => setPreview(null)} />}
      {action && (
        <Confirm
          title={
            action.kind === "delete"
              ? "Xóa khoản thu?"
              : action.fee.active
                ? "Ngừng sử dụng khoản thu?"
                : "Bật lại khoản thu?"
          }
          description={`${action.fee.name}. Các khoản phải thu đã lập giữ nguyên.`}
          danger={action.kind === "delete"}
          busy={busy}
          onClose={() => setAction(null)}
          onConfirm={perform}
        />
      )}
    </>
  );
}
