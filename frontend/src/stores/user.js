import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(null)

  function setToken(t) {
    token.value = t
    localStorage.setItem('token', t)
  }

  function setUserId(id) {
    userId.value = id
  }

  function logout() {
    token.value = ''
    userId.value = null
    localStorage.removeItem('token')
  }

  return { token, userId, setToken, setUserId, logout }
})
