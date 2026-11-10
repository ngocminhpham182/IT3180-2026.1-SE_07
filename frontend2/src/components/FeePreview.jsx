import React, { useState } from "react";
import { Link } from "react-router-dom";
import { feeApi } from "../api/services";
import { Alert, DataLine, Modal, SaveButton } from "./UI";
import HouseholdPicker from "./HouseholdPicker";
import { money } from "../utils/supplement";
import { formatArea } from "../utils/format";
export default function FeePreview({ fee, onClose }) {
  const [householdId, setHouseholdId] = useState("");
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    if (busy) return;
    if (!householdId) {
      setError({ fields: { householdId: "Chọn hộ cần tính phí." } });
      return;
    }
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      setResult(
        await feeApi.preview({
          householdId: Number(householdId),
          feeCategoryId: fee.id,
        }),
      );
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={`Tính thử — ${fee.name}`}
      busy={busy}
      onClose={onClose}
      footer={
        <SaveButton form="preview-form" busy={busy}>
          Tính thử
        </SaveButton>
      }
    >
      <form id="preview-form" onSubmit={submit} noValidate>
        <fieldset className="form-fields" disabled={busy}>
          <HouseholdPicker
            value={householdId}
            onChange={(v) => {
              setHouseholdId(v);
              setResult(null);
              setError(null);
            }}
            error={error?.fields?.householdId}
          />
        </fieldset>
      </form>
      <Alert>
        {error?.message}
        {error?.code === "MISSING_AREA" && (
          <>
            {" "}
            Hộ chưa có diện tích.{" "}
            <Link
              to={`/ho-gia-dinh/${error.details?.householdId || householdId}`}
            >
              Cập nhật hồ sơ hộ
            </Link>
          </>
        )}
      </Alert>
      {result && (
        <div className="calculation-result" aria-live="polite">
          <h3>
            {result.householdCode} · {result.apartmentNo}
          </h3>
          <dl className="data-list">
            <DataLine label="Diện tích">{formatArea(result.area)}</DataLine>
            <DataLine label="Đơn giá">{money(result.unitPrice)}</DataLine>
            <DataLine label="Công thức">{result.formula}</DataLine>
            <DataLine label="Thành tiền">
              <strong>{money(result.amount)}</strong>
            </DataLine>
          </dl>
          <p>Đây là kết quả tính thử, chưa tạo khoản phải thu.</p>
        </div>
      )}
    </Modal>
  );
}
