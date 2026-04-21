const { createApp, computed, reactive, ref } = Vue;

const defaultUsers = [
  {
    id: "u-1001",
    username: "admin",
    nickname: "平台管理员",
    email: "admin@manifest.local",
    mobile: "13800000001",
    role: "平台管理员",
    status: "enabled",
  },
  {
    id: "u-1002",
    username: "tenant_ops",
    nickname: "企业操作员",
    email: "ops@manifest.local",
    mobile: "13800000002",
    role: "业务操作员",
    status: "enabled",
  },
  {
    id: "u-1003",
    username: "auditor",
    nickname: "审计员",
    email: "audit@manifest.local",
    mobile: "13800000003",
    role: "企业管理员",
    status: "disabled",
  },
];

const billData = {
  "u-1001": [
    { blNo: "MRBL240001", vesselVoyage: "COSCO Star / 042E", pol: "Shanghai", pod: "Los Angeles", status: "已确认" },
    { blNo: "MRBL240002", vesselVoyage: "Ever Bloom / 118W", pol: "Ningbo", pod: "Hamburg", status: "待确认" },
  ],
  "u-1002": [
    { blNo: "MRBL240003", vesselVoyage: "OOCL Asia / 063E", pol: "Qingdao", pod: "Singapore", status: "解析中" },
  ],
  "u-1003": [
    { blNo: "MRBL240004", vesselVoyage: "Maersk Pearl / 221A", pol: "Xiamen", pod: "Rotterdam", status: "待确认" },
  ],
};

createApp({
  setup() {
    const session = reactive({
      loggedIn: false,
      role: "SUPER_ADMIN",
    });

    const loginForm = reactive({
      username: "admin",
      password: "",
      role: "SUPER_ADMIN",
    });

    const users = ref([...defaultUsers]);
    const currentView = ref("dashboard");
    const sidebarCollapsed = ref(false);
    const selectedUserId = ref(defaultUsers[0].id);
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

    const filteredUsers = computed(() => {
      const keyword = filters.keyword.toLowerCase();
      return users.value.filter((user) => {
        const text = `${user.username} ${user.nickname} ${user.email} ${user.mobile}`.toLowerCase();
        const keywordMatched = !keyword || text.includes(keyword);
        const statusMatched = !filters.status || user.status === filters.status;
        return keywordMatched && statusMatched;
      });
    });

    const selectedUser = computed(() => users.value.find((user) => user.id === selectedUserId.value));
    const selectedBills = computed(() => billData[selectedUserId.value] || []);

    function login() {
      session.loggedIn = true;
      session.role = loginForm.role;
      currentView.value = "dashboard";
    }

    function logout() {
      session.loggedIn = false;
      currentView.value = "dashboard";
    }

    function switchView(view) {
      if (view === "permissions" && session.role !== "SUPER_ADMIN") {
        return;
      }
      currentView.value = view;
    }

    function openUserData(user) {
      selectedUserId.value = user.id;
      currentView.value = "userData";
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

    function saveUser() {
      const payload = {
        id: editingUserId.value || `u-${Date.now()}`,
        ...userForm,
      };

      if (editingUserId.value) {
        users.value = users.value.map((user) => (user.id === editingUserId.value ? payload : user));
      } else {
        users.value = [payload, ...users.value];
      }

      if (!selectedUserId.value) {
        selectedUserId.value = payload.id;
      }

      closeUserDialog();
    }

    function toggleUserStatus(user) {
      user.status = user.status === "enabled" ? "disabled" : "enabled";
    }

    function deleteUser(user) {
      if (!confirm(`确认删除用户 ${user.nickname}？`)) {
        return;
      }
      users.value = users.value.filter((item) => item.id !== user.id);
      if (selectedUserId.value === user.id) {
        selectedUserId.value = users.value[0]?.id || "";
      }
    }

    return {
      session,
      loginForm,
      users,
      currentView,
      sidebarCollapsed,
      selectedUserId,
      editingUserId,
      userDialog,
      filters,
      userForm,
      dashboardTasks,
      permissions,
      currentMeta,
      roleText,
      sidebarItems,
      filteredUsers,
      selectedUser,
      selectedBills,
      login,
      logout,
      switchView,
      openUserData,
      openUserDialog,
      closeUserDialog,
      saveUser,
      toggleUserStatus,
      deleteUser,
    };
  },
}).mount("#app");

