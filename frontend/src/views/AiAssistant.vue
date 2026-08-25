<template>
  <div style="display: flex; height: calc(100vh - 60px); gap: 12px">
    <!-- 左侧：聊天历史侧边栏 -->
    <div class="sidebar">
      <el-button type="primary" style="width: 100%; margin-bottom: 12px" @click="resetSession">
        <el-icon><Plus /></el-icon> 新对话
      </el-button>
      <div class="sidebar-list">
        <div v-if="sessions.length === 0" style="text-align: center; color: #999; padding: 20px 0; font-size: 13px">
          暂无历史记录
        </div>
        <div v-for="s in sessions" :key="s.sessionId" class="sidebar-item"
          :class="{ active: s.sessionId === sessionId }" @click="loadSession(s.sessionId)">
          <div class="sidebar-item-title">{{ s.title }}</div>
          <div class="sidebar-item-meta">
            <span>{{ s.datasourceName }}</span>
            <el-icon class="sidebar-item-delete" @click.stop="handleDeleteSession(s.sessionId)"><Delete /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧：聊天主区域 -->
    <div style="display: flex; flex-direction: column; flex: 1; min-width: 0">
      <!-- 顶部工具栏 -->
      <el-card shadow="never" style="margin-bottom: 12px; flex-shrink: 0">
        <el-row :gutter="16" align="middle">
          <el-col :span="8">
            <el-select v-model="datasourceId" placeholder="选择数据源" style="width: 100%">
              <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-button text type="danger" @click="resetSession">
              <el-icon><Delete /></el-icon> 新对话
            </el-button>
          </el-col>
        </el-row>
      </el-card>

      <!-- 聊天区域 -->
      <div ref="chatBox" class="chat-box">
        <div v-if="messages.length === 0" class="chat-empty">
          <p style="font-size: 16px; color: #999">AI 数据质量规则助手</p>
          <p style="color: #bbb">选择数据源后，输入描述即可自动生成规则</p>
          <p style="color: #bbb">例如："用户表的名称列不能为空"</p>
        </div>
        <div v-for="(msg, idx) in messages" :key="idx" class="chat-msg"
          :class="msg.role === 'user' ? 'msg-right' : 'msg-left'">
          <!-- 用户消息 -->
          <div v-if="msg.role === 'user'" class="bubble bubble-user">
            {{ msg.content }}
          </div>
          <!-- AI 回复 -->
          <div v-else class="bubble bubble-ai">
            <div v-if="msg.loading" class="ai-loading">
              <el-icon class="is-loading"><Loading /></el-icon> AI 正在分析...
            </div>
            <template v-else-if="msg.preview">
              <div style="font-weight: bold; margin-bottom: 10px">
                {{ msg.saved ? '已保存规则' : 'AI 生成规则预览' }}
              </div>
              <div class="rule-form">
                <div class="rule-form-row">
                  <label>表名</label>
                  <el-input v-model="msg.preview.tableName" size="small" :disabled="msg.saved" />
                </div>
                <div class="rule-form-row">
                  <label>字段</label>
                  <el-input v-model="msg.preview.columnName" size="small" :disabled="msg.saved" />
                </div>
                <div class="rule-form-row">
                  <label>规则类型</label>
                  <el-select v-model="msg.preview.ruleType" size="small" style="width: 100%" :disabled="msg.saved">
                    <el-option v-for="rt in ruleTypes" :key="rt.value" :label="rt.label" :value="rt.value" />
                  </el-select>
                </div>
                <div class="rule-form-row">
                  <label>规则名称</label>
                  <el-input v-model="msg.preview.ruleName" size="small" :disabled="msg.saved" />
                </div>
                <div class="rule-form-row">
                  <label>规则配置</label>
                  <el-input v-model="msg.preview.ruleConfig" type="textarea" :rows="2" size="small" :disabled="msg.saved" />
                </div>
                <div class="rule-form-row" v-if="msg.preview.description">
                  <label>描述</label>
                  <el-input v-model="msg.preview.description" type="textarea" :rows="2" size="small" :disabled="msg.saved" />
                </div>
              </div>
              <div style="margin-top: 10px; text-align: right">
                <el-button size="small" type="primary" :loading="msg.saving"
                  :disabled="msg.saved" @click="handleSave(msg, idx)">
                  {{ msg.saved ? '已保存' : '确认保存' }}
                </el-button>
              </div>
            </template>
            <div v-else-if="msg.error" style="color: #f56c6c">
              {{ msg.error }}
            </div>
            <div v-else-if="msg.restored" style="color: #606266">
              {{ msg.content }}
            </div>
          </div>
        </div>
      </div>

      <!-- 底部输入区 -->
      <div class="chat-input">
        <el-input v-model="inputText" placeholder="描述你需要的数据质量规则..." :disabled="sending"
          @keyup.enter="handleSend" clearable>
          <template #append>
            <el-button type="primary" :loading="sending" @click="handleSend">
              发送
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Loading, Plus } from '@element-plus/icons-vue'
import { listDataSources } from '../api/datasource'
import { aiPreview, aiSave, listChatSessions, getChatSession, deleteChatSession } from '../api/aiRule'

const datasources = ref([])
const datasourceId = ref(null)
const sessionId = ref(null)
const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const chatBox = ref(null)
const sessions = ref([])

