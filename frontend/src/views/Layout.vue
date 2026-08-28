<template>
  <el-container style="height: 100vh">
    <el-aside width="200px" style="background: #304156">
      <div style="color: #fff; text-align: center; padding: 20px 0; font-size: 16px; font-weight: bold">
        数据治理平台
      </div>
      <el-menu :default-active="route.path" router background-color="#304156" text-color="#bfcbd9"
        active-text-color="#409EFF">
        <el-menu-item index="/datasource">
          <el-icon><Monitor /></el-icon>
          <span>数据源管理</span>
        </el-menu-item>
        <el-menu-item index="/rules">
          <el-icon><Document /></el-icon>
          <span>规则管理</span>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Collection /></el-icon>
          <span>知识库管理</span>
        </el-menu-item>
        <el-menu-item index="/ai">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 助手</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header style="display: flex; align-items: center; justify-content: flex-end; border-bottom: 1px solid #eee; background: #fff">
        <el-button type="danger" text @click="handleLogout">退出登录</el-button>
      </el-header>
      <el-main style="background: #f5f7fa; overflow-y: auto">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { Monitor, Document, Collection, ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>
