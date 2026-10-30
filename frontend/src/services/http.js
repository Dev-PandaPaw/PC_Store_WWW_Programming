import axios from "axios";

const http = axios.create({
  baseURL: "/api",
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

// Local Spring profile exposes a demo admin through HTTP Basic authentication.
// Keep these development credentials out of production builds.
if (import.meta.env.DEV) {
  const username = import.meta.env.VITE_ADMIN_USERNAME || "admin";
  const password = import.meta.env.VITE_ADMIN_PASSWORD || "admin1234";
  http.defaults.headers.common.Authorization = `Basic ${btoa(`${username}:${password}`)}`;
}

// Fetch the CSRF cookie on the first write request, then send it as a header.
let csrfRequest;
const csrfHeaderName = "X-XSRF-TOKEN";

http.interceptors.request.use(async (config) => {
  const method = (config.method || "get").toLowerCase();
  if (["get", "head", "options", "trace"].includes(method)) return config;

  let token = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)?.[1];

  if (!token) {
    csrfRequest ||= http.get("/csrf").finally(() => {
      csrfRequest = null;
    });
    await csrfRequest;
    token = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)?.[1];
  }

  if (!token) throw new Error("Không lấy được token CSRF từ backend.");
  config.headers[csrfHeaderName] = decodeURIComponent(token);
  return config;
});

export default http;

export async function ensureCsrfToken() {
  if (!document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/))
    await http.get("/csrf");
}
