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
        <span>Manifest Reader</span>
      </div>
      <div class="public-nav-actions">
        <button class="ghost-nav-button" type="button" @click="openAuthDialog('login')">登录</button>
        <button class="primary-nav-button" type="button" @click="openAuthDialog('register')">开始使用</button>
      </div>
    </nav>

    <main class="public-shell">
      <section class="public-hero">
        <div class="public-copy">
          <p class="eyebrow">DIGITAL FREIGHT OPERATING PLATFORM</p>
          <h1>看得见流程，管得住提单，也能直接发起货运协作。</h1>
          <p class="public-summary">
            把提单管理、模板提取、按模板导出与货运商城整合进一个专业客户端，让文档处理和业务协作在同一平台完成。
          </p>
          <div class="public-hero-actions">
            <button class="primary-button" type="button" @click="openAuthDialog('login')">进入平台</button>
            <button class="ghost-button public-secondary-cta" type="button" @click="openAuthDialog('register')">创建账号</button>
          </div>
          <div class="public-proof-band" aria-label="Platform highlights">
            <span v-for="item in landingProofPoints" :key="item">{{ item }}</span>
          </div>
        </div>

        <div class="public-visual-stage" aria-label="Platform capability showcase">
          <div class="visual-orb orb-one"></div>
          <div class="visual-orb orb-two"></div>
          <div class="visual-grid">
            <article
              v-for="capability in landingCapabilities"
              :key="capability.title"
              class="capability-card"
            >
              <span class="capability-icon" aria-hidden="true" v-html="getIconSvg(capability.icon)"></span>
              <strong>{{ capability.title }}</strong>
              <p>{{ capability.desc }}</p>
            </article>
          </div>
        </div>
      </section>

      <section class="public-support-grid">
        <article class="public-support-block">
          <p class="eyebrow">Platform Scope</p>
          <h2>一个客户端覆盖文档处理与货运协作。</h2>
          <p>把原本分散的提单录入、模板整理、导出文件和货运成交放进一条更短的操作路径。</p>
        </article>
        <article class="public-workflow-strip">
          <div v-for="step in landingWorkflow" :key="step.title" class="workflow-step">
            <span>{{ step.index }}</span>
            <strong>{{ step.title }}</strong>
            <p>{{ step.desc }}</p>
          </div>
        </article>
      </section>
    </main>

    <section v-if="authDialogOpen" class="auth-dialog-backdrop" @click.self="closeAuthDialog">
      <article class="auth-dialog-shell">
        <div class="auth-dialog-brand">
          <p class="eyebrow">{{ authMode === "login" ? "Platform Login" : "Platform Register" }}</p>
          <h2>{{ authMode === "login" ? "进入 Manifest Reader" : "创建客户端账号" }}</h2>
          <p>
            {{ authMode === "login"
              ? "通过用户名或四字编号进入平台工作台。"
              : "创建账号后即可返回登录，进入综合总览与业务工作台。" }}
          </p>
        </div>

        <form v-if="authMode === 'login'" class="login-card auth-dialog-card" novalidate @submit.prevent="login">
          <div class="auth-card-head">
            <div>
              <p class="card-kicker">鉴权入口</p>
              <h2>登录客户端</h2>
            </div>
            <button class="ghost-button" type="button" @click="switchAuthMode('register')">注册账号</button>
          </div>
          <label>
            用户名 / 四字编号
            <input v-model.trim="loginForm.identity" placeholder="tenant_user 或 TEST" @input="clearLoginError" />
          </label>
          <label>
            密码
            <input v-model.trim="loginForm.password" type="password" placeholder="请输入密码" @input="clearLoginError" />
          </label>
          <p v-if="loginError" class="form-error">{{ loginError }}</p>
          <button class="primary-button" type="submit">进入工作台</button>
        </form>

        <form v-else class="login-card register-card auth-dialog-card" novalidate @submit.prevent="register">
          <div class="auth-card-head">
            <div>
              <p class="card-kicker">新用户入口</p>
              <h2>注册客户端</h2>
              <p>创建测试租户下的普通用户账号，注册成功后可直接返回登录。</p>
            </div>
            <button class="ghost-button" type="button" @click="switchAuthMode('login')">返回登录</button>
          </div>
          <label>
            用户名
            <input v-model.trim="registerForm.username" placeholder="new_user" @input="clearRegisterError" />
          </label>
          <label>
            密码
            <input v-model.trim="registerForm.password" type="password" placeholder="至少 6 位密码" @input="clearRegisterError" />
          </label>
          <label>
            确认密码
            <input v-model.trim="registerForm.confirmPassword" type="password" placeholder="再次输入密码" @input="clearRegisterError" />
          </label>
          <label>
            昵称
            <input v-model.trim="registerForm.nickname" placeholder="可选" @input="clearRegisterError" />
          </label>
          <label>
            邮箱
            <input v-model.trim="registerForm.email" type="email" placeholder="可选" @input="clearRegisterError" />
          </label>
          <p v-if="registerError" class="form-error">{{ registerError }}</p>
          <button class="primary-button" type="submit">创建账号</button>
        </form>
      </article>
    </section>
  </section>

  <section v-else class="client-shell workspace-shell">
    <header class="workspace-topbar">
      <div class="workspace-brand">
        <span class="workspace-brand-mark">MR</span>
        <div>
          <strong>Manifest Reader</strong>
          <small>Client Workspace</small>
        </div>
      </div>

      <nav class="workspace-primary-nav" aria-label="Primary workspace navigation">
        <button
          v-for="item in primaryNavItems"
          :key="item.key"
          class="workspace-primary-tab"
          :class="{ active: primaryWorkspace === item.key }"
          type="button"
          @click="switchPrimaryView(item.key)"
        >
          <span class="workspace-primary-icon" aria-hidden="true" v-html="getIconSvg(item.icon)"></span>
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="workspace-account-strip">
        <div class="workspace-account-chip">
          <span class="avatar" :title="session.nickname">{{ avatarText }}</span>
          <div>
            <strong>{{ session.nickname || session.username }}</strong>
            <small>{{ session.companyCode || "TEST" }}</small>
          </div>
        </div>
        <button class="workspace-logout-button" type="button" @click="logout">退出</button>
      </div>
    </header>

    <main class="workspace-frame">
      <section v-if="primaryWorkspace === 'overview'" class="module-scene dashboard-scene">
        <section class="overview-hero">
          <div class="overview-hero-copy">
            <p class="eyebrow">Workspace Overview</p>
            <h1>欢迎回来，{{ session.nickname || session.username }}。</h1>
            <p>这里汇总你今天最需要关注的业务状态、待办动作和高频入口，让你更快进入工作节奏。</p>
          </div>
          <div class="overview-account-card">
            <span>当前工作区</span>
            <strong>{{ session.companyCode || "TEST" }}</strong>
            <small>已连接客户端业务工作台</small>
          </div>
        </section>

        <section class="overview-kpi-grid">
          <article v-for="item in clientQuickStats" :key="item.label" class="overview-kpi-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </section>

        <section class="overview-main-grid">
          <article class="panel-card overview-shortcuts-panel">
            <div class="panel-title compact">
              <h2>主流程快捷入口</h2>
              <p>从最常用的入口直接开始工作。</p>
            </div>
            <div class="overview-shortcut-grid">
              <button
                v-for="item in overviewShortcuts"
                :key="item.title"
                class="overview-shortcut"
                type="button"
                @click="handleOverviewShortcut(item.action)"
              >
                <span class="overview-shortcut-icon" aria-hidden="true" v-html="getIconSvg(item.icon)"></span>
                <strong>{{ item.title }}</strong>
                <p>{{ item.desc }}</p>
              </button>
            </div>
          </article>

          <article class="panel-card overview-todo-panel">
            <div class="panel-title compact">
              <h2>待办与系统提醒</h2>
              <p>优先处理当前仍在队列中的工作。</p>
            </div>
            <div class="overview-todo-list">
              <div v-for="item in overviewTodos" :key="item.title" class="overview-todo-item">
                <span class="overview-todo-dot"></span>
                <div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.desc }}</p>
                </div>
              </div>
            </div>
          </article>
        </section>

        <section class="overview-activity-grid">
          <article class="panel-card overview-activity-panel">
            <div class="panel-title compact">
              <h2>最近业务活动</h2>
              <p>最近更新的提单、模板和导出任务。</p>
            </div>
            <div class="overview-activity-list">
              <div v-for="item in recentWorkspaceActivity" :key="item.title" class="overview-activity-item">
                <strong>{{ item.title }}</strong>
                <p>{{ item.desc }}</p>
              </div>
            </div>
          </article>

          <article class="panel-card overview-activity-panel">
            <div class="panel-title compact">
              <h2>市场动态</h2>
              <p>关注货运商城的最新变化与成交状态。</p>
            </div>
            <div class="overview-activity-list">
              <div v-for="item in marketActivityFeed" :key="item.title" class="overview-activity-item">
                <strong>{{ item.title }}</strong>
                <p>{{ item.desc }}</p>
              </div>
            </div>
          </article>
        </section>
      </section>

      <section v-else-if="primaryWorkspace === 'data'" class="workspace-section-shell">
        <aside class="section-sidebar" :class="{ collapsed: sidebarCollapsed }">
          <div class="section-sidebar-head">
            <div v-if="!sidebarCollapsed">
              <p class="eyebrow">Data Workspace</p>
              <h3>数据管理</h3>
              <span>提单、模板与导出统一在一个工作区内。</span>
            </div>
            <button class="section-sidebar-toggle" type="button" @click="sidebarCollapsed = !sidebarCollapsed">
              {{ sidebarCollapsed ? "›" : "‹" }}
            </button>
          </div>
          <nav class="section-sidebar-nav" aria-label="Data navigation">
            <button
              v-for="item in dataNavItems"
              :key="item.key"
              class="section-sidebar-link"
              :class="{ active: currentView === item.key }"
              type="button"
              @click="switchDataView(item.key)"
            >
              <span class="menu-icon" aria-hidden="true" v-html="getIconSvg(item.icon)"></span>
              <span v-if="!sidebarCollapsed">{{ item.label }}</span>
            </button>
          </nav>
        </aside>

        <div class="section-canvas">
          <header class="workspace-header">
            <div>
              <p class="eyebrow">{{ currentMeta.eyebrow }}</p>
              <h1>{{ currentMeta.title }}</h1>
              <p>{{ currentMeta.description }}</p>
            </div>
            <div class="workspace-header-stats">
              <span v-for="item in clientQuickStats" :key="item.label" class="inline-stat">
                <strong>{{ item.value }}</strong>
                <small>{{ item.label }}</small>
              </span>
            </div>
          </header>

          <section v-if="currentView === 'bills'" class="panel-card">
        <div class="panel-title">
          <div>
            <h2>已存提单数据</h2>
            <p>支持分页、搜索、新增、编辑和删除。点击任一提单行展开完整字段预览。</p>
          </div>
          <div class="bill-actions">
            <button class="ghost-button" type="button" @click="resetBillFilters">重置</button>
            <button class="primary-button" type="button" @click="startCreateBill">新增提单</button>
          </div>
        </div>

        <div class="bill-toolbar">
          <label>
            关键词
            <input v-model.trim="billQuery.keyword" placeholder="提单号 / 订舱号 / 船名航次" @keyup.enter="searchBills" />
          </label>
          <label>
            状态
            <select v-model="billQuery.status" @change="searchBills">
              <option value="">全部状态</option>
              <option value="DRAFT">草稿</option>
              <option value="CONFIRMED">已确认</option>
              <option value="ARCHIVED">已归档</option>
            </select>
          </label>
          <button class="secondary-button" type="button" @click="searchBills">查询</button>
        </div>

        <div class="batch-toolbar">
          <label class="check-control">
            <input type="checkbox" :checked="allCurrentBillsSelected" :disabled="!savedBills.length" @change="toggleSelectCurrentPage" />
            <span>全选当前页</span>
          </label>
          <span>已选择 {{ selectedBillIds.length }} 条</span>
          <button class="danger-button" type="button" :disabled="!selectedBillIds.length" @click="removeSelectedBills">
            批量删除
          </button>
          <button class="ghost-button" type="button" :disabled="!selectedBillIds.length" @click="clearBillSelection">
            清空选择
          </button>
        </div>

        <form v-if="billEditor.open" class="bill-editor" novalidate @submit.prevent="submitBill">
          <div class="editor-title">
            <div>
              <strong>{{ billEditor.mode === "create" ? "新增提单" : "编辑提单" }}</strong>
            </div>
            <button class="ghost-button" type="button" @click="closeBillEditor">取消</button>
          </div>
          <div class="editor-grid">
            <label>
              提单号
              <input v-model.trim="billForm.blNo" placeholder="TEST-BL-0002" />
            </label>
            <label>
              订舱号
              <input v-model.trim="billForm.bookingNo" placeholder="BOOK-0002" />
            </label>
            <label>
              船名航次
              <input v-model.trim="billForm.vesselVoyage" placeholder="MSC TEST V002" />
            </label>
            <label>
              状态
              <select v-model="billForm.status">
                <option value="DRAFT">草稿</option>
                <option value="CONFIRMED">已确认</option>
                <option value="ARCHIVED">已归档</option>
              </select>
            </label>
            <label>
              起运港
              <input v-model.trim="billForm.portOfLoading" placeholder="SHANGHAI" />
            </label>
            <label>
              目的港
              <input v-model.trim="billForm.portOfDischarge" placeholder="SINGAPORE" />
            </label>
            <label>
              收货地
              <input v-model.trim="billForm.placeOfReceipt" placeholder="SHANGHAI" />
            </label>
            <label>
              交付地
              <input v-model.trim="billForm.placeOfDelivery" placeholder="SINGAPORE" />
            </label>
            <label>
              货运商品名称
              <input v-model.trim="billForm.goodsName" placeholder="Electronic parts" />
            </label>
            <label>
              数量
              <input v-model.number="billForm.quantity" type="number" min="0" placeholder="100" />
            </label>
            <label>
              单位
              <input v-model.trim="billForm.packageUnit" placeholder="CTN" />
            </label>
            <label>
              备注
              <input v-model.trim="billForm.remark" placeholder="可选备注" />
            </label>
          </div>
          <p v-if="billEditor.error" class="form-error">{{ billEditor.error }}</p>
          <button class="primary-button full" type="submit">
            {{ billEditor.mode === "create" ? "创建提单" : "保存修改" }}
          </button>
        </form>

        <div class="bill-list" role="list">
          <article
            v-for="bill in savedBills"
            :key="bill.id"
            class="bill-row"
            :class="{ checked: selectedBillIds.includes(bill.id) }"
            role="listitem"
          >
            <div class="bill-row-main">
              <label class="check-control row-check" @click.stop>
                <input type="checkbox" :checked="selectedBillIds.includes(bill.id)" @change="toggleBillSelection(bill.id)" />
                <span class="sr-only">选择 {{ bill.blNo }}</span>
              </label>
              <button class="bill-grid-row" type="button" @click="openBillDialog(bill)">
                <span class="bill-cell bill-cell-primary">
                  <small>提单号</small>
                  <strong>{{ bill.blNo }}</strong>
                </span>
                <span class="bill-cell">
                  <small>船名航次</small>
                  <strong>{{ bill.vessel }}</strong>
                </span>
                <span class="bill-cell">
                  <small>货运商品名称</small>
                  <strong>{{ bill.goodsName }}</strong>
                </span>
                <span class="bill-cell bill-cell-compact">
                  <small>数量</small>
                  <strong>{{ bill.quantity }}</strong>
                </span>
                <span class="bill-cell bill-cell-status">
                  <small>状态</small>
                  <span class="pill">{{ bill.status }}</span>
                </span>
              </button>
            </div>
          </article>
        </div>
        <div v-if="!savedBills.length" class="empty-state">暂无提单数据，可以点击“新增提单”创建第一条。</div>
        <div class="pagination-bar">
          <span>共 {{ billPage.total }} 条，第 {{ billPage.current }} / {{ billTotalPages }} 页</span>
          <div>
            <button class="ghost-button" type="button" :disabled="billPage.current <= 1" @click="changeBillPage(-1)">上一页</button>
            <button class="ghost-button" type="button" :disabled="billPage.current >= billTotalPages" @click="changeBillPage(1)">下一页</button>
          </div>
        </div>

        <section
          v-if="billDetailDialog.open && activeBillDetail"
          class="bill-detail-dialog-backdrop"
          @click.self="closeBillDialog"
        >
          <article class="bill-detail-dialog">
            <header class="bill-detail-dialog-head">
              <div>
                <p class="eyebrow">Bill Detail</p>
                <h2>{{ activeBillDetail.blNo }}</h2>
                <span class="pill">{{ activeBillDetail.status }}</span>
              </div>
              <button class="ghost-button" type="button" @click="closeBillDialog">关闭</button>
            </header>

            <div class="detail-grid bill-detail-dialog-grid">
              <div v-for="field in activeBillDetail.detailFields" :key="field.label" class="detail-field">
                <span>{{ field.label }}</span>
                <strong>{{ field.value }}</strong>
              </div>
            </div>

            <footer class="bill-detail-dialog-actions">
              <button class="secondary-button" type="button" @click="startEditBill(activeBillDetail)">编辑</button>
              <button class="danger-button" type="button" @click="removeBill(activeBillDetail)">删除</button>
              <button class="ghost-button" type="button" @click="closeBillDialog">关闭</button>
            </footer>
          </article>
        </section>
          </section>

          <section v-if="currentView === 'extract'" class="module-scene">
        <div class="work-grid">
        <article class="panel-card">
          <div class="panel-title compact">
            <h2>提单模板提取</h2>
            <p>上传样本文件，提取字段结构和可复用模板。</p>
          </div>
          <label class="drop-zone" :class="{ active: extractFile }">
            <input type="file" accept=".pdf,.doc,.docx,.xlsx,.xls,.png,.jpg,.jpeg" @change="handleExtractFile" />
            <span class="upload-symbol">↑</span>
            <strong>{{ extractFile ? extractFile.name : "上传提单样本文件" }}</strong>
            <p>支持 PDF、Word、Excel、图片。</p>
          </label>
          <button
            class="primary-button full"
            type="button"
            :disabled="extractingTemplate || isCurrentExtractFileDone"
            @click="extractTemplate"
          >
            {{ extractButtonText }}
          </button>
        </article>

        <article class="panel-card dark-panel">
          <h2>提取结果</h2>
          <div v-if="extractedTemplates.length" class="result-list">
            <button
              v-for="item in extractedTemplates"
              :key="item.id"
              class="extract-result-card"
              :class="{ active: selectedExtractId === item.id }"
              type="button"
              @click="selectExtractResult(item.id)"
            >
              <div class="extract-result-head">
                <div>
                  <span>{{ item.name }}</span>
                  <small>{{ item.templateMessage || item.source }}</small>
                </div>
                <strong>{{ item.fields }} fields</strong>
              </div>
            </button>
          </div>
          <p v-else>上传文件后，这里会预览模板字段、置信度和可编辑映射。</p>
        </article>
        </div>
          </section>

          <section v-if="currentView === 'templates'" class="panel-card">
        <div class="panel-title">
          <div>
            <h2>模板管理</h2>
            <p>管理已经保存的提单模板，查看存储位置、版本、字段数量和启用状态。</p>
          </div>
          <div class="bill-actions">
            <button class="ghost-button" type="button" @click="resetTemplateFilters">重置</button>
            <button class="secondary-button" type="button" @click="loadManagedTemplates">刷新</button>
          </div>
        </div>

        <div class="bill-toolbar template-toolbar">
          <label>
            关键词
            <input v-model.trim="templateQuery.keyword" placeholder="模板名称 / 模板编码" @keyup.enter="searchManagedTemplates" />
          </label>
          <label>
            状态
            <select v-model="templateQuery.status" @change="searchManagedTemplates">
              <option value="">全部状态</option>
              <option value="1">启用</option>
              <option value="0">停用</option>
            </select>
          </label>
          <button class="secondary-button" type="button" @click="searchManagedTemplates">查询</button>
        </div>

        <div class="template-manage-list">
          <article v-for="template in managedTemplates" :key="template.id" class="template-manage-card">
            <div class="template-manage-main">
              <div>
                <span class="template-type">{{ template.templateType || "BILL_DOCX" }}</span>
                <h3>{{ template.templateName }}</h3>
                <p>{{ template.templateCode }}</p>
              </div>
              <div class="template-status-stack">
                <span class="pill" :class="{ muted: Number(template.status) !== 1 }">{{ Number(template.status) === 1 ? "启用" : "停用" }}</span>
                <button
                  class="switch-control"
                  type="button"
                  role="switch"
                  :aria-checked="Number(template.status) === 1"
                  :class="{ on: Number(template.status) === 1 }"
                  :disabled="isTemplateStatusUpdating(template.id)"
                  @click="toggleTemplateStatus(template)"
                >
                  <span class="switch-thumb"></span>
                  <span class="switch-text">{{ Number(template.status) === 1 ? "ON" : "OFF" }}</span>
                </button>
              </div>
            </div>
            <div class="template-storage-grid">
              <div>
                <span>文件名</span>
                <strong>{{ template.fileName || "未关联文件" }}</strong>
              </div>
              <div>
                <span>存储方式</span>
                <strong>{{ template.storageType || "LOCAL" }}</strong>
              </div>
              <div>
                <span>存储位置</span>
                <strong>{{ template.objectKey || "暂无存储位置" }}</strong>
              </div>
              <div>
                <span>版本 / 字段数</span>
                <strong>v{{ template.versionNo || 1 }} / {{ template.fieldCount || 0 }}</strong>
              </div>
            </div>
            <div class="template-manage-actions">
              <button class="danger-button" type="button" @click="removeManagedTemplate(template)">删除</button>
            </div>
          </article>
        </div>
        <div v-if="!managedTemplates.length" class="empty-state">暂无模板。可以先在“提单模版提取”里保存一个模板。</div>
        <div class="pagination-bar">
          <span>共 {{ templatePage.total }} 条，第 {{ templatePage.current }} / {{ templateTotalPages }} 页</span>
          <div>
            <button class="ghost-button" type="button" :disabled="templatePage.current <= 1" @click="changeTemplatePage(-1)">上一页</button>
            <button class="ghost-button" type="button" :disabled="templatePage.current >= templateTotalPages" @click="changeTemplatePage(1)">下一页</button>
          </div>
        </div>
          </section>

          <section v-if="currentView === 'export'" class="module-scene">
        <div class="work-grid">
        <article class="panel-card">
          <div class="panel-title compact">
            <h2>按模板导出</h2>
            <p>选择模板并上传目标文件，生成适配目标格式的导出任务。</p>
          </div>
          <label>
            选择模板
            <select v-model="exportForm.templateId" :disabled="!templateOptions.length">
              <option v-if="!templateOptions.length" value="">暂无可导出的 DOCX 模板</option>
              <option v-for="template in templateOptions" :key="template.id" :value="template.id">
                {{ template.name }}
              </option>
            </select>
          </label>
          <label>
            导出格式
            <select v-model="exportForm.outputFormat">
              <option value="DOCX">DOCX 标准文档</option>
              <option value="PDF">PDF 文档</option>
            </select>
          </label>
          <label class="drop-zone slim" :class="{ active: exportFile }">
            <input type="file" accept=".doc,.docx,.xlsx,.xls,.pdf" @change="handleExportFile" />
            <span class="upload-symbol">＋</span>
            <strong>{{ exportFile ? exportFile.name : "上传目标文件" }}</strong>
            <p>用于承载模板导出的目标文档。</p>
          </label>
          <button class="primary-button full" type="button" :disabled="exportingTemplate" @click="createExportJob">
            {{ exportingTemplate ? "正在抽取并生成" : "创建导出任务" }}
          </button>
        </article>

        <article class="panel-card">
          <h2>导出队列</h2>
          <div class="job-list">
            <div v-for="job in exportJobs" :key="job.id" class="job-card">
              <div class="job-main">
                <span>{{ job.template }}</span>
                <strong>{{ job.file }}</strong>
                <small>{{ job.status }}</small>
              </div>
              <div v-if="job.downloadUrl" class="job-actions">
                <button class="secondary-button" type="button" @click="openExportDialog(job)">查看并保存</button>
              </div>
              <details v-if="job.fields?.length" class="export-field-preview">
                <summary>查看 Dify 提取字段 {{ job.fields.length }}</summary>
                <p v-for="field in job.fields" :key="field.key">
                  <b>{{ field.key }}</b>
                  <span>{{ field.value }}</span>
                </p>
              </details>
              <div v-if="job.missing?.length" class="form-error">
                缺失字段：{{ job.missing.join("、") }}
              </div>
            </div>
          </div>
        </article>
        </div>
          </section>
        </div>
      </section>

      <section v-else-if="primaryWorkspace === 'market'" class="workspace-section-shell market-workspace">
        <aside class="section-sidebar" :class="{ collapsed: sidebarCollapsed }">
          <div class="section-sidebar-head">
            <div v-if="!sidebarCollapsed">
              <p class="eyebrow">Marketplace</p>
              <h3>货运商城</h3>
              <span>发布需求、管理报价并推进履约。</span>
            </div>
            <button class="section-sidebar-toggle" type="button" @click="sidebarCollapsed = !sidebarCollapsed">
              {{ sidebarCollapsed ? "›" : "‹" }}
            </button>
          </div>
          <nav class="section-sidebar-nav" aria-label="Market navigation">
            <button
              v-for="item in marketNavItems"
              :key="item.key"
              class="section-sidebar-link"
              :class="{ active: activeMarketSection === item.key }"
              type="button"
              @click="switchMarketSection(item.key)"
            >
              <span class="menu-icon" aria-hidden="true" v-html="getIconSvg(item.icon)"></span>
              <span v-if="!sidebarCollapsed">{{ item.label }}</span>
            </button>
          </nav>
          <button v-if="!sidebarCollapsed" class="primary-button full" type="button" @click="openMarketDemandEditor">发布货运需求</button>
        </aside>

        <div class="section-canvas">
          <header class="workspace-header">
            <div>
              <p class="eyebrow">{{ currentMeta.eyebrow }}</p>
              <h1>货运商城</h1>
              <p>浏览平台需求、管理自己的发布，并把接单履约统一放在一个更轻量的市场工作区里。</p>
            </div>
            <div class="workspace-header-stats">
              <span v-for="item in marketSummaryCards" :key="item.label" class="inline-stat">
                <strong>{{ item.value }}</strong>
                <small>{{ item.label }}</small>
              </span>
            </div>
          </header>

          <section class="panel-card market-stage-panel">
            <div v-if="marketTab !== 'orders'" class="bill-toolbar market-toolbar">
              <label>
                关键词
                <input v-model.trim="marketQuery.keyword" placeholder="商品名称 / 起运港 / 目的港" @keyup.enter="searchMarketRecords" />
              </label>
              <label>
                状态
                <select v-model="marketQuery.status" @change="searchMarketRecords">
                  <option value="">全部状态</option>
                  <option value="PENDING_REVIEW">待审核</option>
                  <option value="PUBLISHED">待报价</option>
                  <option value="QUOTING">报价中</option>
                  <option value="LOCKED">已锁单</option>
                  <option value="FULFILLING">履约中</option>
                  <option value="COMPLETED">已完结</option>
                  <option value="CANCELLED">已取消</option>
                </select>
              </label>
              <button class="secondary-button" type="button" @click="searchMarketRecords">查询</button>
              <button class="ghost-button" type="button" @click="resetMarketFilters">重置</button>
            </div>

            <form v-if="marketDemandEditor.open" class="bill-editor market-demand-editor" novalidate @submit.prevent="submitMarketDemand">
              <div class="editor-title">
                <div>
                  <strong>发布货运需求</strong>
                </div>
                <button class="ghost-button" type="button" @click="closeMarketDemandEditor">取消</button>
              </div>
              <div class="editor-grid">
                <label>
                  需求标题
                  <input v-model.trim="marketDemandForm.title" placeholder="上海到鹿特丹整柜运输" />
                </label>
                <label>
                  商品名称
                  <input v-model.trim="marketDemandForm.goodsName" placeholder="机械设备" />
                </label>
                <label>
                  起运港
                  <input v-model.trim="marketDemandForm.departurePort" placeholder="SHANGHAI" />
                </label>
                <label>
                  目的港
                  <input v-model.trim="marketDemandForm.destinationPort" placeholder="ROTTERDAM" />
                </label>
                <label>
                  期望船期
                  <input v-model="marketDemandForm.expectedShippingDate" type="date" />
                </label>
                <label>
                  数量
                  <input v-model.number="marketDemandForm.quantity" type="number" min="0" placeholder="10" />
                </label>
                <label>
                  单位
                  <input v-model.trim="marketDemandForm.quantityUnit" placeholder="BOX / CBM / TON" />
                </label>
                <label>
                  预算金额
                  <input v-model.number="marketDemandForm.budgetAmount" type="number" min="0" placeholder="5000" />
                </label>
                <label>
                  币种
                  <input v-model.trim="marketDemandForm.currencyCode" placeholder="CNY" />
                </label>
                <label>
                  联系人
                  <input v-model.trim="marketDemandForm.contactName" placeholder="张三" />
                </label>
                <label>
                  联系电话
                  <input v-model.trim="marketDemandForm.contactPhone" placeholder="13800000000" />
                </label>
                <label class="editor-grid-wide">
                  备注
                  <textarea v-model.trim="marketDemandForm.remark" rows="3" placeholder="补充货物情况、报关要求或时效要求"></textarea>
                </label>
              </div>
              <p v-if="marketDemandEditor.error" class="form-error">{{ marketDemandEditor.error }}</p>
              <button class="primary-button full" type="submit" :disabled="marketSavingDemand">
                {{ marketSavingDemand ? "正在提交" : "提交需求" }}
              </button>
            </form>

            <div class="market-card-grid">
              <button
                v-for="item in marketCardRecords"
                :key="item.id"
                class="market-grid-card"
                :class="{ active: marketTab === 'orders' ? selectedMarketOrderId === item.id : selectedMarketDemandId === item.id }"
                type="button"
                @click="selectMarketRecord(item)"
              >
                <template v-if="marketTab === 'orders'">
                  <div class="market-card-topline">
                    <span class="market-card-kicker">Order</span>
                    <span class="pill">{{ formatMarketOrderStatus(item.orderStatus) }}</span>
                  </div>
                  <strong>{{ item.orderNo }}</strong>
                  <p>需求 #{{ item.demandId }}</p>
                  <div class="market-card-meta">
                    <span>成交报价 #{{ item.acceptedQuoteId }}</span>
                    <span>点击查看履约详情</span>
                  </div>
                </template>
                <template v-else>
                  <div class="market-card-topline">
                    <span class="market-card-kicker">{{ marketTab === "browse" ? "Market Hall" : "My Post" }}</span>
                    <span class="pill">{{ formatMarketDemandStatus(item.demandStatus) }}</span>
                  </div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.goodsName }}</p>
                  <div class="market-card-route">{{ item.departurePort }} → {{ item.destinationPort }}</div>
                  <div class="market-card-meta">
                    <span>{{ item.currencyCode || "CNY" }} {{ item.budgetAmount || "-" }}</span>
                    <span>{{ item.expectedShippingDate || "待确认船期" }}</span>
                  </div>
                </template>
              </button>
            </div>

            <div v-if="!marketCardRecords.length" class="empty-state">
              {{ marketTab === "browse" ? "暂无可浏览的货运需求。" : marketTab === "mine" ? "你还没有发布货运需求。" : "你还没有接单记录。" }}
            </div>

            <div class="pagination-bar" v-if="marketTab === 'browse' || marketTab === 'mine'">
              <span>
                {{ marketTab === "browse"
                  ? `共 ${marketPage.total} 条，第 ${marketPage.current} / ${marketTotalPages} 页`
                  : `共 ${myMarketPage.total} 条，第 ${myMarketPage.current} / ${myMarketTotalPages} 页` }}
              </span>
              <div>
                <button class="ghost-button" type="button" @click="changeMarketPage(-1)" :disabled="marketTab === 'browse' ? marketPage.current <= 1 : myMarketPage.current <= 1">上一页</button>
                <button class="ghost-button" type="button" @click="changeMarketPage(1)" :disabled="marketTab === 'browse' ? marketPage.current >= marketTotalPages : myMarketPage.current >= myMarketTotalPages">下一页</button>
              </div>
            </div>

            <div class="pagination-bar" v-else>
              <span>共 {{ orderPage.total }} 条，第 {{ orderPage.current }} / {{ orderTotalPages }} 页</span>
              <div>
                <button class="ghost-button" type="button" @click="changeOrderPage(-1)" :disabled="orderPage.current <= 1">上一页</button>
                <button class="ghost-button" type="button" @click="changeOrderPage(1)" :disabled="orderPage.current >= orderTotalPages">下一页</button>
              </div>
            </div>
          </section>
        </div>
      </section>

      <section v-else class="module-scene news-scene">
        <header class="workspace-header">
          <div>
            <p class="eyebrow">{{ currentMeta.eyebrow }}</p>
            <h1>{{ currentMeta.title }}</h1>
            <p>{{ currentMeta.description }}</p>
          </div>
        </header>
        <section class="news-placeholder-grid">
          <article v-for="item in newsHighlights" :key="item.title" class="panel-card news-placeholder-card">
            <span class="news-tag">{{ item.tag }}</span>
            <h2>{{ item.title }}</h2>
            <p>{{ item.desc }}</p>
          </article>
        </section>
      </section>

      <section
        v-if="marketDetailDialog.open && (marketDetailDialog.mode === 'order' ? selectedMarketOrder : selectedMarketDemandDetail)"
        class="bill-detail-dialog-backdrop"
        @click.self="closeMarketDetailDialog"
      >
        <article class="bill-detail-dialog market-detail-dialog">
          <header class="bill-detail-dialog-head">
            <div>
              <p class="eyebrow">{{ marketDialogTitle }}</p>
              <h2>{{ marketDetailDialog.mode === "order" ? selectedMarketOrder?.orderNo : selectedMarketDemandDetail?.title }}</h2>
              <span class="pill">
                {{ marketDetailDialog.mode === "order"
                  ? formatMarketOrderStatus(selectedMarketOrder?.orderStatus)
                  : formatMarketDemandStatus(selectedMarketDemandDetail?.demandStatus) }}
              </span>
            </div>
            <button class="ghost-button" type="button" @click="closeMarketDetailDialog">关闭</button>
          </header>

          <template v-if="marketDetailDialog.mode === 'order' && selectedMarketOrder">
            <div class="detail-grid market-detail-grid">
              <div class="detail-field"><span>需求 ID</span><strong>{{ selectedMarketOrder.demandId }}</strong></div>
              <div class="detail-field"><span>成交报价</span><strong>#{{ selectedMarketOrder.acceptedQuoteId }}</strong></div>
              <div class="detail-field"><span>订单状态</span><strong>{{ formatMarketOrderStatus(selectedMarketOrder.orderStatus) }}</strong></div>
            </div>
            <footer class="bill-detail-dialog-actions">
              <button class="primary-button" type="button" :disabled="selectedMarketOrder.orderStatus !== 'CREATED' || marketProcessingOrder" @click="startSelectedOrder(selectedMarketOrder.id)">
                {{ marketProcessingOrder && selectedMarketOrder.orderStatus === "CREATED" ? "正在开工" : "开始履约" }}
              </button>
              <button class="ghost-button" type="button" @click="closeMarketDetailDialog">关闭</button>
            </footer>
          </template>

          <template v-else-if="selectedMarketDemandDetail">
            <div class="market-detail-stack">
              <div class="market-detail-head">
                <div>
                  <p>{{ selectedMarketDemandDetail.goodsName }} · {{ selectedMarketDemandDetail.departurePort }} → {{ selectedMarketDemandDetail.destinationPort }}</p>
                </div>
                <div class="market-status-stack">
                  <span class="pill muted">{{ formatMarketAuditStatus(selectedMarketDemandDetail.auditStatus) }}</span>
                </div>
              </div>

              <div class="detail-grid market-detail-grid">
                <div class="detail-field"><span>预算</span><strong>{{ selectedMarketDemandDetail.currencyCode || "CNY" }} {{ selectedMarketDemandDetail.budgetAmount || "-" }}</strong></div>
                <div class="detail-field"><span>数量</span><strong>{{ selectedMarketDemandDetail.quantity || "-" }} {{ selectedMarketDemandDetail.quantityUnit || "" }}</strong></div>
                <div class="detail-field"><span>期望船期</span><strong>{{ selectedMarketDemandDetail.expectedShippingDate || "-" }}</strong></div>
                <div class="detail-field"><span>联系人</span><strong>{{ selectedMarketDemandDetail.contactName || "-" }}</strong></div>
                <div class="detail-field"><span>联系电话</span><strong>{{ selectedMarketDemandDetail.contactPhone || "-" }}</strong></div>
                <div class="detail-field detail-field-wide"><span>备注</span><strong>{{ selectedMarketDemandDetail.remark || "暂无备注" }}</strong></div>
              </div>

              <div v-if="marketTab === 'mine'" class="market-action-row">
                <button class="danger-button" type="button" :disabled="!canCancelSelectedDemand" @click="cancelSelectedDemand">取消需求</button>
                <button class="primary-button" type="button" :disabled="!canCompleteSelectedDemandOrder || marketProcessingOrder" @click="completeSelectedDemandOrder">
                  {{ marketProcessingOrder && canCompleteSelectedDemandOrder ? "正在完结" : "确认完结" }}
                </button>
              </div>

              <section v-if="marketTab === 'browse'" class="market-quote-form">
                <div class="panel-title compact">
                  <h3>提交报价</h3>
                  <p>作为代理方提交报价，后续可在“我的接单”中查看状态。</p>
                </div>
                <div class="editor-grid">
                  <label>
                    报价金额
                    <input v-model.number="marketQuoteForm.priceAmount" type="number" min="0" placeholder="4800" />
                  </label>
                  <label>
                    币种
                    <input v-model.trim="marketQuoteForm.currencyCode" placeholder="CNY" />
                  </label>
                  <label>
                    预计天数
                    <input v-model.number="marketQuoteForm.estimatedDays" type="number" min="0" placeholder="12" />
                  </label>
                  <label class="editor-grid-wide">
                    服务说明
                    <textarea v-model.trim="marketQuoteForm.serviceNote" rows="3" placeholder="可提供拖车、报关与提箱服务"></textarea>
                  </label>
                </div>
                <button class="primary-button" type="button" :disabled="marketSubmittingQuote" @click="submitSelectedDemandQuote">
                  {{ marketSubmittingQuote ? "正在报价" : "提交报价" }}
                </button>
              </section>

              <section class="market-quote-board">
                <div class="panel-title compact">
                  <h3>报价列表</h3>
                  <p>{{ selectedMarketQuotes.length ? `当前共 ${selectedMarketQuotes.length} 条报价。` : "当前还没有报价记录。" }}</p>
                </div>
                <div class="market-quote-list">
                  <article v-for="quote in selectedMarketQuotes" :key="quote.id" class="market-quote-card">
                    <div class="market-list-head">
                      <strong>{{ quote.currencyCode || "CNY" }} {{ quote.priceAmount || "-" }}</strong>
                      <span class="pill">{{ formatMarketQuoteStatus(quote.quoteStatus) }}</span>
                    </div>
                    <p>{{ quote.serviceNote || "暂无服务说明" }}</p>
                    <small>预计 {{ quote.estimatedDays || "-" }} 天</small>
                    <div v-if="marketTab === 'mine'" class="market-action-row compact">
                      <button class="secondary-button" type="button" :disabled="quote.quoteStatus !== 'SUBMITTED'" @click="acceptSelectedQuote(quote.id)">接受报价</button>
                    </div>
                  </article>
                </div>
              </section>
            </div>
          </template>
        </article>
      </section>

      <section v-if="extractDialogOpen && selectedExtractResult" class="extract-dialog-backdrop">
        <article class="extract-dialog">
          <header class="extract-dialog-head">
            <div>
              <p class="eyebrow">Template Builder</p>
              <h2>确认生成模板</h2>
              <span>{{ selectedExtractResult.name }}</span>
            </div>
            <button class="mini-button" type="button" @click="closeExtractDialog">关闭</button>
          </header>
          <section v-if="extractSaveFeedback.type" class="save-result-panel" :class="extractSaveFeedback.type">
            <div class="save-result-mark">
              <span v-if="extractSaveFeedback.type === 'saving'" class="save-spinner"></span>
              <svg v-else-if="extractSaveFeedback.type === 'success'" viewBox="0 0 64 64" aria-hidden="true">
                <circle cx="32" cy="32" r="28"></circle>
                <path d="M19 33.5 28 42 46 23"></path>
              </svg>
              <svg v-else viewBox="0 0 64 64" aria-hidden="true">
                <circle cx="32" cy="32" r="28"></circle>
                <path d="M23 23 41 41"></path>
                <path d="M41 23 23 41"></path>
              </svg>
            </div>
            <p class="eyebrow">{{ extractSaveFeedback.type === "success" ? "Template Saved" : extractSaveFeedback.type === "error" ? "Save Failed" : "Saving" }}</p>
            <h3>{{ extractSaveFeedback.title }}</h3>
            <p>{{ extractSaveFeedback.message }}</p>
            <button class="secondary-button" type="button" @click="closeExtractDialog">立即关闭</button>
          </section>
          <div class="extract-workbench">
            <section class="file-preview-pane">
              <div class="source-preview-frame" :class="selectedExtractResult.previewType">
                <iframe
                  v-if="selectedExtractResult.previewType === 'pdf'"
                  :src="selectedExtractResult.previewUrl"
                  title="替换后模板预览"
                ></iframe>
                <img
                  v-else-if="selectedExtractResult.previewType === 'image'"
                  :src="selectedExtractResult.previewUrl"
                  alt="上传文件预览"
                />
                <object
                  v-else-if="selectedExtractResult.previewType === 'word'"
                  :data="selectedExtractResult.previewUrl"
                  :type="selectedExtractResult.previewMimeType"
                >
                  <div class="word-preview-fallback">
                    <strong>{{ selectedExtractResult.previewLabel }}</strong>
                    <p>浏览器通常不能完整渲染 DOC/DOCX 版式；可以下载预览文件核对占位符效果。</p>
                    <a class="download-template-link" :href="selectedExtractResult.previewUrl" :download="selectedExtractResult.previewFileName">
                      下载预览文件
                    </a>
                  </div>
                </object>
                <div v-else class="word-preview-fallback">
                  <strong>{{ selectedExtractResult.previewLabel }}</strong>
                  <p>当前文件类型暂不支持浏览器内预览，但字段对应关系仍可继续编辑和保存。</p>
                  <a class="download-template-link" :href="selectedExtractResult.previewUrl" :download="selectedExtractResult.previewFileName">
                    下载预览文件
                  </a>
                </div>
              </div>
              <div class="preview-footer-actions">
                <a class="download-template-link neutral" :href="selectedExtractResult.previewUrl" :download="selectedExtractResult.previewFileName">
                  下载预览文件
                </a>
                <a
                  v-if="selectedExtractResult.blankTemplateDownloadUrl"
                  class="download-template-link"
                  :href="buildUserApiUrl(selectedExtractResult.blankTemplateDownloadUrl.replace(/^\/user/, ''))"
                  download
                >
                  下载 DOCX 模板
                </a>
              </div>
              <div class="template-preview-foot">
                保存模板时只保存右侧字段结构、占位符和排序，不保存样本业务数据。
              </div>
            </section>

            <section class="mapping-editor-pane">
              <div class="panel-title compact">
                <h2>数据与占位符对应</h2>
                <p>这些数据只用于本次校对占位符位置，确认保存模板时不会作为提单业务数据入库。</p>
              </div>

              <div class="mapping-editor-list">
                <article v-for="(mapping, index) in selectedExtractResult.mappings" :key="mapping.id" class="mapping-editor-row">
                  <div class="mapping-row-head">
                    <strong>#{{ index + 1 }} {{ mapping.placeholderKey || "未命名字段" }}</strong>
                    <div>
                      <button class="mini-button" type="button" :disabled="index === 0" @click="moveMapping(index, -1)">上移</button>
                      <button class="mini-button" type="button" :disabled="index === selectedExtractResult.mappings.length - 1" @click="moveMapping(index, 1)">下移</button>
                    </div>
                  </div>
                  <label>
                    模板占位符
                    <input v-model.trim="mapping.placeholderKey" placeholder="placeholder_key" />
                  </label>
                  <label>
                    被剔除的数据
                    <textarea v-model="mapping.originalText" rows="3" placeholder="Dify 提取出的原始内容，仅用于核对"></textarea>
                  </label>
                  <label>
                    字段描述
                    <input v-model.trim="mapping.description" placeholder="字段说明" />
                  </label>
                </article>
              </div>

              <details v-if="selectedExtractResult.rawText" class="raw-dify-preview">
                <summary>查看原始 Dify JSON/Text</summary>
                <pre>{{ selectedExtractResult.rawText }}</pre>
              </details>

              <div class="extract-save-actions">
                <button class="primary-button" type="button" :disabled="savingTemplate" @click="saveTemplateDefinition">
                  {{ savingTemplate ? "正在保存模板" : "确认保存模板" }}
                </button>
                <a
                  v-if="selectedExtractResult.blankTemplateDownloadUrl"
                  class="download-template-link"
                  :href="buildUserApiUrl(selectedExtractResult.blankTemplateDownloadUrl.replace(/^\/user/, ''))"
                  download
                >
                  下载 DOCX 模板
                </a>
              </div>
            </section>
          </div>
        </article>
      </section>
      <section v-if="exportDialogOpen && selectedExportJob" class="extract-dialog-backdrop">
        <article class="extract-dialog">
          <header class="extract-dialog-head">
            <div>
              <p class="eyebrow">Template Export</p>
              <h2>确认导出文件</h2>
              <span>{{ selectedExportJob.file }}</span>
            </div>
            <button class="mini-button" type="button" @click="closeExportDialog">关闭</button>
          </header>

          <section v-if="exportSaveFeedback.type" class="save-result-panel" :class="exportSaveFeedback.type">
            <div class="save-result-mark">
              <svg v-if="exportSaveFeedback.type === 'success'" viewBox="0 0 64 64" aria-hidden="true">
                <circle cx="32" cy="32" r="28"></circle>
                <path d="M19 33.5 28 42 46 23"></path>
              </svg>
              <svg v-else viewBox="0 0 64 64" aria-hidden="true">
                <circle cx="32" cy="32" r="28"></circle>
                <path d="M23 23 41 41"></path>
                <path d="M41 23 23 41"></path>
              </svg>
            </div>
            <p class="eyebrow">{{ exportSaveFeedback.type === "success" ? "Saved" : "Notice" }}</p>
            <h3>{{ exportSaveFeedback.title }}</h3>
            <p>{{ exportSaveFeedback.message }}</p>
            <button class="secondary-button" type="button" @click="resetExportSaveFeedback">继续查看</button>
          </section>

          <div class="extract-workbench">
            <section class="file-preview-pane">
              <div class="source-preview-frame" :class="selectedExportJob.previewType">
                <iframe
                  v-if="selectedExportJob.previewType === 'pdf'"
                  :src="selectedExportJob.downloadUrl"
                  title="导出文件预览"
                ></iframe>
                <object
                  v-else-if="selectedExportJob.previewType === 'word'"
                  :data="selectedExportJob.downloadUrl"
                  :type="selectedExportJob.previewMimeType"
                >
                  <div class="word-preview-fallback">
                    <strong>DOCX 导出文件</strong>
                    <p>浏览器可能无法完整预览 Word 版式，可保存到本地后打开核对。</p>
                    <a class="download-template-link" :href="selectedExportJob.downloadUrl" :download="selectedExportJob.file">
                      保存到本地
                    </a>
                  </div>
                </object>
                <div v-else class="word-preview-fallback">
                  <strong>{{ selectedExportJob.outputFormat || "导出文件" }}</strong>
                  <p>当前格式无法直接内嵌预览，可保存到本地查看。</p>
                  <a class="download-template-link" :href="selectedExportJob.downloadUrl" :download="selectedExportJob.file">
                    保存到本地
                  </a>
                </div>
              </div>
            </section>

            <section class="mapping-editor-pane">
              <div class="panel-title compact">
                <h2>字段对应</h2>
              </div>

              <div v-if="selectedExportJob.missing?.length" class="form-error">
                缺失占位符：{{ selectedExportJob.missing.join("、") }}
              </div>

              <div class="mapping-editor-list export-field-list">
                <article v-for="(field, index) in selectedExportJob.fields" :key="field.key" class="mapping-editor-row">
                  <div class="mapping-row-head">
                    <strong>#{{ index + 1 }} {{ field.key }}</strong>
                    <span class="field-status-pill">已对应</span>
                  </div>
                  <label>
                    提取字段
                    <input :value="field.key" readonly />
                  </label>
                  <label>
                    填充值
                    <textarea :value="field.value" rows="3" readonly></textarea>
                  </label>
                </article>
              </div>

              <details v-if="selectedExportJob.rawText" class="raw-dify-preview">
                <summary>查看原始 Dify JSON/Text</summary>
                <pre>{{ selectedExportJob.rawText }}</pre>
              </details>

              <div class="extract-save-actions export-dialog-actions">
                <button class="primary-button" type="button" @click="saveExportToLocal(selectedExportJob)">
                  保存到本地
                </button>
                <button class="secondary-button" type="button" @click="confirmExportSavedToMinio(selectedExportJob)">
                  保存到 MinIO
                </button>
              </div>
            </section>
          </div>
        </article>
      </section>
    </main>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import {
  acceptMarketQuote,
  buildUserApiUrl,
  cancelMarketDemand,
  createBill,
  createMarketDemand,
  deleteBill,
  deleteTemplateDefinition,
  extractTemplateFile,
  exportTemplateFile,
  fetchBillPage,
  fetchMarketDemandDetail,
  fetchMarketDemandPage,
  fetchMarketQuotes,
  fetchMyAcceptedOrders,
  fetchMyMarketDemands,
  fetchExportableTemplates,
  fetchTemplateManagePage,
  getTemplateSaveTask,
  loginClient,
  registerClient,
  saveExtractedBillData,
  saveGeneratedTemplate,
  startMarketOrder,
  setAccessToken,
  submitMarketQuote,
  completeMarketOrder,
  updateTemplateStatus,
  updateBill,
} from "./api/clientApi";
import { normalizeDifyWorkflowMappings } from "./utils/difyWorkflow";