const ruleTypes = [
  { value: 'NOT_NULL', label: '非空校验 (NOT_NULL)' },
  { value: 'UNIQUE', label: '唯一性校验 (UNIQUE)' },
  { value: 'LENGTH', label: '长度校验 (LENGTH)' },
  { value: 'RANGE', label: '范围校验 (RANGE)' },
  { value: 'REGEX', label: '正则校验 (REGEX)' },
  { value: 'ENUM', label: '枚举校验 (ENUM)' },
  { value: 'CUSTOM_SQL', label: '自定义 SQL (CUSTOM_SQL)' }
]

onMounted(async () => {
  const res = await listDataSources()
  datasources.value = res.data || []
  await refreshSessions()
})

async function refreshSessions() {
  try {
    const res = await listChatSessions()
    sessions.value = res.data || []
  } catch (e) {
    sessions.value = []
  }
}

function resetSession() {
  sessionId.value = null
  messages.value = []
}

function scrollToBottom() {
  nextTick(() => {
    if (chatBox.value) {
      chatBox.value.scrollTop = chatBox.value.scrollHeight
    }
  })
}

async function loadSession(sid) {
  try {
    const res = await getChatSession(sid)
    const data = res.data
    if (!data) return

    sessionId.value = data.sessionId
    datasourceId.value = data.datasourceId
    messages.value = []

    // 恢复对话历史：用 rules 渲染规则卡片，history 补全用户消息
    const historyList = data.history || []
    const rulesList = data.rules || []

    historyList.forEach((h, i) => {
      // 用户消息
      messages.value.push({ role: 'user', content: h.user })

      // AI 回复：优先用 rules 渲染预览卡片，否则显示文字
      const rule = rulesList[i]
      if (rule) {
        const ruleConfig = typeof rule.ruleConfig === 'string'
          ? rule.ruleConfig
          : JSON.stringify(rule.ruleConfig || {})
        messages.value.push({
          role: 'ai',
          preview: {
            datasourceId: data.datasourceId,
            tableName: rule.tableName,
            columnName: rule.columnName,
            ruleType: rule.ruleType,
            ruleName: rule.ruleName,
            ruleConfig: ruleConfig,
            description: h.assistant,
            sessionId: data.sessionId
          },
          saving: false,
          saved: true   // 历史会话中的规则默认视为已保存，避免重复入库
        })
      } else {
        // 没有对应规则（可能当时 AI 未成功生成），显示文字摘要
        messages.value.push({ role: 'ai', restored: true, content: h.assistant })
      }
    })
    scrollToBottom()
  } catch (e) {
    ElMessage.error(e.message || '加载会话失败')
  }
}

async function handleDeleteSession(sid) {
  try {
    await ElMessageBox.confirm('确定删除该对话记录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteChatSession(sid)
    ElMessage.success('已删除')
    // 如果删除的是当前会话，清空界面
    if (sid === sessionId.value) {
      resetSession()
    }
    await refreshSessions()
  } catch (e) {
    // 用户取消时不提示
    if (e !== 'cancel' && e?.toString() !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text) return
  if (!datasourceId.value) {
    ElMessage.warning('请先选择数据源')
    return
  }

  // 添加用户消息
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()

  // 添加 AI 加载占位
  const aiIdx = messages.value.length
  messages.value.push({ role: 'ai', loading: true })
  scrollToBottom()

  sending.value = true
  try {
    const res = await aiPreview({
      datasourceId: datasourceId.value,
      description: text,
      sessionId: sessionId.value || undefined
    })
    const preview = res.data
    // 保存 sessionId 以串联多轮对话
    if (preview?.sessionId) {
      sessionId.value = preview.sessionId
    }
    messages.value[aiIdx] = {
      role: 'ai',
      preview,
      saving: false,
      saved: false
    }
    // 刷新侧边栏列表
    await refreshSessions()
  } catch (e) {
    messages.value[aiIdx] = {
      role: 'ai',
      error: e.message || '请求失败，请重试'
    }
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

async function handleSave(msg, idx) {
  msg.saving = true
  try {
    await aiSave(msg.preview)
    ElMessage.success('规则保存成功')
    messages.value[idx] = { ...msg, saved: true, saving: false }
  } catch (e) {
    msg.saving = false
  }
}
</script>

<style scoped>
.sidebar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
}

.sidebar-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}

.sidebar-item:hover {
  background: #f5f7fa;
}

.sidebar-item.active {
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
}

.sidebar-item-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.sidebar-item-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.sidebar-item-delete {
  cursor: pointer;
  color: #c0c4cc;
  transition: color 0.2s;
}

.sidebar-item-delete:hover {
  color: #f56c6c;
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.chat-msg {
  display: flex;
  margin-bottom: 16px;
}

.msg-right {
  justify-content: flex-end;
}

.msg-left {
  justify-content: flex-start;
}

.bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  word-break: break-word;
}

.bubble-user {
  background: #409eff;
  color: #fff;
  border-top-right-radius: 4px;
}

.bubble-ai {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-top-left-radius: 4px;
  min-width: 400px;
}

.rule-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rule-form-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.rule-form-row label {
  flex-shrink: 0;
  width: 64px;
  line-height: 32px;
  font-size: 13px;
  color: #606266;
  text-align: right;
}

.ai-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
}

.chat-input {
  flex-shrink: 0;
}
</style>
