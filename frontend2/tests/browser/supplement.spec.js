import { test, expect } from "@playwright/test";
// Contract-backed fixtures: browser checks do not claim live backend integration.
test("supplement: search, movement, fees and billing work at desktop/mobile sizes", async ({
  page,
}, info) => {
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  let generated = false;
  let moved = false;
  const posts = [];
  const resident = {
    id: 2,
    householdId: 1,
    householdCode: "H001",
    apartmentNo: "A101",
    fullName: "Nguyễn Bình",
    dateOfBirth: "2000-01-01",
    residencyStatus: "RESIDING",
    relationshipToHead: "Con",
  };
  const fee = {
    id: 2,
    code: "SERVICE",
    name: "Phí dịch vụ",
    feeKind: "MANDATORY",
    calculationMethod: "PER_AREA",
    unitPrice: 7000,
    billingCycle: "MONTHLY",
    dueDay: 10,
    active: true,
    inUse: true,
  };
  const paged = (content) => ({
    content,
    page: 1,
    size: 20,
    totalPages: 1,
    totalElements: content.length,
  });
  await page.addInitScript(() =>
    sessionStorage.setItem("bluemoon_access_token", "browser-fixture"),
  );
  await page.route("**/api/v1/**", async (route) => {
    const req = route.request();
    const u = new URL(req.url());
    const p = u.pathname.replace("/api/v1", "");
    if (req.method() !== "GET") posts.push(req.url());
    let data;
    if (p === "/me")
      data = {
        id: 1,
        fullName: "Nguyễn Quản Trị",
        username: "manager",
        roles: ["BQT"],
      };
    else if (p === "/search")
      data = {
        empty: false,
        households: [],
        residents: [resident],
        totals: { households: 0, residents: 1 },
        hasMore: { households: false, residents: false },
      };
    else if (p === "/residents/2")
      data = {
        ...resident,
        residencyStatus: moved ? "TEMPORARILY_ABSENT" : "RESIDING",
      };
    else if (p === "/residents/2/movements") {
      if (req.method() === "POST") {
        moved = true;
        data = {
          resident: { ...resident, residencyStatus: "TEMPORARILY_ABSENT" },
        };
      } else
        data = moved
          ? [
              {
                id: 1,
                movementType: "TEMPORARY_ABSENCE",
                effectiveDate: "2025-01-01",
                startDate: "2025-01-01",
                note: "Đi công tác",
                recordedBy: { fullName: "Nguyễn Quản Trị" },
                recordedAt: "2025-01-01T00:00:00Z",
              },
            ]
          : [];
    } else if (p === "/fee-categories") data = [fee];
    else if (p === "/households")
      data = paged([{ id: 1, householdCode: "H001", apartmentNo: "A101" }]);
    else if (p === "/fee-calculation/preview")
      data = {
        householdCode: "H001",
        apartmentNo: "A101",
        area: 60,
        unitPrice: 7000,
        amount: 420000,
        formula: "60 x 7000",
      };
    else if (p === "/billing-periods/10")
      data = {
        id: 10,
        code: "2026-10",
        name: "Tháng 10/2026",
        startDate: "2026-10-01",
        endDate: "2026-10-31",
        status: "OPEN",
        generated,
        summary: {
          householdCount: generated ? 1 : 0,
          receivableCount: generated ? 1 : 0,
          totalDue: generated ? 420000 : 0,
        },
      };
    else if (p === "/billing-periods/10/generate") {
      if (!u.searchParams.has("dryRun")) generated = true;
      data = {
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
        inactiveHouseholdCount: 0,
      };
    } else if (p === "/billing-periods/10/receivables")
      data = {
        ...paged([
          {
            id: 1,
            householdId: 1,
            householdCode: "H001",
            apartmentNo: "A101",
            feeName: "Phí dịch vụ",
            quantity: 60,
            unitPriceApplied: 7000,
            amountDue: 420000,
            dueDate: "2026-10-10",
            paidAmount: 0,
            outstanding: 420000,
          },
        ]),
        summary: { totalDue: 420000 },
      };
    else if (p === "/resident-requests") data = paged([]);
    else throw Error("Unhandled request " + p);
    await route.fulfill({ json: { success: true, data } });
  });
  await page.goto("/tim-kiem");
  await page.getByLabel("Từ khóa tìm kiếm").fill("nguyen");
  await page.getByRole("link", { name: "Nguyễn Bình" }).click();
  await page.getByRole("button", { name: "Ghi nhận biến động" }).click();
  await page.getByLabel("Loại biến động").selectOption("TEMPORARY_ABSENCE");
  await page.getByLabel("Ngày bắt đầu").fill("2025-01-01");
  await page.getByLabel("Ghi chú").fill("Đi công tác");
  await page.getByRole("button", { name: "Ghi nhận", exact: true }).click();
  await expect(page.getByText("Đi công tác")).toBeVisible();
  await page.goto("/khoan-thu");
  await page.getByRole("button", { name: "Tính thử", exact: true }).click();
  await page.getByLabel("Hộ đích / hộ tính phí").selectOption("1");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Tính thử" })
    .click();
  await expect(page.getByText("420.000 đ")).toBeVisible();
  await page.screenshot({
    path: info.outputPath("fee-preview.png"),
    fullPage: true,
  });
  await page.keyboard.press("Escape");
  await page.goto("/ky-thu/10");
  await page
    .getByRole("button", { name: "Lập danh sách", exact: true })
    .click();
  await expect(
    page.getByRole("link", { name: "H009 · căn A305" }),
  ).toBeVisible();
  await page.screenshot({
    path: info.outputPath("billing-preview.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "Xác nhận lập danh sách" }).click();
  await expect(page.getByText("Đơn giá áp dụng")).toBeVisible();
  await expect(page.getByText("7.000 đ")).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: info.outputPath("billing-result.png"),
    fullPage: true,
  });
  expect(posts.filter((x) => x.endsWith("/generate"))).toHaveLength(1);
  expect(errors).toEqual([]);
});
