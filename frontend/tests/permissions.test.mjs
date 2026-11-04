import test from "node:test";
import assert from "node:assert/strict";
import {
  canManage,
  canReadResidents,
  homeForRoles,
  canAssignRoles,
} from "../src/auth/permissions.js";
for (const role of ["BQT", "ACCOUNTANT", "RESIDENT", "AUTHORITY"])
  test("permissions: " + role, () => {
    assert.equal(canManage([role]), role === "BQT");
    assert.equal(
      canReadResidents([role]),
      ["BQT", "ACCOUNTANT"].includes(role),
    );
    assert.equal(
      homeForRoles([role]),
      ["BQT", "ACCOUNTANT"].includes(role) ? "/ho-gia-dinh" : "/ho-so",
    );
  });
test("self-lockout, invalid roles and missing resident link are rejected", () => {
  const current = { id: 1 };
  assert.equal(canAssignRoles(current, { id: "1" }, ["ACCOUNTANT"]), false);
  assert.equal(canAssignRoles(current, { id: 2 }, ["RESIDENT"]), false);
  assert.equal(canAssignRoles(current, { id: 2 }, ["ADMIN"]), false);
  assert.equal(canAssignRoles(current, { id: 2 }, ["BQT", "BQT"]), false);
  assert.equal(
    canAssignRoles(current, { id: 2, residentId: 3 }, [
      "RESIDENT",
      "ACCOUNTANT",
    ]),
    true,
  );
});
