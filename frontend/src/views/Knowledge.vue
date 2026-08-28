<template>
  <div>
    <!-- 顶部工具栏 -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-row :gutter="16" align="middle">
        <el-col :span="8">
          <el-input v-model="searchQuery" placeholder="输入关键词搜索知识库..." clearable
            @keyup.enter="handleSearch" @clear="searchResults = []">
            <template #append>
              <el-button @click="handleSearch" :loading="searching">搜索</el-button>
            </template>
          </el-input>
        </el-col>
        <el-col :span="16" style="text-align: right">
          <el-button type="success" @click="handleLoadDir" :loading="loadingDir">
            <el-icon><FolderOpened /></el-icon> 加载目录
          </el-button>
          <el-button type="primary" @click="openAddDialog">
            <el-icon><Plus /></el-icon> 新增知识
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 搜索结果面板 -->
    <el-card v-if="searchResults.length > 0" shadow="never" style="margin-bottom: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>搜索结果（{{ searchResults.length }} 条命中）</span>
          <el-button text type="primary" @click="searchResults = []">关闭</el-button>
        </div>
      </template>
      <div v-for="(item, idx) in searchResults" :key="idx" class="search-result-item">
        <div class="search-result-header">
          <el-tag size="small" type="info">{{ item.source || item.title }}</el-tag>
          <span class="search-result-score">相似度：{{ (item.score * 100).toFixed(1) }}%</span>
        </div>
        <div class="search-result-content">{{ item.content }}</div>
      </div>
    </el-card>

    <!-- 知识文档列表 -->
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>知识文档列表（共 {{ docs.length }} 篇）</span>
          <el-button text type="primary" @click="fetchDocs" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>
      <el-table :data="docs" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="180" show-overflow-tooltip />
        <el-table-column prop="chunkCount" label="切片数" width="100" align="center" />
        <el-table-column prop="docId" label="文档ID" width="280" show-overflow-tooltip />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openViewDialog(row)">查看</el-button>
            <el-button text type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 查看文档弹窗 -->
    <el-dialog v-model="viewDialogVisible" title="查看文档" width="800px" destroy-on-close
      top="5vh">
      <template v-if="viewDoc">
        <el-descriptions :column="2" border style="margin-bottom: 16px">
          <el-descriptions-item label="标题" :span="2">{{ viewDoc.title }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ viewDoc.source }}</el-descriptions-item>
          <el-descriptions-item label="切片数">{{ viewDoc.chunkCount }}</el-descriptions-item>
          <el-descriptions-item label="文档ID" :span="2">
            <code style="font-size: 12px">{{ viewDoc.docId }}</code>
          </el-descriptions-item>
        </el-descriptions>
        <el-card shadow="never" class="view-content-card">
          <template #header>
            <span>文档内容</span>
          </template>
          <div class="view-content" v-html="renderedContent"></div>
        </el-card>
      </template>
      <div v-else style="text-align: center; padding: 40px; color: #909399">
        <el-icon :size="48" style="margin-bottom: 12px"><FolderOpened /></el-icon>
        <p>加载中...</p>
      </div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑知识文档' : '新增知识文档'" width="700px"
      destroy-on-close @close="resetForm">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="输入文档标题" />
        </el-form-item>
        <el-form-item label="来源" v-if="!isEdit">
          <el-input v-model="form.source" placeholder="来源文件名（可选）" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="15"
            placeholder="输入文档全文内容（支持 Markdown 格式）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存修改' : '确认添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, FolderOpened, Refresh } from '@element-plus/icons-vue'
import {
  listKnowledge,
  getKnowledge,
  addKnowledge,
  updateKnowledge,
  deleteKnowledge,
  searchKnowledge,
  loadKnowledgeDir
} from '../api/knowledge'

const docs = ref([])
const loading = ref(false)
const searching = ref(false)
const loadingDir = ref(false)
const searchQuery = ref('')
const searchResults = ref([])

