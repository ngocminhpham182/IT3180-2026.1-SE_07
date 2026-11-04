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
  house,
  resident,
  roles,
  server,
  ok,
  fail,
  page,
  app,
  fill,
  fillResident,
  writes,
} from "./helpers";
const login = () => {
  fill(/Tên đăng nhập/, "manager01");
  fill(/^Mật khẩu/, "test-only-password");
  fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));
};
it("US08 AC1 verifies /me before entering role-specific pages", async () => {
  let verify;
  const fetcher = server((p) => {
    if (p === "/auth/login")
      return ok({ accessToken: "test-login", expiresIn: 3600, user: account });
    if (p === "/me")
      return new Promise((r) => {
        verify = r;
      });
  });
  app("/dang-nhap", false);
  login();
  await waitFor(() => expect(verify).toBeTypeOf("function"));
  expect(
    screen.queryByRole("heading", { name: "Hộ gia đình" }),
  ).not.toBeInTheDocument();
  verify(ok(account));
  await screen.findByRole("heading", { name: "Hộ gia đình" });
  expect(sessionStorage.getItem("bluemoon_access_token")).toBe("test-login");
  expect(
    fetcher.mock.calls.find(([u]) => u.endsWith("/me"))[1].headers
      .Authorization,
  ).toBe("Bearer test-login");
});
it("US08 AC2 invalid credentials do not show expired-token notice", async () => {
  server((p) =>
    p === "/auth/login" ? fail("INVALID_CREDENTIALS", 401) : undefined,
  );
  app("/dang-nhap", false);
  login();
  await screen.findByText("Sai tài khoản hoặc mật khẩu");
  expect(screen.queryByText(/Phiên đăng nhập/)).not.toBeInTheDocument();
  expect(sessionStorage.getItem("bluemoon_access_token")).toBeNull();
});
it("US08 AC3 allows B to log in when A is locked", async () => {
  server((p, m, b) =>
    p === "/auth/login"
      ? b.username === "manager01"
        ? fail("ACCOUNT_LOCKED", 423, {}, { remainingSeconds: 900 })
        : ok({ accessToken: "test-B", expiresIn: 3600, user: account })
      : undefined,
  );
  app("/dang-nhap", false);
  login();
  await screen.findByText(/Thử lại sau/);
  expect(screen.getByRole("button", { name: "Đăng nhập" })).toBeDisabled();
  fill(/Tên đăng nhập/, "manager02");
  expect(screen.getByRole("button", { name: "Đăng nhập" })).toBeEnabled();
  fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));
  await screen.findByRole("heading", { name: "Hộ gia đình" });
});
it.each(["TOKEN_EXPIRED", "TOKEN_INVALID"])(
  "US08 redirects revoked/expired session: %s",
  async (code) => {
    server((p) => (p === "/me" ? fail(code, 401) : undefined));
    app("/ho-gia-dinh");
    await screen.findByRole("button", { name: "Đăng nhập" });
    expect(sessionStorage.getItem("bluemoon_access_token")).toBeNull();
  },
);
it("US08 expires an idle session at stored expiry and clears data on logout", async () => {
  sessionStorage.setItem("bluemoon_expires_at", String(Date.now() - 1));
  server();
  app("/ho-so");
  await screen.findByRole("button", { name: "Đăng nhập" });
  expect(sessionStorage.getItem("bluemoon_access_token")).toBeNull();
});
it("US08 explicitly logs out and clears the session", async () => {
  server();
  app("/ho-so");
  await screen.findByRole("heading", { name: "Hồ sơ cá nhân" });
  fireEvent.click(screen.getByText("Đăng xuất"));
  await screen.findByRole("button", { name: "Đăng nhập" });
  expect(sessionStorage.getItem("bluemoon_access_token")).toBeNull();
});
it("US09 AC3 resident cannot open BQT routes or fetch users", async () => {
  const fetcher = server(() => undefined, { ...account, roles: ["RESIDENT"] });
  app("/tai-khoan");
  await screen.findByRole("heading", { name: "Không có quyền truy cập" });
  expect(fetcher.mock.calls.some(([url]) => url.endsWith("/users"))).toBe(
    false,
  );
});
it.each(["RESIDENT", "AUTHORITY"])(
  "US08 routes %s to own profile",
  async (role) => {
    const user = { ...account, roles: [role] };
    server(
      (p) =>
        p === "/auth/login"
          ? ok({ accessToken: "test-role", expiresIn: 3600, user })
          : undefined,
      user,
    );
    app("/dang-nhap", false);
    login();
    await screen.findByRole("heading", { name: "Hồ sơ cá nhân" });
    expect(
      screen.queryByRole("link", { name: "Hộ gia đình" }),
    ).not.toBeInTheDocument();
  },
);
it("US09 accountant can read household and residents without mutation controls", async () => {
  const fetcher = server(() => undefined, {
    ...account,
    roles: ["ACCOUNTANT"],
  });
  app("/ho-gia-dinh/1");
  await screen.findByRole("heading", { name: "Căn hộ A101" });
  expect(screen.getByText(resident.fullName)).toBeInTheDocument();
  for (const name of ["Thêm nhân khẩu", "Chỉnh sửa", "Xóa hộ", "Đổi chủ hộ"])
    expect(screen.queryByRole("button", { name })).not.toBeInTheDocument();
  expect(writes(fetcher)).toHaveLength(0);
});
it("US01 AC3 cannot delete transacted household and offers explicit deactivation", async () => {
  let status = "ACTIVE";
  const fetcher = server((p, m, b) => {
    if (p === "/households/1" && m === "DELETE")
      return fail(
        "HAS_TRANSACTIONS",
        409,
        {},
        { suggestedAction: "SET_INACTIVE", paymentCount: 1 },
      );
    if (p === "/households/1/status") {
      status = b.status;
      return ok({ ...house, status });
    }
    if (p === "/households/1") return ok({ ...house, status });
  });
  app("/ho-gia-dinh/1");
  await screen.findByRole("heading", { name: "Căn hộ A101" });
  fireEvent.click(screen.getByRole("button", { name: "Xóa hộ" }));
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", { name: "Xóa hộ" }),
  );
  await screen.findByRole("dialog", { name: "Không thể xóa hộ" });
  expect(writes(fetcher)).toHaveLength(1);
  fireEvent.click(screen.getByText("Chuyển sang ngừng hoạt động"));
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", {
      name: "Ngừng hoạt động",
    }),
  );
  await screen.findByText("Kích hoạt lại");
  expect(writes(fetcher).map(([, o]) => o.method)).toEqual(["DELETE", "PATCH"]);
});
it("US02 AC2/AC5 adds member, reloads only current household and updates both counts", async () => {
  let members = [resident];
  const fetcher = server((p, m, b) => {
    if (p === "/households/1/residents") {
      if (m === "POST") {
        members = [...members, { ...resident, id: 3, fullName: b.fullName }];
        return ok(members[1], 201);
      }
      return ok(members);
    }
    if (p === "/households/1")
      return ok({
        ...house,
        residentCount: members.length,
        presentCount: members.length,
      });
  });
  app("/ho-gia-dinh/1");
  await screen.findByText(resident.fullName);
  fireEvent.click(screen.getByRole("button", { name: "Thêm nhân khẩu" }));
  fillResident();
  fill(/Họ và tên/, "Nguyễn Mới");
  fireEvent.click(screen.getByText("Lưu thông tin"));
  await screen.findByRole("link", { name: "Nguyễn Mới" });
  const count = document.querySelectorAll(".data-line");
  expect(
    [...count].find((el) => el.textContent.includes("Số người thuộc hộ")),
  ).toHaveTextContent("2");
  expect(
    [...count].find((el) => el.textContent.includes("Số người thực tế đang ở")),
  ).toHaveTextContent("2");
  expect(
    fetcher.mock.calls.every(([url]) => !url.includes("/households/2")),
  ).toBe(true);
});
it("US02 AC4 hides add capability for inactive household", async () => {
  server((p) =>
    p === "/households/1" ? ok({ ...house, status: "INACTIVE" }) : undefined,
  );
  app("/ho-gia-dinh/1");
  await screen.findByRole("heading", { name: "Căn hộ A101" });
  expect(screen.getByRole("button", { name: "Thêm nhân khẩu" })).toBeDisabled();
});
it.each(["HAS_HISTORY", "HAS_ACCOUNT", "INVALID_NEW_HEAD"])(
  "US02 preserves resident when delete is rejected: %s",
  async (code) => {
    const fetcher = server((p, m) =>
      m === "DELETE"
        ? fail(code, code === "INVALID_NEW_HEAD" ? 422 : 409)
        : undefined,
    );
    app("/nhan-khau/2");
    await screen.findByRole("heading", { name: resident.fullName });
    fireEvent.click(screen.getByText("Xóa nhập nhầm"));
    fireEvent.click(screen.getByRole("button", { name: "Xóa nhân khẩu" }));
    await waitFor(() =>
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
    );
    expect(
      screen.getByRole("heading", { name: resident.fullName }),
    ).toBeInTheDocument();
    expect(screen.getByRole("alert")).toBeInTheDocument();
    expect(writes(fetcher)).toHaveLength(1);
  },
);
it("US09 prevents BQT self-lockout and self-removal of BQT role", async () => {
  const fetcher = server();
  app("/tai-khoan");
  await screen.findByText("manager01");
  expect(screen.getByRole("button", { name: "Khóa" })).toBeDisabled();
  fireEvent.click(screen.getByRole("button", { name: "Phân quyền" }));
  expect(screen.getByRole("checkbox", { name: "Ban quản trị" })).toBeDisabled();
  expect(writes(fetcher)).toHaveLength(0);
});
it("US10 AC1 submits only own editable fields and displays updated name", async () => {
  const fetcher = server((p, m, b) =>
    p === "/me" && m === "PUT" ? ok({ ...account, ...b }) : undefined,
  );
  app("/ho-so");
  await screen.findByRole("heading", { name: "Hồ sơ cá nhân" });
  fill(/Họ và tên/, "Nguyễn Đã Sửa");
  fireEvent.click(screen.getByText("Lưu thay đổi"));
  await screen.findByText("Đã cập nhật hồ sơ cá nhân thành công.");
  expect(
    screen.getByRole("heading", { name: "Nguyễn Đã Sửa" }),
  ).toBeInTheDocument();
  expect(Object.keys(JSON.parse(writes(fetcher)[0][1].body)).sort()).toEqual([
    "email",
    "fullName",
    "phone",
  ]);
  expect(writes(fetcher)[0][0]).toContain("/api/v1/me");
});
it.each([true, false])(
  "US10 password change success=%s uses /me/password, handles old password errors",
  async (success) => {
    const fetcher = server((p) =>
      p === "/me/password"
        ? success
          ? ok(null, 204)
          : fail("VALIDATION_ERROR", 400, {
              oldPassword: "Mật khẩu cũ không đúng",
            })
        : undefined,
    );
    app("/ho-so");
    await screen.findByRole("heading", { name: "Hồ sơ cá nhân" });
    fireEvent.click(screen.getByRole("button", { name: "Đổi mật khẩu" }));
    fill(/Mật khẩu hiện tại/, "test-old-password");
    fill(/^Mật khẩu mới/, "test-new-password");
    fill(/Xác nhận mật khẩu mới/, "test-new-password");
    fireEvent.click(screen.getByText("Xác nhận đổi mật khẩu"));
    if (success) {
      await screen.findByRole("button", { name: "Đăng nhập" });
      expect(sessionStorage.getItem("bluemoon_access_token")).toBeNull();
    } else {
      await screen.findByText("Mật khẩu cũ không đúng");
      expect(sessionStorage.getItem("bluemoon_access_token")).toBe(
        "test-session",
      );
      expect(screen.getByLabelText(/Mật khẩu hiện tại/)).toHaveAttribute(
        "aria-invalid",
        "true",
      );
    }
    expect(writes(fetcher)[0][0]).toContain("/me/password");
  },
);
it("US01 changes head only through dedicated endpoint", async () => {
  const fetcher = server((p) =>
    p.endsWith("/head") ? ok({ ...house, head: resident }) : undefined,
  );
  app("/ho-gia-dinh/1");
  await screen.findByRole("heading", { name: "Căn hộ A101" });
  fireEvent.click(screen.getByRole("button", { name: "Đổi chủ hộ" }));
  fill(/Chủ hộ mới/, "2");
  fireEvent.click(screen.getByText("Xác nhận đổi chủ hộ"));
  await screen.findByText("Đã đổi chủ hộ thành công.");
  expect(writes(fetcher)[0][0]).toContain("/households/1/head");
  expect(JSON.parse(writes(fetcher)[0][1].body)).toEqual({ residentId: 2 });
});

