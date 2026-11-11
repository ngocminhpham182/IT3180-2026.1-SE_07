import React from "react";
import { expect, it } from "vitest";
import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import {
  account,
  app,
  fail,
  fill,
  house,
  ok,
  page,
  resident,
  server,
  writes,
} from "./helpers";
import { profilePayload } from "../src/utils/residentRequests";
const user = { ...account, id: 7, roles: ["RESIDENT"], residentId: 2 };
const household = {
  household: house,
  myResidentId: 2,
  residents: [resident],
  pendingRequests: [],
};
const request = {
  id: 31,
  type: "PROFILE_UPDATE",
  status: "PENDING",
  household: house,
  resident,
  submittedBy: user,
  submittedAt: "2026-11-02T03:00:00Z",
  note: "Sửa họ tên",
  before: profilePayload(resident),
  proposed: { ...profilePayload(resident), fullName: "Nguyễn Bình An" },
  changes: ["fullName"],
  decision: null,
  result: null,
};
function setup(handler = () => undefined, roles = ["RESIDENT"]) {
  return server(
    async (p, m, b, url) => {
      const custom = await handler(p, m, b, url);
      if (custom) return custom;
      if (p === "/me/household") return ok(household);
      if (p === "/me/resident-requests" || p === "/resident-requests")
        return ok(page([request]));
      if (p === "/me/resident-requests/31" || p === "/resident-requests/31")
        return ok(request);
    },
    { ...user, roles },
  );
}
it("US05 prefilled form blocks no-change then submits all six fields without updating resident", async () => {
  const f = setup((p, m, body) =>
    p === "/me/resident-requests" && m === "POST"
      ? ok({ ...request, proposed: body.profile }, 201)
      : undefined,
  );
  app("/ho-cua-toi");
  fireEvent.click(await screen.findByRole("button", { name: "Đề nghị sửa" }));
  expect(screen.getByLabelText(/Họ và tên/)).toHaveValue(resident.fullName);
  fireEvent.click(screen.getByRole("button", { name: "Gửi đề nghị" }));
  expect(await screen.findByText(/Chưa có thay đổi nào/)).toBeInTheDocument();
  expect(writes(f)).toHaveLength(0);
  fill(/Họ và tên/, "Nguyễn Bình An");
  fireEvent.click(screen.getByRole("button", { name: "Gửi đề nghị" }));
  await screen.findByRole("heading", { name: "Chi tiết đề nghị #31" });
  const [url, options] = writes(f)[0];
  expect(url).toContain("/me/resident-requests");
  expect(JSON.parse(options.body)).toEqual({
    type: "PROFILE_UPDATE",
    residentId: 2,
    note: null,
    profile: request.proposed,
  });
  expect(writes(f)).toHaveLength(1);
});
it("US06 missing dates never submits; server field errors map to the form", async () => {
  const f = setup((p, m) =>
    p === "/me/resident-requests" && m === "POST"
      ? fail("VALIDATION_ERROR", 400, {
          startDate: "Ngày bắt đầu trước biến động gần nhất",
        })
      : undefined,
  );
  app("/ho-cua-toi");
  fireEvent.click(
    await screen.findByRole("button", { name: "Khai báo tạm vắng" }),
  );
  fireEvent.click(screen.getByRole("button", { name: "Gửi đề nghị" }));
  expect(
    await screen.findByText("Vui lòng nhập ngày bắt đầu hợp lệ."),
  ).toBeInTheDocument();
  expect(writes(f)).toHaveLength(0);
  fill(/Ngày bắt đầu/, "2020-01-01");
  fill(/Ngày kết thúc/, "2027-01-01");
  fireEvent.click(screen.getByRole("button", { name: "Gửi đề nghị" }));
  expect(
    await screen.findByText("Ngày bắt đầu trước biến động gần nhất"),
  ).toBeInTheDocument();
  expect(screen.getByLabelText(/Ngày bắt đầu/)).toHaveAttribute(
    "aria-invalid",
    "true",
  );
});
it("US06 guest request sends person, not residentId; repeated click sends once", async () => {
  let finish;
  const f = setup((p, m) =>
    p === "/me/resident-requests" && m === "POST"
      ? new Promise((resolve) => {
          finish = resolve;
        })
      : undefined,
  );
  app("/ho-cua-toi");
  fireEvent.click(
    await screen.findByRole("button", { name: "Khai báo tạm trú" }),
  );
  fill(/Họ và tên/, "Lê Văn Khách");
  fill(/Ngày sinh/, "2000-01-01");
  fill(/Quan hệ với chủ hộ/, "Bạn");
  fill(/Ngày bắt đầu/, "2020-01-01");
  fill(/Ngày kết thúc/, "2027-01-01");
  const button = screen.getByRole("button", { name: "Gửi đề nghị" });
  fireEvent.click(button);
  fireEvent.click(button);
  await waitFor(() => expect(finish).toBeTypeOf("function"));
  expect(writes(f)).toHaveLength(1);
  const body = JSON.parse(writes(f)[0][1].body);
  expect(body.type).toBe("TEMPORARY_RESIDENCE");
  expect(body.person.fullName).toBe("Lê Văn Khách");
  expect(body).not.toHaveProperty("residentId");
  finish(ok({ ...request, type: body.type, proposed: body }, 201));
  await screen.findByRole("heading", { name: "Chi tiết đề nghị #31" });
});
it("pending household requests disable matching actions even if submitted by another member", async () => {
  setup((p) =>
    p === "/me/household"
      ? ok({
          ...household,
          pendingRequests: [
            { id: 40, type: "PROFILE_UPDATE", residentId: 2 },
            { id: 41, type: "TEMPORARY_ABSENCE", residentId: 2 },
          ],
        })
      : undefined,
  );
  app("/ho-cua-toi");
  expect(
    await screen.findByRole("button", {
      name: "Sửa thông tin: đang chờ duyệt",
    }),
  ).toBeDisabled();
  expect(
    screen.getByRole("button", { name: "Tạm vắng: đang chờ duyệt" }),
  ).toBeDisabled();
});
it.each(["ACCOUNTANT", "AUTHORITY"])(
  "%s cannot access resident or approval routes",
  async (role) => {
    const f = setup(undefined, [role]);
    app("/duyet-de-nghi");
    await screen.findByRole("heading", { name: "Không có quyền truy cập" });
    expect(f.mock.calls.some(([u]) => u.includes("/resident-requests"))).toBe(
      false,
    );
    expect(
      screen.queryByRole("link", { name: "Hộ của tôi" }),
    ).not.toBeInTheDocument();
  },
);
it("resident cannot access another household or the BQT queue", async () => {
  const f = setup();
  app("/ho-gia-dinh/999");
  await screen.findByRole("heading", { name: "Không có quyền truy cập" });
  expect(f.mock.calls.some(([u]) => u.includes("/households"))).toBe(false);
  expect(
    screen.queryByRole("link", { name: "Duyệt đề nghị" }),
  ).not.toBeInTheDocument();
});
it("BQT sees diff, must enter rejection reason, and result contains reason", async () => {
  const f = setup(
    (p, m, b) =>
      p.endsWith("/reject")
        ? ok({
            ...request,
            status: "REJECTED",
            decision: {
              reason: b.reason,
              decidedBy: account,
              decidedAt: "2026-11-03T02:00:00Z",
            },
          })
        : undefined,
    ["BQT"],
  );
  app("/duyet-de-nghi/31");
  const reject = await screen.findByRole("button", { name: "Từ chối đề nghị" });
  expect(reject).toBeDisabled();
  expect(screen.getByText("Họ và tên · Thay đổi")).toBeInTheDocument();
  fill(/Lý do từ chối/, "Cần đối chiếu CCCD");
  fireEvent.click(reject);
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", {
      name: "Xác nhận",
    }),
  );
  await screen.findByRole("heading", { name: "Kết quả xử lý" });
  expect(screen.getByText("Cần đối chiếu CCCD")).toBeInTheDocument();
  expect(JSON.parse(writes(f)[0][1].body)).toEqual({
    reason: "Cần đối chiếu CCCD",
  });
});
it("BQT handles outdated information and can reject rather than overwrite", async () => {
  setup(
    (p) =>
      p.endsWith("/approve")
        ? fail("REQUEST_OUTDATED", 409, {}, { fields: ["fullName"] })
        : undefined,
    ["BQT"],
  );
  app("/duyet-de-nghi/31");
  fireEvent.click(await screen.findByRole("button", { name: "Duyệt đề nghị" }));
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", {
      name: "Xác nhận",
    }),
  );
  await screen.findByText(/Hãy từ chối kèm lý do/);
  expect(screen.getByRole("button", { name: "Duyệt đề nghị" })).toBeDisabled();
  fill(/Lý do từ chối/, "Dữ liệu đã thay đổi, vui lòng gửi lại");
  expect(screen.getByRole("button", { name: "Từ chối đề nghị" })).toBeEnabled();
});
it("approved absence provides link to resident history", async () => {
  setup(
    (p) =>
      p.endsWith("/approve")
        ? ok({
            ...request,
            type: "TEMPORARY_ABSENCE",
            status: "APPROVED",
            proposed: { startDate: "2026-11-01", endDate: "2026-12-01" },
            decision: { decidedBy: account },
            result: { residentId: 2, movementId: 12 },
          })
        : undefined,
    ["BQT"],
  );
  app("/duyet-de-nghi/31");
  fireEvent.click(await screen.findByRole("button", { name: "Duyệt đề nghị" }));
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", {
      name: "Xác nhận",
    }),
  );
  expect(
    await screen.findByRole("link", { name: /Xem hồ sơ và lịch sử/ }),
  ).toHaveAttribute("href", "/nhan-khau/2");
});
it("cancel conflict reloads detail and removes actions after another decision", async () => {
  let resolved = false;
  setup((p) => {
    if (p.endsWith("/cancel")) {
      resolved = true;
      return fail("REQUEST_NOT_PENDING", 409);
    }
    if (resolved && p === "/me/resident-requests/31")
      return ok({
        ...request,
        status: "APPROVED",
        decision: { decidedBy: account },
      });
  });
  app("/de-nghi/31");
  fireEvent.click(await screen.findByRole("button", { name: "Hủy đề nghị" }));
  fireEvent.click(
    within(screen.getByRole("dialog")).getByRole("button", {
      name: "Xác nhận",
    }),
  );
  await screen.findByRole("heading", { name: "Kết quả xử lý" });
  expect(
    screen.queryByRole("button", { name: "Hủy đề nghị" }),
  ).not.toBeInTheDocument();
});
it("my request list applies status and type filters with page reset", async () => {
  const f = setup();
  app("/de-nghi");
  await screen.findByRole("link", { name: "Xem #31" });
  fill("Trạng thái", "REJECTED");
  fill("Loại đề nghị", "PROFILE_UPDATE");
  await waitFor(() =>
    expect(
      f.mock.calls.some(
        ([u]) =>
          u.includes("status=REJECTED") &&
          u.includes("type=PROFILE_UPDATE") &&
          u.includes("page=1"),
      ),
    ).toBe(true),
  );
});
