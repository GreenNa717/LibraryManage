<template>
  <view class="login-page">
    <view class="brand-panel">
      <text class="brand-icon">📖</text>
      <text class="brand-title">图书馆管理系统</text>
      <text class="brand-sub">扫码借阅 · 入库管理 · 快速归还</text>
    </view>
    <view class="login-card mp-card">
      <view class="form-field">
        <text class="field-label">用户名</text>
        <input class="mp-input" v-model="username" placeholder="请输入用户名" />
      </view>
      <view class="form-field">
        <text class="field-label">密码</text>
        <input class="mp-input" type="safe-password" v-model="password" placeholder="请输入密码" />
      </view>
      <button class="mp-button mp-button--primary login-btn" :disabled="loading" @click="onLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </view>
    <view v-if="showDemoHint" class="demo-hint">
      <text class="mp-muted">读者：reader001 / user123</text>
      <text class="mp-muted">管理员：admin / admin123</text>
    </view>
  </view>
</template>

<script>
import { useAuthStore } from '@/stores/auth'
import env from '@/config/env'

export default {
  data() {
    return {
      username: '',
      password: '',
      loading: false,
      showDemoHint: env.env !== 'prod'
    }
  },
  onLoad() {
    const auth = useAuthStore()
    auth.hydrate()
    if (auth.isLoggedIn) {
      if (auth.isAdmin) {
        uni.reLaunch({ url: '/pages/admin/index' })
      } else {
        uni.switchTab({ url: '/pages/books/index' })
      }
    }
  },
  methods: {
    async onLogin() {
      if (this.loading) return
      if (!this.username.trim()) {
        uni.showToast({ title: '请输入用户名', icon: 'none' })
        return
      }
      if (!this.password.trim()) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return
      }
      this.loading = true
      try {
        const auth = useAuthStore()
        const role = await auth.login(this.username.trim(), this.password)
        if (role === 0 || role === 2) {
          uni.reLaunch({ url: '/pages/admin/index' })
        } else {
          uni.switchTab({ url: '/pages/books/index' })
        }
      } catch (err) {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss">
@import '@/styles/tokens.scss';

.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #eaf3ff 0%, #f4f7fb 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 48rpx;
}

.brand-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 48rpx;
}

.brand-icon {
  font-size: 96rpx;
  margin-bottom: 16rpx;
}

.brand-title {
  font-size: 40rpx;
  font-weight: 700;
  color: $mp-primary;
  margin-bottom: 8rpx;
}

.brand-sub {
  font-size: 26rpx;
  color: $mp-text-sub;
}

.login-card {
  width: 100%;
}

.form-field {
  margin-bottom: 24rpx;
}

.field-label {
  font-size: 26rpx;
  color: $mp-text-sub;
  margin-bottom: 8rpx;
  display: block;
}

.login-btn {
  margin-top: 16rpx;
  width: 100%;
}

.demo-hint {
  margin-top: 32rpx;
  text-align: center;
}
</style>
