import { mockBillsByUser, mockMarketDemands, mockUsers } from "../mock/adminMock";

const API_BASE = import.meta.env.VITE_ADMIN_API_BASE || "/admin";

let users = [...mockUsers];
let marketDemands = [...mockMarketDemands];

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
    ...options,
  });

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  const payload = await response.json();
  if (payload && payload.success === false) {
    throw new Error(payload.message || "请求处理失败");
  }
  return withSource(payload.data ?? payload, "backend");
}

function withSource(data, source) {
  if (Array.isArray(data)) {
    return Object.assign([...data], { __source: source });
  }

  if (data !== null && typeof data === "object") {
    return { ...data, __source: source };
  }

  return { value: data, __source: source };
}

export async function loginAdmin(payload) {
  return request("/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function fetchUsers(filters = {}) {
  try {
    const params = new URLSearchParams();
    if (filters.keyword) params.set("keyword", filters.keyword);
    if (filters.status) params.set("status", filters.status);
    return await request(`/users?${params.toString()}`);
  } catch (error) {
    const keyword = (filters.keyword || "").toLowerCase();
    const result = users.filter((user) => {
      const text = `${user.username} ${user.nickname} ${user.email} ${user.mobile}`.toLowerCase();
      const keywordMatched = !keyword || text.includes(keyword);
      const statusMatched = !filters.status || user.status === filters.status;
      return keywordMatched && statusMatched;
    });
    return Object.assign(result, { __source: "mock" });
  }
}

export async function saveUser(payload) {
  const isEdit = Boolean(payload.id);

  try {
    return await request(isEdit ? `/users/${payload.id}` : "/users", {
      method: isEdit ? "PUT" : "POST",
      body: JSON.stringify(payload),
    });
  } catch (error) {
    const nextUser = {
      ...payload,
      id: payload.id || `u-${Date.now()}`,
    };

    if (isEdit) {
      users = users.map((user) => (user.id === nextUser.id ? nextUser : user));
    } else {
      users = [nextUser, ...users];
    }

    return { ...nextUser, __source: "mock" };
  }
}

export async function updateUserStatus(userId, status) {
  try {
    return await request(`/users/${userId}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    });
  } catch (error) {
    users = users.map((user) => (user.id === userId ? { ...user, status } : user));
    return { id: userId, status, __source: "mock" };
  }
}

export async function removeUser(userId) {
  try {
    return await request(`/users/${userId}`, {
      method: "DELETE",
    });
  } catch (error) {
    users = users.filter((user) => user.id !== userId);
    return { value: true, __source: "mock" };
  }
}

export async function fetchUserBills(userId) {
  try {
    return await request(`/users/${userId}/bills`);
  } catch (error) {
    return Object.assign([...(mockBillsByUser[userId] || [])], { __source: "mock" });
  }
}

export async function fetchMarketReviewDemands(params = {}) {
  const search = new URLSearchParams();
  search.set("pageNo", params.pageNo || 1);
  search.set("pageSize", params.pageSize || 10);
  if (params.keyword) {
    search.set("keyword", params.keyword);
  }
  if (params.auditStatus) {
    search.set("auditStatus", params.auditStatus);
  }
  try {
    return await request(`/market/demands/review/page?${search.toString()}`);
  } catch (error) {
    const pageNo = Number(params.pageNo || 1);
    const pageSize = Number(params.pageSize || 10);
    const keyword = String(params.keyword || "").toLowerCase();
    const filtered = marketDemands.filter((item) => {
      const text = `${item.demandNo} ${item.title} ${item.goodsName} ${item.departurePort} ${item.destinationPort}`.toLowerCase();
      const keywordMatched = !keyword || text.includes(keyword);
      const auditMatched = !params.auditStatus || item.auditStatus === params.auditStatus;
      return keywordMatched && auditMatched;
    });
    const start = (pageNo - 1) * pageSize;
    const records = filtered.slice(start, start + pageSize);
    return {
      records,
      total: filtered.length,
      current: pageNo,
      size: pageSize,
      __source: "mock",
    };
  }
}

export async function auditMarketDemand(demandId, payload) {
  try {
    return await request(`/market/demands/${demandId}/audit`, {
      method: "POST",
      body: JSON.stringify(payload),
    });
  } catch (error) {
    marketDemands = marketDemands.map((item) => {
      if (item.id !== demandId) {
        return item;
      }
      return {
        ...item,
        auditStatus: payload.auditStatus,
        demandStatus: payload.auditStatus === "APPROVED" ? "PUBLISHED" : "REJECTED",
      };
    });
    const updated = marketDemands.find((item) => item.id === demandId);
    return { ...updated, __source: "mock" };
  }
}
