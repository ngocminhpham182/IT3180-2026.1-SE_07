import { test, expect } from "@playwright/test";
const resident = {
  id: 2,
  fullName: "Nguyễn Bình",
  dateOfBirth: "2000-01-01",
  gender: "MALE",
  identityNumber: "012345678901",
  phone: "0901234567",
  relationshipToHead: "Con",
  residencyStatus: "RESIDING",
  isHead: false,
};
const household = {
  id: 1,
  householdCode: "H001",
  apartmentNo: "A101",
  floor: 1,
  area: 75.5,
  status: "ACTIVE",
};
test("US05 resident submits and cancels a request on desktop/mobile", async ({
  page,
}, info) => {
  const errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  const user = {
    id: 7,
    username: "resident",
    fullName: "Nguyễn Bình",
    roles: ["RESIDENT"],
    residentId: 2,
  };
  let request;
  await page.addInitScript(() =>
    sessionStorage.setItem("bluemoon_access_token", "browser-test"),
  );
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname.replace("/api/v1", "");
    let data;
    if (path === "/me") data = user;
    else if (path === "/me/household")
      data = {
        household,
        myResidentId: 2,
        residents: [resident],
        pendingRequests: [],
      };
    else if (
      path === "/me/resident-requests" &&
      route.request().method() === "POST"
    ) {
      const body = route.request().postDataJSON();
      expect(Object.keys(body.profile)).toHaveLength(6);
      expect(body.residentId).toBe(2);
      request = {
        id: 31,
        type: body.type,
        status: "PENDING",
        household,
        resident,
        submittedBy: user,
        submittedAt: "2026-11-02T03:00:00Z",
        before: resident,
        proposed: body.profile,
        changes: ["fullName"],
        note: body.note,
        decision: null,
        result: null,
      };
      data = request;
    } else if (path.endsWith("/cancel")) {
      request = {
        ...request,
        status: "CANCELLED",
        decision: {
          outcome: "CANCELLED",
          decidedBy: user,
          decidedAt: "2026-11-03T03:00:00Z",
        },
      };
      data = request;
    } else if (path === "/me/resident-requests/31") data = request;
    else throw new Error(`Unexpected request: ${path}`);
    await route.fulfill({ json: { success: true, data } });
  });
  await page.goto("/ho-cua-toi");
  await expect(page.getByText("Căn hộ A101", { exact: true })).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: info.outputPath("my-household.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "Đề nghị sửa", exact: true }).click();
  await page.getByLabel("Họ và tên").fill("Nguyễn Bình An");
  await page.screenshot({
    path: info.outputPath("profile-request-form.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "Gửi đề nghị", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Chi tiết đề nghị #31" }),
  ).toBeVisible();
  await expect(page.getByText("Họ và tên · Thay đổi")).toBeVisible();
  await page.getByRole("button", { name: "Hủy đề nghị", exact: true }).click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Xác nhận" })
    .click();
  await expect(page.getByText("Đã hủy", { exact: true })).toBeVisible();
  expect(errors).toEqual([]);
});
test("US06 BQT approves a guest and opens resulting resident history", async ({
  page,
}, info) => {
  const user = {
    id: 1,
    username: "bqt",
    fullName: "Ban quản trị",
    roles: ["BQT"],
  };
  let request = {
    id: 32,
    type: "TEMPORARY_RESIDENCE",
    status: "PENDING",
    household,
    resident: null,
    submittedBy: { id: 7, fullName: "Nguyễn Bình" },
    submittedAt: "2026-11-02T03:00:00Z",
    proposed: {
      person: { ...resident, fullName: "Lê Văn Khách" },
      startDate: "2026-11-01",
      endDate: "2026-12-01",
    },
    decision: null,
    result: null,
  };
  await page.addInitScript(() =>
    sessionStorage.setItem("bluemoon_access_token", "browser-test"),
  );
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname.replace("/api/v1", "");
    let data;
    if (path === "/me") data = user;
    else if (path === "/resident-requests")
      data = {
        content: [request],
        page: 1,
        totalPages: 1,
        totalElements: request.status === "PENDING" ? 1 : 0,
      };
    else if (path.endsWith("/approve")) {
      request = {
        ...request,
        status: "APPROVED",
        decision: {
          outcome: "APPROVED",
          decidedBy: user,
          decidedAt: "2026-11-03T03:00:00Z",
          note: route.request().postDataJSON().note,
        },
        result: { residentId: 8, movementId: null },
      };
      data = request;
    } else if (path === "/resident-requests/32") data = request;
    else throw new Error(`Unexpected request: ${path}`);
    await route.fulfill({ json: { success: true, data } });
  });
  await page.goto("/duyet-de-nghi");
  await page.getByRole("link", { name: "Xem #32" }).click();
  await expect(page.getByText("01/12/2026", { exact: true })).toBeVisible();
  await page.getByLabel("Ghi chú duyệt").fill("Đã đối chiếu CCCD");
  await page.screenshot({
    path: info.outputPath("review-guest.png"),
    fullPage: true,
  });
  await page
    .getByRole("button", { name: "Duyệt đề nghị", exact: true })
    .click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Xác nhận" })
    .click();
  await expect(
    page.getByRole("link", { name: /Xem hồ sơ và lịch sử/ }),
  ).toHaveAttribute("href", "/nhan-khau/8");
  await expect(
    page.getByText("Đã đối chiếu CCCD", { exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});
