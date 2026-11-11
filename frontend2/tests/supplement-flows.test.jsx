import React from "react";
import { expect, it, vi } from "vitest";
import {
  screen,
  fireEvent,
  waitFor,
  within,
  act,
} from "@testing-library/react";
import {
  account,
  resident,
  server,
  ok,
  fail,
  page,
  app,
  fill,
  writes,
} from "./helpers";
const fee = {
  id: 2,
  code: "SERVICE",
  name: "Phí dịch vụ",
  description: "",
  feeKind: "MANDATORY",
  calculationMethod: "PER_AREA",
  unitPrice: 7000,
  billingCycle: "MONTHLY",
  dueDay: 10,
  active: true,
  inUse: false,
};
const period = {
  id: 10,
  code: "2026-10",
  name: "Tháng 10/2026",
  startDate: "2026-10-01",
  endDate: "2026-10-31",
  status: "OPEN",
  generated: false,
  summary: { householdCount: 0, receivableCount: 0, totalDue: 0 },
};
const summary = {
  billingPeriodId: 10,
  dryRun: true,
  generatedReceivables: 1,
  householdCount: 1,
  totalAmount: 420000,
  byFee: [
    {
      feeCategoryId: 2,
      feeName: "Phí dịch vụ",
      receivableCount: 1,
      totalAmount: 420000,
    },
  ],
  skipped: [
    {
      householdId: 9,
      householdCode: "H009",
      apartmentNo: "A305",
      feeCode: "SERVICE",
      reason: "MISSING_AREA",
    },
  ],
  inactiveHouseholdCount: 1,
};
const receivable = {
  id: 15,
  householdId: 1,
  householdCode: "H001",
  apartmentNo: "A101",
  feeCategoryId: 2,
  feeName: "Phí dịch vụ",
  quantity: 60,
  unitPriceApplied: 7000,
  amountDue: 420000,
  dueDate: "2026-10-10",
  paidAmount: 0,
  outstanding: 420000,
};
function billing(handler = () => undefined, user = account) {
  return server(
    (p, m, b, u) =>
      handler(p, m, b, u) ||
      (p === "/billing-periods/10"
        ? ok(period)
        : p === "/billing-periods/10/generate"
          ? ok(summary)
          : p === "/billing-periods/10/receivables"
            ? ok({ ...page([receivable]), summary: { totalDue: 420000 } })
            : p === "/fee-categories"
              ? ok([fee])
              : undefined),
    user,
  );
}
it("US03 records temporary absence, updates resident, and renders immutable history", async () => {
  let saved = false;
  const fetcher = server((p, m, b) => {
    if (p === "/residents/2/movements") {
      if (m === "POST") {
        saved = true;
        return ok({
          resident: { ...resident, residencyStatus: "TEMPORARILY_ABSENT" },
          movement: { id: 1 },
        });
      }
      return ok(
        saved
          ? [
              {
                id: 1,
                movementType: "TEMPORARY_ABSENCE",
                effectiveDate: "2025-01-01",
                startDate: "2025-01-01",
                endDate: null,
                note: "Đi công tác",
                recordedBy: { fullName: "Người ghi" },
                recordedAt: "2025-01-01T00:00:00Z",
              },
            ]
          : [],
      );
    }
  });
  app("/nhan-khau/2");
  fireEvent.click(
    await screen.findByRole("button", { name: "Ghi nhận biến động" }),
  );
  // The history must be available before a mutation is enabled.
  await waitFor(() =>
    expect(
      screen.getByRole("button", { name: "Ghi nhận biến động" }),
    ).toBeEnabled(),
  );
  fireEvent.click(screen.getByRole("button", { name: "Ghi nhận biến động" }));
  fill(/Loại biến động/, "TEMPORARY_ABSENCE");
  fill(/Ngày bắt đầu/, "2025-01-01");
  fill(/Ghi chú/, "Đi công tác");
  fireEvent.click(
    screen.getByRole("button", { name: "Ghi nhận", exact: true }),
  );
  await screen.findByText(
    "Đã ghi nhận biến động và cập nhật tình trạng cư trú.",
  );
  expect(await screen.findByText("Đi công tác")).toBeInTheDocument();
  expect(writes(fetcher)[0][1].body).not.toContain("effectiveDate");
  expect(
    screen.queryByRole("button", { name: "Xóa biến động" }),
  ).not.toBeInTheDocument();
});
it("US03 HEAD_REQUIRED preserves data and retries with selected replacement", async () => {
  let count = 0;
  const fetcher = server((p, m) =>
    p === "/residents/2/movements" && m === "POST"
      ? ++count === 1
        ? fail(
            "HEAD_REQUIRED",
            422,
            {},
            {
              candidates: [
                { id: 3, fullName: "Chủ hộ mới", relationshipToHead: "Vợ" },
              ],
            },
          )
        : ok({ resident: { ...resident, residencyStatus: "MOVED_OUT" } })
      : undefined,
  );
  app("/nhan-khau/2");
  await waitFor(() =>
    expect(
      screen.getByRole("button", { name: "Ghi nhận biến động" }),
    ).toBeEnabled(),
  );
  fireEvent.click(screen.getByRole("button", { name: "Ghi nhận biến động" }));
  fill(/Ngày hiệu lực/, "2025-01-01");
  fireEvent.click(
    screen.getByRole("button", { name: "Ghi nhận", exact: true }),
  );
  await screen.findByLabelText(/Chủ hộ mới của hộ cũ/);
  fill(/Chủ hộ mới của hộ cũ/, "3");
  fireEvent.click(
    screen.getByRole("button", { name: "Ghi nhận", exact: true }),
  );
  await waitFor(() => expect(writes(fetcher)).toHaveLength(2));
  expect(JSON.parse(writes(fetcher)[1][1].body)).toMatchObject({
    effectiveDate: "2025-01-01",
    newHeadResidentId: 3,
  });
});
it("US03 accountant sees history but cannot record changes", async () => {
  server(undefined, { ...account, roles: ["ACCOUNTANT"] });
  app("/nhan-khau/2");
  await screen.findByText("Chưa có biến động");
  expect(
    screen.queryByRole("button", { name: "Ghi nhận biến động" }),
  ).not.toBeInTheDocument();
});
it("US04 validates keyword, renders empty state and current resident status", async () => {
  const fetcher = server((p, m, b, u) =>
    p === "/search"
      ? ok(
          u.searchParams.get("keyword") === "zz"
            ? {
                empty: true,
                households: [],
                residents: [],
                totals: { households: 0, residents: 0 },
                hasMore: { households: false, residents: false },
              }
            : {
                empty: false,
                households: [],
                residents: [{ ...resident, residencyStatus: "MOVED_OUT" }],
                totals: { households: 0, residents: 1 },
                hasMore: { households: false, residents: false },
              },
        )
      : undefined,
  );
  app("/tim-kiem");
  await screen.findByLabelText("Từ khóa tìm kiếm");
  fill("Từ khóa tìm kiếm", "n");
  expect(
    fetcher.mock.calls.filter(([u]) => u.includes("/search")),
  ).toHaveLength(0);
  fill("Từ khóa tìm kiếm", "zz");
  await screen.findByText("Không tìm thấy");
  fill("Từ khóa tìm kiếm", "nguyen");
  expect(await screen.findByText("Đã chuyển đi")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: resident.fullName })).toHaveAttribute(
    "href",
    "/nhan-khau/2",
  );
});
it.each(["/tim-kiem", "/khoan-thu", "/ky-thu", "/ky-thu/10"])(
  "new route %s rejects resident role",
  async (path) => {
    const f = server(undefined, { ...account, roles: ["RESIDENT"] });
    app(path);
    await screen.findByText("Không có quyền truy cập");
    expect(f.mock.calls).toHaveLength(1);
  },
);
it("US12 creates a voluntary category without fee, due day or debt fields", async () => {
  const f = server((p, m) =>
    p === "/fee-categories" ? ok(m === "GET" ? [] : fee) : undefined,
  );
  app("/khoan-thu");
  fireEvent.click(
    await screen.findByRole("button", { name: "Thêm khoản thu" }),
  );
  fill(/Mã khoản thu/, "quy");
  fill(/Tên khoản thu/, "Quỹ tự nguyện");
  fill(/Loại khoản thu/, "VOLUNTARY");
  expect(screen.queryByLabelText(/Đơn giá/)).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Lưu thông tin" }));
  await waitFor(() => expect(writes(f)).toHaveLength(1));
  expect(JSON.parse(writes(f)[0][1].body)).toEqual({
    code: "QUY",
    name: "Quỹ tự nguyện",
    description: "",
    feeKind: "VOLUNTARY",
    calculationMethod: "MANUAL",
  });
});
it("US12 locks in-use fields and deletion but allows price change", async () => {
  server((p) =>
    p === "/fee-categories" ? ok([{ ...fee, inUse: true }]) : undefined,
  );
  app("/khoan-thu");
  await screen.findByText(fee.name);
  expect(
    screen.getByRole("button", { name: "Xóa", exact: true }),
  ).toBeDisabled();
  fireEvent.click(screen.getByRole("button", { name: "Sửa", exact: true }));
  for (const label of [
    /Mã khoản thu/,
    /Loại khoản thu/,
    /Cách tính/,
    /Chu kỳ thu/,
  ])
    expect(
      within(screen.getByRole("dialog")).getByLabelText(label),
    ).toBeDisabled();
  expect(screen.getByLabelText(/Đơn giá/)).toBeEnabled();
});
it("US13 displays server formula and exact amount; missing area links to household", async () => {
  let missing = false;
  server((p, m) =>
    p === "/fee-categories"
      ? ok([fee])
      : p === "/fee-calculation/preview"
        ? missing
          ? fail("MISSING_AREA", 422, {}, { householdId: 1 })
          : ok({
              householdCode: "H001",
              apartmentNo: "A101",
              area: 60,
              unitPrice: 7000,
              amount: 420000,
              formula: "60 x 7000",
            })
        : undefined,
  );
  app("/khoan-thu");
  fireEvent.click(
    await screen.findByRole("button", { name: "Tính thử", exact: true }),
  );
  await waitFor(() => expect(screen.getByLabelText(/Hộ đích/)).toBeEnabled());
  fill(/Hộ đích/, "1");
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", {
      name: "Tính thử",
    }),
  );
  await screen.findByText("420.000 đ");
  expect(screen.getByText("60 x 7000")).toBeInTheDocument();
  missing = true;
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", {
      name: "Tính thử",
    }),
  );
  expect(
    await screen.findByRole("link", { name: "Cập nhật hồ sơ hộ" }),
  ).toHaveAttribute("href", "/ho-gia-dinh/1");
  expect(screen.queryByText("420.000 đ")).not.toBeInTheDocument();
});
it("US14 previews skipped households before generating once and showing frozen prices", async () => {
  let generated = false;
  const f = billing((p, m, b, u) => {
    if (p === "/billing-periods/10") return ok({ ...period, generated });
    if (p.endsWith("/generate")) {
      if (!u.searchParams.has("dryRun")) generated = true;
      return ok({ ...summary, dryRun: !generated });
    }
  });
  app("/ky-thu/10");
  fireEvent.click(
    await screen.findByRole("button", { name: "Lập danh sách", exact: true }),
  );
  await screen.findByRole("dialog");
  expect(writes(f)).toHaveLength(1);
  expect(writes(f)[0][0]).toContain("dryRun=true");
  expect(screen.getByRole("link", { name: "H009 · căn A305" })).toHaveAttribute(
    "href",
    "/ho-gia-dinh/9",
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Xác nhận lập danh sách" }),
  );
  await screen.findByText("Đơn giá áp dụng");
  expect(await screen.findByText("7.000 đ")).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Lập danh sách", exact: true }),
  ).not.toBeInTheDocument();
  expect(writes(f)).toHaveLength(2);
});
it("US14 NO_ACTIVE_FEE gives a path to create fees", async () => {
  billing((p) => (p.endsWith("/generate") ? fail("NO_ACTIVE_FEE") : undefined));
  app("/ky-thu/10");
  fireEvent.click(
    await screen.findByRole("button", { name: "Lập danh sách", exact: true }),
  );
  expect(
    await screen.findByRole("link", {
      name: "Tạo khoản thu bắt buộc đang hiệu lực",
    }),
  ).toHaveAttribute("href", "/khoan-thu");
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
});
it("US14 ALREADY_GENERATED switches to receivables without a second POST", async () => {
  const f = billing((p) =>
    p.endsWith("/generate")
      ? fail("ALREADY_GENERATED", 409)
      : p === "/billing-periods/10"
        ? ok({ ...period, generated: writes(f).length > 0 })
        : undefined,
  );
  app("/ky-thu/10");
  fireEvent.click(
    await screen.findByRole("button", { name: "Lập danh sách", exact: true }),
  );
  await screen.findByText("Đơn giá áp dụng");
  expect(writes(f)).toHaveLength(1);
});
it("US14 uncertain POST is checked by GET before any retry; failed check blocks generation", async () => {
  let attempted = false;
  let checked = false;
  const f = billing((p, m, b, u) => {
    if (p.endsWith("/generate") && !u.searchParams.has("dryRun")) {
      attempted = true;
      return fail("TIMEOUT", 504);
    }
    if (p === "/billing-periods/10" && attempted) {
      checked = true;
      return fail("SERVER_ERROR", 503);
    }
  });
  app("/ky-thu/10");
  fireEvent.click(
    await screen.findByRole("button", { name: "Lập danh sách", exact: true }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Xác nhận lập danh sách" }),
  );
  await screen.findByRole("button", { name: "Kiểm tra trạng thái kỳ" });
  expect(checked).toBe(true);
  expect(
    screen.queryByRole("button", { name: "Lập danh sách", exact: true }),
  ).not.toBeInTheDocument();
  expect(writes(f)).toHaveLength(2);
});
it("US14 accountant cannot close or reopen a period", async () => {
  billing(
    (p) =>
      p === "/billing-periods/10"
        ? ok({ ...period, generated: true })
        : undefined,
    { ...account, roles: ["ACCOUNTANT"] },
  );
  app("/ky-thu/10");
  await screen.findByText("Đơn giá áp dụng");
  expect(
    screen.queryByRole("button", { name: "Đóng sổ" }),
  ).not.toBeInTheDocument();
});
it("US04 discards stale search results after a newer query", async () => {
  let resolveOld;
  server((p, m, b, u) =>
    p === "/search"
      ? u.searchParams.get("keyword") === "old"
        ? new Promise((resolve) => {
            resolveOld = resolve;
          })
        : ok({
            empty: true,
            households: [],
            residents: [],
            totals: { households: 0, residents: 0 },
            hasMore: { households: false, residents: false },
          })
      : undefined,
  );
  app("/tim-kiem");
  await screen.findByLabelText("Từ khóa tìm kiếm");
  fill("Từ khóa tìm kiếm", "old");
  await waitFor(() => expect(resolveOld).toBeTypeOf("function"));
  fill("Từ khóa tìm kiếm", "new");
  await screen.findByText("Không tìm thấy");
  await act(async () =>
    resolveOld(
      ok({
        empty: false,
        households: [],
        residents: [resident],
        totals: { households: 0, residents: 1 },
        hasMore: { households: false, residents: false },
      }),
    ),
  );
  expect(screen.queryByText(resident.fullName)).not.toBeInTheDocument();
});
it("US12 duplicate code stays on form with field error and preserves input", async () => {
  server((p, m) =>
    p === "/fee-categories"
      ? m === "POST"
        ? fail("DUPLICATE", 409, { code: "Mã khoản thu đã tồn tại" })
        : ok([])
      : undefined,
  );
  app("/khoan-thu");
  fireEvent.click(
    await screen.findByRole("button", { name: "Thêm khoản thu" }),
  );
  fill(/Mã khoản thu/, "QUY");
  fill(/Tên khoản thu/, "Quỹ cư dân");
  fill(/Loại khoản thu/, "VOLUNTARY");
  fireEvent.click(screen.getByRole("button", { name: "Lưu thông tin" }));
  await screen.findByText("Mã khoản thu đã tồn tại");
  expect(screen.getByLabelText(/Tên khoản thu/)).toHaveValue("Quỹ cư dân");
  expect(screen.getByRole("dialog")).toBeInTheDocument();
});
it("US14 creates a monthly period using exact month boundaries", async () => {
  const f = billing((p, m) =>
    p === "/billing-periods" ? ok(m === "POST" ? period : page([])) : undefined,
  );
  app("/ky-thu");
  fireEvent.click(await screen.findByRole("button", { name: "Tạo kỳ thu" }));
  fill("Điền nhanh theo tháng", "2026-02");
  fireEvent.click(screen.getByRole("button", { name: "Lưu thông tin" }));
  await waitFor(() => expect(writes(f)).toHaveLength(1));
  expect(JSON.parse(writes(f)[0][1].body)).toEqual({
    code: "2026-02",
    name: "Tháng 2/2026",
    startDate: "2026-02-01",
    endDate: "2026-02-28",
  });
});
it("US14 BQT closes generated period only after confirmation", async () => {
  let closed = false;
  const f = billing((p, m, b) =>
    p === "/billing-periods/10/status"
      ? ((closed = true), ok({ ...period, generated: true, status: b.status }))
      : p === "/billing-periods/10"
        ? ok({ ...period, generated: true, status: closed ? "CLOSED" : "OPEN" })
        : undefined,
  );
  app("/ky-thu/10");
  fireEvent.click(await screen.findByRole("button", { name: "Đóng sổ" }));
  expect(writes(f)).toHaveLength(0);
  fireEvent.click(screen.getByRole("button", { name: "Xác nhận" }));
  await screen.findByRole("button", { name: "Mở lại kỳ" });
  expect(JSON.parse(writes(f)[0][1].body)).toEqual({ status: "CLOSED" });
});
it("US14 timeout after a committed generation reconciles to existing receivables", async () => {
  let generated = false;
  const f = billing((p, m, b, u) => {
    if (p.endsWith("/generate") && !u.searchParams.has("dryRun")) {
      generated = true;
      return fail("TIMEOUT", 504);
    }
    if (p === "/billing-periods/10") return ok({ ...period, generated });
  });
  app("/ky-thu/10");
  fireEvent.click(
    await screen.findByRole("button", { name: "Lập danh sách", exact: true }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Xác nhận lập danh sách" }),
  );
  await screen.findByText("Đơn giá áp dụng");
  expect(writes(f)).toHaveLength(2);
  expect(
    screen.queryByRole("button", { name: "Lập danh sách", exact: true }),
  ).not.toBeInTheDocument();
});
it("new pages do not render empty error banners", async () => {
  billing();
  app("/ky-thu/10");
  await screen.findByRole("heading", { name: period.name });
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
