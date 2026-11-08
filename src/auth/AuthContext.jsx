import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { authApi } from "../api/services";
export { homeForRoles } from "./permissions";
const AuthContext = createContext(null);
const TOKEN = "bluemoon_access_token";
const EXPIRY = "bluemoon_expires_at";
function expiresAt(token) {
  try {
    const value = JSON.parse(
      atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")),
    ).exp;
    return Number(value) * 1000;
  } catch {
    return 0;
  }
}
export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => sessionStorage.getItem(TOKEN));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(() =>
    Boolean(sessionStorage.getItem(TOKEN)),
  );
  const [notice, setNotice] = useState("");
  const generation = useRef(0);
  const signOut = useCallback((message = "") => {
    generation.current += 1;
    sessionStorage.removeItem(TOKEN);
    sessionStorage.removeItem(EXPIRY);
    sessionStorage.removeItem("bluemoon_user");
    setToken(null);
    setUser(null);
    setLoading(false);
    setNotice(message);
  }, []);
  const updateUser = useCallback((value) => setUser(value), []);
  const login = useCallback(
    async (credentials) => {
      const attempt = ++generation.current;
      const result = await authApi.login(credentials);
      if (!result?.accessToken)
        throw new Error("Máy chủ chưa cung cấp phiên đăng nhập hợp lệ.");
      if (attempt !== generation.current)
        throw new Error("Yêu cầu đăng nhập đã bị hủy.");
      sessionStorage.setItem(TOKEN, result.accessToken);
      const expiry =
        Date.parse(result.expiresAt) ||
        expiresAt(result.accessToken) ||
        (Number(result.expiresIn) > 0
          ? Date.now() + Number(result.expiresIn) * 1000
          : 0);
      if (expiry) sessionStorage.setItem(EXPIRY, String(expiry));
      else sessionStorage.removeItem(EXPIRY);
      try {
        const verified = await authApi.me();
        if (attempt !== generation.current)
          throw new Error("Yêu cầu đăng nhập đã bị hủy.");
        if (
          !verified?.id ||
          !Array.isArray(verified.roles) ||
          verified.isActive === false
        )
          throw new Error("Không thể xác minh tài khoản với máy chủ.");
        setUser(verified);
        setToken(result.accessToken);
        setLoading(false);
        setNotice("");
        return verified;
      } catch (error) {
        if (attempt === generation.current) signOut();
        throw error;
      }
    },
    [signOut],
  );
  useEffect(() => {
    if (!token || user) return;
    const attempt = generation.current;
    let active = true;
    authApi
      .me()
      .then((value) => {
        if (active && attempt === generation.current) {
          if (
            !value?.id ||
            !Array.isArray(value.roles) ||
            value.isActive === false
          )
            throw new Error("Không thể xác minh tài khoản.");
          updateUser(value);
          setLoading(false);
        }
      })
      .catch(() => {
        if (active && attempt === generation.current)
          signOut(
            "Không thể xác minh phiên đăng nhập. Vui lòng đăng nhập lại.",
          );
      });
    return () => {
      active = false;
    };
  }, [token, user, signOut, updateUser]);
  useEffect(() => {
    if (!token) return;
    const expiry = Number(sessionStorage.getItem(EXPIRY)) || expiresAt(token);
    if (!expiry) return;
    const check = () => {
      if (Date.now() >= expiry)
        signOut("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
    };
    const timer = setTimeout(
      check,
      Math.max(0, Math.min(expiry - Date.now(), 2147483647)),
    );
    window.addEventListener("focus", check);
    document.addEventListener("visibilitychange", check);
    return () => {
      clearTimeout(timer);
      window.removeEventListener("focus", check);
      document.removeEventListener("visibilitychange", check);
    };
  }, [token, signOut]);
  useEffect(() => {
    const handler = (event) =>
      signOut(
        event.detail ||
          "Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại.",
      );
    window.addEventListener("bluemoon:session-expired", handler);
    return () =>
      window.removeEventListener("bluemoon:session-expired", handler);
  }, [signOut]);
  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        loading,
        notice,
        setNotice,
        login,
        signOut,
        updateUser,
        hasRole: (role) => (user?.roles || []).includes(role),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
export const useAuth = () => useContext(AuthContext);
