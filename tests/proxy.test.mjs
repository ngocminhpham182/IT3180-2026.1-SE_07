import { test } from "node:test";
import assert from "node:assert/strict";
import config from "../vite.config.js";
test("v5 proxy strips the frontend prefix and preserves backend path and query", () => {
  const proxy = config({ mode: "test" }).server.proxy["/api/v1"];
  assert.equal(
    proxy.rewrite("/api/v1/billing-periods/10/generate?dryRun=true"),
    "/billing-periods/10/generate?dryRun=true",
  );
  assert.equal(proxy.rewrite("/api/v1/auth/login"), "/auth/login");
  assert.equal(proxy.rewrite("/api/v10/households"), "/api/v10/households");
});
