const AUTH_BASE = import.meta.env.VITE_AUTH_API_BASE || "/auth";
const USER_BASE = import.meta.env.VITE_USER_API_BASE || "/user";

let accessToken = "";

export function setAccessToken(token) {
  accessToken = token || "";
}

export function getAccessToken() {
  return accessToken;
}

async function request(base, path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...options.headers,
  };
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  const response = await fetch(`${base}${path}`, {
    ...options,
    headers,
  });

  let payload = null;
  try {
    payload = await response.json();
  } catch (error) {
    payload = null;
  }

  if (!response.ok || payload?.success === false) {
    if (response.status === 401) {
      throw new Error(payload?.message || "账号或密码错误，请确认后重试。");
    }
    throw new Error(payload?.message || `请求失败：${response.status}`);
  }

  return payload?.data ?? payload;
}

export async function loginClient(payload) {
  return request(AUTH_BASE, "/login", {
    method: "POST",
    body: JSON.stringify({
      identity: payload.identity || payload.username,
      username: payload.username,
      password: payload.password,
      tenantCode: payload.companyCode,
    }),
  });
}

export async function registerClient(payload) {
  return request(AUTH_BASE, "/users", {
    method: "POST",
    body: JSON.stringify({
      companyId: payload.companyId || 2,
      username: payload.username,
      password: payload.password,
      nickname: payload.nickname,
      mobile: payload.mobile,
      email: payload.email,
      status: 1,
    }),
  });
}

export async function fetchBillPage(params = {}) {
  const search = new URLSearchParams();
  search.set("pageNo", params.pageNo || 1);
  search.set("pageSize", params.pageSize || 5);
  if (params.keyword) {
    search.set("keyword", params.keyword);
  }
  if (params.status) {
    search.set("status", params.status);
  }
  return request(USER_BASE, `/bills/page?${search.toString()}`);
}

export async function createBill(payload) {
  return request(USER_BASE, "/bills", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateBill(id, payload) {
  return request(USER_BASE, `/bills/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function deleteBill(id) {
  return request(USER_BASE, `/bills/${id}`, {
    method: "DELETE",
  });
}

export async function fetchTemplateOptions() {
  return request(USER_BASE, "/templates/usable");
}

export async function initFileUpload(file, bizType) {
  return request(USER_BASE, "/files/upload/init", {
    method: "POST",
    body: JSON.stringify({
      originalName: file.name,
      contentType: file.type || "application/octet-stream",
      fileSize: file.size,
      bizType,
    }),
  });
}