const authMode = ref("login");
const authDialogOpen = ref(false);

const loginForm = reactive({
  identity: "tenant_user",
  password: "",
});

const registerForm = reactive({
  username: "",
  password: "",
  confirmPassword: "",
  nickname: "",
  mobile: "",
  email: "",
  companyId: 2,
});

const session = reactive({
  loggedIn: false,
  username: "",
  nickname: "",
  companyCode: "",
});

const sidebarCollapsed = ref(false);
const currentView = ref("overview");
const marketTab = ref("browse");
const marketDetailDialog = reactive({
  open: false,
  mode: "demand",
});
const selectedBillIds = ref([]);
const extractFile = ref(null);
const exportFile = ref(null);
const extractingTemplate = ref(false);
const exportingTemplate = ref(false);
const savingTemplate = ref(false);
const extractedFileKeys = ref(new Set());
const selectedExtractId = ref("");
const extractDialogOpen = ref(false);
const saveCloseTimer = ref(null);
const extractSaveFeedback = reactive({
  type: "",
  title: "",
  message: "",
});
const toasts = ref([]);
const loginError = ref("");
const registerError = ref("");
const marketListLoading = ref(false);
const marketDetailLoading = ref(false);
const marketSavingDemand = ref(false);
const marketSubmittingQuote = ref(false);
const marketProcessingOrder = ref(false);

