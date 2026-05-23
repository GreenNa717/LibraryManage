import { useAuthStore } from '@/stores/auth'

export function requireLogin() {
  const auth = useAuthStore()
  if (!auth.initialized) auth.hydrate()
  if (!auth.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

export function requireAdmin() {
  const auth = useAuthStore()
  if (!auth.initialized) auth.hydrate()
  if (!auth.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  if (!auth.isAdmin) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

export function redirectToLogin() {
  uni.reLaunch({ url: '/pages/login/index' })
}

export function redirectToHome() {
  uni.switchTab({ url: '/pages/books/index' })
}

export function redirectToAdmin() {
  uni.reLaunch({ url: '/pages/admin/index' })
}
