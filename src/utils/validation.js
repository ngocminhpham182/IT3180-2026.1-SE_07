import { todayLocal } from "./format.js";
const CONTACT = /^(?:0\d{9}|\+84\d{9})$/;
const CODE = /^[A-Za-z0-9._-]+$/;
const validDate = (d) =>
  /^\d{4}-\d{2}-\d{2}$/.test(d || "") &&
  !Number.isNaN(Date.parse(d)) &&
  new Date(d).toISOString().slice(0, 10) === d;
const validDOB = (d) => validDate(d) && d >= "1900-01-01" && d <= todayLocal();
const genderValid = (g) => !g || ["MALE", "FEMALE", "OTHER"].includes(g);
const isEmail = (x) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(x);
const validPhone = (x) => !x || CONTACT.test(x);
const text = (x) => String(x ?? "").trim();
export function validateHousehold(f, creating) {
  const e = {};
  if (!CODE.test(text(f.householdCode)) || text(f.householdCode).length > 30)
    e.householdCode = "Mã hộ gồm 1–30 ký tự: chữ, số, dấu chấm, _ hoặc -.";
  if (!CODE.test(text(f.apartmentNo)) || text(f.apartmentNo).length > 20)
    e.apartmentNo = "Số căn hộ gồm 1–20 ký tự hợp lệ.";
  if (
    f.floor !== "" &&
    f.floor !== null &&
    f.floor !== undefined &&
    (!/^\d+$/.test(String(f.floor)) || +f.floor < 1 || +f.floor > 60)
  )
    e.floor = "Tầng phải là số nguyên từ 1 đến 60.";
  if (
    !/^(?:\d+)(?:\.\d{1,2})?$/.test(String(f.area)) ||
    +f.area <= 0 ||
    +f.area > 1000
  )
    e.area = "Diện tích phải lớn hơn 0, tối đa 1000 m² và 2 chữ số thập phân.";
  if (creating) {
    const head = f.head || {};
    if (!genderValid(head.gender)) e["head.gender"] = "Giới tính không hợp lệ.";
    if (text(head.fullName).length < 2 || text(head.fullName).length > 150)
      e["head.fullName"] = "Họ tên chủ hộ gồm 2–150 ký tự.";
    if (!validDOB(head.dateOfBirth))
      e["head.dateOfBirth"] = "Ngày sinh từ 01/01/1900 đến hôm nay.";
    if (head.identityNumber && !/^(?:\d{9}|\d{12})$/.test(head.identityNumber))
      e["head.identityNumber"] = "CCCD/CMND phải gồm 9 hoặc 12 chữ số.";
    if (!validPhone(text(head.phone)))
      e["head.phone"] = "Số điện thoại không đúng định dạng.";
  }
  return e;
}
export function validateResident(f, creating) {
  const e = {};
  if (text(f.fullName).length < 2 || text(f.fullName).length > 150)
    e.fullName = "Họ tên gồm 2–150 ký tự.";
  if (!validDOB(f.dateOfBirth))
    e.dateOfBirth = "Ngày sinh từ 01/01/1900 đến hôm nay.";
  if (
    text(f.relationshipToHead).length < 1 ||
    text(f.relationshipToHead).length > 50
  )
    e.relationshipToHead = "Quan hệ với chủ hộ gồm 1–50 ký tự.";
  if (f.identityNumber && !/^(?:\d{9}|\d{12})$/.test(f.identityNumber))
    e.identityNumber = "CCCD/CMND phải gồm 9 hoặc 12 chữ số.";
  if (!validPhone(text(f.phone)))
    e.phone = "Số điện thoại không đúng định dạng.";
  if (!genderValid(f.gender)) e.gender = "Giới tính không hợp lệ.";
  if (creating) {
    if (
      !["MOVE_IN", "BIRTH", "TEMPORARY_RESIDENCE", "EXISTING"].includes(
        f.entryType,
      )
    )
      e.entryType = "Hình thức vào hộ không hợp lệ.";
    if (f.entryDate && (!validDate(f.entryDate) || f.entryDate > todayLocal()))
      e.entryDate = "Ngày vào hộ không được ở tương lai.";
    if (f.entryType === "TEMPORARY_RESIDENCE") {
      if (!validDate(f.startDate))
        e.startDate = "Vui lòng chọn ngày bắt đầu tạm trú.";
      if (
        f.endDate &&
        (!validDate(f.endDate) || (f.startDate && f.endDate < f.startDate))
      )
        e.endDate = "Ngày kết thúc không được sớm hơn ngày bắt đầu.";
    }
  }
  return e;
}
export function validateUser(f) {
  const e = {};
  if (
    text(f.username).length < 4 ||
    text(f.username).length > 50 ||
    !CODE.test(text(f.username))
  )
    e.username = "Tên đăng nhập gồm 4–50 ký tự chữ/số/./_/-.";
  if (
    f.password.length < 8 ||
    f.password.length > 128 ||
    f.password.toLowerCase() === f.username.toLowerCase()
  )
    e.password = "Mật khẩu gồm 8–128 ký tự, không trùng tên đăng nhập.";
  if (text(f.fullName).length < 2 || text(f.fullName).length > 150)
    e.fullName = "Họ tên gồm 2–150 ký tự.";
  if (f.email && !isEmail(text(f.email)))
    e.email = "Email không đúng định dạng.";
  if (!validPhone(text(f.phone)))
    e.phone = "Số điện thoại không đúng định dạng.";
  if (
    !f.roles?.length ||
    f.roles.some(
      (r) => !["BQT", "ACCOUNTANT", "RESIDENT", "AUTHORITY"].includes(r),
    ) ||
    new Set(f.roles).size !== f.roles.length
  )
    e.roles = "Chọn ít nhất một vai trò.";
  if (f.roles?.includes("RESIDENT") && !f.residentId)
    e.residentId = "Tài khoản cư dân phải liên kết một nhân khẩu đang cư trú.";
  return e;
}
export function validateProfile(f) {
  const e = {};
  if (text(f.fullName).length < 2 || text(f.fullName).length > 150)
    e.fullName = "Họ tên gồm 2–150 ký tự.";
  if (f.email && !isEmail(text(f.email)))
    e.email = "Email không đúng định dạng.";
  if (!validPhone(text(f.phone)))
    e.phone = "Số điện thoại không đúng định dạng.";
  return e;
}
export function validatePassword(f, username = "") {
  const e = {};
  if (!f.oldPassword) e.oldPassword = "Nhập mật khẩu hiện tại.";
  if (
    f.newPassword.length < 8 ||
    f.newPassword.length > 128 ||
    f.newPassword.toLowerCase() === username.toLowerCase() ||
    f.newPassword === f.oldPassword
  )
    e.newPassword =
      "Mật khẩu mới dài 8–128 ký tự, khác mật khẩu cũ và tên đăng nhập.";
  if (f.confirmPassword !== f.newPassword)
    e.confirmPassword = "Mật khẩu xác nhận không khớp.";
  return e;
}
