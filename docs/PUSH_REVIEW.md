# Bản rà soát trước push

Nhánh: `feature/fe-us01-us02-us08-us09-us10`

Đích PR đã xác nhận: `frontend`

Các nhóm commit local (SHA chính xác xem `git log origin/frontend..HEAD --oneline`):

1. `feat: complete Sprint 1 frontend workflows`
2. `test: cover Sprint 1 frontend acceptance flows`
3. `ci: verify frontend pull requests`
4. `docs: document Sprint 1 frontend integration`

Kết quả: npm ci, 77 unit/component/HTTP tests, 4 browser tests, production build và format check PASS. API trong test là mock. Backend thật, DB/audit/BCrypt/thu hồi JWT còn BLOCKED; xem INTEGRATION.md và AC_COVERAGE.md.

Chỉ thay frontend của 5 US và kiểm thử/tài liệu/CI liên quan. Không sửa backend/SQL, không track ZIP/tài liệu đầu vào/secret/build artifacts, không sửa lịch sử chung. Chưa push, chưa tạo PR trên GitHub; mô tả PR nằm trong PR_DESCRIPTION.md. Người dùng phải xác nhận `PUSH` trước khi chạy:

```bash
git push -u origin feature/fe-us01-us02-us08-us09-us10
```

## Danh sách file sẽ được push

Danh sách đối chiếu với `origin/frontend`:

- `.env.example`
- `.github/workflows/frontend.yml`
- `.gitignore`
- `.prettierignore`
- `README.md`
- `docs/AC_COVERAGE.md`
- `docs/INTEGRATION.md`
- `docs/PR_DESCRIPTION.md`
- `docs/PUSH_REVIEW.md`
- `docs/VERIFICATION.md`
- `package-lock.json`
- `package.json`
- `playwright.config.js`
- `src/App.jsx`
- `src/api/http.js`
- `src/api/services.js`
- `src/auth/AuthContext.jsx`
- `src/auth/permissions.js`
- `src/components/HouseholdForm.jsx`
- `src/components/Layout.jsx`
- `src/components/ResidentForm.jsx`
- `src/components/UI.jsx`
- `src/components/UserForm.jsx`
- `src/main.jsx`
- `src/pages/HomePage.jsx`
- `src/pages/HouseholdDetailPage.jsx`
- `src/pages/HouseholdsPage.jsx`
- `src/pages/LoginPage.jsx`
- `src/pages/ProfilePage.jsx`
- `src/pages/ResidentDetailPage.jsx`
- `src/pages/ResidentsPage.jsx`
- `src/pages/UsersPage.jsx`
- `src/styles.css`
- `src/utils/format.js`
- `src/utils/validation.js`
- `tests/browser/smoke.spec.js`
- `tests/flows.test.jsx`
- `tests/forms.test.jsx`
- `tests/helpers.jsx`
- `tests/http.test.js`
- `tests/permissions.test.mjs`
- `tests/setup.js`
- `tests/validation.test.mjs`
- `vite.config.js`
- `vitest.config.js`
