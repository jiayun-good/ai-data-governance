<template>
  <div>
    <!-- 顶部筛选 -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-select v-model="datasourceId" placeholder="选择数据源" style="width: 100%" @change="onDatasourceChange">
            <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="tableName" placeholder="选择表" style="width: 100%" :disabled="!datasourceId"
            @change="onTableChange">
            <el-option v-for="t in tables" :key="t" :label="t" :value="t" />
          </el-select>
        </el-col>
        <el-col :span="12" style="text-align: right">
          <el-button type="primary" :disabled="!tableName" @click="openCreateDialog">手动创建规则</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 规则列表 -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <template #header>
        <span style="font-weight: bold">{{ tableName ? `${tableName} - 规则列表` : '规则列表' }}</span>
      </template>
      <el-table :data="rules" v-loading="rulesLoading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="ruleName" label="规则名称" min-width="180" />
        <el-table-column prop="ruleType" label="规则类型" width="140" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :loading="row._checking" @click="handleCheck(row)">
              执行
            </el-button>
            <el-button size="small" @click="viewRuleHistory(row)">历史</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 执行结果弹窗 -->
    <el-dialog v-model="checkResultVisible" title="执行结果" width="700px">
      <template v-if="checkResult">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="规则名称">{{ checkResult.ruleName }}</el-descriptions-item>
          <el-descriptions-item label="规则ID">{{ checkResult.ruleId }}</el-descriptions-item>
          <el-descriptions-item label="表名">{{ checkResult.tableName }}</el-descriptions-item>
          <el-descriptions-item label="字段">{{ checkResult.columnName }}</el-descriptions-item>
          <el-descriptions-item label="总数据量">{{ checkResult.total }}</el-descriptions-item>
          <el-descriptions-item label="通过数量">
            <span style="color: #67c23a">{{ checkResult.successCount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="异常数量">
            <span style="color: #f56c6c">{{ checkResult.errorCount }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="checkResult.errorData?.length" style="margin-top: 16px">
          <div style="font-weight: bold; margin-bottom: 8px">异常数据（前 20 条）</div>
          <el-table :data="checkResult.errorData" stripe size="small" max-height="300">
            <el-table-column v-for="key in Object.keys(checkResult.errorData[0] || {})" :key="key" :prop="key"
              :label="key" min-width="120" show-overflow-tooltip />
          </el-table>
        </div>
      </template>
    </el-dialog>

    <!-- 检查历史弹窗 -->
    <el-dialog v-model="historyVisible" :title="historyTitle" width="900px">
      <el-table :data="historyList" v-loading="historyLoading" stripe>
        <el-table-column prop="ruleName" label="规则名称" min-width="140" />
        <el-table-column prop="tableName" label="表名" width="120" />
        <el-table-column prop="columnName" label="字段" width="120" />
        <el-table-column prop="totalCount" label="总量" width="80" />
        <el-table-column prop="successCount" label="通过" width="80" />
        <el-table-column prop="errorCount" label="异常" width="80" />
        <el-table-column prop="status" label="状态" width="80" />
        <el-table-column prop="errorMessage" label="错误信息" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createTime" label="执行时间" width="170" />
      </el-table>
      <div style="text-align: right; margin-top: 16px">
        <el-pagination v-model:current-page="historyPage" :page-size="historySize" :total="historyTotal"
          layout="prev, pager, next" @current-change="fetchHistory" />
      </div>
    </el-dialog>

    <!-- 创建规则弹窗 -->
    <el-dialog v-model="createVisible" title="创建规则" width="550px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="数据源" prop="datasourceId">
          <el-select v-model="createForm.datasourceId" placeholder="选择数据源" style="width: 100%"
            @change="onCreateDsChange">
            <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="表名" prop="tableName">
          <el-select v-model="createForm.tableName" placeholder="选择表" style="width: 100%"
            @change="onCreateTableChange">
            <el-option v-for="t in createTables" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="字段" prop="columnName">
          <el-select v-model="createForm.columnName" placeholder="选择字段" style="width: 100%">
            <el-option v-for="c in createColumns" :key="c.columnName" :label="`${c.columnName} (${c.dataType})`"
              :value="c.columnName" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="createForm.ruleType" placeholder="选择规则类型" style="width: 100%">
            <el-option label="非空检查 (NOT_NULL)" value="NOT_NULL" />
            <el-option label="唯一性 (UNIQUE)" value="UNIQUE" />
            <el-option label="长度检查 (LENGTH)" value="LENGTH" />
            <el-option label="范围检查 (RANGE)" value="RANGE" />
            <el-option label="正则检查 (REGEX)" value="REGEX" />
            <el-option label="枚举检查 (ENUM)" value="ENUM" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="createForm.ruleName" placeholder="如：用户姓名不能为空" />
        </el-form-item>
        <el-form-item label="规则配置" prop="ruleConfig">
          <el-input v-model="createForm.ruleConfig" type="textarea" :rows="3"
            placeholder='JSON格式，如：{"minLength": 1, "maxLength": 50}' />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="createForm.status" :active-value="1" :inactive-value="0" active-text="启用"
            inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listDataSources, getTables, getColumns } from '../api/datasource'
import { listRules, createRule, checkRule, listCheckHistory, listRuleCheckHistory } from '../api/rule'

// ---- 数据源 & 表 ----
const datasources = ref([])
const datasourceId = ref(null)
const tables = ref([])
const tableName = ref('')

// ---- 规则列表 ----
const rules = ref([])
const rulesLoading = ref(false)

// ---- 执行结果 ----
const checkResultVisible = ref(false)
const checkResult = ref(null)

// ---- 检查历史 ----
const historyVisible = ref(false)
const historyTitle = ref('检查历史')
const historyList = ref([])
const historyLoading = ref(false)
const historyPage = ref(1)
const historySize = 10
const historyTotal = ref(0)
let historyRuleId = null

// ---- 创建规则 ----
const createVisible = ref(false)
const createFormRef = ref()
const createLoading = ref(false)
const createTables = ref([])
const createColumns = ref([])

const createForm = reactive({
  datasourceId: null,
  tableName: '',
  columnName: '',
  ruleType: '',
  ruleName: '',
  ruleConfig: '',
  status: 1
})

const createRules = {
  datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  tableName: [{ required: true, message: '请选择表', trigger: 'change' }],
  columnName: [{ required: true, message: '请选择字段', trigger: 'change' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }]
}

onMounted(async () => {
  const res = await listDataSources()
  datasources.value = res.data || []
})

// ---- 顶部筛选联动 ----
async function onDatasourceChange(id) {
  tableName.value = ''
  tables.value = []
  rules.value = []
  if (!id) return
  const res = await getTables(id)
  tables.value = res.data || []
}

async function onTableChange(t) {
  rules.value = []
  if (!t) return
  await fetchRules()
}

async function fetchRules() {
  rulesLoading.value = true
  try {
    const res = await listRules(tableName.value)
    rules.value = (res.data || []).map(r => ({ ...r, _checking: false }))
  } finally {
    rulesLoading.value = false
  }
}

// ---- 执行规则 ----
async function handleCheck(row) {
  row._checking = true
  try {
    const res = await checkRule(row.id)
    checkResult.value = res.data
    checkResultVisible.value = true
  } finally {
    row._checking = false
  }
}

// ---- 查看历史 ----
function viewRuleHistory(row) {
  historyRuleId = row.id
  historyTitle.value = `${row.ruleName} - 检查历史`
  historyPage.value = 1
  historyVisible.value = true
  fetchHistory()
}

async function fetchHistory() {
  historyLoading.value = true
  try {
    const res = historyRuleId
      ? await listRuleCheckHistory(historyRuleId, { page: historyPage.value, size: historySize })
      : await listCheckHistory({ page: historyPage.value, size: historySize })
    const page = res.data
    historyList.value = page?.records || []
    historyTotal.value = page?.total || 0
  } finally {
    historyLoading.value = false
  }
}

// ---- 创建规则 ----
function openCreateDialog() {
  createForm.datasourceId = datasourceId.value
  createForm.tableName = tableName.value
  createForm.columnName = ''
  createForm.ruleType = ''
  createForm.ruleName = ''
  createForm.ruleConfig = ''
  createForm.status = 1
  createTables.value = []
  createColumns.value = []
  if (datasourceId.value) onCreateDsChange(datasourceId.value)
  createVisible.value = true
}

async function onCreateDsChange(id) {
  createForm.tableName = ''
  createForm.columnName = ''
  createTables.value = []
  createColumns.value = []
  if (!id) return
  const res = await getTables(id)
  createTables.value = res.data || []
}

async function onCreateTableChange(t) {
  createForm.columnName = ''
  createColumns.value = []
  if (!t) return
  const res = await getColumns(createForm.datasourceId, t)
  createColumns.value = res.data || []
}

async function handleCreate() {
  await createFormRef.value.validate()
  createLoading.value = true
  try {
    await createRule(createForm)
    ElMessage.success('创建成功')
    createVisible.value = false
    if (tableName.value) fetchRules()
  } finally {
    createLoading.value = false
  }
}
</script>
