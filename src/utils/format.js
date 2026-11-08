export const roleLabel = {
  BQT: "Ban quản trị",
  ACCOUNTANT: "Kế toán",
  RESIDENT: "Cư dân",
  AUTHORITY: "Cơ quan chức năng",
};
export const householdLabel = {
  ACTIVE: "Đang hoạt động",
  INACTIVE: "Ngừng hoạt động",
};
export const residenceLabel = {
  RESIDING: "Đang cư trú",
  TEMPORARILY_ABSENT: "Tạm vắng",
  TEMPORARY_RESIDENCE: "Tạm trú",
  MOVED_OUT: "Đã chuyển đi",
  DECEASED: "Đã mất",
};
export const genderLabel = { MALE: "Nam", FEMALE: "Nữ", OTHER: "Khác" };
export const entryLabel = {
  MOVE_IN: "Chuyển đến",
  BIRTH: "Khai sinh",
  TEMPORARY_RESIDENCE: "Tạm trú",
  EXISTING: "Nhập dữ liệu sẵn có",
};
export function todayLocal() {
  const d = new Date();
  return [
    d.getFullYear(),
    String(d.getMonth() + 1).padStart(2, "0"),
    String(d.getDate()).padStart(2, "0"),
  ].join("-");
}
export const formatDate = (d) =>
  d ? String(d).split("T")[0].split("-").reverse().join("/") : "—";
export const formatDateTime = (d) =>
  d ? new Date(d).toLocaleString("vi-VN") : "—";
export const formatArea = (v) =>
  v === null || v === undefined
    ? "—"
    : `${Number(v).toLocaleString("vi-VN", { maximumFractionDigits: 2 })} m²`;
export const firstRoleName = (roles = []) =>
  roles.map((r) => roleLabel[r] || r).join(", ") || "Chưa phân quyền";
