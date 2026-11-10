import React, { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { feeApi, periodApi } from "../api/services";
import { useAuth } from "../auth/AuthContext";
import {
  Alert,
  Confirm,
  EmptyState,
  LoadState,
  Modal,
  PageError,
  PageHeader,
  Pagination,
  Panel,
  StatusBadge,
} from "../components/UI";
import PeriodForm from "../components/PeriodForm";
import { formatDate, formatDateTime } from "../utils/format";
import { money } from "../utils/supplement";
function Skipped({ items = [] }) {
  return (
    items.length > 0 && (
      <div className="skipped-list">
        <strong>Các khoản bị bỏ qua do thiếu dữ liệu ({items.length})</strong>
        <ul>
          {items.map((s, i) => (
            <li key={`${s.householdId}-${s.feeCategoryId}-${i}`}>
              <Link to={`/ho-gia-dinh/${s.householdId}`}>
                {s.householdCode} · căn {s.apartmentNo}
              </Link>{" "}
              — {s.feeCode}:{" "}
              {s.reason === "MISSING_AREA" ? "Thiếu diện tích" : s.reason}
            </li>
          ))}
        </ul>
      </div>
    )
  );
}
function GenerationSummary({ value }) {
  return (
    <>
      <p>
        <strong>{value.generatedReceivables}</strong> khoản cho{" "}
        <strong>{value.householdCount}</strong> hộ · Tổng{" "}
        <strong>{money(value.totalAmount)}</strong>
      </p>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Khoản thu</th>
              <th>Số khoản</th>
              <th>Tổng tiền</th>
            </tr>
          </thead>
          <tbody>
            {value.byFee?.map((f) => (
              <tr key={f.feeCategoryId}>
                <td>{f.feeName}</td>
                <td>{f.receivableCount}</td>
                <td>{money(f.totalAmount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p>
        {value.inactiveHouseholdCount || 0} hộ ngừng hoạt động không được đưa
        vào danh sách.
      </p>
      <Skipped items={value.skipped} />
    </>
  );
}
function Receivables({ id, revision }) {
  const [filters, setFilters] = useState({
    keyword: "",
    householdId: "",
    feeCategoryId: "",
  });
  const [page, setPage] = useState(1);
  const [data, setData] = useState(null);
  const [fees, setFees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retry, setRetry] = useState(0);
  const [feeError, setFeeError] = useState(null);
  useEffect(() => {
    const ctrl = new AbortController();
    feeApi
      .list(undefined, { signal: ctrl.signal })
      .then((v) => {
        if (!ctrl.signal.aborted) {
          setFees(v);
          setFeeError(null);
        }
      })
      .catch((e) => {
        if (!ctrl.signal.aborted) setFeeError(e);
      });
    return () => ctrl.abort();
  }, [retry]);
  useEffect(() => {
    const ctrl = new AbortController();
    setLoading(true);
    setError(null);
    const timer = setTimeout(
      () =>
        periodApi
          .receivables(
            id,
            { ...filters, page, size: 20 },
            { signal: ctrl.signal },
          )
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
      clearTimeout(timer);
      ctrl.abort();
    };
  }, [id, filters, page, revision, retry]);
  return (
    <Panel title="Danh sách khoản phải thu">
      <div className="filter-bar">
        <input
          aria-label="Tìm hộ trong kỳ thu"
          placeholder="Mã hộ hoặc căn hộ"
          value={filters.keyword}
          onChange={(e) => {
            setFilters({
              ...filters,
              keyword: e.target.value,
              householdId: "",
            });
            setPage(1);
          }}
        />
        <select
          aria-label="Lọc khoản thu"
          value={filters.feeCategoryId}
          onChange={(e) => {
            setFilters({ ...filters, feeCategoryId: e.target.value });
            setPage(1);
          }}
        >
          <option value="">Tất cả khoản thu</option>
          {fees.map((f) => (
            <option key={f.id} value={f.id}>
              {f.name}
            </option>
          ))}
        </select>
        {filters.householdId && (
          <button
            className="btn btn-secondary"
            onClick={() => {
              setFilters({ ...filters, householdId: "" });
              setPage(1);
            }}
          >
            Bỏ lọc hộ
          </button>
        )}
      </div>
      {feeError && (
        <Alert>
          Không tải được bộ lọc khoản thu.{" "}
          <button
            className="btn btn-secondary"
            onClick={() => setRetry((x) => x + 1)}
          >
            Thử tải lại
          </button>
        </Alert>
      )}
      {error ? (
        <PageError error={error} retry={() => setRetry((x) => x + 1)} />
      ) : loading ? (
        <LoadState />
      ) : !data?.content?.length ? (
        <EmptyState title="Không có khoản phải thu phù hợp" />
      ) : (
        <>
          <p className="info-inline">
            Tổng theo bộ lọc: <strong>{money(data.summary?.totalDue)}</strong>.
            Diện tích và đơn giá dưới đây được lưu tại thời điểm lập kỳ.
          </p>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Hộ / căn hộ</th>
                  <th>Khoản thu</th>
                  <th>Diện tích đã tính</th>
                  <th>Đơn giá áp dụng</th>
                  <th>Phải thu</th>
                  <th>Hạn nộp</th>
                  <th>Đã thu</th>
                  <th>Còn lại</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((r) => (
                  <tr key={r.id}>
                    <td>
                      <button
                        className="table-link text-button"
                        onClick={() => {
                          setFilters({
                            ...filters,
                            householdId: r.householdId,
                            keyword: "",
                          });
                          setPage(1);
                        }}
                        aria-label={`Xem khoản thu hộ ${r.householdCode}`}
                      >
                        {r.householdCode} · {r.apartmentNo}
                      </button>
                    </td>
                    <td>{r.feeName}</td>
                    <td>
                      {r.quantity == null ? "Cố định" : `${r.quantity} m²`}
                    </td>
                    <td>{money(r.unitPriceApplied)}</td>
                    <td>
                      <strong>{money(r.amountDue)}</strong>
                    </td>
                    <td>{formatDate(r.dueDate)}</td>
                    <td>{money(r.paidAmount)}</td>
                    <td>{money(r.outstanding)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            page={page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onChange={setPage}
          />
        </>
      )}
    </Panel>
  );
}
function Detail({ id }) {
  const navigate = useNavigate();
  const { hasRole } = useAuth();
  const [period, setPeriod] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retry, setRetry] = useState(0);
  const [editing, setEditing] = useState(false);
  const [action, setAction] = useState(null);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [preview, setPreview] = useState(null);
  const [result, setResult] = useState(null);
  const [uncertain, setUncertain] = useState(false);
  const [notice, setNotice] = useState("");
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    periodApi
      .get(id)
      .then((p) => {
        if (active) setPeriod(p);
      })
      .catch((e) => {
        if (active) setError(e);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [id, retry]);
  async function reconcile() {
    const current = await periodApi.get(id);
    setPeriod(current);
    setUncertain(false);
    setRevision((x) => x + 1);
    setNotice(
      current.generated
        ? "Kỳ này đã được lập danh sách. Đã tải trạng thái mới nhất."
        : "Đã kiểm tra: kỳ chưa được lập. Bạn có thể xem trước và xác nhận lại.",
    );
    return current;
  }
  async function perform(kind) {
    if (busy) return;
    setBusy(true);
    setActionError(null);
    setNotice("");
    if (kind === "generate") {
      setPreview(null);
      setUncertain(true);
    }
    try {
      if (kind === "preview") setPreview(await periodApi.generate(id, true));
      if (kind === "generate") {
        const generated = await periodApi.generate(id);
        setResult(generated);
        // A successful POST is definitive even if the following refresh fails.
        setPeriod((p) => ({ ...p, generated: true }));
        setUncertain(false);
        setRevision((x) => x + 1);
        setNotice("Đã lập danh sách khoản phải thu.");
        try {
          setPeriod(await periodApi.get(id));
        } catch {
          setNotice(
            "Đã lập danh sách. Thông tin tổng hợp chưa tải lại được; hãy tải lại trang để cập nhật.",
          );
        }
      }
      if (kind === "check") await reconcile();
      if (kind === "delete") {
        await periodApi.remove(id);
        navigate("/ky-thu", { replace: true });
      }
      if (kind === "status") {
        await periodApi.updateStatus(
          id,
          period.status === "OPEN" ? "CLOSED" : "OPEN",
        );
        setPeriod(await periodApi.get(id));
      }
      setAction(null);
    } catch (e) {
      setActionError(e);
      setAction(null);
      if (e.code === "ALREADY_GENERATED") {
        setPreview(null);
        setPeriod((p) => ({ ...p, generated: true }));
        setUncertain(false);
        setRevision((x) => x + 1);
        setNotice("Kỳ này đã được lập danh sách.");
        try {
          setPeriod(await periodApi.get(id));
        } catch {
          /* The server already confirmed generation. */
        }
      } else if (kind === "generate") {
        if (
          [
            "TIMEOUT",
            "NETWORK_ERROR",
            "SERVER_ERROR",
            "INVALID_RESPONSE",
          ].includes(e.code) ||
          e.status >= 500
        ) {
          try {
            await reconcile();
          } catch {
            setUncertain(true);
            setNotice(
              "Chưa xác định được kết quả. Hãy kiểm tra trạng thái kỳ trước khi lập lại.",
            );
          }
        } else {
          setUncertain(false);
        }
      }
      if (e.code === "PERIOD_CLOSED") {
        setPreview(null);
        setPeriod((p) => ({ ...p, status: "CLOSED" }));
      }
    } finally {
      setBusy(false);
    }
  }
  if (error)
    return <PageError error={error} retry={() => setRetry((x) => x + 1)} />;
  if (loading || !period) return <LoadState />;
  const canGenerate =
    !period.generated && period.status === "OPEN" && !uncertain;
  return (
    <>
      <div className="back-link">
        <Link to="/ky-thu">← Danh sách kỳ thu</Link>
      </div>
      <PageHeader
        eyebrow="US-14 · CHI TIẾT KỲ THU"
        title={period.name}
        description={`${period.code} · ${formatDate(period.startDate)} – ${formatDate(period.endDate)}`}
        actions={
          <div className="action-row">
            {!period.generated && (
              <>
                <button
                  className="btn btn-secondary"
                  disabled={busy || uncertain}
                  onClick={() => setEditing(true)}
                >
                  Sửa kỳ
                </button>
                <button
                  className="btn btn-outline-danger"
                  disabled={busy || uncertain}
                  onClick={() => setAction("delete")}
                >
                  Xóa kỳ
                </button>
              </>
            )}
            {canGenerate && (
              <button
                className="btn btn-primary"
                disabled={busy}
                onClick={() => perform("preview")}
              >
                {busy ? "Đang xử lý..." : "Lập danh sách"}
              </button>
            )}
            {hasRole("BQT") && period.generated && (
              <button
                className="btn btn-secondary"
                disabled={busy}
                onClick={() => setAction("status")}
              >
                {period.status === "OPEN" ? "Đóng sổ" : "Mở lại kỳ"}
              </button>
            )}
          </div>
        }
      />
      <div className="period-summary">
        <StatusBadge tone={period.status === "OPEN" ? "green" : "slate"}>
          {period.status === "OPEN" ? "Đang mở" : "Đã đóng sổ"}
        </StatusBadge>
        <StatusBadge tone={period.generated ? "green" : "amber"}>
          {period.generated ? "Đã lập danh sách" : "Chưa lập danh sách"}
        </StatusBadge>
        {period.generated && (
          <span>
            {formatDateTime(period.generatedAt)} ·{" "}
            {period.generatedBy?.fullName || "—"}
          </span>
        )}
      </div>
      <Alert tone="success">{notice}</Alert>
      <Alert>
        {actionError?.message}
        {actionError?.code === "NO_ACTIVE_FEE" && (
          <>
            {" "}
            <Link to="/khoan-thu">Tạo khoản thu bắt buộc đang hiệu lực</Link>
          </>
        )}
      </Alert>
      <Skipped items={actionError?.details?.skipped} />
      {uncertain && (
        <Alert>
          Chưa xác định kết quả lần lập danh sách trước.{" "}
          <button
            className="btn btn-secondary"
            disabled={busy}
            onClick={() => perform("check")}
          >
            Kiểm tra trạng thái kỳ
          </button>
        </Alert>
      )}
      {period.summary && (
        <div className="billing-stats">
          <div>
            <span>Số hộ</span>
            <strong>{period.summary.householdCount}</strong>
          </div>
          <div>
            <span>Khoản phải thu</span>
            <strong>{period.summary.receivableCount}</strong>
          </div>
          <div>
            <span>Tổng phải thu</span>
            <strong>{money(period.summary.totalDue)}</strong>
          </div>
        </div>
      )}
      {result && (
        <Panel title="Kết quả lập danh sách">
          <GenerationSummary value={result} />
        </Panel>
      )}
      {period.generated ? (
        <Receivables id={id} revision={revision} />
      ) : (
        <EmptyState
          title="Kỳ chưa lập danh sách"
          description="Chọn Lập danh sách để xem trước các khoản bắt buộc của hộ đang hoạt động. Khoản tự nguyện không tạo công nợ."
        />
      )}
      {preview && (
        <Modal
          title="Xem trước danh sách đầu kỳ"
          busy={busy}
          onClose={() => setPreview(null)}
          maxWidth={800}
          footer={
            <>
              <button
                className="btn btn-ghost"
                disabled={busy}
                onClick={() => setPreview(null)}
              >
                Hủy
              </button>
              <button
                className="btn btn-primary"
                disabled={busy || !canGenerate}
                onClick={() => perform("generate")}
              >
                Xác nhận lập danh sách
              </button>
            </>
          }
        >
          <GenerationSummary value={preview} />
          <p className="info-inline">
            Chưa ghi dữ liệu. Số liệu sẽ được tính lại theo thông tin mới nhất
            khi xác nhận.
          </p>
        </Modal>
      )}
      {editing && (
        <PeriodForm
          value={period}
          onClose={() => setEditing(false)}
          onSaved={(p) => {
            setPeriod(p);
            setEditing(false);
            setNotice("Đã cập nhật kỳ thu.");
          }}
        />
      )}
      {action && (
        <Confirm
          busy={busy}
          title={
            action === "delete"
              ? "Xóa kỳ thu?"
              : period.status === "OPEN"
                ? "Đóng sổ kỳ thu?"
                : "Mở lại kỳ thu?"
          }
          description={
            action === "delete"
              ? "Chỉ xóa được kỳ chưa lập danh sách."
              : period.status === "OPEN"
                ? "Sau khi đóng sổ, không thể ghi nhận thu tiền trong kỳ."
                : "Kỳ sẽ được mở lại; các khoản đã lập vẫn giữ nguyên."
          }
          danger={action === "delete"}
          onClose={() => setAction(null)}
          onConfirm={() => perform(action)}
        />
      )}
    </>
  );
}
export default function PeriodDetailPage() {
  const { id } = useParams();
  return <Detail key={id} id={id} />;
}
