import test from "node:test";
import assert from "node:assert/strict";
import {
  validateHousehold,
  validateResident,
  validateUser,
  validateProfile,
  validatePassword,
} from "../src/utils/validation.js";
import { formatDate, todayLocal } from "../src/utils/format.js";
const goodHousehold = {
  householdCode: "H001",
  apartmentNo: "A101",
  floor: 1,
  area: 75.5,
  head: {
    fullName: "Nguyễn Văn An",
    dateOfBirth: "1980-01-01",
    gender: "MALE",
    identityNumber: "012345678901",
    phone: "0901234567",
  },
};
test("US-01: form hộ và chủ hộ hợp lệ", () =>
  assert.deepEqual(validateHousehold(goodHousehold, true), {}));
test("US-01: sai mã hộ, diện tích, trùng trường head identity format", () => {
  const v = validateHousehold(
    {
      ...goodHousehold,
      householdCode: "Sai mã",
      area: "75.555",
      head: { ...goodHousehold.head, identityNumber: "123" },
    },
    true,
  );
  assert.ok(v.householdCode);
  assert.ok(v.area);
  assert.ok(v["head.identityNumber"]);
});
test("US-02: thông tin bắt buộc, ngày sinh <= hôm nay", () => {
  const v = validateResident(
    {
      fullName: "",
      dateOfBirth: "2999-01-01",
      relationshipToHead: "",
      identityNumber: "",
      phone: "",
      entryType: "MOVE_IN",
      entryDate: todayLocal(),
    },
    true,
  );
  for (const x of ["fullName", "dateOfBirth", "relationshipToHead"])
    assert.ok(v[x]);
});
test("US-02: tạm trú yêu cầu startDate và endDate không trước startDate", () => {
  const v = validateResident(
    {
      fullName: "Nguyễn Văn A",
      dateOfBirth: "2000-01-01",
      relationshipToHead: "Con",
      identityNumber: "",
      phone: "",
      entryType: "TEMPORARY_RESIDENCE",
      entryDate: "2026-10-20",
      startDate: "2026-10-20",
      endDate: "2026-10-10",
    },
    true,
  );
  assert.ok(v.endDate);
  assert.ok(!v.startDate);
});
test("US-09: RESIDENT cần residentId và có roles", () => {
  const v = validateUser({
    username: "ct01",
    password: "longPassword123",
    fullName: "Nguyễn Văn C",
    email: "",
    phone: "",
    roles: ["RESIDENT"],
    residentId: null,
  });
  assert.ok(v.residentId);
  assert.ok(!v.username);
});
test("US-09: tạo tài khoản hợp lệ", () =>
  assert.deepEqual(
    validateUser({
      username: "kt01",
      password: "longPassword123",
      fullName: "Trần Thị Kế Toán",
      email: "kt@example.com",
      phone: "0901234567",
      roles: ["ACCOUNTANT"],
      residentId: null,
    }),
    {},
  ));
test("US-10: định dạng hồ sơ và xác nhận mật khẩu", () => {
  assert.ok(
    validateProfile({ fullName: "A", email: "saiEmail", phone: "123" }).email,
  );
  const v = validatePassword(
    {
      oldPassword: "oldpass123",
      newPassword: "newpass123",
      confirmPassword: "wrong",
    },
    "user01",
  );
  assert.ok(v.confirmPassword);
});
test("format ngày: ngày ISO không đảo múi giờ", () =>
  assert.equal(formatDate("2026-10-20T23:00:00Z"), "20/10/2026"));

test("reject nonexistent calendar date and unsupported gender", () => {
  const value = validateResident(
    {
      fullName: "Nguyễn An",
      dateOfBirth: "2025-02-31",
      gender: "UNKNOWN",
      relationshipToHead: "Con",
    },
    false,
  );
  assert.ok(value.dateOfBirth);
  assert.ok(value.gender);
});
test("household boundaries and missing head do not crash", () => {
  assert.ok(
    validateHousehold({ ...goodHousehold, head: undefined }, true)[
      "head.fullName"
    ],
  );
  for (const area of [0, 1000.01, -1, "NaN", "1.001"])
    assert.ok(validateHousehold({ ...goodHousehold, area }, false).area);
  assert.deepEqual(
    validateHousehold({ ...goodHousehold, area: 1000, floor: 60 }, false),
    {},
  );
});
test("password length, username equality, old password reuse and confirmation", () => {
  for (const newPassword of [
    "short",
    "x".repeat(129),
    "USER01",
    "old-password",
  ])
    assert.ok(
      validatePassword(
        {
          oldPassword: "old-password",
          newPassword,
          confirmPassword: newPassword,
        },
        "user01",
      ).newPassword,
    );
  assert.deepEqual(
    validatePassword(
      {
        oldPassword: "old-password",
        newPassword: "new-password",
        confirmPassword: "new-password",
      },
      "user01",
    ),
    {},
  );
});
test("profile requires valid name, phone and email", () => {
  const errors = validateProfile({
    fullName: "A",
    phone: "123",
    email: "invalid",
  });
  assert.deepEqual(Object.keys(errors).sort(), ["email", "fullName", "phone"]);
  assert.deepEqual(
    validateProfile({
      fullName: "Nguyễn An",
      phone: "+84901234567",
      email: "an@example.com",
    }),
    {},
  );
});
