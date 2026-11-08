import { test, expect } from "@playwright/test";
// Browser execution against built frontend. HTTP is mocked; this is not Spring Boot E2E.
const user = {
  id: 1,
  username: "test-bqt",
  fullName: "Nguyễn Quản Trị",
  roles: ["BQT"],
  isActive: true,
};
const house = {
  id: 1,
  householdCode: "H001",
  apartmentNo: "A101",
  status: "ACTIVE",
  area: 75.5,
  headName: "Nguyễn An",
  residentCount: 1,
  presentCount: 1,
};
test("built app: login, responsive list, modal focus and session logout", async ({
  page,
}, testInfo) => {
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    const data = path.endsWith("/auth/login")
      ? { accessToken: "browser-test-session", expiresIn: 3600, user }
      : path.endsWith("/me")
        ? user
        : {
            content: [house],
            page: 1,
            size: 20,
            totalElements: 1,
            totalPages: 1,
          };
    await route.fulfill({ json: { success: true, message: "OK", data } });
  });
  await page.goto("/dang-nhap");
  await page.getByLabel("Tên đăng nhập").fill("test-bqt");
  await page.getByLabel(/^Mật khẩu/).fill("test-only-password");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Hộ gia đình", exact: true }),
  ).toBeVisible();
  await expect(page.getByText("H001", { exact: true })).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  await page.getByRole("button", { name: "Thêm hộ gia đình" }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await expect(
    dialog.getByRole("button", { name: "Đóng", exact: true }),
  ).toBeFocused();
  await page.keyboard.press("Shift+Tab");
  await expect(
    dialog.getByRole("button", { name: "Lưu thông tin" }),
  ).toBeFocused();
  await page.screenshot({
    path: testInfo.outputPath("household-form.png"),
    fullPage: true,
  });
  await page.keyboard.press("Escape");
  await expect(dialog).not.toBeVisible();
  if (testInfo.project.name === "mobile")
    await page.getByRole("button", { name: "Mở menu" }).click();
  await page.getByRole("button", { name: "Đăng xuất" }).click();
  await expect(
    page.getByRole("heading", { name: "Đăng nhập", exact: true }),
  ).toBeVisible();
  expect(errors).toEqual([]);
});
test("built app: direct protected route requires login", async ({ page }) => {
  await page.goto("/tai-khoan");
  await expect(
    page.getByRole("heading", { name: "Đăng nhập", exact: true }),
  ).toBeVisible();
});
