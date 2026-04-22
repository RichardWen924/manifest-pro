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

      <form v-if="authMode === 'login'" class="login-card" novalidate @submit.prevent="login">
        <div class="auth-card-head">
          <div>
            <p class="card-kicker">鉴权入口</p>
            <h2>登录客户端</h2>
            <p>输入用户名或航运公司四字母编号，再使用密码进入用户工作台。</p>
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

      <form v-else class="login-card register-card" novalidate @submit.prevent="register">
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
          @click="switchView(item.key)"
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
              <span>先维护核心字段，复杂解析字段后续接入模板和文件解析。</span>
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
            :class="{ expanded: selectedBillId === bill.id, checked: selectedBillIds.includes(bill.id) }"
            role="listitem"
          >
            <div class="bill-row-main">
              <label class="check-control row-check" @click.stop>
                <input type="checkbox" :checked="selectedBillIds.includes(bill.id)" @change="toggleBillSelection(bill.id)" />
                <span class="sr-only">选择 {{ bill.blNo }}</span>
              </label>
              <button class="bill-summary" type="button" @click="toggleBill(bill.id)">
                <span class="expand-indicator">{{ selectedBillId === bill.id ? "−" : "+" }}</span>
                <span>
                  <small>提单号</small>
                  <strong>{{ bill.blNo }}</strong>
                </span>
                <span>
                  <small>船名航次</small>
                  <strong>{{ bill.vessel }}</strong>
                </span>
                <span>
                  <small>货运商品名称</small>
                  <strong>{{ bill.goodsName }}</strong>
                </span>
                <span>
                  <small>数量</small>
                  <strong>{{ bill.quantity }}</strong>
                </span>
                <span class="pill">{{ bill.status }}</span>
              </button>
            </div>

            <div v-if="selectedBillId === bill.id" class="bill-detail-panel">
              <div class="row-actions">
                <button class="secondary-button" type="button" @click="startEditBill(bill)">编辑</button>
                <button class="danger-button" type="button" @click="removeBill(bill)">删除</button>
              </div>
              <div class="detail-grid">
                <div v-for="field in bill.detailFields" :key="field.label" class="detail-field">
                  <span>{{ field.label }}</span>
                  <strong>{{ field.value }}</strong>
                </div>
              </div>
              <div class="field-hint">
                <span></span>
                <p>后续 30+ 字段会继续放入这个展开区域，保持列表层只展示关键摘要。</p>
              </div>
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
              <span class="pill" :class="{ muted: template.status !== 1 }">{{ template.status === 1 ? "启用" : "停用" }}</span>
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
              <button class="ghost-button" type="button" @click="toggleTemplateStatus(template)">
                {{ template.status === 1 ? "停用" : "启用" }}
              </button>
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
              <div class="panel-title compact">
                <h2>原文件实时预览</h2>
                <p>左侧展示上传的 PDF / Word / 图片原文件，右侧编辑 Dify 提取出的字段对应关系。</p>
              </div>
              <div class="blank-template-note">
                <strong>{{ selectedExtractResult.previewLabel }}</strong>
                <span>{{ selectedExtractResult.templateMessage }}</span>
              </div>
              <div class="source-preview-frame" :class="selectedExtractResult.previewType">
                <iframe
                  v-if="selectedExtractResult.previewType === 'pdf'"
                  :src="selectedExtractResult.previewUrl"
                  title="PDF 原文件预览"
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
                    <p>浏览器通常不能完整渲染本地 DOC/DOCX 版式；这里保留原文件预览入口，右侧字段可继续编辑。</p>
                    <a class="download-template-link" :href="selectedExtractResult.previewUrl" :download="selectedExtractResult.fileName">
                      打开或下载原文件
                    </a>
                  </div>
                </object>
                <div v-else class="word-preview-fallback">
                  <strong>{{ selectedExtractResult.previewLabel }}</strong>
                  <p>当前文件类型暂不支持浏览器内预览，但字段对应关系仍可继续编辑和保存。</p>
                  <a class="download-template-link" :href="selectedExtractResult.previewUrl" :download="selectedExtractResult.fileName">
                    下载原文件
                  </a>
                </div>
              </div>
              <div class="preview-footer-actions">
                <a class="download-template-link neutral" :href="selectedExtractResult.previewUrl" :download="selectedExtractResult.fileName">
                  下载原文件
                </a>
                <a
                  v-if="selectedExtractResult.blankTemplateDownloadUrl"
                  class="download-template-link"
                  :href="buildUserApiUrl(selectedExtractResult.blankTemplateDownloadUrl.replace(/^\/user/, ''))"
                  download
                >
                  下载剔除数据后的模板
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
                  下载空白模板
                </a>
              </div>
            </section>
          </div>
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
import {
  buildUserApiUrl,
  createBill,
  deleteBill,
  deleteTemplateDefinition,
  extractTemplateFile,
  fetchBillPage,
  fetchTemplateManagePage,
  fetchTemplateOptions,
  initFileUpload,
  loginClient,
  registerClient,
  saveGeneratedTemplate,
  setAccessToken,
  updateTemplateStatus,
  updateBill,
} from "./api/clientApi";
import { normalizeDifyWorkflowMappings } from "./utils/difyWorkflow";

const authMode = ref("login");

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
const selectedBillId = ref("BL-001");
const selectedBillIds = ref([]);
const extractFile = ref(null);
const exportFile = ref(null);
const extractingTemplate = ref(false);
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

