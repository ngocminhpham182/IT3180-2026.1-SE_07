import React, { useEffect, useRef } from "react";
import {
  AlertCircle,
  Check,
  ChevronLeft,
  ChevronRight,
  CircleHelp,
  LoaderCircle,
  Search,
  X,
} from "lucide-react";
export function Field({
  label,
  name,
  error,
  required,
  note,
  children,
  wide = false,
}) {
  return (
    <div className={`field ${wide ? "field-wide" : ""}`}>
      <label htmlFor={name}>
        {label}
        {required && <span className="required"> *</span>}
      </label>
      {React.isValidElement(children) &&
      (children.type === TextInput || children.type === SelectInput)
        ? React.cloneElement(children, {
            error,
            required,
            "aria-describedby": error ? `${name}-error` : undefined,
          })
        : children}
      {note && <span className="field-note">{note}</span>}
      {error && (
        <span className="field-error" id={`${name}-error`} role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
export function TextInput({ name, error, ...props }) {
  return (
    <input
      id={name}
      name={name}
      aria-invalid={Boolean(error)}
      className={error ? "invalid" : ""}
      {...props}
    />
  );
}
export function SelectInput({ name, error, children, ...props }) {
  return (
    <select
      id={name}
      name={name}
      aria-invalid={Boolean(error)}
      className={error ? "invalid" : ""}
      {...props}
    >
      {children}
    </select>
  );
}
export function Alert({ children, tone = "danger", onClose }) {
  if (!children) return null;
  return (
    <div className={`alert alert-${tone}`} role="alert">
      {tone === "success" ? <Check size={18} /> : <AlertCircle size={18} />}
      <span>{children}</span>
      {onClose && (
        <button
          className="icon-btn"
          type="button"
          onClick={onClose}
          aria-label="Đóng thông báo"
        >
          <X size={17} />
        </button>
      )}
    </div>
  );
}
export function StatusBadge({ children, tone = "muted" }) {
  return <span className={`badge badge-${tone}`}>{children}</span>;
}
export function PillStatus({ status, label }) {
  const tone = ["ACTIVE", "RESIDING"].includes(status)
    ? "green"
    : ["INACTIVE", "MOVED_OUT", "DECEASED"].includes(status)
      ? "slate"
      : "amber";
  return <StatusBadge tone={tone}>{label || status}</StatusBadge>;
}
export function Modal({
  title,
  children,
  onClose,
  footer,
  maxWidth = 650,
  busy = false,
}) {
  const dialog = useRef(null);
  const state = useRef({ busy, onClose });
  state.current = { busy, onClose };
  useEffect(() => {
    const previous = document.activeElement;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const focusable = () => [
      ...dialog.current.querySelectorAll(
        'button:not(:disabled), input:not(:disabled), select:not(:disabled), a[href], [tabindex="0"]',
      ),
    ];
    (focusable()[0] || dialog.current).focus();
    const handler = (event) => {
      if (event.key === "Escape") {
        event.preventDefault();
        if (!state.current.busy) state.current.onClose();
      }
      if (event.key === "Tab") {
        const items = focusable();
        const first = items[0];
        const last = items.at(-1);
        if (!first) {
          event.preventDefault();
          dialog.current.focus();
        } else if (
          event.shiftKey &&
          (document.activeElement === first ||
            !dialog.current.contains(document.activeElement))
        ) {
          event.preventDefault();
          last.focus();
        } else if (
          !event.shiftKey &&
          (document.activeElement === last ||
            !dialog.current.contains(document.activeElement))
        ) {
          event.preventDefault();
          first.focus();
        }
      }
    };
    window.addEventListener("keydown", handler);
    return () => {
      window.removeEventListener("keydown", handler);
      document.body.style.overflow = overflow;
      previous?.focus();
    };
  }, []);
  const close = () => {
    if (!busy) onClose();
  };
  return (
    <div
      className="modal-backdrop"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) close();
      }}
    >
      <div
        ref={dialog}
        tabIndex={-1}
        className="modal"
        style={{ maxWidth }}
        role="dialog"
        aria-modal="true"
        aria-busy={busy}
        aria-label={title}
      >
        <header className="modal-header">
          <h2>{title}</h2>
          <button
            type="button"
            className="icon-btn"
            disabled={busy}
            onClick={close}
            aria-label="Đóng"
          >
            <X size={20} />
          </button>
        </header>
        <div className="modal-body">{children}</div>
        {footer && <footer className="modal-footer">{footer}</footer>}
      </div>
    </div>
  );
}
export function Confirm({
  title = "Xác nhận thao tác",
  description,
  danger = false,
  action = "Xác nhận",
  onClose,
  onConfirm,
  busy = false,
}) {
  return (
    <Modal
      busy={busy}
      title={title}
      onClose={onClose}
      maxWidth={500}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose} disabled={busy}>
            Hủy
          </button>
          <button
            className={`btn ${danger ? "btn-danger" : "btn-primary"}`}
            onClick={onConfirm}
            disabled={busy}
          >
            {busy ? "Đang xử lý..." : action}
          </button>
        </>
      }
    >
      <p className="confirm-text">{description}</p>
    </Modal>
  );
}
export function PageHeader({ eyebrow, title, description, actions }) {
  return (
    <div className="page-header">
      <div>
        {eyebrow && <div className="eyebrow">{eyebrow}</div>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {actions && <div className="header-actions">{actions}</div>}
    </div>
  );
}
export function EmptyState({
  title = "Không có dữ liệu",
  description = "Chưa có bản ghi phù hợp với bộ lọc hiện tại.",
  action,
}) {
  return (
    <div className="empty">
      <div className="empty-symbol">
        <Search size={22} />
      </div>
      <strong>{title}</strong>
      <p>{description}</p>
      {action}
    </div>
  );
}
export function LoadState() {
  return (
    <div className="load-state">
      <LoaderCircle className="spin" size={20} /> Đang tải dữ liệu...
    </div>
  );
}
export function PageError({ error, retry }) {
  return (
    <div className="panel">
      <Alert>{error?.message || "Không tải được dữ liệu."}</Alert>
      {retry && (
        <button className="btn btn-secondary" onClick={retry}>
          Thử lại
        </button>
      )}
    </div>
  );
}
export function Pagination({
  page,
  totalPages = 1,
  totalElements = 0,
  onChange,
}) {
  return (
    <div className="pagination">
      <span>
        Trang <strong>{page}</strong> / {Math.max(1, totalPages)} ·{" "}
        {totalElements} kết quả
      </span>
      <div className="pagination-actions">
        <button
          type="button"
          aria-label="Trang trước"
          disabled={page <= 1}
          onClick={() => onChange(page - 1)}
        >
          <ChevronLeft size={17} />
        </button>
        <button
          type="button"
          aria-label="Trang sau"
          disabled={page >= totalPages}
          onClick={() => onChange(page + 1)}
        >
          <ChevronRight size={17} />
        </button>
      </div>
    </div>
  );
}
export function HelpText({ children }) {
  return (
    <div className="help-text">
      <CircleHelp size={15} />
      <span>{children}</span>
    </div>
  );
}
export function SaveButton({ busy, children = "Lưu thông tin", form }) {
  return (
    <button
      form={form}
      type="submit"
      className="btn btn-primary"
      disabled={busy}
    >
      {busy && <LoaderCircle className="spin" size={16} />}
      {busy ? "Đang lưu..." : children}
    </button>
  );
}
export function DataLine({ label, children }) {
  return (
    <div className="data-line">
      <dt>{label}</dt>
      <dd>{children ?? "—"}</dd>
    </div>
  );
}
export function Panel({ title, children, extra }) {
  return (
    <section className="panel">
      <div className="panel-header">
        <h2>{title}</h2>
        {extra}
      </div>
      {children}
    </section>
  );
}
