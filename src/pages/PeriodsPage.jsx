import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { periodApi } from "../api/services";
import {
  EmptyState,
  LoadState,
  PageError,
  PageHeader,
  Pagination,
  StatusBadge,
} from "../components/UI";
import PeriodForm from "../components/PeriodForm";
import { formatDate } from "../utils/format";
export default function PeriodsPage() {
  const [filters, setFilters] = useState({
    year: "",
    status: "",
    generated: "",
  });
  const [page, setPage] = useState(1);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retry, setRetry] = useState(0);
  const [create, setCreate] = useState(false);
  const navigate = useNavigate();
  useEffect(() => {
    const ctrl = new AbortController();
    setLoading(true);
    setError(null);
    const timer = setTimeout(
      () =>
        periodApi
          .list({ ...filters, page, size: 20 }, { signal: ctrl.signal })
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
  }, [filters, page, retry]);
  const filter = (k, v) => {
    setFilters({ ...filters, [k]: v });
    setPage(1);
  };
  return (
    <>
      <PageHeader
        eyebrow="US-14 · KHOẢN PHẢI THU"
        title="Kỳ thu"
        description="Lập danh sách đầu kỳ, kiểm tra số tiền từng hộ và theo dõi kỳ đã đóng sổ."
        actions={
          <button className="btn btn-primary" onClick={() => setCreate(true)}>
            Tạo kỳ thu
          </button>
        }
      />
      <section className="panel">
        <div className="filter-bar">
          <input
            aria-label="Lọc năm"
            type="number"
            placeholder="Năm"
            value={filters.year}
            onChange={(e) => filter("year", e.target.value)}
          />
          <select
            aria-label="Trạng thái kỳ"
            value={filters.status}
            onChange={(e) => filter("status", e.target.value)}
          >
            <option value="">Tất cả trạng thái</option>
            <option value="OPEN">Đang mở</option>
            <option value="CLOSED">Đã đóng</option>
          </select>
          <select
            aria-label="Tình trạng lập danh sách"
            value={filters.generated}
            onChange={(e) => filter("generated", e.target.value)}
          >
            <option value="">Tất cả kỳ</option>
            <option value="true">Đã lập danh sách</option>
            <option value="false">Chưa lập danh sách</option>
          </select>
        </div>
        {error ? (
          <PageError error={error} retry={() => setRetry((x) => x + 1)} />
        ) : loading ? (
          <LoadState />
        ) : !data?.content?.length ? (
          <EmptyState title="Chưa có kỳ thu phù hợp" />
        ) : (
          <>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Kỳ thu</th>
                    <th>Khoảng thời gian</th>
                    <th>Trạng thái</th>
                    <th>Danh sách</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((p) => (
                    <tr key={p.id}>
                      <td>
                        <Link className="table-link" to={`/ky-thu/${p.id}`}>
                          {p.name}
                        </Link>
                        <small className="cell-note">{p.code}</small>
                      </td>
                      <td>
                        {formatDate(p.startDate)} — {formatDate(p.endDate)}
                      </td>
                      <td>
                        <StatusBadge
                          tone={p.status === "OPEN" ? "green" : "slate"}
                        >
                          {p.status === "OPEN" ? "Đang mở" : "Đã đóng"}
                        </StatusBadge>
                      </td>
                      <td>{p.generated ? "Đã lập" : "Chưa lập"}</td>
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
      </section>
      {create && (
        <PeriodForm
          onClose={() => setCreate(false)}
          onSaved={(p) => navigate(`/ky-thu/${p.id}`)}
        />
      )}
    </>
  );
}