it("US09 AC1 creates account with multiple roles and chosen resident then refreshes list", async () => {
  let users = [account];
  const fetcher = server((p, m, b) => {
    if (p === "/users" && m === "POST") {
      const created = { ...b, id: 2, isActive: true };
      delete created.password;
      users.push(created);
      return ok(created, 201);
    }
    if (p === "/users") return ok(page(users));
  });
  app("/tai-khoan");
  await screen.findByText("manager01");
  fireEvent.click(screen.getByText("Cấp tài khoản"));
  fill(/Tên đăng nhập/, "resident02");
  fill(/Mật khẩu ban đầu/, "test-only-password");
  fill(/Họ và tên/, "Nguyễn Tài Khoản");
  fireEvent.click(screen.getByRole("checkbox", { name: "Cư dân" }));
  fireEvent.click(screen.getByRole("checkbox", { name: "Kế toán" }));
  fill(/Liên kết nhân khẩu/, "Nguyễn");
  fireEvent.click(await screen.findByRole("button", { name: /Nguyễn Bình/ }));
  fireEvent.click(screen.getByText("Tạo tài khoản"));
  await screen.findByText("resident02");
  const body = JSON.parse(writes(fetcher)[0][1].body);
  expect(body.roles).toEqual(["RESIDENT", "ACCOUNTANT"]);
  expect(body.residentId).toBe(2);
  expect(screen.queryByText("test-only-password")).not.toBeInTheDocument();
});
it("household list sends search/status/page to API and renders server paging", async () => {
  const fetcher = server((p, m, b, url) =>
    p === "/households"
      ? ok({
          ...page([house]),
          page: Number(url.searchParams.get("page")),
          totalPages: 2,
          totalElements: 21,
        })
      : undefined,
  );
  app("/ho-gia-dinh");
  await screen.findByText("H001");
  fireEvent.click(screen.getByLabelText("Trang sau"));
  await waitFor(() =>
    expect(
      fetcher.mock.calls.some(
        ([u]) => new URL(u).searchParams.get("page") === "2",
      ),
    ).toBe(true),
  );
  fill("Tìm mã hộ hoặc số căn hộ", "A10");
  await waitFor(() =>
    expect(
      fetcher.mock.calls.some(
        ([u]) =>
          new URL(u).searchParams.get("keyword") === "A10" &&
          new URL(u).searchParams.get("page") === "1",
      ),
    ).toBe(true),
  );
  fill("Lọc trạng thái hộ", "INACTIVE");
  await waitFor(() =>
    expect(
      fetcher.mock.calls.some(
        ([u]) => new URL(u).searchParams.get("status") === "INACTIVE",
      ),
    ).toBe(true),
  );
});
it("resident list displays overdue and sends status filter", async () => {
  const fetcher = server((p) =>
    p === "/residents"
      ? ok(
          page([
            {
              ...resident,
              statusExpired: true,
              residencyStatus: "TEMPORARILY_ABSENT",
            },
          ]),
        )
      : undefined,
  );
  app("/nhan-khau");
  await screen.findByText("Quá hạn");
  fill("Lọc tình trạng cư trú", "TEMPORARILY_ABSENT");
  fireEvent.click(screen.getByLabelText("Chỉ quá hạn"));
  await waitFor(() =>
    expect(
      fetcher.mock.calls.some(
        ([u]) =>
          new URL(u).searchParams.get("statusExpired") === "true" &&
          new URL(u).searchParams.get("status") === "TEMPORARILY_ABSENT",
      ),
    ).toBe(true),
  );
});
it("US09 updates another account roles and status using dedicated endpoints", async () => {
  let user = { ...account, id: 2, username: "other01", roles: ["ACCOUNTANT"] };
  const fetcher = server((p, m, b) => {
    if (p === "/users") return ok(page([user]));
    if (p === "/users/2/roles") {
      user = { ...user, roles: b.roles };
      return ok(user);
    }
    if (p === "/users/2/status") {
      user = { ...user, isActive: b.active };
      return ok(user);
    }
  });
  app("/tai-khoan");
  await screen.findByText("other01");
  fireEvent.click(screen.getByRole("button", { name: "Phân quyền" }));
  fireEvent.click(screen.getByRole("checkbox", { name: "Ban quản trị" }));
  fireEvent.click(screen.getByText("Lưu phân quyền"));
  await screen.findByText("Đã cập nhật quyền tài khoản.");
  fireEvent.click(screen.getByRole("button", { name: "Khóa" }));
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", {
      name: "Khóa tài khoản",
    }),
  );
  await screen.findByRole("button", { name: "Mở khóa" });
  expect(writes(fetcher).map(([u]) => new URL(u).pathname)).toEqual([
    "/api/v1/users/2/roles",
    "/api/v1/users/2/status",
  ]);
});
it("US02 successful deletion returns to household with refreshed members and counts", async () => {
  let deleted = false;
  server((p, m) => {
    if (p === "/residents/2" && m === "DELETE") {
      deleted = true;
      return ok(null, 204);
    }
    if (p === "/households/1")
      return ok({
        ...house,
        residentCount: deleted ? 0 : 1,
        presentCount: deleted ? 0 : 1,
      });
    if (p === "/households/1/residents") return ok(deleted ? [] : [resident]);
  });
  app("/nhan-khau/2");
  await screen.findByRole("heading", { name: resident.fullName });
  fireEvent.click(screen.getByText("Xóa nhập nhầm"));
  fireEvent.click(screen.getByRole("button", { name: "Xóa nhân khẩu" }));
  await screen.findByText("Hộ chưa có nhân khẩu phù hợp");
  expect(
    screen.getByRole("heading", { name: "Căn hộ A101" }),
  ).toBeInTheDocument();
});
