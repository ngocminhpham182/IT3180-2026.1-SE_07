import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, Search } from "lucide-react";
import { residentApi } from "../api/services";
import {
  Alert,
  EmptyState,
  LoadState,
  PageError,
  PageHeader,
  Pagination,
  PillStatus,
  StatusBadge,
} from "../components/UI";
import { formatDate, residenceLabel } from "../utils/format";
export default function ResidentsPage() {
  const [keyword, setKeyword] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("CURRENT");
  const [expired, setExpired] = useState(false);
  const [page, setPage] = useState(1);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reload, setReload] = useState(0);
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(keyword.trim());
      setPage(1);
    }, 350);
    return () => clearTimeout(timer);
  }, [keyword]);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    const params = {
      keyword: query,
      page,
      size: 20,
      ...(status === "CURRENT" ? {} : { status }),
      ...(expired ? { statusExpired: true } : {}),
    };
    residentApi
      .list(params, { signal: controller.signal })
      .then(setData)
      .catch((e) => {
        if (e.name !== "AbortError") setError(e);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [query, status, expired, page, reload]);
  return (
    <>
      <PageHeader
        eyebrow="US-02 · QUẢN LÝ CƯ DÂN"
        title="Nhân khẩu"
        description="Tra cứu thông tin nhân khẩu trong toàn chung cư. Để thêm mới, mở chi tiết hộ gia đình tương ứng."
      />
      <section className="panel">
        <div className="filter-bar">
          <div className="search-box">
            <Search size={18} />
            <input
              aria-label="Tìm theo tên nhân khẩu"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Tìm theo họ tên (có hoặc không dấu)..."
            />
          </div>
          <select
            aria-label="Lọc tình trạng cư trú"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(1);
            }}
          >
            <option value="CURRENT">Người đang thuộc hộ</option>
            <option value="ALL">Tất cả trạng thái</option>
            <option value="RESIDING">Đang cư trú</option>
            <option value="TEMPORARILY_ABSENT">Tạm vắng</option>
            <option value="TEMPORARY_RESIDENCE">Tạm trú</option>
            <option value="MOVED_OUT">Đã chuyển đi</option>
            <option value="DECEASED">Đã mất</option>
          </select>
          <label className="checkline">
            <input
              type="checkbox"
              checked={expired}
              onChange={(e) => {
                setExpired(e.target.checked);
                setPage(1);
              }}
            />{" "}
            Chỉ quá hạn
          </label>
        </div>
        {error ? (
          <PageError error={error} retry={() => setReload((x) => x + 1)} />
        ) : loading ? (
          <LoadState />
        ) : !data?.content?.length ? (
          <EmptyState
            title="Không có nhân khẩu phù hợp"
            description="Thử tìm theo tên khác hoặc đổi bộ lọc."
          />
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Họ và tên</th>
                  <th>Ngày sinh</th>
                  <th>Hộ</th>
                  <th>Căn hộ</th>
                  <th>Vai trò trong hộ</th>
                  <th>Trạng thái</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((r) => (
                  <tr key={r.id}>
                    <td>
                      <Link className="table-link" to={`/nhan-khau/${r.id}`}>
                        {r.fullName}
                      </Link>
                    </td>
                    <td>{formatDate(r.dateOfBirth)}</td>
                    <td>
                      <Link to={`/ho-gia-dinh/${r.householdId}`}>
                        {r.householdCode}
                      </Link>
                    </td>
                    <td>{r.apartmentNo}</td>
                    <td>{r.isHead ? "Chủ hộ" : r.relationshipToHead || "—"}</td>
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
                        Chi tiết <ArrowRight size={15} />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {data && !error && (
          <Pagination
            page={page}
            totalElements={data.totalElements}
            totalPages={data.totalPages}
            onChange={setPage}
          />
        )}
      </section>
      <Alert tone="info">
        Khi cần ghi nhận chuyển đi, tạm vắng hoặc tạm trú cho người đã có hồ sơ,
        hãy liên hệ người phụ trách biến động cư trú.
      </Alert>
    </>
  );
}