const exportForm = reactive({
  templateId: "tpl-001",
  outputFormat: "DOCX",
});

const billQuery = reactive({
  keyword: "",
  status: "",
});

const templateQuery = reactive({
  keyword: "",
  status: "",
});

const marketQuery = reactive({
  keyword: "",
  status: "",
});

const billPage = reactive({
  current: 1,
  size: 5,
  total: 0,
});

const templatePage = reactive({
  current: 1,
  size: 8,
  total: 0,
});

const marketPage = reactive({
  current: 1,
  size: 8,
  total: 0,
});

const myMarketPage = reactive({
  current: 1,
  size: 8,
  total: 0,
});

const orderPage = reactive({
  current: 1,
  size: 8,
  total: 0,
});

const billEditor = reactive({
  open: false,
  mode: "create",
  editingId: null,
  error: "",
});
const billDetailDialog = reactive({
  open: false,
  billId: "",
});
const marketDemandEditor = reactive({
  open: false,
  error: "",
});

const billForm = reactive(createEmptyBillForm());
const marketDemandForm = reactive(createEmptyMarketDemandForm());
const marketQuoteForm = reactive(createEmptyMarketQuoteForm());

const prototypeBills = [
  {
    id: "BL-001",
    blNo: "MSKU-938201",
    vessel: "COSCO TAURUS / 046E",
    pol: "Shanghai",
    pod: "Los Angeles",
    status: "已确认",
    goodsName: "Bluetooth Speaker",
    quantity: "1,248 CTNS",
    detailFields: [
      { label: "起运港", value: "Shanghai" },
      { label: "目的港", value: "Los Angeles" },
      { label: "收货地", value: "Suzhou ICD" },
      { label: "交付地", value: "LA Warehouse" },
      { label: "毛重", value: "18,420 KGS" },
      { label: "体积", value: "67.8 CBM" },
      { label: "箱号", value: "MSKU7782910" },
      { label: "封号", value: "CN884201" },
    ],
  },
  {
    id: "BL-002",
    blNo: "ONEY-771904",
    vessel: "EVER GLOBE / 122W",
    pol: "Ningbo",
    pod: "Hamburg",
    status: "待复核",
    goodsName: "LED Desk Lamp",
    quantity: "860 CTNS",
    detailFields: [
      { label: "起运港", value: "Ningbo" },
      { label: "目的港", value: "Hamburg" },
      { label: "收货地", value: "Yiwu" },
      { label: "交付地", value: "Hamburg Depot" },
      { label: "毛重", value: "9,760 KGS" },
      { label: "体积", value: "42.3 CBM" },
      { label: "箱号", value: "ONEY3820192" },
      { label: "封号", value: "NB221908" },
    ],
  },
  {
    id: "BL-003",
    blNo: "HLCU-204488",
    vessel: "MAERSK ELBA / 18N",
    pol: "Yantian",
    pod: "Rotterdam",
    status: "草稿",
    goodsName: "Kitchen Storage Set",
    quantity: "2,036 PCS",
    detailFields: [
      { label: "起运港", value: "Yantian" },
      { label: "目的港", value: "Rotterdam" },
      { label: "收货地", value: "Shenzhen" },
      { label: "交付地", value: "Rotterdam DC" },
      { label: "毛重", value: "12,210 KGS" },
      { label: "体积", value: "58.1 CBM" },
      { label: "箱号", value: "HLCU5502381" },
      { label: "封号", value: "YT662310" },
    ],
  },
];

