import { test } from "node:test";
import assert from "node:assert/strict";
import {
  allowedMovements,
  validateMovement,
  movementBody,
  validateFee,
  feeBody,
  validatePeriod,
} from "../src/utils/supplement.js";
const resident = {
  residencyStatus: "RESIDING",
  householdId: 1,
  dateOfBirth: "2000-01-01",
  moveInDate: "2024-01-01",
};
const move = {
  movementType: "TEMPORARY_ABSENCE",
  startDate: "2025-01-02",
  endDate: "2025-01-01",
  note: "",
};
const fee = {
  code: "SERVICE",
  name: "Phí dịch vụ",
  description: "",
  feeKind: "MANDATORY",
  calculationMethod: "PER_AREA",
  unitPrice: "7000",
  billingCycle: "MONTHLY",
  dueDay: "10",
};
test("US03 rejects invalid dates, reverse intervals and backdated history", () => {
  assert.ok(validateMovement(move, resident).endDate);
  assert.ok(
    validateMovement({ ...move, startDate: "2025-02-30" }, resident).startDate,
  );
  assert.ok(
    validateMovement({ ...move, endDate: "" }, resident, [
      { effectiveDate: "2025-02-01" },
    ]).startDate,
  );
  assert.ok(
    validateMovement(
      { ...move, movementType: "MOVE_OUT", effectiveDate: "2023-12-31" },
      resident,
    ).effectiveDate,
  );
  assert.ok(
    validateMovement({ ...move, startDate: "2999-01-01" }, resident).startDate,
  );
});
test("US03 terminal and temporary status transitions match the contract", () => {
  assert.deepEqual(allowedMovements("DECEASED"), []);
  assert.deepEqual(allowedMovements("MOVED_OUT"), ["MOVE_IN"]);
  assert.ok(!allowedMovements("TEMPORARILY_ABSENT").includes("TRANSFER"));
  assert.ok(
    validateMovement(move, { ...resident, residencyStatus: "MOVED_OUT" })
      .movementType,
  );
  assert.ok(
    validateMovement(
      {
        ...move,
        movementType: "TRANSFER",
        effectiveDate: "2025-01-01",
        newHouseholdId: "1",
        relationshipToHead: "Con",
      },
      resident,
    ).newHouseholdId,
  );
});
test("US03 payload sends only applicable fields, preserving replacement head", () => {
  assert.deepEqual(
    movementBody({
      ...move,
      endDate: "",
      effectiveDate: "2025-01-01",
      newHeadResidentId: "4",
    }),
    {
      movementType: "TEMPORARY_ABSENCE",
      startDate: "2025-01-02",
      endDate: null,
      note: "",
    },
  );
  assert.equal(
    movementBody({
      ...move,
      movementType: "MOVE_OUT",
      effectiveDate: "2025-01-01",
      newHeadResidentId: "4",
    }).newHeadResidentId,
    4,
  );
});
test("US12 mandatory price and deadline constraints; voluntary never carries mandatory amounts", () => {
  assert.deepEqual(validateFee(fee), {});
  for (const unitPrice of ["0", "-1", "1.001", "1000000001", "NaN"])
    assert.ok(validateFee({ ...fee, unitPrice }).unitPrice);
  for (const dueDay of ["", 0, 29, 1.5])
    assert.ok(validateFee({ ...fee, dueDay }).dueDay);
  assert.deepEqual(feeBody({ ...fee, feeKind: "VOLUNTARY" }), {
    code: "SERVICE",
    name: "Phí dịch vụ",
    description: "",
    feeKind: "VOLUNTARY",
    calculationMethod: "MANUAL",
  });
  assert.equal(feeBody(fee).unitPrice, 7000);
});
test("US13 prices are configurable, not hardcoded to the example range", () => {
  for (const unitPrice of ["2500", "16500", "2500.50", "1000000000"])
    assert.deepEqual(validateFee({ ...fee, unitPrice }), {});
});
test("US14 period date and code validation", () => {
  const p = {
    code: "2026-02",
    name: "Tháng 2",
    startDate: "2026-02-01",
    endDate: "2026-02-28",
  };
  assert.deepEqual(validatePeriod(p), {});
  assert.ok(validatePeriod({ ...p, endDate: "2026-02-30" }).endDate);
  assert.ok(validatePeriod({ ...p, endDate: "2026-01-31" }).endDate);
  assert.ok(validatePeriod({ ...p, code: "a/b" }).code);
});