const exportForm = reactive({
  templateId: "tpl-001",
});

const billQuery = reactive({
  keyword: "",
  status: "",
});

const templateQuery = reactive({
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

const billEditor = reactive({
  open: false,
  mode: "create",
  editingId: null,
  error: "",
});

const billForm = reactive(createEmptyBillForm());

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

const navItems = [
  { key: "overview", label: "用户总览", icon: "◎" },
  { key: "bills", label: "已存提单数据", icon: "▤" },
  { key: "extract", label: "提单模版提取", icon: "◇" },
  { key: "templates", label: "模板管理", icon: "▧" },
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
  templates: {
    eyebrow: "Template Library",
    title: "模板管理",
    description: "查看已保存模板、存储位置、版本与可用状态。",
  },
  export: {
    eyebrow: "Template Export",
    title: "按模版导出",
    description: "上传目标文件，按照选定模板生成导出任务。",
  },
};

const savedBills = ref([...prototypeBills]);

const templateOptions = ref([...prototypeTemplates]);

const extractedTemplates = ref([]);
const managedTemplates = ref([]);
const exportJobs = ref([]);

const currentMeta = computed(() => metaMap[currentView.value]);
const avatarText = computed(() => (session.nickname || session.username || "U").slice(0, 2).toUpperCase());
const billTotalPages = computed(() => Math.max(1, Math.ceil(billPage.total / billPage.size)));
const templateTotalPages = computed(() => Math.max(1, Math.ceil(templatePage.total / templatePage.size)));
const currentExtractFileKey = computed(() => (extractFile.value ? buildFileKey(extractFile.value) : ""));
const isCurrentExtractFileDone = computed(() => Boolean(currentExtractFileKey.value && extractedFileKeys.value.has(currentExtractFileKey.value)));
const selectedExtractResult = computed(() => extractedTemplates.value.find((item) => item.id === selectedExtractId.value));
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
    session.username = result.username || loginPayload.username;
    session.nickname = result.username === "tenant_user" ? "测试用户" : result.username || loginPayload.username;
    session.companyCode = (loginPayload.companyCode || "TEST").toUpperCase();
    notify("登录成功", "已通过 auth-service 校验，正在同步用户端数据。", "backend");
    await Promise.allSettled([loadBills(), loadTemplates(), loadManagedTemplates()]);
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
  currentView.value = "overview";
  notify("已退出", "客户端会话已结束。", "backend");
}

function switchView(view) {
  currentView.value = view;
  if (view === "templates") {
    loadManagedTemplates();
  }
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
    selectedBillId.value = savedBills.value[0]?.id || "";
    syncBillSelectionWithCurrentPage();
    notify("提单数据已同步", "已读取 user-service 提单分页接口。", "backend");
  } catch (error) {
    savedBills.value = [...prototypeBills];
    billPage.total = savedBills.value.length;
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
  Object.assign(billForm, createEmptyBillForm());
  billEditor.mode = "create";
  billEditor.editingId = null;
  billEditor.error = "";
  billEditor.open = true;
}

function startEditBill(bill) {
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
    const records = await fetchTemplateOptions();
    templateOptions.value = Array.isArray(records) && records.length ? records.map(normalizeTemplate) : [...prototypeTemplates];
  } catch (error) {
    templateOptions.value = [...prototypeTemplates];
    notify("模板接口待检查", error.message || "已保留原型模板选项。", "error");
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
  try {
    const nextStatus = template.status === 1 ? 0 : 1;
    await updateTemplateStatus(template.id, nextStatus);
    notify("模板状态已更新", `${template.templateName} 已${nextStatus === 1 ? "启用" : "停用"}。`, "backend");
    await Promise.allSettled([loadManagedTemplates(), loadTemplates()]);
  } catch (error) {
    notify("模板状态更新失败", error.message || "请检查 user-service。", "error");
  }
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

function toggleBill(id) {
  selectedBillId.value = selectedBillId.value === id ? "" : id;
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
      previewUrl: URL.createObjectURL(extractFile.value),
      previewType: getPreviewType(extractFile.value),
      previewLabel: getPreviewLabel(extractFile.value),
      previewMimeType: getPreviewMimeType(extractFile.value),
      source: mappings.length ? "已兼容 Dify workflow mappings" : "原型占位结果",
    },
    ...extractedTemplates.value,
  ];
  selectedExtractId.value = resultId;
  extractDialogOpen.value = true;
  notify(
    "模板已提取",
    mappings.length ? `已解析 ${mappings.length} 个 Dify 字段映射。` : "已生成原型模板结果，可在后续接入真实解析。",
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
    const saved = await saveGeneratedTemplate({
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
    scheduleExtractDialogClose();
  } finally {
    savingTemplate.value = false;
  }
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
  try {
    await initFileUpload(exportFile.value, "TEMPLATE_EXPORT");
  } catch (error) {
    notify("文件接口待实现", error.message || "后端上传初始化暂未完成，先进入原型队列。", "error");
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
  };
}

function notify(title, message, type = "backend") {
  const id = Date.now() + Math.random();
  toasts.value = [{ id, title, message, type }, ...toasts.value].slice(0, 3);
  window.setTimeout(() => {
    toasts.value = toasts.value.filter((toast) => toast.id !== id);
  }, 3200);
}
</script>