const prototypeTemplates = [
  { id: "tpl-001", name: "标准海运提单模板" },
  { id: "tpl-002", name: "北美线提单模板" },
  { id: "tpl-003", name: "欧线订舱导出模板" },
];

const iconMap = Object.freeze({
  dashboard:
    '<svg viewBox="0 0 24 24" aria-hidden="true"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"><rect width="7" height="9" x="3" y="3" rx="1"/><rect width="7" height="5" x="14" y="3" rx="1"/><rect width="7" height="9" x="14" y="12" rx="1"/><rect width="7" height="5" x="3" y="16" rx="1"/></g></svg>',
  files:
    '<svg viewBox="0 0 24 24" aria-hidden="true"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"><path d="M15 2h-4a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V8"/><path d="M16.706 2.706A2.4 2.4 0 0 0 15 2v5a1 1 0 0 0 1 1h5a2.4 2.4 0 0 0-.706-1.706zM5 7a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h8a2 2 0 0 0 1.732-1"/></g></svg>',
  searchFile:
    '<svg viewBox="0 0 24 24" aria-hidden="true"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"><path d="M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z"/><path d="M14 2v5a1 1 0 0 0 1 1h5"/><circle cx="11.5" cy="14.5" r="2.5"/><path d="M13.3 16.3L15 18"/></g></svg>',
  library:
    '<svg viewBox="0 0 24 24" aria-hidden="true"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"><path d="M5 7a2 2 0 0 0-2 2v11"/><path d="M5.803 18H5a2 2 0 0 0 0 4h9.5a.5.5 0 0 0 .5-.5V21m-6-6V4a2 2 0 0 1 2-2h9.5a.5.5 0 0 1 .5.5v14a.5.5 0 0 1-.5.5H11a2 2 0 0 1 0-4h10"/></g></svg>',
  output:
    '<svg viewBox="0 0 24 24" aria-hidden="true"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"><path d="M4.226 20.925A2 2 0 0 0 6 22h12a2 2 0 0 0 2-2V8a2.4 2.4 0 0 0-.706-1.706l-3.588-3.588A2.4 2.4 0 0 0 14 2H6a2 2 0 0 0-2 2v3.127"/><path d="M14 2v5a1 1 0 0 0 1 1h5M5 11l-3 3m3 3l-3-3h10"/></g></svg>',
  market:
    '<svg viewBox="0 0 24 24" aria-hidden="true"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"><path d="M3 7.5h18"/><path d="M5 7.5V18a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V7.5"/><path d="M8 11h8"/><path d="M9 15h3"/><path d="M6 4h12l1 3.5H5z"/></g></svg>',
  news:
    '<svg viewBox="0 0 24 24" aria-hidden="true"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"><path d="M4 5.5A1.5 1.5 0 0 1 5.5 4h13A1.5 1.5 0 0 1 20 5.5v11A1.5 1.5 0 0 1 18.5 18H7"/><path d="M7 18a3 3 0 0 1-3-3V7"/><path d="M8.5 8.5h7"/><path d="M8.5 12h7"/><path d="M8.5 15.5H13"/></g></svg>',
});

