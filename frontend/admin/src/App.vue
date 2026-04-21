<template>
  <div class="toast-stack" aria-live="polite">
    <div
      v-for="toast in toasts"
      :key="toast.id"
      class="toast"
      :class="toast.type"
    >
      <strong>{{ toast.title }}</strong>
      <span>{{ toast.message }}</span>
    </div>
  </div>

  <section v-if="!session.loggedIn" class="login-scene">
    <header class="glass-nav">
      <div class="brand">
        <span class="brand-mark">MR</span>
        <span>Manifest Reader Admin</span>
      </div>
      <span class="nav-note">Vite + Vue</span>
    </header>

    <main class="login-main">
      <div class="hero-copy">
        <p class="eyebrow">Admin Console</p>
        <h1>进入面向提单业务的管理中心。</h1>
        <p>登录后使用左侧边栏进入工作台、用户管理、用户数据管理和权限管理。</p>
      </div>

      <form class="login-card" @submit.prevent="login">
        <div>
          <h2>登录管理端</h2>
          <p>选择角色以预览不同权限表现。</p>
        </div>
        <label>
          账号
          <input v-model.trim="loginForm.username" placeholder="admin" required />
        </label>
        <label>
          密码
          <input v-model.trim="loginForm.password" type="password" placeholder="请输入账号密码" required />
        </label>
        <label>
          登录身份
          <select v-model="loginForm.role">
            <option value="SUPER_ADMIN">超级管理员</option>
            <option value="ADMIN">管理员</option>
          </select>
        </label>
        <button class="primary-button" type="submit">进入管理端</button>
      </form>
    </main>
  </section>

  <section v-else class="admin-shell" :class="{ collapsed: sidebarCollapsed }">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-head">
        <div class="sidebar-profile">
          <span class="brand-mark">MR</span>
          <div v-if="!sidebarCollapsed" class="sidebar-profile-copy">
            <strong>Admin</strong>
            <span>{{ roleText }}</span>
          </div>
        </div>
        <button class="icon-button" type="button" @click="sidebarCollapsed = !sidebarCollapsed">
          {{ sidebarCollapsed ? "›" : "‹" }}
        </button>
      </div>

      <nav class="side-nav" aria-label="Admin sidebar">
        <button
          v-for="item in sidebarItems"
          :key="item.key"
          class="side-nav-item"
          :class="{ active: currentView === item.key, locked: item.locked }"
          type="button"
          :disabled="item.locked"
          @click="switchView(item.key)"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span v-if="!sidebarCollapsed">{{ item.label }}</span>
          <small v-if="item.locked && !sidebarCollapsed">超级管理员</small>
        </button>
      </nav>
    </aside>

    <main class="workspace">
      <header class="workspace-topbar">
        <div>
          <p class="eyebrow">{{ currentMeta.eyebrow }}</p>
          <h1>{{ currentMeta.title }}</h1>
          <p class="connection-note" :class="{ mock: apiSource === 'mock' }">
            {{ apiSource === 'backend' ? '已连接后端 /admin/**' : '当前使用本地 mock 数据，未连接到后端接口' }}
          </p>
        </div>
        <div class="topbar-actions">
          <button class="secondary-button" type="button" @click="sidebarCollapsed = !sidebarCollapsed">
            {{ sidebarCollapsed ? "展开边栏" : "收起边栏" }}
          </button>
          <button class="ghost-light-button" type="button" @click="logout">退出</button>
        </div>
      </header>

      <section v-if="currentView === 'dashboard'" class="content-grid">
        <article
          v-for="chart in dashboardCharts"
          :key="chart.key"
          class="chart-card"
        >
          <div class="chart-head">
            <div>
              <h2>{{ chart.title }} <span class="help-dot">?</span></h2>
              <p>过去 7 天</p>
            </div>
            <div class="chart-stat">
              <strong>{{ chart.total }}</strong>
              <span class="trend-badge" :class="chart.trendClass">{{ chart.trendText }}</span>
            </div>
          </div>
          <div class="chart-stage">
            <svg viewBox="0 0 720 240" role="img" :aria-label="chart.title">
              <g class="grid-lines">
                <line v-for="line in chart.grid" :key="line.y" x1="54" :y1="line.y" x2="690" :y2="line.y" />
                <line v-for="x in chart.xLines" :key="x" :x1="x" y1="32" :x2="x" y2="196" />
              </g>
              <g class="axis-labels">
                <text v-for="line in chart.grid" :key="line.label" x="42" :y="line.y + 5">{{ line.label }}</text>
                <text v-for="point in chart.points" :key="point.label" :x="point.x" y="224">{{ point.label }}</text>
              </g>
              <polyline class="chart-line" :style="{ stroke: chart.color }" :points="chart.polyline" />
              <circle
                v-for="point in chart.points"
                :key="point.label + point.value"
                class="chart-point"
                :style="{ fill: chart.color }"
                :cx="point.x"
                :cy="point.y"
                r="4"
              />
            </svg>
          </div>
        </article>
      </section>

      <section v-if="currentView === 'users'" class="panel-card">
        <div class="panel-title row">
          <div>
            <h2>用户管理</h2>
            <p>管理用户账号、角色和启停状态。</p>
          </div>
          <button class="primary-button" type="button" @click="openUserDialog()">新增用户</button>
        </div>

        <div class="toolbar">
          <label>
            搜索
            <input v-model.trim="filters.keyword" placeholder="用户名 / 昵称 / 邮箱" @input="loadUsers" />
          </label>
          <label>
            状态
            <select v-model="filters.status" @change="loadUsers">
              <option value="">全部</option>
              <option value="enabled">启用</option>
              <option value="disabled">禁用</option>
            </select>
          </label>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>用户</th>
                <th>邮箱</th>
                <th>手机号</th>
                <th>角色</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td>
                  <button class="user-link" type="button" @click="openUserData(user)">
                    <span class="avatar">{{ user.nickname.slice(0, 2) }}</span>
                    <span>
                      <strong>{{ user.nickname }}</strong>
                      <small>{{ user.username }}</small>
                    </span>
                  </button>
                </td>
                <td>{{ user.email }}</td>
                <td>{{ user.mobile }}</td>
                <td>{{ user.role }}</td>
                <td>
                  <span class="status-pill" :class="{ disabled: user.status === 'disabled' }">
                    {{ user.status === "enabled" ? "启用" : "禁用" }}
                  </span>
                </td>
                <td>
                  <div class="row-actions">
                    <button class="link-button" type="button" @click="openUserDialog(user)">编辑</button>
                    <button class="link-button" type="button" @click="toggleUserStatus(user)">
                      {{ user.status === "enabled" ? "禁用" : "启用" }}
                    </button>
                    <button class="link-button" type="button" @click="deleteUser(user)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-if="currentView === 'userData'" class="panel-card">
        <div class="panel-title row">
          <div>
            <h2>用户数据管理</h2>
            <p>用户后续上传的提单数据会在这里查看和管理。点击用户可进入对应数据视图。</p>
          </div>
          <select v-model="selectedUserId" @change="loadSelectedBills">
            <option v-for="user in users" :key="user.id" :value="user.id">{{ user.nickname }}</option>
          </select>
        </div>

        <div class="data-summary">
          <article>
            <span>当前用户</span>
            <strong>{{ selectedUser?.nickname }}</strong>
          </article>
          <article>
            <span>提单数量</span>
            <strong>{{ selectedBills.length }}</strong>
          </article>
          <article>
            <span>待确认</span>
            <strong>{{ selectedBills.filter((item) => item.status === "待确认").length }}</strong>
          </article>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>提单号</th>
                <th>船名航次</th>
                <th>装货港</th>
                <th>卸货港</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="bill in selectedBills" :key="bill.blNo">
                <td>{{ bill.blNo }}</td>
                <td>{{ bill.vesselVoyage }}</td>
                <td>{{ bill.pol }}</td>
                <td>{{ bill.pod }}</td>
                <td><span class="status-pill">{{ bill.status }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-if="currentView === 'permissions'" class="panel-card">
        <div class="panel-title">
          <h2>权限管理</h2>
          <p>仅超级管理员可访问。当前先预留角色、菜单、接口权限配置入口。</p>
        </div>
        <div class="permission-grid">
          <article v-for="permission in permissions" :key="permission.title">
            <h3>{{ permission.title }}</h3>
            <p>{{ permission.desc }}</p>
            <button class="pill-link" type="button">配置</button>
          </article>
        </div>
      </section>
    </main>

    <dialog ref="userDialog" class="user-dialog">
      <form @submit.prevent="saveUserForm">
        <div class="dialog-header">
          <div>
            <p class="eyebrow">User</p>
            <h2>{{ editingUserId ? "编辑用户" : "新增用户" }}</h2>
          </div>
          <button class="icon-button" type="button" @click="closeUserDialog">×</button>
        </div>
        <div class="form-grid">
          <label>
            用户名
            <input v-model.trim="userForm.username" required />
          </label>
          <label>
            昵称
            <input v-model.trim="userForm.nickname" required />
          </label>
          <label>
            邮箱
            <input v-model.trim="userForm.email" type="email" required />
          </label>
          <label>
            手机号
            <input v-model.trim="userForm.mobile" required />
          </label>
          <label>
            角色
            <select v-model="userForm.role">
              <option>平台管理员</option>
              <option>企业管理员</option>
              <option>业务操作员</option>
            </select>
          </label>
          <label>
            状态
            <select v-model="userForm.status">
              <option value="enabled">启用</option>
              <option value="disabled">禁用</option>
            </select>
          </label>
        </div>
        <div class="dialog-actions">
          <button class="secondary-button" type="button" @click="closeUserDialog">取消</button>
          <button class="primary-button" type="submit">保存</button>
        </div>
      </form>
    </dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import {
  fetchUserBills,
  fetchUsers,
  loginAdmin,
  removeUser,
  saveUser,
  updateUserStatus,
} from "./api/adminApi";

const session = reactive({
  loggedIn: false,
  role: "SUPER_ADMIN",
});

const loginForm = reactive({
  username: "admin",
  password: "",
  role: "SUPER_ADMIN",
});

const users = ref([]);
const apiSource = ref("mock");
const toasts = ref([]);
const currentView = ref("dashboard");
const sidebarCollapsed = ref(false);
const selectedUserId = ref("");
const selectedBills = ref([]);
const editingUserId = ref("");
const userDialog = ref(null);

const filters = reactive({
  keyword: "",
  status: "",
});

const userForm = reactive({
  username: "",
  nickname: "",
  email: "",
  mobile: "",
  role: "业务操作员",
  status: "enabled",
});

const dashboardSeries = [
  {
    key: "billCount",
    title: "提单数量",
    color: "#20c7c9",
    values: [12, 18, 15, 26, 32, 28, 41],
  },
  {
    key: "tokenCost",
    title: "提单处理 Token 消耗",
    color: "#2f7cff",
    values: [6800, 8200, 7600, 11200, 13400, 12800, 15600],
  },
  {
    key: "activeUsers",
    title: "用户活跃数量",
    color: "#ff7a45",
    values: [3, 5, 4, 8, 9, 7, 11],
  },
];

const permissions = [
  { title: "角色管理", desc: "配置平台管理员、企业管理员、业务操作员等角色。" },
  { title: "菜单权限", desc: "控制左侧菜单和页面级功能是否可见。" },
  { title: "接口权限", desc: "后续与后端 `/admin/**` 权限校验策略对齐。" },
];

const currentMeta = computed(() => {
  const metaMap = {
    dashboard: { eyebrow: "Dashboard", title: "工作台看板" },
    users: { eyebrow: "Users", title: "用户管理" },
    userData: { eyebrow: "Bill Data", title: "用户数据管理" },
    permissions: { eyebrow: "Access Control", title: "权限管理" },
  };
  return metaMap[currentView.value];
});

const roleText = computed(() => (session.role === "SUPER_ADMIN" ? "超级管理员" : "管理员"));

const sidebarItems = computed(() => [
  { key: "dashboard", label: "工作台看板", icon: "⌂", locked: false },
  { key: "users", label: "用户管理", icon: "◎", locked: false },
  { key: "userData", label: "用户数据管理", icon: "▦", locked: false },
  { key: "permissions", label: "权限管理", icon: "◇", locked: session.role !== "SUPER_ADMIN" },
]);

const selectedUser = computed(() => users.value.find((user) => user.id === selectedUserId.value));

const dashboardCharts = computed(() => dashboardSeries.map(createLineChart));

async function login() {
  try {
    const result = await loginAdmin(loginForm);
    apiSource.value = result.__source || "backend";
    session.loggedIn = true;
    session.role = result.role || loginForm.role;
    currentView.value = "dashboard";
    await loadUsers();
    notify("登录成功", "已通过后端账号密码校验。", apiSource.value);
  } catch (error) {
    notify("登录失败", error.message || "请检查后端服务和网络连接。", "error");
  }
}

function logout() {
  session.loggedIn = false;
  currentView.value = "dashboard";
  notify("已退出", "管理端会话已结束。", "backend");
}

function switchView(view) {
  if (view === "permissions" && session.role !== "SUPER_ADMIN") {
    return;
  }
  currentView.value = view;
  if (view === "userData") {
    loadSelectedBills();
  }
}

async function loadUsers() {
  const result = await fetchUsers(filters);
  apiSource.value = result.__source || "backend";
  users.value = [...result];
  if (!selectedUserId.value && users.value.length > 0) {
    selectedUserId.value = users.value[0].id;
  }
}

async function loadSelectedBills() {
  if (!selectedUserId.value) {
    selectedBills.value = [];
    return;
  }
  const result = await fetchUserBills(selectedUserId.value);
  apiSource.value = result.__source || "backend";
  selectedBills.value = [...result];
}

async function openUserData(user) {
  selectedUserId.value = user.id;
  currentView.value = "userData";
  await loadSelectedBills();
}

function openUserDialog(user) {
  editingUserId.value = user?.id || "";
  Object.assign(userForm, user || {
    username: "",
    nickname: "",
    email: "",
    mobile: "",
    role: "业务操作员",
    status: "enabled",
  });
  userDialog.value.showModal();
}

function closeUserDialog() {
  userDialog.value.close();
}

async function saveUserForm() {
  const result = await saveUser({
    id: editingUserId.value,
    ...userForm,
  });
  apiSource.value = result.__source || "backend";
  filters.keyword = "";
  filters.status = "";
  await loadUsers();
  closeUserDialog();
  notify(editingUserId.value ? "用户已更新" : "用户已创建", "用户管理数据已刷新。", apiSource.value);
}

async function toggleUserStatus(user) {
  const nextStatus = user.status === "enabled" ? "disabled" : "enabled";
  const result = await updateUserStatus(user.id, nextStatus);
  apiSource.value = result.__source || "backend";
  await loadUsers();
  notify("状态已更新", `${user.nickname} 已${nextStatus === "enabled" ? "启用" : "禁用"}。`, apiSource.value);
}

async function deleteUser(user) {
  if (!confirm(`确认删除用户 ${user.nickname}？`)) {
    return;
  }
  const result = await removeUser(user.id);
  apiSource.value = result.__source || "backend";
  if (selectedUserId.value === user.id) {
    selectedUserId.value = "";
  }
  await loadUsers();
  notify("用户已删除", `${user.nickname} 已从列表移除。`, apiSource.value);
}

function createLineChart(series) {
  const maxValue = Math.max(...series.values, 1);
  const upper = Math.ceil(maxValue * 1.18);
  const latest = series.values.at(-1) ?? 0;
  const previous = series.values.at(-2) ?? latest;
  const trend = previous === 0 ? 0 : Math.round(((latest - previous) / previous) * 100);
  const labels = getLastSevenDayLabels();
  const width = 636;
  const left = 54;
  const top = 32;
  const height = 164;
  const step = width / (series.values.length - 1);
  const points = series.values.map((value, index) => {
    const x = left + index * step;
    const y = top + height - (value / upper) * height;
    return {
      label: labels[index],
      value,
      x,
      y,
    };
  });
  return {
    ...series,
    total: formatCompactNumber(latest),
    trendText: `${trend > 0 ? "+" : ""}${trend}%`,
    trendClass: trend > 0 ? "positive" : trend < 0 ? "negative" : "flat",
    points,
    polyline: points.map((point) => `${point.x},${point.y}`).join(" "),
    xLines: points.map((point) => point.x),
    grid: [1, 0.75, 0.5, 0.25, 0].map((ratio) => ({
      y: top + height * (1 - ratio),
      label: formatCompactNumber(Math.round(upper * ratio)),
    })),
  };
}

function getLastSevenDayLabels() {
  const formatter = new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
  });
  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() - (6 - index));
    return formatter.format(date);
  });
}

function formatCompactNumber(value) {
  if (value >= 10000) {
    return `${(value / 1000).toFixed(1)}k`;
  }
  return String(value);
}

function notify(title, message, type = "backend") {
  const toast = {
    id: Date.now() + Math.random(),
    title,
    message,
    type,
  };
  toasts.value = [toast, ...toasts.value].slice(0, 3);
  window.setTimeout(() => {
    toasts.value = toasts.value.filter((item) => item.id !== toast.id);
  }, 3600);
}

onMounted(loadUsers);
</script>
