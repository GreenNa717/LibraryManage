<template>
  <div class="login-wrapper">
    <div class="login-bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <el-icon :size="36"><Reading /></el-icon>
        </div>
        <h1>图书馆管理系统</h1>
        <p>欢迎回来，请登录您的账号</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            :prefix-icon="User"
            size="large"
            class="login-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            size="large"
            class="login-input"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '../../api/admin'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (loading.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(form)
    authStore.setUser(res.data)
    ElMessage.success('登录成功')
    const role = Number(res.data.role)
    if ([0, 2].includes(role)) {
      router.push('/admin/dashboard')
    } else if (role === 1) {
      router.push('/user/books')
    } else {
      ElMessage.warning('游客账号暂无可访问页面')
      authStore.logout()
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-login-bg);
  overflow: hidden;
  position: relative;
}

.login-bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
}
.shape-1 {
  width: 600px; height: 600px;
  background: var(--app-login-shape-1);
  top: -200px; right: -150px;
  animation: float 8s ease-in-out infinite;
}
.shape-2 {
  width: 400px; height: 400px;
  background: var(--app-login-shape-2);
  bottom: -100px; left: -100px;
  animation: float 10s ease-in-out infinite reverse;
}
.shape-3 {
  width: 300px; height: 300px;
  background: var(--app-login-shape-3);
  top: 50%; left: 60%;
  animation: float 7s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-30px) scale(1.05); }
}

.login-card {
  background: var(--app-overlay);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  padding: 48px 40px;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.12);
  width: 420px;
  z-index: 1;
  animation: slideUp 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-logo {
  width: 72px; height: 72px;
  margin: 0 auto 20px;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--app-primary) 0%, var(--app-primary-600) 100%);
  color: var(--app-text-inverse);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.35);
  position: relative;
  transition: transform 0.3s ease;
}
.login-logo::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 22px;
  background: linear-gradient(135deg, var(--app-primary), var(--app-primary-600));
  opacity: 0.3;
  z-index: -1;
  filter: blur(12px);
  animation: pulseGlow 3s ease-in-out infinite;
}
@keyframes pulseGlow {
  0%, 100% { opacity: 0.25; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.05); }
}

.login-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: var(--app-text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.login-header p {
  font-size: 14px;
  color: var(--app-text-muted);
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  transition: all 0.25s;
  box-shadow: 0 0 0 1px var(--app-border);
}
.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--app-border-strong);
}
.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--app-primary), 0 0 0 4px var(--app-primary-soft);
}

.login-btn {
  width: 100%;
  height: 46px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(135deg, var(--app-primary) 0%, var(--app-primary-600) 100%);
  border: none;
  transition: all 0.3s;
}
.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px var(--app-primary-soft);
}
.login-btn:active {
  transform: translateY(0);
}
</style>