const primaryNavItems = [
  { key: "overview", label: "总览", icon: "dashboard" },
  { key: "data", label: "数据管理", icon: "files" },
  { key: "market", label: "商城", icon: "market" },
  { key: "news", label: "新闻", icon: "news" },
];

const dataNavItems = [
  { key: "bills", label: "提单管理", icon: "files" },
  { key: "extract", label: "模板提取", icon: "searchFile" },
  { key: "templates", label: "模板中心", icon: "library" },
  { key: "export", label: "导出任务", icon: "output" },
];

const marketNavItems = [
  { key: "lobby", label: "市场大厅", icon: "market" },
  { key: "my-posts", label: "我的发布", icon: "files" },
  { key: "my-orders", label: "我的接单", icon: "output" },
];

const landingCapabilities = [
  { icon: "files", title: "提单管理", desc: "结构化归档、查询与编辑提单数据。" },
  { icon: "searchFile", title: "模板提取", desc: "从样本文件中沉淀字段结构与模板。" },
  { icon: "output", title: "按模板导出", desc: "把抽取结果快速生成目标文件。" },
  { icon: "market", title: "货运商城", desc: "发布需求、接收报价并跟进履约。" },
];

const landingProofPoints = ["BL Workspace", "Template Extraction", "Template Export", "Freight Collaboration"];

const landingWorkflow = [
  { index: "01", title: "整理提单", desc: "把文件转换成可编辑、可检索的结构化数据。" },
  { index: "02", title: "沉淀模板", desc: "从样本中提取字段规则，沉淀可复用模板。" },
  { index: "03", title: "完成协作", desc: "导出文件、发布需求、推进报价与履约流程。" },
];

