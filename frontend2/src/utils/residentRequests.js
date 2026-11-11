export const requestTypes = {
  PROFILE_UPDATE: "Đề nghị sửa thông tin",
  TEMPORARY_ABSENCE: "Khai báo tạm vắng",
  TEMPORARY_RESIDENCE: "Khai báo tạm trú",
};
export const requestStatuses = {
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Từ chối",
  CANCELLED: "Đã hủy",
};
export const profileFields = {
  fullName: "Họ và tên",
  dateOfBirth: "Ngày sinh",
  gender: "Giới tính",
  identityNumber: "CCCD/CMND",
  phone: "Số điện thoại",
  relationshipToHead: "Quan hệ với chủ hộ",
};
export const todayVietnam = (date = new Date()) =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
export const profilePayload = (person = {}) =>
  Object.fromEntries(
    Object.keys(profileFields).map((key) => [
      key,
      String(person[key] ?? "").trim() || null,
    ]),
  );
const validDate = (value) =>
  /^\d{4}-\d{2}-\d{2}$/.test(value || "") &&
  !Number.isNaN(Date.parse(value)) &&
  new Date(value).toISOString().slice(0, 10) === value;
export function validateRequest(body, resident, today = todayVietnam()) {
  const errors = {};
  if (!requestTypes[body.type]) errors.type = "Chọn loại đề nghị hợp lệ.";
  if (
    body.type !== "TEMPORARY_RESIDENCE" &&
    (!resident || String(body.residentId) !== String(resident.id))
  )
    errors.residentId = "Chọn thành viên thuộc hộ của bạn.";
  const prefix =
    body.type === "PROFILE_UPDATE"
      ? "profile"
      : body.type === "TEMPORARY_RESIDENCE"
        ? "person"
        : null;
  if (prefix) {
    const p = profilePayload(body[prefix]);
    if (!p.fullName || p.fullName.length < 2 || p.fullName.length > 150)
      errors[`${prefix}.fullName`] = "Họ tên phải có từ 2 đến 150 ký tự.";
    if (
      !validDate(p.dateOfBirth) ||
      p.dateOfBirth < "1900-01-01" ||
      p.dateOfBirth > today
    )
      errors[`${prefix}.dateOfBirth`] =
        "Ngày sinh phải từ 01/01/1900 đến hôm nay.";
    if (p.gender && !["MALE", "FEMALE"].includes(p.gender))
      errors[`${prefix}.gender`] = "Giới tính không hợp lệ.";
    if (p.identityNumber && !/^(\d{9}|\d{12})$/.test(p.identityNumber))
      errors[`${prefix}.identityNumber`] =
        "CCCD/CMND phải gồm 9 hoặc 12 chữ số.";
    if (p.phone && !/^(0\d{9}|\+84\d{9})$/.test(p.phone))
      errors[`${prefix}.phone`] = "Số điện thoại không hợp lệ.";
    if (!p.relationshipToHead || p.relationshipToHead.length > 50)
      errors[`${prefix}.relationshipToHead`] =
        "Quan hệ với chủ hộ phải có từ 1 đến 50 ký tự.";
    if (
      body.type === "PROFILE_UPDATE" &&
      JSON.stringify(p) === JSON.stringify(profilePayload(resident))
    )
      errors.form = "Chưa có thay đổi nào so với thông tin hiện tại.";
  }
  if (["TEMPORARY_ABSENCE", "TEMPORARY_RESIDENCE"].includes(body.type)) {
    const dob =
      body.type === "TEMPORARY_ABSENCE"
        ? resident?.dateOfBirth
        : body.person?.dateOfBirth;
    if (!validDate(body.startDate))
      errors.startDate = "Vui lòng nhập ngày bắt đầu hợp lệ.";
    else if (body.startDate > today)
      errors.startDate = "Ngày bắt đầu không được ở tương lai.";
    else if (dob && body.startDate < dob)
      errors.startDate = "Ngày bắt đầu không được trước ngày sinh.";
    if (!validDate(body.endDate))
      errors.endDate = "Vui lòng nhập ngày kết thúc hợp lệ.";
    else if (body.endDate < body.startDate)
      errors.endDate = "Ngày kết thúc không được sớm hơn ngày bắt đầu.";
    if (
      body.type === "TEMPORARY_ABSENCE" &&
      resident?.residencyStatus !== "RESIDING"
    )
      errors.residentId = "Chỉ khai tạm vắng cho thành viên đang cư trú.";
  }
  if ((body.note || "").trim().length > 500)
    errors.note = "Ghi chú tối đa 500 ký tự.";
  return errors;
}
export const requestsChanged = () =>
  window.dispatchEvent(new Event("bluemoon:requests-changed"));
