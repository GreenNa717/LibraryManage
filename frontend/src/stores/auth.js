import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || null,
    username: localStorage.getItem('username') || '',
    realName: localStorage.getItem('realName') || '',
    nickname: localStorage.getItem('nickname') || '',
    email: localStorage.getItem('email') || '',
    avatar: localStorage.getItem('avatar') || '',
    role: localStorage.getItem('role') || null
  }),
  actions: {
    setUser(data) {
      this.token = data.token
      this.userId = data.userId
      this.username = data.username
      this.realName = data.realName
      this.nickname = data.nickname || this.nickname || ''
      this.email = data.email || this.email || ''
      this.avatar = data.avatar || this.avatar || ''
      this.role = data.role
      localStorage.setItem('token', data.token)
      localStorage.setItem('userId', data.userId)
      localStorage.setItem('username', data.username)
      localStorage.setItem('realName', data.realName)
      localStorage.setItem('nickname', this.nickname)
      localStorage.setItem('email', this.email)
      localStorage.setItem('avatar', this.avatar)
      localStorage.setItem('role', data.role)
    },
    updateProfile(data) {
      if (Object.prototype.hasOwnProperty.call(data, 'username') && data.username) {
        this.username = data.username
        localStorage.setItem('username', data.username)
      }
      if (Object.prototype.hasOwnProperty.call(data, 'realName')) {
        this.realName = data.realName || ''
        localStorage.setItem('realName', this.realName)
      }
      if (Object.prototype.hasOwnProperty.call(data, 'nickname')) {
        this.nickname = data.nickname || ''
        localStorage.setItem('nickname', this.nickname)
      }
      if (Object.prototype.hasOwnProperty.call(data, 'email')) {
        this.email = data.email || ''
        localStorage.setItem('email', this.email)
      }
      if (Object.prototype.hasOwnProperty.call(data, 'avatar')) {
        this.avatar = data.avatar || ''
        localStorage.setItem('avatar', this.avatar)
      }
    },
    logout() {
      this.token = ''
      this.userId = null
      this.username = ''
      this.realName = ''
      this.nickname = ''
      this.email = ''
      this.avatar = ''
      this.role = null
      localStorage.clear()
    }
  }
})
