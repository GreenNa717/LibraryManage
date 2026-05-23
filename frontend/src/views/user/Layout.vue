<template>
  <div class="user-shell">
    <div class="mobile-frame">
      <header class="user-header">
        <div class="brand" @click="router.push('/user/books')">
          <el-icon :size="22"><Reading /></el-icon>
          <span>图书馆</span>
        </div>
        <el-dropdown trigger="click" @command="handleCommand">
          <div class="user-trigger">
            <el-avatar :size="30" :src="authStore.avatar || ''">
              {{ displayName.slice(0, 1) }}
            </el-avatar>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人资料</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <main class="user-main">
        <router-view />
      </main>

      <nav class="bottom-nav">
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          :class="['nav-btn', { active: route.path === item.path }]"
          @click="router.push(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </nav>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Collection, Reading, Tickets, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { getAccountMe } from '../../api/user'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const displayName = computed(() => authStore.nickname || authStore.realName || authStore.username || '读者')
const navItems = [
  { path: '/user/books', label: '图书', icon: Collection },
  { path: '/user/borrows', label: '借阅', icon: Tickets },
  { path: '/user/profile', label: '我的', icon: User }
]

const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/user/profile')
    return
  }
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}

onMounted(async () => {
  try {
    const res = await getAccountMe()
    authStore.updateProfile(res.data || {})
  } catch {
    // 资料刷新失败不影响已登录用户继续使用页面。
  }
})
</script>

<style scoped>
.user-shell {
  min-height: 100vh;
  background: var(--app-bg);
  display: flex;
  justify-content: center;
}

.mobile-frame {
  width: 100%;
  max-width: 480px;
  min-height: 100vh;
  background: var(--app-bg);
  position: relative;
  box-shadow: 0 0 0 1px var(--app-border);
}

.user-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: var(--app-glass-bg);
  border-bottom: 1px solid var(--app-border);
  position: sticky;
  top: 0;
  z-index: 20;
  backdrop-filter: blur(var(--app-glass-blur));
  -webkit-backdrop-filter: blur(var(--app-glass-blur));
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: var(--app-text-primary);
  cursor: pointer;
  white-space: nowrap;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--app-text-primary);
  cursor: pointer;
  padding: 4px;
  border-radius: 8px;
}

.user-trigger:hover {
  background: var(--app-primary-soft);
}

.user-main {
  padding: 16px 14px 86px;
}

.bottom-nav {
  position: fixed;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  height: 66px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  background: var(--app-card-bg);
  border-top: 1px solid var(--app-border);
  box-shadow: 0 -8px 20px rgba(0, 0, 0, 0.08);
  z-index: 30;
}

.nav-btn {
  border: none;
  background: transparent;
  color: var(--app-text-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 12px;
  cursor: pointer;
}

.nav-btn .el-icon {
  font-size: 20px;
}

.nav-btn.active {
  color: var(--app-primary);
  font-weight: 600;
}

@media (min-width: 520px) {
  .mobile-frame {
    min-height: calc(100vh - 24px);
    margin: 12px 0;
    border-radius: 18px;
    overflow: hidden;
  }

  .bottom-nav {
    bottom: 12px;
    border-radius: 0 0 18px 18px;
  }
}
</style>