const metaMap = {
  overview: {
    eyebrow: "Overview",
    title: "用户总览",
    description: "",
  },
  market: {
    eyebrow: "Freight Marketplace",
    title: "货运商城",
    description: "浏览货运需求、发布自己的货盘并管理接单履约。",
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
  templates: {
    eyebrow: "Template Library",
    title: "模板管理",
    description: "模板文件、存储位置与启用状态。",
  },
  export: {
    eyebrow: "Template Export",
    title: "按模版导出",
    description: "上传目标文件，按照选定模板生成导出任务。",
  },
  news: {
    eyebrow: "News Center",
    title: "新闻中心",
    description: "预留行业动态、平台公告和内容服务入口。",
  },
};

const savedBills = ref([...prototypeBills]);

const templateOptions = ref([]);
const marketDemands = ref([]);
const myMarketDemands = ref([]);
const myAcceptedOrders = ref([]);
const selectedMarketDemandId = ref("");
const selectedMarketOrderId = ref("");
const selectedMarketDemandDetail = ref(null);
const selectedMarketQuotes = ref([]);

const extractedTemplates = ref([]);
const managedTemplates = ref([]);
const exportJobs = ref([]);
const templateStatusUpdating = ref(new Set());
const exportDialogOpen = ref(false);
const selectedExportJobId = ref("");
const exportSaveFeedback = reactive({
  type: "",
  title: "",
  message: "",
});

const currentMeta = computed(() => metaMap[currentView.value]);
const avatarText = computed(() => (session.nickname || session.username || "U").slice(0, 2).toUpperCase());
const clientQuickStats = computed(() => [
  { label: "提单记录", value: savedBills.value.length },
  { label: "货运需求", value: myMarketDemands.value.length },
  { label: "模板库", value: managedTemplates.value.length },
  { label: "导出队列", value: exportJobs.value.length },
]);
const marketQuickStats = computed(() => [
  { label: "市场需求", value: marketDemands.value.length },
  { label: "我的发布", value: myMarketDemands.value.length },
  { label: "我的接单", value: myAcceptedOrders.value.length },
]);
const overviewShortcuts = computed(() => [
  { icon: "files", title: "新增提单", desc: "录入或补充一条提单业务数据。", action: "create-bill" },
  { icon: "searchFile", title: "模板提取", desc: "上传样本文件并沉淀模板结构。", action: "extract" },
  { icon: "output", title: "按模板导出", desc: "快速生成标准文档和导出任务。", action: "export" },
  { icon: "market", title: "发布需求", desc: "在商城中发布新的货运协作需求。", action: "market-demand" },
]);
const overviewTodos = computed(() => [
  { title: `${exportJobs.value.length} 个导出任务待查看`, desc: "检查导出结果并完成本地或 MinIO 保存。" },
  { title: `${myMarketDemands.value.length} 条货运需求正在跟进`, desc: "关注报价进度、审核状态与履约动作。" },
  { title: `${savedBills.value.length} 条提单数据已进入工作台`, desc: "继续补全字段、编辑状态或执行批量整理。" },
]);
const recentWorkspaceActivity = computed(() => [
  { title: savedBills.value[0]?.blNo || "暂无提单更新", desc: savedBills.value[0] ? `最近提单：${savedBills.value[0].goodsName}` : "等待新的提单业务数据进入工作台。" },
  { title: managedTemplates.value[0]?.templateName || "暂无模板更新", desc: managedTemplates.value[0] ? `模板编码：${managedTemplates.value[0].templateCode}` : "提取模板后会在这里显示最近更新。" },
  { title: exportJobs.value[0]?.file || "暂无导出任务", desc: exportJobs.value[0] ? `导出状态：${exportJobs.value[0].status}` : "导出工作完成后会在这里汇总最近动作。" },
]);
const marketActivityFeed = computed(() => [
  { title: myAcceptedOrders.value[0]?.orderNo || "暂无接单更新", desc: myAcceptedOrders.value[0] ? `当前状态：${formatMarketOrderStatus(myAcceptedOrders.value[0].orderStatus)}` : "接单与履约进度会在这里显示。" },
  { title: marketDemands.value[0]?.title || "暂无市场动态", desc: marketDemands.value[0] ? `${marketDemands.value[0].departurePort} → ${marketDemands.value[0].destinationPort}` : "市场大厅的新需求会在这里展示。" },
]);
const activeBillDetail = computed(() => savedBills.value.find((bill) => bill.id === billDetailDialog.billId));
const billTotalPages = computed(() => Math.max(1, Math.ceil(billPage.total / billPage.size)));
const templateTotalPages = computed(() => Math.max(1, Math.ceil(templatePage.total / templatePage.size)));
const marketTotalPages = computed(() => Math.max(1, Math.ceil(marketPage.total / marketPage.size)));
const myMarketTotalPages = computed(() => Math.max(1, Math.ceil(myMarketPage.total / myMarketPage.size)));
const orderTotalPages = computed(() => Math.max(1, Math.ceil(orderPage.total / orderPage.size)));
const currentExtractFileKey = computed(() => (extractFile.value ? buildFileKey(extractFile.value) : ""));
const isCurrentExtractFileDone = computed(() => Boolean(currentExtractFileKey.value && extractedFileKeys.value.has(currentExtractFileKey.value)));
const selectedExtractResult = computed(() => extractedTemplates.value.find((item) => item.id === selectedExtractId.value));
const selectedExportJob = computed(() => exportJobs.value.find((item) => item.id === selectedExportJobId.value));
const primaryWorkspace = computed(() => {
  if (["bills", "extract", "templates", "export"].includes(currentView.value)) {
    return "data";
  }
  if (currentView.value === "market") {
    return "market";
  }
  if (currentView.value === "news") {
    return "news";
  }
  return "overview";
});
const activeMarketSection = computed(() => {
  const map = {
    browse: "lobby",
    mine: "my-posts",
    orders: "my-orders",
  };
  return map[marketTab.value] || "lobby";
});
const marketSummaryCards = computed(() => [
  { label: "大厅需求", value: marketPage.total || marketDemands.value.length, hint: "可浏览可报价" },
  { label: "我的发布", value: myMarketPage.total || myMarketDemands.value.length, hint: "跟进报价与履约" },
  { label: "我的接单", value: orderPage.total || myAcceptedOrders.value.length, hint: "履约中的合作单" },
]);
const marketCardRecords = computed(() => marketTab.value === "orders" ? myAcceptedOrders.value : activeMarketDemandRecords.value);
const marketDialogTitle = computed(() => marketDetailDialog.mode === "order" ? "接单详情" : "需求详情");
const newsHighlights = [
  { title: "行业快讯位", desc: "后续可接入航运市场指数、热门航线动态与订舱提醒。", tag: "内容预留" },
  { title: "平台公告位", desc: "用于发布模板更新、导出能力升级和商城规则调整。", tag: "系统通知" },
  { title: "市场情报位", desc: "汇总平台高频货类、报价趋势和近期成交热度。", tag: "运营分析" },
];
const extractButtonText = computed(() => {
  if (extractingTemplate.value) {
    return "正在提取，请稍候";
  }
  if (isCurrentExtractFileDone.value) {
    return "该文件已提取，不会重复请求 Dify";
  }
  return "开始提取模板";
});
const allCurrentBillsSelected = computed(
  () => savedBills.value.length > 0 && savedBills.value.every((bill) => selectedBillIds.value.includes(bill.id))
);
const activeMarketDemandRecords = computed(() => marketTab.value === "browse" ? marketDemands.value : myMarketDemands.value);
const selectedMarketOrder = computed(() => myAcceptedOrders.value.find((item) => item.id === selectedMarketOrderId.value));
const canCancelSelectedDemand = computed(() => {
  const detail = selectedMarketDemandDetail.value;
  return marketTab.value === "mine"
    && Boolean(detail)
    && ["PENDING_REVIEW", "PUBLISHED", "QUOTING", "REJECTED"].includes(detail.demandStatus);
});
const canCompleteSelectedDemandOrder = computed(() => {
  const detail = selectedMarketDemandDetail.value;
  return marketTab.value === "mine"
    && Boolean(detail?.acceptedOrderId)
    && detail.demandStatus === "FULFILLING";
});

function getIconSvg(name) {
  return iconMap[name] || "";
}

function openAuthDialog(mode = "login") {
  switchAuthMode(mode);
  authDialogOpen.value = true;
}

function closeAuthDialog() {
  authDialogOpen.value = false;
}

function handleOverviewShortcut(action) {
  if (action === "create-bill") {
    switchDataView("bills");
    startCreateBill();
    return;
  }
  if (action === "extract") {
    switchDataView("extract");
    return;
  }
  if (action === "export") {
    switchDataView("export");
    return;
  }
  if (action === "market-demand") {
    switchPrimaryView("market");
    openMarketDemandEditor();
  }
}

async function login() {
  loginError.value = validateLoginForm();
  if (loginError.value) {
    return;
  }

  try {
    const loginPayload = buildLoginPayload();
    const result = await loginClient(loginPayload);
    setAccessToken(result.accessToken);
    session.loggedIn = true;
    authDialogOpen.value = false;
    session.username = result.username || loginPayload.username;
    session.nickname = result.username === "tenant_user" ? "测试用户" : result.username || loginPayload.username;
    session.companyCode = (loginPayload.companyCode || "TEST").toUpperCase();
    notify("登录成功", "已通过 auth-service 校验，正在同步用户端数据。", "backend");
    await Promise.allSettled([loadBills(), loadTemplates(), loadManagedTemplates(), preloadMarketWorkspace()]);
  } catch (error) {
    loginError.value = error.message || "请检查 auth-service、账号密码和数据库连接。";
  }
}

function validateLoginForm() {
  if (!loginForm.identity) {
    return "请输入用户名或航运公司四字母编号。";
  }
  if (!loginForm.password) {
    return "请输入密码。";
  }
  return "";
}

function buildLoginPayload() {
  const identity = loginForm.identity.trim();
  const isCompanyCode = /^[a-zA-Z]{4}$/.test(identity);
  return {
    identity,
    username: isCompanyCode ? identity.toUpperCase() : identity,
    password: loginForm.password,
    companyCode: isCompanyCode ? identity.toUpperCase() : "",
  };
}

function clearLoginError() {
  loginError.value = "";
}

async function register() {
  registerError.value = validateRegisterForm();
  if (registerError.value) {
    return;
  }

  try {
    await registerClient(registerForm);
    loginForm.identity = registerForm.username;
    loginForm.password = registerForm.password;
    resetRegisterForm();
    authMode.value = "login";
    authDialogOpen.value = true;
    notify("注册成功", "账号已创建，可直接点击进入工作台。", "backend");
  } catch (error) {
    registerError.value = error.message || "注册失败，请检查 auth-service。";
  }
}

function validateRegisterForm() {
  if (!registerForm.username) {
    return "请输入用户名。";
  }
  if (!/^[a-zA-Z0-9_]{3,32}$/.test(registerForm.username)) {
    return "用户名需为 3-32 位字母、数字或下划线。";
  }
  if (!registerForm.password || registerForm.password.length < 6) {
    return "密码至少需要 6 位。";
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    return "两次输入的密码不一致。";
  }
  return "";
}

function switchAuthMode(mode) {
  authMode.value = mode;
  loginError.value = "";
  registerError.value = "";
}

function clearRegisterError() {
  registerError.value = "";
}

function resetRegisterForm() {
  Object.assign(registerForm, {
    username: "",
    password: "",
    confirmPassword: "",
    nickname: "",
    mobile: "",
    email: "",
    companyId: 2,
  });
}

function logout() {
  setAccessToken("");
  session.loggedIn = false;
  authDialogOpen.value = false;
  currentView.value = "overview";
  marketTab.value = "browse";
  marketDetailDialog.open = false;
  marketDemands.value = [];
  myMarketDemands.value = [];
  myAcceptedOrders.value = [];
  selectedMarketDemandId.value = "";
  selectedMarketOrderId.value = "";
  selectedMarketDemandDetail.value = null;
  selectedMarketQuotes.value = [];
  notify("已退出", "客户端会话已结束。", "backend");
}

function switchView(view) {
  currentView.value = view;
  if (view !== "market") {
    marketDetailDialog.open = false;
  }
  if (view === "market") {
    loadMarketRecords();
  } else if (view === "templates") {
    loadManagedTemplates();
  }
}

function switchPrimaryView(view) {
  if (view === "overview") {
    switchView("overview");
    return;
  }
  if (view === "data") {
    switchDataView(dataNavItems.some((item) => item.key === currentView.value) ? currentView.value : "bills");
    return;
  }
  if (view === "market") {
    switchView("market");
    return;
  }
  switchView("news");
}

function switchDataView(view) {
  sidebarCollapsed.value = false;
  switchView(view);
}

function switchMarketSection(section) {
  const tabMap = {
    lobby: "browse",
    "my-posts": "mine",
    "my-orders": "orders",
  };
  sidebarCollapsed.value = false;
  switchView("market");
  switchMarketTab(tabMap[section] || "browse");
}

async function preloadMarketWorkspace() {
  await Promise.allSettled([
    loadMarketBrowsePage({ preserveSelection: false }),
    loadMyMarketDemandsPage({ preserveSelection: false }),
    loadMyAcceptedOrdersPage({ preserveSelection: false }),
  ]);
}

async function loadMarketRecords() {
  if (marketTab.value === "browse") {
    await loadMarketBrowsePage();
    return;
  }
  if (marketTab.value === "mine") {
    await loadMyMarketDemandsPage();
    return;
  }
  await loadMyAcceptedOrdersPage();
}

async function loadMarketBrowsePage(options = {}) {
  marketListLoading.value = true;
  try {
    const page = await fetchMarketDemandPage({
      pageNo: marketPage.current,
      pageSize: marketPage.size,
      keyword: marketQuery.keyword,
      status: marketQuery.status,
    });
    marketDemands.value = Array.isArray(page?.records) ? page.records : [];
    marketPage.current = Number(page?.current || marketPage.current);
    marketPage.size = Number(page?.size || marketPage.size);
    marketPage.total = Number(page?.total || 0);
    await syncSelectedMarketDemand(options.preserveSelection !== false ? selectedMarketDemandId.value : "");
  } catch (error) {
    marketDemands.value = [];
    marketPage.total = 0;
    clearSelectedMarketDemand();
    notify("货运商城加载失败", error.message || "请检查 market-service。", "error");
  } finally {
    marketListLoading.value = false;
  }
}

async function loadMyMarketDemandsPage(options = {}) {
  marketListLoading.value = true;
  try {
    const page = await fetchMyMarketDemands({
      pageNo: myMarketPage.current,
      pageSize: myMarketPage.size,
      keyword: marketQuery.keyword,
      status: marketQuery.status,
    });
    myMarketDemands.value = Array.isArray(page?.records) ? page.records : [];
    myMarketPage.current = Number(page?.current || myMarketPage.current);
    myMarketPage.size = Number(page?.size || myMarketPage.size);
    myMarketPage.total = Number(page?.total || 0);
    await syncSelectedMarketDemand(options.preserveSelection !== false ? selectedMarketDemandId.value : "");
  } catch (error) {
    myMarketDemands.value = [];
    myMarketPage.total = 0;
    clearSelectedMarketDemand();
    notify("我的发布加载失败", error.message || "请检查 market-service。", "error");
  } finally {
    marketListLoading.value = false;
  }
}

async function loadMyAcceptedOrdersPage(options = {}) {
  marketListLoading.value = true;
  try {
    const page = await fetchMyAcceptedOrders({
      pageNo: orderPage.current,
      pageSize: orderPage.size,
    });
    myAcceptedOrders.value = Array.isArray(page?.records) ? page.records : [];
    orderPage.current = Number(page?.current || orderPage.current);
    orderPage.size = Number(page?.size || orderPage.size);
    orderPage.total = Number(page?.total || 0);
    syncSelectedMarketOrder(options.preserveSelection !== false ? selectedMarketOrderId.value : "");
  } catch (error) {
    myAcceptedOrders.value = [];
    orderPage.total = 0;
    selectedMarketOrderId.value = "";
    notify("我的接单加载失败", error.message || "请检查 market-service。", "error");
  } finally {
    marketListLoading.value = false;
  }
}

async function syncSelectedMarketDemand(preferredId = "") {
  const records = activeMarketDemandRecords.value;
  if (!records.length) {
    clearSelectedMarketDemand();
    return;
  }
  const matched = records.find((item) => item.id === preferredId);
  const targetId = matched?.id || records[0].id;
  await loadMarketDemandDetailById(targetId);
}

function syncSelectedMarketOrder(preferredId = "") {
  if (!myAcceptedOrders.value.length) {
    selectedMarketOrderId.value = "";
    return;
  }
  const matched = myAcceptedOrders.value.find((item) => item.id === preferredId);
  selectedMarketOrderId.value = matched?.id || myAcceptedOrders.value[0].id;
}

function clearSelectedMarketDemand() {
  selectedMarketDemandId.value = "";
  selectedMarketDemandDetail.value = null;
  selectedMarketQuotes.value = [];
}

function closeMarketDetailDialog() {
  marketDetailDialog.open = false;
}

async function loadMarketDemandDetailById(demandId) {
  if (!demandId) {
    clearSelectedMarketDemand();
    return;
  }
  marketDetailLoading.value = true;
  try {
    const [detail, quotes] = await Promise.all([
      fetchMarketDemandDetail(demandId),
      fetchMarketQuotes(demandId),
    ]);
    selectedMarketDemandId.value = demandId;
    selectedMarketDemandDetail.value = detail;
    selectedMarketQuotes.value = Array.isArray(quotes) ? quotes : [];
  } catch (error) {
    clearSelectedMarketDemand();
    notify("需求详情加载失败", error.message || "请检查 market-service。", "error");
  } finally {
    marketDetailLoading.value = false;
  }
}

function switchMarketTab(tab) {
  if (marketTab.value === tab) {
    return;
  }
  marketTab.value = tab;
  if (tab === "orders") {
    selectedMarketDemandId.value = "";
    selectedMarketDemandDetail.value = null;
    selectedMarketQuotes.value = [];
  } else {
    selectedMarketOrderId.value = "";
  }
  loadMarketRecords();
}

function searchMarketRecords() {
  if (marketTab.value === "browse") {
    marketPage.current = 1;
  } else if (marketTab.value === "mine") {
    myMarketPage.current = 1;
  } else {
    orderPage.current = 1;
  }
  loadMarketRecords();
}

function resetMarketFilters() {
  marketQuery.keyword = "";
  marketQuery.status = "";
  marketPage.current = 1;
  myMarketPage.current = 1;
  orderPage.current = 1;
  loadMarketRecords();
}

function changeMarketPage(step) {
  const pageState = marketTab.value === "browse" ? marketPage : myMarketPage;
  const totalPages = marketTab.value === "browse" ? marketTotalPages.value : myMarketTotalPages.value;
  const nextPage = pageState.current + step;
  if (nextPage < 1 || nextPage > totalPages) {
    return;
  }
  pageState.current = nextPage;
  loadMarketRecords();
}

function changeOrderPage(step) {
  const nextPage = orderPage.current + step;
  if (nextPage < 1 || nextPage > orderTotalPages.value) {
    return;
  }
  orderPage.current = nextPage;
  loadMyAcceptedOrdersPage();
}

function openMarketDemandEditor() {
  marketDemandEditor.open = true;
  marketDemandEditor.error = "";
}

function closeMarketDemandEditor() {
  marketDemandEditor.open = false;
  marketDemandEditor.error = "";
  Object.assign(marketDemandForm, createEmptyMarketDemandForm());
}

async function submitMarketDemand() {
  marketDemandEditor.error = validateMarketDemandForm();
  if (marketDemandEditor.error) {
    return;
  }
  marketSavingDemand.value = true;
  try {
    await createMarketDemand({
      ...marketDemandForm,
      expectedShippingDate: marketDemandForm.expectedShippingDate || null,
      quantity: marketDemandForm.quantity === "" || marketDemandForm.quantity == null ? null : Number(marketDemandForm.quantity),
      budgetAmount: marketDemandForm.budgetAmount === "" || marketDemandForm.budgetAmount == null ? null : Number(marketDemandForm.budgetAmount),
    });
    notify("需求已发布", "货运需求已提交，等待管理端审核上架。", "backend");
    closeMarketDemandEditor();
    marketTab.value = "mine";
    myMarketPage.current = 1;
    await loadMyMarketDemandsPage({ preserveSelection: false });
  } catch (error) {
    marketDemandEditor.error = error.message || "需求发布失败。";
  } finally {
    marketSavingDemand.value = false;
  }
}

function validateMarketDemandForm() {
  if (!marketDemandForm.title) {
    return "请输入需求标题。";
  }
  if (!marketDemandForm.goodsName) {
    return "请输入商品名称。";
  }
  if (!marketDemandForm.departurePort || !marketDemandForm.destinationPort) {
    return "请补充起运港和目的港。";
  }
  return "";
}

async function selectMarketRecord(item) {
  if (!item?.id) {
    return;
  }
  if (marketTab.value === "orders") {
    selectedMarketOrderId.value = item.id;
    marketDetailDialog.mode = "order";
    marketDetailDialog.open = true;
    return;
  }
  await loadMarketDemandDetailById(item.id);
  marketDetailDialog.mode = "demand";
  marketDetailDialog.open = true;
}

async function submitSelectedDemandQuote() {
  const detail = selectedMarketDemandDetail.value;
  if (!detail?.id) {
    notify("请先选择需求", "在市场大厅中选择一条货运需求后再报价。", "error");
    return;
  }
  if (!marketQuoteForm.priceAmount && marketQuoteForm.priceAmount !== 0) {
    notify("请填写报价金额", "报价金额不能为空。", "error");
    return;
  }
  marketSubmittingQuote.value = true;
  try {
    await submitMarketQuote(detail.id, {
      priceAmount: Number(marketQuoteForm.priceAmount),
      currencyCode: marketQuoteForm.currencyCode || "CNY",
      estimatedDays: marketQuoteForm.estimatedDays === "" || marketQuoteForm.estimatedDays == null ? null : Number(marketQuoteForm.estimatedDays),
      serviceNote: marketQuoteForm.serviceNote,
    });
    notify("报价已提交", "你已向该货运需求提交报价。", "backend");
    Object.assign(marketQuoteForm, createEmptyMarketQuoteForm());
    await loadMarketDemandDetailById(detail.id);
    await loadMarketBrowsePage();
  } catch (error) {
    notify("报价失败", error.message || "请检查 market-service。", "error");
  } finally {
    marketSubmittingQuote.value = false;
  }
}

async function acceptSelectedQuote(quoteId) {
  const detail = selectedMarketDemandDetail.value;
  if (!detail?.id || !quoteId) {
    return;
  }
  marketProcessingOrder.value = true;
  try {
    await acceptMarketQuote(detail.id, quoteId);
    notify("报价已接受", "订单已锁定，后续由接单方开始履约。", "backend");
    await Promise.allSettled([
      loadMyMarketDemandsPage(),
      loadMarketBrowsePage(),
      loadMyAcceptedOrdersPage({ preserveSelection: false }),
    ]);
    await loadMarketDemandDetailById(detail.id);
  } catch (error) {
    notify("接受报价失败", error.message || "请检查 market-service。", "error");
  } finally {
    marketProcessingOrder.value = false;
  }
}

async function cancelSelectedDemand() {
  const detail = selectedMarketDemandDetail.value;
  if (!detail?.id || !canCancelSelectedDemand.value) {
    return;
  }
  marketProcessingOrder.value = true;
  try {
    await cancelMarketDemand(detail.id);
    notify("需求已取消", "该货运需求和未成交报价已同步关闭。", "backend");
    await Promise.allSettled([loadMyMarketDemandsPage(), loadMarketBrowsePage()]);
  } catch (error) {
    notify("取消需求失败", error.message || "请检查 market-service。", "error");
  } finally {
    marketProcessingOrder.value = false;
  }
}

async function completeSelectedDemandOrder() {
  const detail = selectedMarketDemandDetail.value;
  if (!detail?.acceptedOrderId || !canCompleteSelectedDemandOrder.value) {
    return;
  }
  marketProcessingOrder.value = true;
  try {
    await completeMarketOrder(detail.acceptedOrderId);
    notify("订单已完结", "履约完成，需求状态已同步关闭。", "backend");
    await Promise.allSettled([loadMyMarketDemandsPage(), loadMyAcceptedOrdersPage()]);
    await loadMarketDemandDetailById(detail.id);
  } catch (error) {
    notify("确认完结失败", error.message || "请检查 market-service。", "error");
  } finally {
    marketProcessingOrder.value = false;
  }
}

async function startSelectedOrder(orderId) {
  if (!orderId) {
    return;
  }
  marketProcessingOrder.value = true;
  try {
    await startMarketOrder(orderId);
    notify("履约已开始", "接单记录已进入履约中。", "backend");
    await loadMyAcceptedOrdersPage();
  } catch (error) {
    notify("开始履约失败", error.message || "请检查 market-service。", "error");
  } finally {
    marketProcessingOrder.value = false;
  }
}

function formatMarketDemandStatus(status) {
  const statusMap = {
    PENDING_REVIEW: "待审核",
    PUBLISHED: "待报价",
    QUOTING: "报价中",
    LOCKED: "已锁单",
    FULFILLING: "履约中",
    COMPLETED: "已完结",
    CANCELLED: "已取消",
    REJECTED: "已驳回",
  };
  return statusMap[status] || status || "未知";
}

function formatMarketAuditStatus(status) {
  const statusMap = {
    PENDING: "待审核",
    APPROVED: "审核通过",
    REJECTED: "审核驳回",
  };
  return statusMap[status] || status || "未知";
}

function formatMarketQuoteStatus(status) {
  const statusMap = {
    SUBMITTED: "已提交",
    ACCEPTED: "已接受",
    REJECTED: "已拒绝",
    WITHDRAWN: "已撤回",
  };
  return statusMap[status] || status || "未知";
}

function formatMarketOrderStatus(status) {
  const statusMap = {
    CREATED: "待开工",
    IN_PROGRESS: "履约中",
    COMPLETED: "已完结",
    CANCELLED: "已取消",
  };
  return statusMap[status] || status || "未知";
}

async function loadBills() {
  try {
    const page = await fetchBillPage({
      pageNo: billPage.current,
      pageSize: billPage.size,
      keyword: billQuery.keyword,
      status: billQuery.status,
    });
    const records = Array.isArray(page?.records) ? page.records : [];
    savedBills.value = records.map(normalizeBill);
    billPage.current = Number(page?.current || billPage.current);
    billPage.size = Number(page?.size || billPage.size);
    billPage.total = Number(page?.total || 0);
    if (!savedBills.value.some((bill) => bill.id === billDetailDialog.billId)) {
      closeBillDialog();
    }
    syncBillSelectionWithCurrentPage();
    notify("提单数据已同步", "已读取 user-service 提单分页接口。", "backend");
  } catch (error) {
    savedBills.value = [...prototypeBills];
    billPage.total = savedBills.value.length;
    if (!savedBills.value.some((bill) => bill.id === billDetailDialog.billId)) {
      closeBillDialog();
    }
    syncBillSelectionWithCurrentPage();
    notify("提单接口待检查", error.message || "已保留原型示例数据。", "error");
  }
}

function searchBills() {
  billPage.current = 1;
  loadBills();
}

function resetBillFilters() {
  billQuery.keyword = "";
  billQuery.status = "";
  searchBills();
}

function changeBillPage(step) {
  const nextPage = billPage.current + step;
  if (nextPage < 1 || nextPage > billTotalPages.value) {
    return;
  }
  billPage.current = nextPage;
  loadBills();
}

function startCreateBill() {
  closeBillDialog();
  Object.assign(billForm, createEmptyBillForm());
  billEditor.mode = "create";
  billEditor.editingId = null;
  billEditor.error = "";
  billEditor.open = true;
}

function startEditBill(bill) {
  closeBillDialog();
  Object.assign(billForm, {
    blNo: bill.blNo,
    bookingNo: bill.bookingNo === "待补充" ? "" : bill.bookingNo,
    vesselVoyage: bill.vessel,
    portOfLoading: bill.pol === "待补充" ? "" : bill.pol,
    portOfDischarge: bill.pod === "待补充" ? "" : bill.pod,
    placeOfReceipt: bill.placeOfReceipt || "",
    placeOfDelivery: bill.placeOfDelivery || "",
    goodsName: bill.goodsName === "待补充" ? "" : bill.goodsName,
    quantity: parseQuantityValue(bill.quantity),
    packageUnit: parseQuantityUnit(bill.quantity),
    status: bill.rawStatus || "DRAFT",
    remark: bill.remark || "",
  });
  billEditor.mode = "edit";
  billEditor.editingId = bill.id;
  billEditor.error = "";
  billEditor.open = true;
}

function closeBillEditor() {
  billEditor.open = false;
  billEditor.error = "";
}

async function submitBill() {
  if (!billForm.blNo) {
    billEditor.error = "请输入提单号。";
    return;
  }
  billEditor.error = "";
  try {
    const payload = {
      ...billForm,
      quantity: billForm.quantity === "" || billForm.quantity == null ? null : Number(billForm.quantity),
    };
    if (billEditor.mode === "create") {
      await createBill(payload);
      notify("提单已创建", "新提单已写入 user-service。", "backend");
    } else {
      await updateBill(billEditor.editingId, payload);
      notify("提单已更新", "修改内容已保存。", "backend");
    }
    closeBillEditor();
    await loadBills();
  } catch (error) {
    billEditor.error = error.message || "提单保存失败。";
  }
}

async function removeBill(bill) {
  try {
    closeBillDialog();
    await deleteBill(bill.id);
    selectedBillIds.value = selectedBillIds.value.filter((id) => id !== bill.id);
    notify("提单已删除", `${bill.blNo} 已从列表移除。`, "backend");
    if (savedBills.value.length === 1 && billPage.current > 1) {
      billPage.current -= 1;
    }
    await loadBills();
  } catch (error) {
    notify("删除失败", error.message || "请检查 user-service。", "error");
  }
}

async function removeSelectedBills() {
  if (!selectedBillIds.value.length) {
    return;
  }
  const ids = [...selectedBillIds.value];
  try {
    await Promise.all(ids.map((id) => deleteBill(id)));
    notify("批量删除完成", `已删除 ${ids.length} 条提单。`, "backend");
    selectedBillIds.value = [];
    if (savedBills.value.length === ids.length && billPage.current > 1) {
      billPage.current -= 1;
    }
    await loadBills();
  } catch (error) {
    notify("批量删除失败", error.message || "请检查 user-service。", "error");
    await loadBills();
  }
}

async function loadTemplates() {
  try {
    const page = await fetchExportableTemplates();
    const records = Array.isArray(page?.records) ? page.records : [];
    templateOptions.value = records.filter(isExportableTemplate).map(normalizeTemplate);
    if (!templateOptions.value.some((item) => item.id === exportForm.templateId)) {
      exportForm.templateId = templateOptions.value[0]?.id || "";
    }
  } catch (error) {
    templateOptions.value = [];
    exportForm.templateId = "";
    notify("可导出模板加载失败", error.message || "请检查 user-service。", "error");
  }
}

async function loadManagedTemplates() {
  try {
    const page = await fetchTemplateManagePage({
      pageNo: templatePage.current,
      pageSize: templatePage.size,
      keyword: templateQuery.keyword,
      status: templateQuery.status,
    });
    managedTemplates.value = Array.isArray(page?.records) ? page.records : [];
    templatePage.current = Number(page?.current || templatePage.current);
    templatePage.size = Number(page?.size || templatePage.size);
    templatePage.total = Number(page?.total || 0);
  } catch (error) {
    managedTemplates.value = [];
    templatePage.total = 0;
    notify("模板管理接口待检查", error.message || "请检查 user-service 模板管理接口。", "error");
  }
}

function searchManagedTemplates() {
  templatePage.current = 1;
  loadManagedTemplates();
}

function resetTemplateFilters() {
  templateQuery.keyword = "";
  templateQuery.status = "";
  searchManagedTemplates();
}

function changeTemplatePage(step) {
  const nextPage = templatePage.current + step;
  if (nextPage < 1 || nextPage > templateTotalPages.value) {
    return;
  }
  templatePage.current = nextPage;
  loadManagedTemplates();
}

async function toggleTemplateStatus(template) {
  if (isTemplateStatusUpdating(template.id)) {
    return;
  }
  templateStatusUpdating.value = new Set([...templateStatusUpdating.value, template.id]);
  try {
    const nextStatus = Number(template.status) === 1 ? 0 : 1;
    await updateTemplateStatus(template.id, nextStatus);
    notify(nextStatus === 1 ? "已启用" : "已停用", "", "backend");
    await Promise.allSettled([loadManagedTemplates(), loadTemplates()]);
  } catch (error) {
    notify("模板状态更新失败", error.message || "请检查 user-service。", "error");
  } finally {
    const next = new Set(templateStatusUpdating.value);
    next.delete(template.id);
    templateStatusUpdating.value = next;
  }
}

function isTemplateStatusUpdating(id) {
  return templateStatusUpdating.value.has(id);
}

async function removeManagedTemplate(template) {
  try {
    await deleteTemplateDefinition(template.id);
    notify("模板已删除", `${template.templateName} 已从模板管理中移除。`, "backend");
    if (managedTemplates.value.length === 1 && templatePage.current > 1) {
      templatePage.current -= 1;
    }
    await Promise.allSettled([loadManagedTemplates(), loadTemplates()]);
  } catch (error) {
    notify("模板删除失败", error.message || "请检查 user-service。", "error");
  }
}

function openBillDialog(bill) {
  billDetailDialog.billId = bill.id;
  billDetailDialog.open = true;
}

function closeBillDialog() {
  billDetailDialog.open = false;
  billDetailDialog.billId = "";
}

function toggleBillSelection(id) {
  selectedBillIds.value = selectedBillIds.value.includes(id)
    ? selectedBillIds.value.filter((selectedId) => selectedId !== id)
    : [...selectedBillIds.value, id];
}

function toggleSelectCurrentPage(event) {
  selectedBillIds.value = event.target.checked ? savedBills.value.map((bill) => bill.id) : [];
}

function clearBillSelection() {
  selectedBillIds.value = [];
}

function syncBillSelectionWithCurrentPage() {
  const currentIds = new Set(savedBills.value.map((bill) => bill.id));
  selectedBillIds.value = selectedBillIds.value.filter((id) => currentIds.has(id));
}

function handleExtractFile(event) {
  extractFile.value = event.target.files?.[0] || null;
}

function handleExportFile(event) {
  exportFile.value = event.target.files?.[0] || null;
}

async function extractTemplate() {
  if (!extractFile.value) {
    notify("请先上传文件", "选择一个提单样本文件后再进行模板提取。", "error");
    return;
  }
  if (extractingTemplate.value) {
    return;
  }
  const fileKey = currentExtractFileKey.value;
  if (extractedFileKeys.value.has(fileKey)) {
    notify("文件已提取", "同一份文件已命中缓存，不会再次请求 Dify；如需重跑请刷新页面或更换文件。", "error");
    return;
  }
  extractingTemplate.value = true;
  let uploadResult = null;
  try {
    uploadResult = await extractTemplateFile(extractFile.value);
  } catch (error) {
    notify("模板提取接口待配置", error.message || "Dify 工作流暂未配置，先生成原型记录。", "error");
    return;
  } finally {
    extractingTemplate.value = false;
  }
  const mappings = Array.isArray(uploadResult?.mappings)
    ? uploadResult.mappings
    : normalizeDifyWorkflowMappings(uploadResult);
  extractedFileKeys.value = new Set([...extractedFileKeys.value, fileKey]);
  const serverPreview = resolveTemplatePreview(uploadResult, extractFile.value);
  const resultId = `extract-${Date.now()}`;
  extractedTemplates.value = [
    {
      id: resultId,
      extractId: uploadResult?.extractId || resultId,
      name: extractFile.value.name.replace(/\.[^.]+$/, "") || "新模板",
      fileName: extractFile.value.name,
      fields: mappings.length || 18,
      mappings: mappings.map((mapping, index) => normalizeEditableMapping(mapping, index)),
      rawText: uploadResult?.rawText || "",
      templateStatus: uploadResult?.templateStatus || "PREVIEW_ONLY",
      templateMessage: uploadResult?.templateMessage || "已生成字段预览。",
      blankTemplateDownloadUrl: uploadResult?.blankTemplateDownloadUrl || "",
      templatePreviewUrl: uploadResult?.templatePreviewUrl || "",
      templatePreviewContentType: uploadResult?.templatePreviewContentType || "",
      sourcePreviewUrl: URL.createObjectURL(extractFile.value),
      previewUrl: serverPreview.url,
      previewType: serverPreview.type,
      previewLabel: serverPreview.label,
      previewMimeType: serverPreview.mimeType,
      previewFileName: serverPreview.fileName,
      source: mappings.length ? "已兼容 Dify workflow mappings" : "原型占位结果",
    },
    ...extractedTemplates.value,
  ];
  selectedExtractId.value = resultId;
  extractDialogOpen.value = true;
  notify(
    "模板已提取",
    mappings.length ? `已解析 ${mappings.length} 个 Dify 字段映射。` : "模板提取完成。",
    "backend",
  );
}

function buildFileKey(file) {
  return [file.name, file.size, file.lastModified].join(":");
}

function selectExtractResult(id) {
  selectedExtractId.value = id;
  extractDialogOpen.value = true;
  resetExtractSaveFeedback();
}

function closeExtractDialog() {
  clearSaveCloseTimer();
  extractDialogOpen.value = false;
  resetExtractSaveFeedback();
}

function moveMapping(index, direction) {
  const result = selectedExtractResult.value;
  if (!result) {
    return;
  }
  const nextIndex = index + direction;
  if (nextIndex < 0 || nextIndex >= result.mappings.length) {
    return;
  }
  const nextMappings = [...result.mappings];
  const [current] = nextMappings.splice(index, 1);
  nextMappings.splice(nextIndex, 0, current);
  result.mappings = nextMappings;
}

async function saveTemplateDefinition() {
  const result = selectedExtractResult.value;
  if (!result) {
    notify("请先选择结果", "点击一个提取结果后再保存。", "error");
    return;
  }
  savingTemplate.value = true;
  clearSaveCloseTimer();
  setExtractSaveFeedback("saving", "正在保存模板", "正在写入模板定义、版本、字段映射与存储位置信息。");
  try {
    const submitted = await saveGeneratedTemplate({
      extractId: result.extractId || result.id,
      fileName: result.name,
      saveAsTemplate: true,
      templateName: result.name,
      templateType: result.templateStatus === "GENERATED" ? "BILL_DOCX" : "BILL_PREVIEW",
      rawText: result.rawText,
      mappings: result.mappings.map((mapping, index) => ({
        originalText: mapping.originalText,
        placeholderKey: mapping.placeholderKey,
        dataType: mapping.dataType,
        description: mapping.description,
        sortNo: index + 1,
      })),
    });
    setExtractSaveFeedback(
      "saving",
      "模板保存任务已提交",
      submitted?.taskNo ? `任务号 ${submitted.taskNo}，正在等待 RabbitMQ 消费完成。` : "正在等待 RabbitMQ 消费完成。",
    );
    const task = await waitForTemplateSaveTask(submitted?.taskNo);
    const saved = task?.result;
    notify(
      "模板已保存",
      saved?.message || `已保存模板定义，字段数 ${result.mappings.length}。`,
      "backend",
    );
    setExtractSaveFeedback("success", "模板保存成功", saved?.message || `模板已入库，字段数 ${result.mappings.length}。`);
    await Promise.allSettled([loadManagedTemplates(), loadTemplates()]);
    scheduleExtractDialogClose();
  } catch (error) {
    const message = error.message || "请检查后端服务和数据库连接。";
    setExtractSaveFeedback("error", "模板保存失败", message);
    notify("模板保存失败", message, "error");
  } finally {
    savingTemplate.value = false;
  }
}

async function waitForTemplateSaveTask(taskNo) {
  if (!taskNo) {
    throw new Error("后端未返回模板保存任务号。");
  }
  for (let attempt = 0; attempt < 30; attempt += 1) {
    const task = await getTemplateSaveTask(taskNo);
    if (task?.status === "SUCCESS") {
      return task;
    }
    if (task?.status === "FAILED") {
      throw new Error(task.errorMessage || "模板保存任务执行失败。");
    }
    await wait(1000);
  }
  throw new Error("模板保存任务仍在处理中，请稍后刷新模板列表确认。");
}

function wait(milliseconds) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, milliseconds);
  });
}

