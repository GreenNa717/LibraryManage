import { defineStore } from 'pinia'
import { login as loginApi } from '@/api/auth'
import { getMe } from '@/api/account'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: uni.getStorageSync('token') || '',
    user: null,
    initialized: false
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isReader: (state) => state.user && state.user.role === 1,
    isAdmin: (state) => state.user && (state.user.role === 0 || state.user.role === 2),
    displayName: (state) => {
      if (!state.user) return ''
      return state.user.nickname || state.user.realName || state.user.username || '?'
    },
    avatarText: (state) => {
      if (!state.user) return '?'
      const name = state.user.nickname || state.user.realName || state.user.username || '?'
      return name.slice(0, 1)
    }
  },

  actions: {
    hydrate() {
      if (this.token && !this.user) {
        try {
          const raw = uni.getStorageSync('userInfo')
          if (raw) this.user = typeof raw === 'string' ? JSON.parse(raw) : raw
        } catch (e) {
          this.user = null
        }
      }
      this.initialized = true
    },

    setToken(token) {
      this.token = token
      uni.setStorageSync('token', token)
    },

    setUser(user) {
      this.user = user
      uni.setStorageSync('userInfo', JSON.stringify(user))
    },

    async login(username, password) {
      const res = await loginApi({ username, password })
      if (res.role === 3) {
        throw { code: -1, msg: '游客账号暂无借阅权限' }
      }
      this.setToken(res.token)
      try {
        const me = await getMe()
        this.setUser(me)
      } catch (e) {
        this.setUser({
          id: res.userId,
          username: res.username,
          realName: res.realName,
          nickname: res.nickname,
          avatar: res.avatar,
          role: res.role
        })
      }
      return res.role
    },

    async fetchMe() {
      const me = await getMe()
      this.setUser(me)
      return me
    },

    logout() {
      this.token = ''
      this.user = null
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
      uni.reLaunch({ url: '/pages/login/index' })
    }
  }
})
