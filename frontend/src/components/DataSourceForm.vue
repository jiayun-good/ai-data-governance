<template>
  <el-dialog :model-value="visible" :title="isEdit ? '编辑数据源' : '新增数据源'" width="500px"
    @update:model-value="$emit('update:visible', $event)" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="数据源名称" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="form.type" placeholder="选择类型" style="width: 100%">
          <el-option label="MySQL" value="MYSQL" />
        </el-select>
      </el-form-item>
      <el-form-item label="主机" prop="host">
        <el-input v-model="form.host" placeholder="127.0.0.1" />
      </el-form-item>
      <el-form-item label="端口" prop="port">
        <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
      </el-form-item>
      <el-form-item label="数据库名" prop="databaseName">
        <el-input v-model="form.databaseName" placeholder="数据库名称" />
      </el-form-item>
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="root" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" placeholder="密码" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'

const props = defineProps({
  visible: Boolean,
  editData: Object
})
const emit = defineEmits(['update:visible', 'submit'])

const formRef = ref()
const loading = ref(false)
const isEdit = ref(false)

const defaultForm = {
  name: '', type: 'MYSQL', host: '127.0.0.1', port: 3306,
  databaseName: '', username: '', password: ''
}

const form = reactive({ ...defaultForm })

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  host: [{ required: true, message: '请输入主机', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  databaseName: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

watch(() => props.visible, (val) => {
  if (val && props.editData) {
    Object.assign(form, props.editData)
    isEdit.value = true
  } else {
    isEdit.value = false
  }
})

function resetForm() {
  Object.assign(form, defaultForm)
  formRef.value?.resetFields()
  isEdit.value = false
}

async function handleSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    emit('submit', { ...form }, isEdit.value)
  } finally {
    loading.value = false
  }
}
</script>