function setExtractSaveFeedback(type, title, message) {
  extractSaveFeedback.type = type;
  extractSaveFeedback.title = title;
  extractSaveFeedback.message = message;
}

function resetExtractSaveFeedback() {
  extractSaveFeedback.type = "";
  extractSaveFeedback.title = "";
  extractSaveFeedback.message = "";
}

function scheduleExtractDialogClose() {
  clearSaveCloseTimer();
  saveCloseTimer.value = window.setTimeout(() => {
    closeExtractDialog();
  }, 3000);
}

function clearSaveCloseTimer() {
  if (saveCloseTimer.value) {
    window.clearTimeout(saveCloseTimer.value);
    saveCloseTimer.value = null;
  }
}

function normalizeEditableMapping(mapping, index) {
  return {
    id: `${mapping.placeholderKey || "field"}-${index}-${Date.now()}`,
    originalText: mapping.originalText ?? mapping.original_text ?? "",
    placeholderKey: mapping.placeholderKey ?? mapping.placeholder_key ?? "",
    dataType: mapping.dataType ?? mapping.data_type ?? "string",
    description: mapping.description ?? "",
  };
}

function getPreviewType(file) {
  if (file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf")) {
    return "pdf";
  }
  if (file.type.startsWith("image/")) {
    return "image";
  }
  if (/\.(docx?|wps)$/i.test(file.name)) {
    return "word";
  }
  return "unsupported";
}

