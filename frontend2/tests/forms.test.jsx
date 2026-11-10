import React from "react";
import { describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import HouseholdForm from "../src/components/HouseholdForm";
import ResidentForm from "../src/components/ResidentForm";
import UserForm from "../src/components/UserForm";
import { Modal } from "../src/components/UI";
import {
  server,
  ok,
  fail,
  house,
  resident,
  roles,
  fill,
  fillHouse,
  fillResident,
  writes,
} from "./helpers";
it("US01 AC1 submits household with nested head and prevents closing/double submit while saving", async () => {
  let finish;
  const saved = vi.fn(),
    close = vi.fn();
  const fetcher = server((p, m) =>
    m === "POST"
      ? new Promise((r) => {
          finish = r;
        })
      : undefined,
  );
  render(<HouseholdForm onClose={close} onSaved={saved} />);
  fillHouse();
  fireEvent.click(screen.getByText("Lưu thông tin"));
  await waitFor(() => expect(finish).toBeTypeOf("function"));
  fireEvent.keyDown(window, { key: "Escape" });
  fireEvent.mouseDown(screen.getByRole("dialog").parentElement);
  fireEvent.click(screen.getByLabelText("Đóng"));
  fireEvent.submit(document.querySelector("form"));
  expect(close).not.toHaveBeenCalled();
  expect(writes(fetcher)).toHaveLength(1);
  expect(JSON.parse(writes(fetcher)[0][1].body)).toMatchObject({
    householdCode: "H002",
    head: { fullName: "Nguyễn An" },
  });
  finish(ok(house, 201));
  await waitFor(() => expect(saved).toHaveBeenCalledWith(house));
});
it("US01 rejects invalid household locally", () => {
  const fetcher = server();
  render(<HouseholdForm onClose={() => {}} onSaved={() => {}} />);
  fireEvent.click(screen.getByText("Lưu thông tin"));
  expect(screen.getByLabelText(/Mã hộ khẩu/)).toHaveAttribute(
    "aria-invalid",
    "true",
  );
  expect(fetcher).not.toHaveBeenCalled();
});
it("US01 AC2 maps duplicate code, apartment and nested identity errors", async () => {
  server(() =>
    fail("DUPLICATE", 409, {
      householdCode: "Mã hộ đã tồn tại",
      apartmentNo: "Căn hộ đã tồn tại",
      "head.identityNumber": "CCCD đã tồn tại",
    }),
  );
  render(<HouseholdForm onClose={() => {}} onSaved={() => {}} />);
  fillHouse();
  fireEvent.click(screen.getByText("Lưu thông tin"));
  await screen.findByText("CCCD đã tồn tại");
  expect(screen.getByLabelText(/Số căn hộ/)).toHaveAttribute(
    "aria-invalid",
    "true",
  );
  expect(screen.getByLabelText(/CCCD/)).toHaveAttribute("aria-invalid", "true");
});
it("household update excludes head, status and counts", async () => {
  const fetcher = server(() => ok(house));
  render(<HouseholdForm value={house} onClose={() => {}} onSaved={() => {}} />);
  fireEvent.click(screen.getByText("Lưu thông tin"));
  await waitFor(() => expect(writes(fetcher)).toHaveLength(1));
  expect(Object.keys(JSON.parse(writes(fetcher)[0][1].body)).sort()).toEqual([
    "apartmentNo",
    "area",
    "floor",
    "householdCode",
  ]);
});
it.each(["MOVE_IN", "BIRTH", "TEMPORARY_RESIDENCE", "EXISTING"])(
  "US02 AC1 creates resident using %s fields only",
  async (entry) => {
    const saved = vi.fn();
    const fetcher = server(() => ok(resident, 201));
    render(
      <ResidentForm
        householdId={1}
        householdStatus="ACTIVE"
        onClose={() => {}}
        onSaved={saved}
      />,
    );
    fillResident();
    fill(/Hình thức vào hộ/, entry);
    if (entry === "TEMPORARY_RESIDENCE") {
      fill(/Bắt đầu tạm trú/, "2026-01-01");
      fill(/Kết thúc tạm trú/, "2027-01-01");
    }
    fireEvent.click(screen.getByText("Lưu thông tin"));
    await waitFor(() => expect(saved).toHaveBeenCalled());
    const body = JSON.parse(writes(fetcher)[0][1].body);
    expect(body.entryType).toBe(entry);
    expect(body).not.toHaveProperty("isHead");
    expect(body).not.toHaveProperty("householdId");
    if (entry === "TEMPORARY_RESIDENCE") {
      expect(body).not.toHaveProperty("entryDate");
      expect(body.startDate).toBe("2026-01-01");
    } else {
      expect(body).not.toHaveProperty("startDate");
    }
    if (entry === "BIRTH") expect(body.entryDate).toBe(body.dateOfBirth);
  },
);
it("US02 AC3 rejects missing name and birth date", () => {
  const fetcher = server();
  render(
    <ResidentForm householdId={1} onClose={() => {}} onSaved={() => {}} />,
  );
  fireEvent.click(screen.getByText("Lưu thông tin"));
  expect(screen.getByLabelText(/Ngày sinh/)).toHaveAttribute(
    "aria-invalid",
    "true",
  );
  expect(fetcher).not.toHaveBeenCalled();
});
it("US02 AC4 blocks inactive households locally", async () => {
  const fetcher = server();
  render(
    <ResidentForm
      householdId={1}
      householdStatus="INACTIVE"
      onClose={() => {}}
      onSaved={() => {}}
    />,
  );
  fillResident();
  fireEvent.click(screen.getByText("Lưu thông tin"));
  expect(screen.getByText("Hộ không còn hoạt động.")).toBeInTheDocument();
  expect(fetcher).not.toHaveBeenCalled();
});
it("US02 displays server inactive rejection if status changes while form is open", async () => {
  server(() => fail("HOUSEHOLD_INACTIVE"));
  render(
    <ResidentForm
      householdId={1}
      householdStatus="ACTIVE"
      onClose={() => {}}
      onSaved={() => {}}
    />,
  );
  fillResident();
  fireEvent.click(screen.getByText("Lưu thông tin"));
  await screen.findByText("Hộ không còn hoạt động.");
  expect(screen.getByRole("dialog")).toBeInTheDocument();
});
it("resident update only submits editable personal fields", async () => {
  const fetcher = server(() => ok(resident));
  render(
    <ResidentForm value={resident} onClose={() => {}} onSaved={() => {}} />,
  );
  fireEvent.click(screen.getByText("Lưu thông tin"));
  await waitFor(() => expect(writes(fetcher)).toHaveLength(1));
  expect(Object.keys(JSON.parse(writes(fetcher)[0][1].body)).sort()).toEqual([
    "dateOfBirth",
    "fullName",
    "gender",
    "identityNumber",
    "phone",
    "relationshipToHead",
  ]);
});
it("US09 AC2 displays duplicate username beside field", async () => {
  server(() =>
    fail("DUPLICATE", 409, { username: "Tên đăng nhập đã tồn tại" }),
  );
  render(<UserForm roles={roles} onClose={() => {}} onSaved={() => {}} />);
  fill(/Tên đăng nhập/, "account01");
  fill(/Mật khẩu ban đầu/, "test-only-password");
  fill(/Họ và tên/, "Nguyễn An");
  fireEvent.click(screen.getByLabelText("Kế toán"));
  fireEvent.click(screen.getByText("Tạo tài khoản"));
  await screen.findByText("Tên đăng nhập đã tồn tại");
  expect(screen.getByLabelText(/Tên đăng nhập/)).toHaveAttribute(
    "aria-invalid",
    "true",
  );
});
it("US09 requires resident link before creating resident account", () => {
  const fetcher = server();
  render(<UserForm roles={roles} onClose={() => {}} onSaved={() => {}} />);
  fill(/Tên đăng nhập/, "resident01");
  fill(/Mật khẩu ban đầu/, "test-only-password");
  fill(/Họ và tên/, "Nguyễn An");
  fireEvent.click(screen.getByLabelText("Cư dân"));
  fireEvent.click(screen.getByText("Tạo tài khoản"));
  expect(
    screen.getByText(/Tài khoản cư dân phải liên kết/),
  ).toBeInTheDocument();
  expect(writes(fetcher)).toHaveLength(0);
});
it("modal traps keyboard focus and returns focus on close", async () => {
  const user = userEvent.setup();
  const trigger = document.createElement("button");
  document.body.append(trigger);
  trigger.focus();
  const close = vi.fn();
  const { unmount } = render(
    <Modal title="Kiểm tra" onClose={close}>
      <button>Cuối</button>
    </Modal>,
  );
  screen.getByText("Cuối").focus();
  await user.tab();
  expect(screen.getByLabelText("Đóng")).toHaveFocus();
  await user.keyboard("{Escape}");
  expect(close).toHaveBeenCalledOnce();
  unmount();
  expect(trigger).toHaveFocus();
  trigger.remove();
});
