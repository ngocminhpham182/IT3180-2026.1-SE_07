import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowRight, Plus, Search } from "lucide-react";
import { householdApi } from "../api/services";
import { useAuth } from "../auth/AuthContext";
import HouseholdForm from "../components/HouseholdForm";
import {
  Alert,
  EmptyState,
  LoadState,
  PageError,
  PageHeader,
  Pagination,
  PillStatus,
} from "../components/UI";
import { formatArea, householdLabel } from "../utils/format";
export default function HouseholdsPage() {
  const [keyword, setKeyword] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("ALL");
  const [page, setPage] = useState(1);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [create, setCreate] = useState(false);
  const { hasRole } = useAuth();
  const navigate = useNavigate();
  const [reload, setReload] = useState(0);
  useEffect(() => {
    const t = setTimeout(() => {
      setQuery(keyword.trim());
      setPage(1);
    }, 350);
    return () => clearTimeout(t);
  }, [keyword]);
  useEffect(() => {
    const ctrl = new AbortController();
    setLoading(true);
    setError(null);
    householdApi
      .list({ keyword: query, status, page, size: 20 }, { signal: ctrl.signal })
      .then(setData)
      .catch((e) => {
        if (e.name !== "AbortError") setError(e);
      })
      .finally(() => {
        if (!ctrl.signal.aborted) setLoading(false);
      });
    return () => ctrl.abort();
  }, [query, status, page, reload]);
  return (
    <>
      <PageHeader
        eyebrow="US-01 · QUẢN LÝ CƯ DÂN"
        title="Hộ gia đình"
        description="Quản lý danh sách căn hộ, chủ hộ và tình trạng hoạt động."
        actions={
          hasRole("BQT") && (
            <button className="btn btn-primary" onClick={() => setCreate(true)}>
              <Plus size={18} /> Thêm hộ gia đình
            </button>
          )
        }
      />
      <section className="panel">
        <div className="filter-bar">
          <div className="search-box">
            <Search size={18} />
            <input
              aria-label="Tìm mã hộ hoặc số căn hộ"
              placeholder="Tìm theo mã hộ, số căn hộ..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>
          <select
            aria-label="Lọc trạng thái hộ"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(1);
            }}
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang hoạt động</option>
            <option value="INACTIVE">Ngừng hoạt động</option>
          </select>
        </div>
        {error ? (
          <PageError error={error} retry={() => setReload((x) => x + 1)} />
        ) : loading ? (
          <LoadState />
        ) : !data?.content?.length ? (
          <EmptyState
            title="Không tìm thấy hộ gia đình"
            description="Thử đổi từ khóa hoặc bộ lọc trạng thái."
          />
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Mã hộ</th>
                  <th>Số căn hộ</th>
                  <th>Chủ hộ</th>
                  <th>Diện tích</th>
                  <th>Đang thuộc hộ</th>
                  <th>Thực tế đang ở</th>
                  <th>Trạng thái</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((h) => (
                  <tr key={h.id}>
                    <td>
                      <Link className="table-link" to={`/ho-gia-dinh/${h.id}`}>
                        {h.householdCode}
                      </Link>
                    </td>
                    <td>{h.apartmentNo}</td>
                    <td>{h.headName || h.head?.fullName || "Chưa có"}</td>
                    <td>{formatArea(h.area)}</td>
                    <td>{h.residentCount ?? "—"}</td>
                    <td>{h.presentCount ?? "—"}</td>
                    <td>
                      <PillStatus
                        status={h.status}
                        label={householdLabel[h.status]}
                      />
                    </td>
                    <td>
                      <Link
                        to={`/ho-gia-dinh/${h.id}`}
                        className="table-action"
                      >
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
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onChange={setPage}
          />
        )}
      </section>
      {create && (
        <HouseholdForm
          onClose={() => setCreate(false)}
          onSaved={(h) => {
            setCreate(false);
            navigate(`/ho-gia-dinh/${h.id}`);
          }}
        />
      )}
    </>
  );
}
