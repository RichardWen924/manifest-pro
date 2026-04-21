<template>
  <div class="toast-stack" aria-live="polite">
    <div v-for="toast in toasts" :key="toast.id" class="toast" :class="toast.type">
      <strong>{{ toast.title }}</strong>
      <span>{{ toast.message }}</span>
    </div>
  </div>

  <section v-if="!session.loggedIn" class="login-stage">
    <nav class="glass-nav">
      <div class="brand">
        <span class="brand-orb">MR</span>
        <span>Manifest Reader Client</span>
      </div>
      <span class="nav-note">Ocean documents, quietly organized.</span>
    </nav>

    <main class="login-canvas">
      <section class="login-copy">
        <p class="eyebrow">Client Workspace</p>
        <h1>让提单文件进入一个安静、清晰的工作台。</h1>
        <p>
          登录后查看已存提单数据，上传文件提取模板，并按模板导出目标文件。
          当前是客户端原型，交互优先，后续接入 user-service。
        </p>
        <div class="signal-row" aria-label="Workspace highlights">
          <span>BL Data</span>
          <span>Template Extract</span>
          <span>Export Flow</span>
        </div>
      </section>

      <form class="login-card" @submit.prevent="login">
        <div>
          <p class="card-kicker">鉴权入口</p>
          <h2>登录客户端</h2>
          <p>使用账号进入用户工作台。原型阶段会生成本地会话。</p>
        </div>
        <label>
          用户名
          <input v-model.trim="loginForm.username" placeholder="tenant_user" required />
        </label>
        <label>
          密码
          <input v-model.trim="loginForm.password" type="password" placeholder="请输入密码" required />
        </label>
        <label>
          航运公司四字母编号
          <input v-model.trim="loginForm.companyCode" maxlength="4" placeholder="MSKU" required />
        </label>
        <button class="primary-button" type="submit">进入工作台</button>
      </form>
    </main>
  </section>

  <section v-else class="client-shell" :class="{ collapsed: sidebarCollapsed }">
    <aside class="client-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="profile-block">
        <div class="avatar" :title="session.nickname">{{ avatarText }}</div>
        <div v-if="!sidebarCollapsed" class="profile-copy">
          <strong>{{ session.nickname }}</strong>
          <span>{{ session.companyCode }}</span>
        </div>
        <button class="collapse-button" type="button" @click="sidebarCollapsed = !sidebarCollapsed">
          {{ sidebarCollapsed ? "›" : "‹" }}
        </button>
      </div>

      <nav class="side-menu" aria-label="Client sidebar">
        <button
          v-for="item in navItems"
          :key="item.key"
          class="menu-item"
          :class="{ active: currentView === item.key }"
          type="button"
          @click="currentView = item.key"
        >
          <span class="menu-icon">{{ item.icon }}</span>
          <span v-if="!sidebarCollapsed">{{ item.label }}</span>
        </button>
      </nav>

      <div v-if="!sidebarCollapsed" class="sidebar-status">
        <span class="status-dot"></span>
        <p>已通过鉴权</p>
      </div>
    </aside>

    <main class="workspace">
      <header class="workspace-hero">
        <div>
          <p class="eyebrow">{{ currentMeta.eyebrow }}</p>
          <h1>{{ currentMeta.title }}</h1>
          <p>{{ currentMeta.description }}</p>
        </div>
        <div class="hero-actions">
          <button class="secondary-button" type="button" @click="sidebarCollapsed = !sidebarCollapsed">
            {{ sidebarCollapsed ? "展开边栏" : "收起边栏" }}
          </button>
          <button class="ghost-button" type="button" @click="logout">退出</button>
        </div>
      </header>

      <section v-if="currentView === 'overview'" class="scene-grid">
        <article class="metric-card dark">
          <span>已存提单</span>
          <strong>{{ savedBills.length }}</strong>
          <p>总览看板预留，后续展示上传趋势、解析成功率和导出记录。</p>
        </article>
        <article class="metric-card">
          <span>模板提取</span>
          <strong>{{ extractedTemplates.length }}</strong>
          <p>从历史提单或上传文件中提取结构化模板。</p>
        </article>
        <article class="metric-card">
          <span>待导出</span>
          <strong>{{ exportJobs.length }}</strong>
          <p>按照模板生成目标文件，保留人工确认节点。</p>
        </article>
        <article class="panel-card wide">
          <h2>总览预留</h2>
          <p>这里后续可接入用户提单统计、近期文件、模板推荐和导出任务队列。</p>
          <div class="placeholder-strip">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </article>
      </section>

      <section v-if="currentView === 'bills'" class="panel-card">
        <div class="panel-title">
          <div>
            <h2>已存提单数据</h2>
            <p>查看用户已保存的提单数据，点击行可切换选中状态。</p>
          </div>
          <button class="primary-button" type="button" @click="notify('已刷新', '提单列表已从原型数据刷新。', 'backend')">刷新</button>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>提单号</th>
                <th>船名航次</th>
                <th>起运港</th>
                <th>目的港</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="bill in savedBills"
                :key="bill.id"
                :class="{ selected: selectedBillId === bill.id }"
                @click="selectedBillId = bill.id"
              >
                <td>{{ bill.blNo }}</td>
                <td>{{ bill.vessel }}</td>
                <td>{{ bill.pol }}</td>
                <td>{{ bill.pod }}</td>
                <td><span class="pill">{{ bill.status }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-if="currentView === 'extract'" class="work-grid">
        <article class="panel-card">
          <div class="panel-title compact">
            <h2>提单模板提取</h2>
            <p>上传样本文件，提取字段结构和可复用模板。</p>
          </div>
          <label class="drop-zone" :class="{ active: extractFile }">
            <input type="file" accept=".pdf,.doc,.docx,.xlsx,.xls,.png,.jpg,.jpeg" @change="handleExtractFile" />
            <span class="upload-symbol">↑</span>
            <strong>{{ extractFile ? extractFile.name : "上传提单样本文件" }}</strong>
            <p>支持 PDF、Word、Excel、图片。当前仅生成交互原型记录。</p>
          </label>
          <button class="primary-button full" type="button" @click="extractTemplate">开始提取模板</button>
        </article>

        <article class="panel-card dark-panel">
          <h2>提取结果</h2>
          <div v-if="extractedTemplates.length" class="result-list">
            <button v-for="item in extractedTemplates" :key="item.id" type="button">
              <span>{{ item.name }}</span>
              <strong>{{ item.fields }} fields</strong>
            </button>
          </div>
          <p v-else>上传文件后，这里会预览模板字段、置信度和可编辑映射。</p>
        </article>
      </section>

      <section v-if="currentView === 'export'" class="work-grid">
        <article class="panel-card">
          <div class="panel-title compact">
            <h2>按模板导出</h2>
            <p>选择模板并上传目标文件，生成适配目标格式的导出任务。</p>
          </div>
          <label>
            选择模板
            <select v-model="exportForm.templateId">
              <option v-for="template in templateOptions" :key="template.id" :value="template.id">
                {{ template.name }}
              </option>
            </select>
          </label>
          <label class="drop-zone slim" :class="{ active: exportFile }">
            <input type="file" accept=".doc,.docx,.xlsx,.xls,.pdf" @change="handleExportFile" />
            <span class="upload-symbol">＋</span>
            <strong>{{ exportFile ? exportFile.name : "上传目标文件" }}</strong>
            <p>用于承载模板导出的目标文档。</p>
          </label>
          <button class="primary-button full" type="button" @click="createExportJob">创建导出任务</button>
        </article>

        <article class="panel-card">
          <h2>导出队列</h2>
          <div class="job-list">
            <div v-for="job in exportJobs" :key="job.id" class="job-card">
              <span>{{ job.template }}</span>
              <strong>{{ job.file }}</strong>
              <small>{{ job.status }}</small>
            </div>
          </div>
        </article>
      </section>
    </main>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from "vue";

const loginForm = reactive({
  username: "tenant_user",
  password: "",
  companyCode: "MSKU",
});

const session = reactive({
  loggedIn: false,
  username: "",
  nickname: "",
  companyCode: "",
});

const sidebarCollapsed = ref(false);
const currentView = ref("overview");
const selectedBillId = ref("BL-001");
const extractFile = ref(null);
const exportFile = ref(null);
const toasts = ref([]);

const exportForm = reactive({
  templateId: "tpl-001",
});

const navItems = [
  { key: "overview", label: "用户总览", icon: "◎" },
  { key: "bills", label: "已存提单数据", icon: "▤" },
  { key: "extract", label: "提单模版提取", icon: "◇" },
  { key: "export", label: "按模版导出", icon: "↗" },
];

const metaMap = {
  overview: {
    eyebrow: "Overview",
    title: "用户总览",
    description: "预留看板位置，后续承载用户文件、提单和导出任务的综合状态。",
  },
  bills: {
    eyebrow: "Stored BL Data",
    title: "已存提单数据",
    description: "查看该用户历史上传和保存的提单结构化数据。",
  },
  extract: {
    eyebrow: "Template Extraction",
    title: "提单模版提取",
    description: "上传提单样本文件，提取模板字段和映射关系。",
  },
  export: {
    eyebrow: "Template Export",
    title: "按模版导出",
    description: "上传目标文件，按照选定模板生成导出任务。",
  },
};

const savedBills = ref([
  { id: "BL-001", blNo: "MSKU-938201", vessel: "COSCO TAURUS / 046E", pol: "Shanghai", pod: "Los Angeles", status: "已确认" },
  { id: "BL-002", blNo: "ONEY-771904", vessel: "EVER GLOBE / 122W", pol: "Ningbo", pod: "Hamburg", status: "待复核" },
  { id: "BL-003", blNo: "HLCU-204488", vessel: "MAERSK ELBA / 18N", pol: "Yantian", pod: "Rotterdam", status: "草稿" },
]);

const templateOptions = ref([
  { id: "tpl-001", name: "标准海运提单模板" },
  { id: "tpl-002", name: "北美线提单模板" },
  { id: "tpl-003", name: "欧线订舱导出模板" },
]);

const extractedTemplates = ref([]);
const exportJobs = ref([]);

const currentMeta = computed(() => metaMap[currentView.value]);
const avatarText = computed(() => (session.nickname || session.username || "U").slice(0, 2).toUpperCase());

function login() {
  session.loggedIn = true;
  session.username = loginForm.username;
  session.nickname = loginForm.username === "tenant_user" ? "测试用户" : loginForm.username;
  session.companyCode = loginForm.companyCode.toUpperCase();
  notify("登录成功", "已进入客户端工作台。", "backend");
}

function logout() {
  session.loggedIn = false;
  currentView.value = "overview";
  notify("已退出", "客户端会话已结束。", "backend");
}

function handleExtractFile(event) {
  extractFile.value = event.target.files?.[0] || null;
}

function handleExportFile(event) {
  exportFile.value = event.target.files?.[0] || null;
}

function extractTemplate() {
  if (!extractFile.value) {
    notify("请先上传文件", "选择一个提单样本文件后再进行模板提取。", "error");
    return;
  }
  extractedTemplates.value = [
    {
      id: Date.now(),
      name: extractFile.value.name.replace(/\.[^.]+$/, "") || "新模板",
      fields: 18,
    },
    ...extractedTemplates.value,
  ];
  notify("模板已提取", "已生成原型模板结果，可在后续接入真实解析。", "backend");
}

function createExportJob() {
  if (!exportFile.value) {
    notify("请先上传目标文件", "选择目标文件后再创建导出任务。", "error");
    return;
  }
  const template = templateOptions.value.find((item) => item.id === exportForm.templateId);
  exportJobs.value = [
    {
      id: Date.now(),
      template: template?.name || "未命名模板",
      file: exportFile.value.name,
      status: "等待生成",
    },
    ...exportJobs.value,
  ];
  notify("导出任务已创建", "任务已进入原型队列，后续接入文件导出服务。", "backend");
}

function notify(title, message, type = "backend") {
  const id = Date.now() + Math.random();
  toasts.value = [{ id, title, message, type }, ...toasts.value].slice(0, 3);
  window.setTimeout(() => {
    toasts.value = toasts.value.filter((toast) => toast.id !== id);
  }, 3200);
}
</script>
