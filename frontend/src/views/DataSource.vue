<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: bold">数据源管理</span>
          <el-button type="primary" @click="openAdd">新增数据源</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="host" label="主机" width="140" />
        <el-table-column prop="port" label="端口" width="80" />
        <el-table-column prop="databaseName" label="数据库" min-width="120" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="success" :loading="row._testing" @click="handleTest(row)">
              测试
            </el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <DataSourceForm v-model:visible="dialogVisible" :edit-data="editItem" @submit="handleSubmit" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listDataSources, addDataSource, updateDataSource, deleteDataSource, testConnection } from '../api/datasource'
import DataSourceForm from '../components/DataSourceForm.vue'

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editItem = ref(null)

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await listDataSources()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editItem.value = null
  dialogVisible.value = true
}

function openEdit(row) {
  editItem.value = { ...row }
  dialogVisible.value = true
}

async function handleSubmit(form, isEdit) {
  try {
    if (isEdit) {
      await updateDataSource(editItem.value.id, form)
      ElMessage.success('修改成功')
    } else {
      await addDataSource(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) { /* handled by interceptor */ }
}

async function handleTest(row) {
  row._testing = true
  try {
    await testConnection(row.id)
    ElMessage.success('连接成功')
  } finally {
    row._testing = false
  }
}

async function handleDelete(row) {
  try {
    await deleteDataSource(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* handled */ }
}
</script>
