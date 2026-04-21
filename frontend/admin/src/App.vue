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
        <article class="metric-card dark">
          <p>今日上传提单</p>
          <strong>28</strong>
          <span>较昨日 +12%</span>
        </article>
        <article class="metric-card">
          <p>待确认解析</p>
          <strong>7</strong>
          <span>需要业务人员复核</span>
        </article>
        <article class="metric-card">
          <p>活跃用户</p>
          <strong>{{ users.length }}</strong>
          <span>来自 API / mock fallback</span>
        </article>
        <article class="panel-card wide">
          <div class="panel-title">
            <h2>工作台看板预留</h2>
            <p>后续可接入上传趋势、解析成功率、异常任务、企业使用排行等数据。</p>
          </div>
          <div class="timeline">
            <div v-for="task in dashboardTasks" :key="task.title" class="timeline-item">
              <span></span>
              <div>
                <strong>{{ task.title }}</strong>
                <p>{{ task.desc }}</p>
              </div>
            </div>
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

const dashboardTasks = [
  { title: "上传趋势", desc: "展示用户上传提单数量、成功率和日环比。" },
  { title: "解析质量", desc: "统计字段识别准确率、失败原因和人工修正次数。" },
  { title: "企业排行", desc: "按企业维度展示使用量、活跃用户和模板使用情况。" },
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
