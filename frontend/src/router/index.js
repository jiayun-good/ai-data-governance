import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/datasource',
    children: [
      {
        path: 'datasource',
        name: 'DataSource',
        component: () => import('../views/DataSource.vue'),
        meta: { title: '数据源管理' }
      },
      {
        path: 'rules',
        name: 'Rules',
        component: () => import('../views/Rules.vue'),
        meta: { title: '规则管理' }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('../views/Knowledge.vue'),
        meta: { title: '知识库管理' }
      },
      {
        path: 'ai',
        name: 'AiAssistant',
        component: () => import('../views/AiAssistant.vue'),
        meta: { title: 'AI 助手' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
