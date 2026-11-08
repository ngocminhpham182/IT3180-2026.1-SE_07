import React, { useEffect, useState } from "react";
import { Plus, Search, ShieldCheck } from "lucide-react";
import { userApi } from "../api/services";
import { useAuth } from "../auth/AuthContext";
import { canAssignRoles, isSelf } from "../auth/permissions";
import UserForm, { RoleChoices } from "../components/UserForm";
import {
  Alert,
  Confirm,
  DataLine,
  EmptyState,
  LoadState,
  Modal,
  PageError,
  PageHeader,
  Pagination,
  PillStatus,
  StatusBadge,
} from "../components/UI";
import { formatDateTime, roleLabel } from "../utils/format";
export default function UsersPage() {
  const { user: current } = useAuth();
  const [keyword, setKeyword] = useState("");
  const [query, setQuery] = useState("");
  const [role, setRole] = useState("");
  const [active, setActive] = useState("");
  const [page, setPage] = useState(1);
  const [roles, setRoles] = useState([]);
  const [rolesError, setRolesError] = useState(null);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reload, setReload] = useState(0);
  const [notice, setNotice] = useState("");
  const [create, setCreate] = useState(false);
  const [viewing, setViewing] = useState(null);
  const [viewError, setViewError] = useState(null);
  const [editing, setEditing] = useState(null);
  const [newRoles, setNewRoles] = useState([]);
  const [confirm, setConfirm] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    let active = true;
    setRolesError(null);
    userApi
      .roles()
      .then((r) => {
        if (active) setRoles(r);
      })
      .catch((e) => {
        if (active) setRolesError(e);
      });
    return () => {
      active = false;
    };
  }, [reload]);
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
    userApi
      .list(
        {
          keyword: query,
          role: role || undefined,
          active: active === "" ? undefined : active,
          page,
          size: 20,
        },
        { signal: ctrl.signal },
      )
      .then(setData)
      .catch((e) => {
        if (e.name !== "AbortError") setError(e);
      })
      .finally(() => {
        if (!ctrl.signal.aborted) setLoading(false);
      });
    return () => ctrl.abort();
  }, [query, role, active, page, reload]);
  const refresh = (message) => {
    setCreate(false);
    setEditing(null);
    setConfirm(null);
    setActionError(null);
    if (message) setNotice(message);
    setReload((x) => x + 1);
  };
  const openDetails = async (id) => {
    setViewing({ id, loading: true });
    setViewError(null);
    try {
      const detail = await userApi.get(id);
      setViewing(detail);
    } catch (err) {
      setViewError(err);
      setViewing({ id, loading: false });
    }
  };
  const saveRoles = async () => {
    if (busy) return;
    if (!canAssignRoles(current, editing, newRoles)) {
      setActionError({
        message:
          "Vai trò không hợp lệ. Giữ quyền Ban quản trị của chính mình và chỉ cấp quyền cư dân khi đã liên kết nhân khẩu.",
      });
      return;
    }
    setBusy(true);
    setActionError(null);
    try {
      if (!newRoles.length) {
        setActionError({ message: "Chọn ít nhất một vai trò." });
        return;
      }
      await userApi.updateRoles(editing.id, newRoles);
      refresh("Đã cập nhật quyền tài khoản.");
    } catch (err) {
      setActionError(err);
    } finally {
      setBusy(false);
    }
  };
  const toggleStatus = async () => {
    if (busy || isSelf(current, confirm)) return;
    setBusy(true);
    setActionError(null);
    try {
      await userApi.updateStatus(confirm.id, !confirm.isActive);
      refresh("Đã cập nhật trạng thái tài khoản.");
    } catch (err) {
      setActionError(err);
      setConfirm(null);
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <PageHeader
        eyebrow="US-09 · QUẢN TRỊ HỆ THỐNG"
        title="Tài khoản & phân quyền"
        description="Cấp quyền truy cập theo vai trò và kiểm soát trạng thái tài khoản."
        actions={
          <button
            className="btn btn-primary"
            disabled={!roles.length || !!rolesError}
            onClick={() => setCreate(true)}
          >
            <Plus size={18} /> Cấp tài khoản
          </button>
        }
      />
      {rolesError && (
        <PageError error={rolesError} retry={() => setReload((x) => x + 1)} />
      )}
      {notice && (
        <Alert tone="success" onClose={() => setNotice("")}>
          {notice}
        </Alert>
      )}
      {actionError && !editing && (
        <Alert onClose={() => setActionError(null)}>
          {actionError.message}
        </Alert>
      )}
      <section className="panel">
        <div className="filter-bar">
          <div className="search-box">
            <Search size={18} />
            <input
              aria-label="Tìm tài khoản"
              placeholder="Tên đăng nhập hoặc họ tên..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>
          <select
            aria-label="Lọc vai trò"
            value={role}
            onChange={(e) => {
              setRole(e.target.value);
              setPage(1);
            }}
          >
            <option value="">Mọi vai trò</option>
            {roles.map((r) => (
              <option value={r.code} key={r.code}>
                {r.name}
              </option>
            ))}
          </select>
          <select
            aria-label="Lọc trạng thái"
            value={active}
            onChange={(e) => {
              setActive(e.target.value);
              setPage(1);
            }}
          >
            <option value="">Mọi trạng thái</option>
            <option value="true">Hoạt động</option>
            <option value="false">Đã khóa</option>
          </select>
        </div>
        {error ? (
          <PageError error={error} retry={() => setReload((x) => x + 1)} />
        ) : loading ? (
          <LoadState />
        ) : !data?.content?.length ? (
          <EmptyState title="Không có tài khoản phù hợp" />
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Tài khoản</th>
                  <th>Họ và tên</th>
                  <th>Vai trò</th>
                  <th>Trạng thái</th>
                  <th>Đăng nhập gần nhất</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <strong>{u.username}</strong>
                      {u.id === current?.id && (
                        <StatusBadge tone="blue">Bạn</StatusBadge>
                      )}
                    </td>
                    <td>{u.fullName}</td>
                    <td>
                      <div className="role-tags">
                        {(u.roles || []).map((r) => (
                          <StatusBadge key={r} tone="blue">
                            {roleLabel[r] || r}
                          </StatusBadge>
                        ))}
                      </div>
                    </td>
                    <td>
                      <PillStatus
                        status={u.isActive ? "ACTIVE" : "INACTIVE"}
                        label={u.isActive ? "Hoạt động" : "Đã khóa"}
                      />
                    </td>
                    <td>{formatDateTime(u.lastLoginAt)}</td>
                    <td>
                      <div className="action-row">
                        <button
                          className="link-btn"
                          onClick={() => openDetails(u.id)}
                        >
                          Chi tiết
                        </button>
                        <button
                          className="link-btn"
                          onClick={() => {
                            setEditing(u);
                            setNewRoles([...(u.roles || [])]);
                            setActionError(null);
                          }}
                        >
                          Phân quyền
                        </button>
                        <button
                          className={`link-btn ${u.isActive ? "danger-text" : ""}`}
                          disabled={u.id === current?.id && u.isActive}
                          title={
                            u.id === current?.id
                              ? "Không thể tự khóa chính mình"
                              : ""
                          }
                          onClick={() => setConfirm(u)}
                        >
                          {u.isActive ? "Khóa" : "Mở khóa"}
                        </button>
                      </div>
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
      {viewing && (
        <Modal
          busy={viewing.loading}
          title={
            viewing.username
              ? `Chi tiết tài khoản: ${viewing.username}`
              : "Chi tiết tài khoản"
          }
          onClose={() => setViewing(null)}
          footer={
            <button
              className="btn btn-ghost"
              disabled={viewing.loading}
              onClick={() => setViewing(null)}
            >
              Đóng
            </button>
          }
        >
          {viewError ? (
            <Alert>{viewError.message}</Alert>
          ) : viewing.loading ? (
            <LoadState />
          ) : (
            <dl className="data-list">
              <DataLine label="Tên đăng nhập">{viewing.username}</DataLine>
              <DataLine label="Họ và tên">{viewing.fullName}</DataLine>
              <DataLine label="Email">{viewing.email || "—"}</DataLine>
              <DataLine label="Điện thoại">{viewing.phone || "—"}</DataLine>
              <DataLine label="Vai trò">
                {(viewing.roles || []).map((r) => roleLabel[r] || r).join(", ")}
              </DataLine>
              <DataLine label="Nhân khẩu liên kết">
                {viewing.residentId ?? "Chưa liên kết"}
              </DataLine>
              <DataLine label="Trạng thái">
                {viewing.isActive ? "Hoạt động" : "Đã khóa"}
              </DataLine>
              <DataLine label="Lần đăng nhập gần nhất">
                {formatDateTime(viewing.lastLoginAt)}
              </DataLine>
              <DataLine label="Ngày tạo">
                {formatDateTime(viewing.createdAt)}
              </DataLine>
            </dl>
          )}
        </Modal>
      )}
      {create && (
        <UserForm
          roles={roles}
          onClose={() => setCreate(false)}
          onSaved={() => refresh("Đã cấp tài khoản mới thành công.")}
        />
      )}
      {editing && (
        <Modal
          busy={busy}
          title={`Phân quyền: ${editing.username}`}
          onClose={() => {
            setEditing(null);
            setActionError(null);
          }}
          footer={
            <>
              <button
                className="btn btn-ghost"
                disabled={busy}
                onClick={() => setEditing(null)}
              >
                Hủy
              </button>
              <button
                className="btn btn-primary"
                disabled={busy || !canAssignRoles(current, editing, newRoles)}
                onClick={saveRoles}
              >
                {busy ? "Đang lưu..." : "Lưu phân quyền"}
              </button>
            </>
          }
        >
          <p className="subtle">
            Quyền mới có hiệu lực ngay từ yêu cầu tiếp theo của người dùng.
          </p>
          {actionError && <Alert>{actionError.message}</Alert>}
          <RoleChoices
            disabledCodes={isSelf(current, editing) ? ["BQT"] : []}
            roles={roles}
            value={newRoles}
            onChange={setNewRoles}
          />
          {newRoles.includes("RESIDENT") && !editing.residentId && (
            <Alert tone="info">
              Tài khoản chưa liên kết nhân khẩu nên chưa thể nhận vai trò Cư
              dân.
            </Alert>
          )}
          {editing.id === current?.id && (
            <p className="info-inline">
              Bạn không thể tự gỡ vai trò Ban quản trị khỏi tài khoản của mình.
            </p>
          )}
        </Modal>
      )}
      {confirm && (
        <Confirm
          title={confirm.isActive ? "Khóa tài khoản?" : "Mở khóa tài khoản?"}
          description={
            confirm.isActive
              ? `Khóa ${confirm.username}: các phiên đăng nhập hiện tại sẽ mất hiệu lực.`
              : `Cho phép ${confirm.username} đăng nhập trở lại.`
          }
          action={confirm.isActive ? "Khóa tài khoản" : "Mở khóa"}
          danger={confirm.isActive}
          busy={busy}
          onClose={() => setConfirm(null)}
          onConfirm={toggleStatus}
        />
      )}
    </>
  );
}
