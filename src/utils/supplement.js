export const movementLabels = {
  MOVE_IN: "Chuyển đến",
  MOVE_OUT: "Chuyển đi",
  TRANSFER: "Chuyển hộ",
  BIRTH: "Khai sinh",
  DEATH: "Ghi nhận mất",
  TEMPORARY_ABSENCE: "Tạm vắng",
  TEMPORARY_RESIDENCE: "Tạm trú",
  RETURN: "Trở về",
};
export const allowedMovements = (status) =>
  ({
    RESIDING: [
      "MOVE_OUT",
      "TRANSFER",
      "DEATH",
      "TEMPORARY_ABSENCE",
      "TEMPORARY_RESIDENCE",
    ],
    TEMPORARILY_ABSENT: ["MOVE_OUT", "DEATH", "RETURN"],
    TEMPORARY_RESIDENCE: ["MOVE_OUT", "TRANSFER", "DEATH"],
    MOVED_OUT: ["MOVE_IN"],
    DECEASED: [],
  })[status] || [];
export const money = (v) =>
  v == null
    ? "—"
    : `${Number(v).toLocaleString("vi-VN", { maximumFractionDigits: 2 })} đ`;
export const methodLabels = {
  PER_AREA: "Theo diện tích",
  FIXED: "Cố định mỗi hộ",
  MANUAL: "Tự nguyện",
};
export const cycleLabels = { MONTHLY: "Hằng tháng", ONE_TIME: "Một lần" };
export const vietnamToday = () =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
export const validDate = (d) =>
  /^\d{4}-\d{2}-\d{2}$/.test(d || "") &&
  !Number.isNaN(Date.parse(d)) &&
  new Date(d).toISOString().slice(0, 10) === d;
export function validateMovement(f, resident, history = []) {
  const e = {};
  if (!allowedMovements(resident.residencyStatus).includes(f.movementType))
    e.movementType = "Loại biến động không phù hợp tình trạng cư trú hiện tại.";
  const temporary = f.movementType.startsWith("TEMPORARY_");
  const key = temporary ? "startDate" : "effectiveDate";
  const date = f[key];
  const latest = history
    .map((x) => x.effectiveDate)
    .filter(Boolean)
    .sort()
    .at(-1);
  if (!validDate(date) || date > vietnamToday())
    e[key] = "Nhập ngày hợp lệ, không ở tương lai.";
  else if (
    date < resident.dateOfBirth ||
    (latest && date < latest) ||
    (["MOVE_OUT", "DEATH"].includes(f.movementType) &&
      resident.moveInDate &&
      date < resident.moveInDate)
  )
    e[key] =
      "Ngày không được trước ngày sinh, ngày vào hộ hoặc biến động gần nhất.";
  if (temporary && f.endDate && (!validDate(f.endDate) || f.endDate < date))
    e.endDate = "Ngày kết thúc không được sớm hơn ngày bắt đầu.";
  if (["TRANSFER", "MOVE_IN"].includes(f.movementType)) {
    if (
      !f.newHouseholdId ||
      (f.movementType === "TRANSFER" &&
        String(f.newHouseholdId) === String(resident.householdId))
    )
      e.newHouseholdId = "Chọn hộ đích hợp lệ, khác hộ hiện tại khi chuyển hộ.";
    if (
      !f.relationshipToHead?.trim() ||
      f.relationshipToHead.trim().length > 50
    )
      e.relationshipToHead = "Quan hệ với chủ hộ gồm 1–50 ký tự.";
  }
  if (f.note?.length > 500) e.note = "Ghi chú tối đa 500 ký tự.";
  return e;
}
export function movementBody(f) {
  const body = { movementType: f.movementType, note: f.note.trim() };
  if (f.movementType.startsWith("TEMPORARY_"))
    Object.assign(body, { startDate: f.startDate, endDate: f.endDate || null });
  else body.effectiveDate = f.effectiveDate;
  if (["TRANSFER", "MOVE_IN"].includes(f.movementType))
    Object.assign(body, {
      newHouseholdId: Number(f.newHouseholdId),
      relationshipToHead: f.relationshipToHead.trim(),
    });
  if (
    f.newHeadResidentId &&
    ["TRANSFER", "MOVE_OUT", "DEATH"].includes(f.movementType)
  )
    body.newHeadResidentId = Number(f.newHeadResidentId);
  return body;
}
export function validateFee(f) {
  const e = {};
  if (!/^[A-Z0-9_]{2,30}$/.test(f.code.trim().toUpperCase()))
    e.code = "Mã gồm 2–30 chữ cái, số hoặc dấu _.";
  if (!f.name.trim() || f.name.trim().length > 150)
    e.name = "Tên khoản thu gồm 1–150 ký tự.";
  if (f.description.length > 1000) e.description = "Mô tả tối đa 1000 ký tự.";
  if (!["MANDATORY", "VOLUNTARY"].includes(f.feeKind))
    e.feeKind = "Chọn loại khoản thu.";
  if (f.feeKind === "MANDATORY") {
    if (!["PER_AREA", "FIXED"].includes(f.calculationMethod))
      e.calculationMethod = "Chọn cách tính hợp lệ.";
    if (
      !/^\d+(\.\d{1,2})?$/.test(String(f.unitPrice)) ||
      +f.unitPrice <= 0 ||
      +f.unitPrice > 1000000000
    )
      e.unitPrice =
        "Đơn giá lớn hơn 0, tối đa 1.000.000.000 và 2 chữ số thập phân.";
    if (!/^\d+$/.test(String(f.dueDay)) || +f.dueDay < 1 || +f.dueDay > 28)
      e.dueDay = "Hạn nộp là ngày từ 1 đến 28, không được để trống.";
    if (!["MONTHLY", "ONE_TIME"].includes(f.billingCycle))
      e.billingCycle = "Chọn chu kỳ thu.";
  }
  return e;
}
export function feeBody(f) {
  const body = {
    code: f.code.trim().toUpperCase(),
    name: f.name.trim(),
    description: f.description.trim(),
    feeKind: f.feeKind,
    calculationMethod:
      f.feeKind === "VOLUNTARY" ? "MANUAL" : f.calculationMethod,
  };
  if (f.feeKind === "MANDATORY")
    Object.assign(body, {
      unitPrice: Number(f.unitPrice),
      dueDay: Number(f.dueDay),
      billingCycle: f.billingCycle,
    });
  return body;
}
export function validatePeriod(f) {
  const e = {};
  if (!/^[A-Za-z0-9._-]{1,30}$/.test(f.code.trim()))
    e.code = "Mã gồm 1–30 chữ, số, dấu chấm, _ hoặc -.";
  if (!f.name.trim() || f.name.trim().length > 100)
    e.name = "Tên kỳ gồm 1–100 ký tự.";
  if (!validDate(f.startDate)) e.startDate = "Nhập ngày bắt đầu hợp lệ.";
  if (!validDate(f.endDate) || f.endDate < f.startDate)
    e.endDate = "Ngày kết thúc không được trước ngày bắt đầu.";
  return e;
}
