import test from "node:test";
import assert from "node:assert/strict";
import {
  profilePayload,
  todayVietnam,
  validateRequest,
} from "../src/utils/residentRequests.js";
const resident = {
  id: 5,
  fullName: "Nguyễn An",
  dateOfBirth: "2000-01-01",
  gender: "MALE",
  identityNumber: null,
  phone: null,
  relationshipToHead: "Con",
  residencyStatus: "RESIDING",
};
const today = "2026-11-11";
test("US05 complete six-field profile; trim optional values and detect no changes", () => {
  const profile = profilePayload({ ...resident, phone: " " });
  assert.equal(Object.keys(profile).length, 6);
  assert.equal(profile.phone, null);
  assert.ok(
    validateRequest(
      { type: "PROFILE_UPDATE", residentId: 5, profile },
      resident,
      today,
    ).form,
  );
  assert.deepEqual(
    validateRequest(
      {
        type: "PROFILE_UPDATE",
        residentId: 5,
        profile: { ...profile, fullName: "Nguyễn Bình" },
      },
      resident,
      today,
    ),
    {},
  );
});
test("US05 validates nested fields, gender, future and nonexistent dates", () => {
  const errors = validateRequest(
    {
      type: "PROFILE_UPDATE",
      residentId: 5,
      profile: {
        ...resident,
        fullName: " ",
        dateOfBirth: "2027-01-01",
        gender: "OTHER",
        identityNumber: "123",
        phone: "abc",
        relationshipToHead: "",
      },
    },
    resident,
    today,
  );
  for (const field of [
    "fullName",
    "dateOfBirth",
    "gender",
    "identityNumber",
    "phone",
    "relationshipToHead",
  ])
    assert.ok(errors[`profile.${field}`]);
  assert.ok(
    validateRequest(
      {
        type: "PROFILE_UPDATE",
        residentId: 5,
        profile: { ...resident, dateOfBirth: "2001-02-29" },
      },
      resident,
      today,
    )["profile.dateOfBirth"],
  );
});
test("US06 requires interval, forbids future starts, reversed intervals and dates before birth", () => {
  const base = { type: "TEMPORARY_ABSENCE", residentId: 5 };
  const missing = validateRequest(base, resident, today);
  assert.ok(missing.startDate);
  assert.ok(missing.endDate);
  assert.ok(
    validateRequest(
      { ...base, startDate: "2026-11-12", endDate: "2026-11-11" },
      resident,
      today,
    ).startDate,
  );
  assert.ok(
    validateRequest(
      { ...base, startDate: "2026-11-11", endDate: "2026-11-10" },
      resident,
      today,
    ).endDate,
  );
  assert.ok(
    validateRequest(
      { ...base, startDate: "1999-12-31", endDate: today },
      resident,
      today,
    ).startDate,
  );
  assert.deepEqual(
    validateRequest(
      { ...base, startDate: today, endDate: today },
      resident,
      today,
    ),
    {},
  );
});
test("US06 prevents invalid members and supports a new guest without residentId", () => {
  const body = {
    type: "TEMPORARY_ABSENCE",
    residentId: 7,
    startDate: today,
    endDate: today,
  };
  assert.ok(validateRequest(body, resident, today).residentId);
  assert.ok(
    validateRequest(
      { ...body, residentId: 5 },
      { ...resident, residencyStatus: "TEMPORARILY_ABSENT" },
      today,
    ).residentId,
  );
  assert.deepEqual(
    validateRequest(
      {
        type: "TEMPORARY_RESIDENCE",
        person: profilePayload(resident),
        startDate: today,
        endDate: today,
      },
      undefined,
      today,
    ),
    {},
  );
});
test("today is calculated in Vietnam even across UTC midnight", () => {
  assert.equal(todayVietnam(new Date("2026-11-10T17:01:00Z")), "2026-11-11");
});
