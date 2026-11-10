import React, { useEffect, useState } from "react";
import { householdApi } from "../api/services";
import { Alert, Field, SelectInput, TextInput, Pagination } from "./UI";
export default function HouseholdPicker({
  value,
  onChange,
  error,
  excludeId,
  name = "householdId",
}) {
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [data, setData] = useState(null);
  const [failure, setFailure] = useState(null);
  const [loading, setLoading] = useState(true);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const ctrl = new AbortController();
    setLoading(true);
    setFailure(null);
    const timer = setTimeout(
      () =>
        householdApi
          .list(
            { keyword: keyword.trim(), status: "ACTIVE", page, size: 20 },
            { signal: ctrl.signal },
          )
          .then((v) => {
            if (!ctrl.signal.aborted) setData(v);
          })
          .catch((e) => {
            if (!ctrl.signal.aborted) setFailure(e);
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
  }, [keyword, page, retry]);
  return (
    <div className="field-wide">
      <Field name={`${name}-search`} label="Tìm hộ đang hoạt động">
        <TextInput
          name={`${name}-search`}
          value={keyword}
          placeholder="Mã hộ hoặc căn hộ"
          onChange={(e) => {
            setKeyword(e.target.value);
            setPage(1);
            onChange("");
          }}
        />
      </Field>
      {failure && (
        <Alert>
          {failure.message}{" "}
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => setRetry((x) => x + 1)}
          >
            Thử tải hộ
          </button>
        </Alert>
      )}
      <Field name={name} label="Hộ đích / hộ tính phí" error={error} required>
        <SelectInput
          name={name}
          value={value}
          disabled={loading || !!failure}
          onChange={(e) => onChange(e.target.value)}
        >
          <option value="">
            {loading ? "Đang tải hộ..." : "Chọn hộ gia đình"}
          </option>
          {!loading &&
            !failure &&
            data?.content
              ?.filter((h) => String(h.id) !== String(excludeId))
              .map((h) => (
                <option key={h.id} value={h.id}>
                  {h.householdCode} — {h.apartmentNo}
                </option>
              ))}
        </SelectInput>
      </Field>
      {!loading && !failure && (
        <Pagination
          page={page}
          totalPages={data?.totalPages}
          totalElements={data?.totalElements}
          onChange={(p) => {
            setPage(p);
            onChange("");
          }}
        />
      )}
    </div>
  );
}