// 查看弹窗相关
const viewDialogVisible = ref(false)
const viewDoc = ref(null)
const renderedContent = ref('')
const loadingView = ref(false)

// 弹窗相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editingDocId = ref(null)
const form = ref({ title: '', content: '', source: '' })

onMounted(() => {
  fetchDocs()
})

async function fetchDocs() {
  loading.value = true
  try {
    const res = await listKnowledge()
    docs.value = res.data || []
  } catch (e) {
    docs.value = []
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  const query = searchQuery.value.trim()
  if (!query) return
  searching.value = true
  try {
    const res = await searchKnowledge({ query, k: 5 })
    searchResults.value = res.data || []
    if (searchResults.value.length === 0) {
      ElMessage.info('未找到匹配的知识片段')
    }
  } catch (e) {
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

async function handleLoadDir() {
  try {
    await ElMessageBox.confirm(
      '将批量加载 rag/knowledge/ 目录下所有 .md 文件到知识库，已存在的同名文档会产生重复，确定继续？',
      '批量加载确认',
      { type: 'warning', confirmButtonText: '确认加载', cancelButtonText: '取消' }
    )
    loadingDir.value = true
    const res = await loadKnowledgeDir()
    const data = res.data
    ElMessage.success(`加载完成：共 ${data.total_docs} 个文档，${data.total_chunks} 个切片`)
    await fetchDocs()
  } catch (e) {
    if (e !== 'cancel' && e?.toString() !== 'cancel') {
      ElMessage.error('加载目录失败')
    }
  } finally {
    loadingDir.value = false
  }
}

function openAddDialog() {
  isEdit.value = false
  editingDocId.value = null
  form.value = { title: '', content: '', source: '' }
  dialogVisible.value = true
}

async function openEditDialog(row) {
  isEdit.value = true
  editingDocId.value = row.docId
  form.value = { title: row.title, content: '', source: row.source }
  dialogVisible.value = true

  // 异步加载文档内容用于编辑回填
  try {
    const res = await getKnowledge(row.docId)
    form.value.content = res.data?.content || ''
    form.value.title = res.data?.title || row.title
  } catch (e) {
    ElMessage.warning('加载文档内容失败，请手动输入完整内容后保存')
  }
}

function resetForm() {
  form.value = { title: '', content: '', source: '' }
  editingDocId.value = null
  isEdit.value = false
}

async function handleSubmit() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入文档标题')
    return
  }
  if (!form.value.content.trim()) {
    ElMessage.warning('请输入文档内容')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateKnowledge(editingDocId.value, {
        title: form.value.title,
        content: form.value.content
      })
      ElMessage.success('文档更新成功')
    } else {
      await addKnowledge(form.value)
      ElMessage.success('文档添加成功')
    }
    dialogVisible.value = false
    await fetchDocs()
  } catch (e) {
    // 错误已由 request.js 拦截器处理
  } finally {
    submitting.value = false
  }
}

