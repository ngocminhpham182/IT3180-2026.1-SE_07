import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { searchApi } from "../api/services";
import {
  EmptyState,
  LoadState,
  PageError,
  PageHeader,
  Panel,
  PillStatus,
  StatusBadge,
} from "../components/UI";
import { formatDate, householdLabel, residenceLabel } from "../utils/format";
export default function SearchPage() {
  const [keyword, setKeyword] = useState("");
  const [type, setType] = useState("ALL");
  const [limit, setLimit] = useState(20);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const ctrl = new AbortController();
    const query = keyword.trim();
    setData(null);
    setError(null);
    if (query.length < 2 || query.length > 100) {
      setLoading(false);
      return () => ctrl.abort();
    }
    setLoading(true);
    const timer = setTimeout(
      () =>
        searchApi
          .search({ keyword: query, type, limit }, { signal: ctrl.signal })
          .then((v) => {
            if (!ctrl.signal.aborted) setData(v);
          })
          .catch((e) => {
            if (!ctrl.signal.aborted) setError(e);
          })
          .finally(() => {
            if (!ctrl.signal.aborted) setLoading(false);
          }),
      300,
    );
    return () => {
      clearTimeout(timer);
      ctrl.abort();
    };
  }, [keyword, type, limit, retry]);
  return (
    <>
      <PageHeader
        eyebrow="US-04 · TRA CỨU"
        title="Tìm kiếm nhanh"
        description="Tra cứu theo tên cư dân (có hoặc không dấu), mã hộ hoặc số căn hộ."
      />
      <section className="panel">
        <div className="filter-bar">
          <input
            aria-label="Từ khóa tìm kiếm"
            maxLength={100}
            value={keyword}
            placeholder="Nhập ít nhất 2 ký tự..."
            onChange={(e) => {
              setKeyword(e.target.value);
              setLimit(20);
            }}
          />
          <select
            aria-label="Loại kết quả"
            value={type}
            onChange={(e) => {
              setType(e.target.value);
              setLimit(20);
            }}
          >
            <option value="ALL">Hộ và cư dân</option>
            <option value="HOUSEHOLD">Hộ gia đình</option>
            <option value="RESIDENT">Cư dân</option>
          </select>
        </div>
        {keyword.trim().length < 2 ? (
          <EmptyState
            title="Nhập ít nhất 2 ký tự"
            description="Kết quả bao gồm cả hộ ngừng hoạt động và cư dân đã chuyển đi hoặc đã mất."
          />
        ) : error ? (
          <PageError error={error} retry={() => setRetry((x) => x + 1)} />
        ) : loading ? (
          <LoadState />
        ) : data?.empty ? (
          <EmptyState
            title="Không tìm thấy"
            description="Thử tên, mã hộ hoặc số căn hộ khác."
          />
        ) : (
          data && (
            <div aria-live="polite">
              {type !== "RESIDENT" && (
                <Panel title={`Hộ gia đình (${data.totals.households})`}>
                  <div className="table-scroll">
                    <table>
                      <thead>
                        <tr>
                          <th>Mã hộ / căn hộ</th>
                          <th>Chủ hộ</th>
                          <th>Tầng</th>
                          <th>Trạng thái</th>
                        </tr>
                      </thead>
                      <tbody>
                        {data.households.map((h) => (
                          <tr key={h.id}>
                            <td>
                              <Link
                                className="table-link"
                                to={`/ho-gia-dinh/${h.id}`}
                              >
                                {h.householdCode} — {h.apartmentNo}
                              </Link>
                            </td>
                            <td>{h.headName || "Chưa có chủ hộ"}</td>
                            <td>{h.floor ?? "—"}</td>
                            <td>
                              <PillStatus
                                status={h.status}
                                label={householdLabel[h.status]}
                              />
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </Panel>
              )}
              {type !== "HOUSEHOLD" && (
                <Panel title={`Cư dân (${data.totals.residents})`}>
                  <div className="table-scroll">
                    <table>
                      <thead>
                        <tr>
                          <th>Họ tên</th>
                          <th>Ngày sinh</th>
                          <th>Hộ / căn hộ</th>
                          <th>Tình trạng cư trú</th>
                        </tr>
                      </thead>
                      <tbody>
                        {data.residents.map((r) => (
                          <tr key={r.id}>
                            <td>
                              <Link
                                className="table-link"
                                to={`/nhan-khau/${r.id}`}
                              >
                                {r.fullName}
                              </Link>
                            </td>
                            <td>{formatDate(r.dateOfBirth)}</td>
                            <td>
                              {r.householdCode} — {r.apartmentNo}
                            </td>
                            <td>
                              <PillStatus
                                status={r.residencyStatus}
                                label={residenceLabel[r.residencyStatus]}
                              />
                              {r.statusExpired && (
                                <StatusBadge tone="red">Quá hạn</StatusBadge>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </Panel>
              )}
              {(data.hasMore.households || data.hasMore.residents) && (
                <div className="info-inline">
                  Còn kết quả chưa hiển thị.{" "}
                  {limit < 50 ? (
                    <button
                      className="btn btn-secondary"
                      onClick={() => setLimit(50)}
                    >
                      Hiển thị tối đa 50 mỗi nhóm
                    </button>
                  ) : (
                    "Hãy nhập từ khóa cụ thể hơn để thu hẹp kết quả."
                  )}
                </div>
              )}
            </div>
          )
        )}
      </section>
    </>
  );
}