function resolveTemplatePreview(uploadResult, file) {
  const contentType = uploadResult?.templatePreviewContentType || "";
  const previewUrl = uploadResult?.templatePreviewUrl || "";
  if (previewUrl) {
    const type = contentType.includes("pdf")
      ? "pdf"
      : contentType.includes("wordprocessingml") || contentType.includes("msword")
        ? "word"
        : "unsupported";
    return {
      url: buildUserApiUrl(previewUrl.replace(/^\/user/, "")),
      type,
      label: type === "pdf" ? "已替换占位符 PDF 预览" : "已替换占位符 DOCX 模板",
      mimeType: contentType || "application/octet-stream",
      fileName: file.name.replace(/\.[^.]+$/, "") + "-template-preview" + (type === "pdf" ? ".pdf" : ".docx"),
    };
  }
  return {
    url: URL.createObjectURL(file),
    type: getPreviewType(file),
    label: getPreviewLabel(file),
    mimeType: getPreviewMimeType(file),
    fileName: file.name,
  };
}

function getPreviewLabel(file) {
  if (file.name.toLowerCase().endsWith(".docx")) {
    return "Word DOCX 原文件";
  }
  if (file.name.toLowerCase().endsWith(".doc")) {
    return "Word DOC 原文件";
  }
  if (file.name.toLowerCase().endsWith(".pdf")) {
    return "PDF 原文件";
  }
  if (file.type.startsWith("image/")) {
    return "图片原文件";
  }
  return "原文件预览待支持";
}

function getPreviewMimeType(file) {
  const lowerName = file.name.toLowerCase();
  if (lowerName.endsWith(".docx")) {
    return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  }
  if (lowerName.endsWith(".doc")) {
    return "application/msword";
  }
  return file.type || "application/octet-stream";
}

async function createExportJob() {
  if (!exportFile.value) {
    notify("请先上传目标文件", "选择目标文件后再创建导出任务。", "error");
    return;
  }
  if (!exportForm.templateId) {
    notify("请先选择模板", "没有可用模板时，请先在模板提取中保存 DOCX 模板。", "error");
    return;
  }
  if (exportingTemplate.value) {
    return;
  }
  exportingTemplate.value = true;
  const template = templateOptions.value.find((item) => item.id === exportForm.templateId);
  const pendingId = Date.now();
  exportJobs.value = [
    {
      id: pendingId,
      template: template?.name || "未命名模板",
      file: exportFile.value.name,
      status: "Dify 正在抽取字段",
      fields: [],
      missing: [],
      downloadUrl: "",
      extractedFields: {},
      sourceFileName: exportFile.value.name,
      templateId: exportForm.templateId,
      savingBusiness: false,
      businessSaved: false,
    },
    ...exportJobs.value,
  ];
  try {
    const result = await exportTemplateFile({
      templateId: exportForm.templateId,
      outputFormat: exportForm.outputFormat,
      file: exportFile.value,
    });
    const completedJob = {
      id: pendingId,
      template: result?.templateName || template?.name || "未命名模板",
      file: result?.outputFileName || exportFile.value.name,
      status: result?.message || "导出完成",
      fields: fieldsToRows(result?.extractedFields),
      missing: result?.missingPlaceholders || [],
      downloadUrl: result?.downloadUrl ? buildUserApiUrl(result.downloadUrl.replace(/^\/user/, "")) : "",
      extractedFields: result?.extractedFields || {},
      sourceFileName: exportFile.value.name,
      templateId: exportForm.templateId,
      exportId: result?.exportId || "",
      outputFormat: result?.outputFormat || exportForm.outputFormat,
      rawText: result?.rawText || "",
      previewType: getExportPreviewType(result?.outputFormat || exportForm.outputFormat, result?.outputFileName || ""),
      previewMimeType: getExportPreviewMimeType(result?.outputFormat || exportForm.outputFormat),
      savingBusiness: false,
      businessSaved: false,
      minioSaved: true,
    };
    exportJobs.value = exportJobs.value.map((job) =>
      job.id === pendingId ? completedJob : job,
    );
    selectedExportJobId.value = pendingId;
    exportDialogOpen.value = true;
    resetExportSaveFeedback();
    notify("导出完成", result?.message || "已生成可下载的标准文档。", "backend");
  } catch (error) {
    exportJobs.value = exportJobs.value.map((job) =>
      job.id === pendingId
        ? { ...job, status: error.message || "导出失败" }
        : job,
    );
    notify("导出失败", error.message || "请检查 Dify 导出工作流和模板文件。", "error");
  } finally {
    exportingTemplate.value = false;
  }
}

function fieldsToRows(fields) {
  return Object.entries(fields || {}).map(([key, value]) => ({
    key,
    value: value == null ? "空白" : String(value),
  }));
}

function getExportPreviewType(outputFormat, fileName = "") {
  const normalized = String(outputFormat || "").toUpperCase();
  if (normalized === "PDF" || fileName.toLowerCase().endsWith(".pdf")) {
    return "pdf";
  }
  if (normalized === "DOCX" || fileName.toLowerCase().endsWith(".docx")) {
    return "word";
  }
  return "unsupported";
}

function getExportPreviewMimeType(outputFormat) {
  return String(outputFormat || "").toUpperCase() === "PDF"
    ? "application/pdf"
    : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
}

function openExportDialog(job) {
  selectedExportJobId.value = job.id;
  exportDialogOpen.value = true;
  resetExportSaveFeedback();
}

function closeExportDialog() {
  exportDialogOpen.value = false;
  resetExportSaveFeedback();
}

function saveExportToLocal(job) {
  if (!job?.downloadUrl) {
    setExportSaveFeedback("error", "暂无文件", "导出文件不存在，请重新创建导出任务。");
    return;
  }
  const link = document.createElement("a");
  link.href = job.downloadUrl;
  link.download = job.file || "template-export.docx";
  document.body.appendChild(link);
  link.click();
  link.remove();
  setExportSaveFeedback("success", "已开始保存", "文件已交给浏览器下载，稍后可在本地下载目录查看。");
}

function confirmExportSavedToMinio(job) {
  if (!job?.exportId) {
    setExportSaveFeedback("error", "保存状态未知", "当前导出结果缺少 exportId，请重新创建导出任务。");
    return;
  }
  exportJobs.value = exportJobs.value.map((item) =>
    item.id === job.id
      ? {
          ...item,
          minioSaved: true,
          status: "导出文件已保存到 MinIO",
        }
      : item,
  );
  setExportSaveFeedback("success", "已保存到 MinIO", "后端导出时已将文件资产写入 MinIO，可在导出队列继续下载。");
}

function setExportSaveFeedback(type, title, message) {
  exportSaveFeedback.type = type;
  exportSaveFeedback.title = title;
  exportSaveFeedback.message = message;
}

function resetExportSaveFeedback() {
  exportSaveFeedback.type = "";
  exportSaveFeedback.title = "";
  exportSaveFeedback.message = "";
}

async function saveExportJobToBusiness(job) {
  if (!job?.extractedFields || Object.keys(job.extractedFields).length === 0) {
    notify("暂无可保存数据", "请先完成 Dify 字段抽取后再保存至业务数据。", "error");
    return;
  }
  exportJobs.value = exportJobs.value.map((item) =>
    item.id === job.id ? { ...item, savingBusiness: true } : item,
  );
  try {
    const saved = await saveExtractedBillData({
      templateId: job.templateId,
      sourceFileName: job.sourceFileName || job.file,
      fields: job.extractedFields,
    });
    exportJobs.value = exportJobs.value.map((item) =>
      item.id === job.id
        ? {
            ...item,
            savingBusiness: false,
            businessSaved: true,
            status: `业务数据已保存：${saved?.blNo || "提单"}`,
          }
        : item,
    );
    notify("业务数据已保存", `${saved?.blNo || "提单"} 已进入已存提单数据。`, "backend");
    billPage.current = 1;
    await loadBills();
    currentView.value = "bills";
  } catch (error) {
    exportJobs.value = exportJobs.value.map((item) =>
      item.id === job.id ? { ...item, savingBusiness: false } : item,
    );
    notify("业务数据保存失败", error.message || "请检查数据库和提单字段。", "error");
  }
}

function normalizeBill(bill) {
  return {
    id: String(bill.id ?? bill.blNo ?? Date.now()),
    blNo: bill.blNo || "未命名提单",
    bookingNo: bill.bookingNo || "待补充",
    vessel: bill.vesselVoyage || bill.vessel || "待补充",
    pol: bill.pol || bill.portOfLoading || "待补充",
    pod: bill.pod || bill.portOfDischarge || "待补充",
    placeOfReceipt: bill.placeOfReceipt || "",
    placeOfDelivery: bill.placeOfDelivery || "",
    rawStatus: bill.status || "DRAFT",
    status: formatBillStatus(bill.status || "DRAFT"),
    parseStatus: bill.parseStatus || "NONE",
    remark: bill.remark || "",
    goodsName: bill.goodsName || "待补充",
    quantity: bill.quantity || "待补充",
    detailFields: [
      { label: "起运港", value: bill.pol || bill.portOfLoading || "待补充" },
      { label: "目的港", value: bill.pod || bill.portOfDischarge || "待补充" },
      { label: "订舱号", value: bill.bookingNo || "待补充" },
      { label: "解析状态", value: bill.parseStatus || "待补充" },
      { label: "收货地", value: bill.placeOfReceipt || "待补充" },
      { label: "交付地", value: bill.placeOfDelivery || "待补充" },
    ],
  };
}

function createEmptyBillForm() {
  return {
    blNo: "",
    bookingNo: "",
    vesselVoyage: "",
    portOfLoading: "",
    portOfDischarge: "",
    placeOfReceipt: "",
    placeOfDelivery: "",
    goodsName: "",
    quantity: "",
    packageUnit: "CTN",
    status: "DRAFT",
    remark: "",
  };
}

function createEmptyMarketDemandForm() {
  return {
    title: "",
    goodsName: "",
    departurePort: "",
    destinationPort: "",
    expectedShippingDate: "",
    quantity: "",
    quantityUnit: "BOX",
    budgetAmount: "",
    currencyCode: "CNY",
    contactName: "",
    contactPhone: "",
    remark: "",
  };
}

function createEmptyMarketQuoteForm() {
  return {
    priceAmount: "",
    currencyCode: "CNY",
    estimatedDays: "",
    serviceNote: "",
  };
}

function formatBillStatus(status) {
  const map = {
    DRAFT: "草稿",
    CONFIRMED: "已确认",
    ARCHIVED: "已归档",
  };
  return map[status] || status || "未知";
}

function parseQuantityValue(quantity) {
  if (!quantity || quantity === "待补充") {
    return "";
  }
  const matched = String(quantity).match(/\d+/);
  return matched ? Number(matched[0]) : "";
}

function parseQuantityUnit(quantity) {
  if (!quantity || quantity === "待补充") {
    return "CTN";
  }
  const parts = String(quantity).trim().split(/\s+/);
  return parts[1] || "CTN";
}

function normalizeTemplate(template) {
  return {
    id: String(template.id ?? template.templateCode ?? Date.now()),
    name: template.templateName || template.name || "未命名模板",
    contentFormat: template.contentFormat || "",
    objectKey: template.objectKey || "",
  };
}

function isExportableTemplate(template) {
  const objectKey = String(template.objectKey || "");
  return Number(template.status) === 1
    && String(template.contentFormat || "").toUpperCase() === "DOCX"
    && objectKey.toLowerCase().endsWith(".docx");
}

function notify(title, message, type = "backend") {
  const id = Date.now() + Math.random();
  toasts.value = [{
    id,
    title: trimToastPunctuation(title),
    message: trimToastPunctuation(message),
    type,
  }].slice(0, 1);
  window.setTimeout(() => {
    toasts.value = toasts.value.filter((toast) => toast.id !== id);
  }, 2600);
}

function trimToastPunctuation(value) {
  return String(value || "").replace(/[。.!！]+$/g, "");
}
</script>