function renderMarkdown(text) {
  if (!text) return ''
  let html = text
    // 转义 HTML 特殊字符
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // 代码块 (```) - 必须在其他行内规则之前处理
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_, lang, code) => {
    const langClass = lang ? ` class="lang-${lang}"` : ''
    return `<pre><code${langClass}>${code.trim()}</code></pre>`
  })

  // 行内代码 (`code`)
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')

  // 加粗 **text** 或 __text__
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/__(.+?)__/g, '<strong>$1</strong>')

  // 斜体 *text* 或 _text_
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  html = html.replace(/_(.+?)_/g, '<em>$1</em>')

  // 标题 (## 到 ######)
  html = html.replace(/^###### (.+)$/gm, '<h6>$1</h6>')
  html = html.replace(/^##### (.+)$/gm, '<h5>$1</h5>')
  html = html.replace(/^#### (.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')

  // 无序列表 (-, *, +)
  html = html.replace(/^[\s]*[-*+] (.+)$/gm, '<li>$1</li>')

  // 有序列表 (1., 2., ...)
  html = html.replace(/^[\s]*\d+\. (.+)$/gm, '<li>$1</li>')

  // 水平分割线
  html = html.replace(/^---$/gm, '<hr>')

  // 段落 (连续两行换行分割)
  const lines = html.split('\n')
  let result = ''
  let inList = false
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const trimmed = line.trim()

    if (trimmed === '') {
      if (inList) {
        result += '</ul>'
        inList = false
      }
      continue
    }

    if (trimmed.startsWith('<h') || trimmed.startsWith('<pre') || trimmed.startsWith('<hr') || trimmed.startsWith('</pre')) {
      if (inList) {
        result += '</ul>'
        inList = false
      }
      result += trimmed + '\n'
      // 如果是 <pre> 则跳过直到 </pre>
      if (trimmed.startsWith('<pre')) {
        while (i + 1 < lines.length && !lines[i + 1].trim().startsWith('</pre>')) {
          i++
          result += lines[i] + '\n'
        }
        if (i + 1 < lines.length) {
          i++
          result += lines[i] + '\n'
        }
      }
      continue
    }

    if (trimmed.startsWith('<li')) {
      if (!inList) {
        result += '<ul>'
        inList = true
      }
      result += trimmed + '\n'
      continue
    }

    if (inList) {
      result += '</ul>'
      inList = false
    }

    result += `<p>${trimmed}</p>\n`
  }
  if (inList) {
    result += '</ul>'
  }

  return result
}

async function openViewDialog(row) {
  viewDoc.value = null
  renderedContent.value = ''
  viewDialogVisible.value = true
  loadingView.value = true

  // 先显示基本元信息
  viewDoc.value = { ...row }

  try {
    const res = await getKnowledge(row.docId)
    const data = res.data
    if (data) {
      viewDoc.value = {
        docId: data.docId || row.docId,
        title: data.title || row.title,
        source: data.source || row.source,
        chunkCount: data.chunk_count ?? row.chunkCount
      }
      renderedContent.value = renderMarkdown(data.content || '')
    }
  } catch (e) {
    renderedContent.value = '<p style="color: #e6a23c">加载文档内容失败</p>'
  } finally {
    loadingView.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除知识文档「${row.title}」吗？该操作将删除该文档的所有 ${row.chunkCount} 个切片。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await deleteKnowledge(row.docId)
    ElMessage.success('删除成功')
    await fetchDocs()
  } catch (e) {
    if (e !== 'cancel' && e?.toString() !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style scoped>
.search-result-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.search-result-score {
  font-size: 12px;
  color: #909399;
}

.search-result-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
  max-height: 120px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.view-content-card {
  max-height: 55vh;
  overflow-y: auto;
}

.view-content {
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  word-break: break-word;
}

.view-content h1,
.view-content h2,
.view-content h3,
.view-content h4,
.view-content h5,
.view-content h6 {
  margin: 16px 0 8px;
  font-weight: 600;
  color: #1a1a2e;
}

.view-content h1 { font-size: 22px; }
.view-content h2 { font-size: 19px; border-bottom: 1px solid #ebeef5; padding-bottom: 6px; }
.view-content h3 { font-size: 17px; }
.view-content h4 { font-size: 15px; }

.view-content p {
  margin: 8px 0;
}

.view-content pre {
  background: #282c34;
  color: #abb2bf;
  padding: 14px 16px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.5;
  margin: 12px 0;
}

.view-content code {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  background: #f5f7fa;
  color: #d63200;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 13px;
}

.view-content pre code {
  background: transparent;
  color: inherit;
  padding: 0;
  border-radius: 0;
  font-size: 13px;
}

.view-content ul,
.view-content ol {
  padding-left: 24px;
  margin: 8px 0;
}

.view-content li {
  margin: 4px 0;
}

.view-content hr {
  border: none;
  border-top: 1px solid #dcdfe6;
  margin: 16px 0;
}

.view-content blockquote {
  border-left: 4px solid #409eff;
  padding: 8px 16px;
  margin: 12px 0;
  background: #f5f7fa;
  color: #606266;
}
</style>
