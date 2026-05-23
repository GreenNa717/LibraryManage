<template>
  <el-container class="layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="aside-header" @click="isCollapse = !isCollapse">
        <div class="aside-logo">
          <el-icon :size="26"><Reading /></el-icon>
        </div>
        <transition name="fade">
          <span v-show="!isCollapse" class="aside-title">图书馆管理</span>
        </transition>
      </div>
      <el-menu
        :default-active="menuActivePath"
        :collapse="isCollapse"
        router
        class="aside-menu"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>数据看板</template>
        </el-menu-item>
        <el-menu-item index="/admin/books">
          <el-icon><Notebook /></el-icon>
          <template #title>图书管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><Collection /></el-icon>
          <template #title>分类管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/shelves">
          <el-icon><Location /></el-icon>
          <template #title>库位管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><UserFilled /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/borrows">
          <el-icon><Tickets /></el-icon>
          <template #title>借阅管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/scan-inbound">
          <el-icon><Search /></el-icon>
          <template #title>扫码入库</template>
        </el-menu-item>
        <el-menu-item index="/admin/isbn-metadata">
          <el-icon><Collection /></el-icon>
          <template #title>ISBN 资料库</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container class="layout-body">
      <!-- 顶栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="20" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleUserMenuCommand">
            <div class="user-menu-trigger">
              <div class="user-avatar">
                <el-avatar :size="32" :src="authStore.avatar || ''">
                  <el-icon :size="18"><UserFilled /></el-icon>
                </el-avatar>
              </div>
              <span class="user-name">{{ displayName }}</span>
              <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  管理员资料
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>
                  设置
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 标签页 -->
      <div class="layout-tabs">
        <div
          v-for="tab in tabs"
          :key="tab.path"
          :class="['tab-item', { active: tab.path === $route.path }]"
          @click="router.push(tab.fullPath || tab.path)"
        >
          <span>{{ tab.title }}</span>
          <el-icon v-if="tabs.length > 1" class="tab-close" @click.stop="closeTab(tab)">
            <Close />
          </el-icon>
        </div>
      </div>

      <!-- 内容区 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component, route: viewRoute }">
          <component v-if="Component" :is="Component" :key="viewRoute.path" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import {
  DataAnalysis, Notebook, Collection, UserFilled, Tickets,
  Fold, Expand, SwitchButton, Close, Reading, Search, User, Setting, ArrowDown, Location
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const isCollapse = ref(false)

const tabs = ref([
  { path: '/admin/dashboard', fullPath: '/admin/dashboard', title: '数据看板' }
])

const menuActivePath = computed(() => {
  if (route.path.startsWith('/admin/books/')) return '/admin/books'
  if (route.path.startsWith('/admin/users/')) return '/admin/users'
  return route.path
})

const currentTitle = computed(() => {
  if (route.path.startsWith('/admin/books/')) return '图书详情'
  if (route.path.startsWith('/admin/users/')) return '用户详情'
  const map = {
    '/admin/dashboard': '数据看板',
    '/admin/books': '图书管理',
    '/admin/categories': '分类管理',
    '/admin/shelves': '库位管理',
    '/admin/users': '用户管理',
    '/admin/borrows': '借阅管理',
    '/admin/scan-inbound': '扫码入库',
    '/admin/isbn-metadata': 'ISBN 资料库',
    '/admin/profile': '管理员资料',
    '/admin/settings': '设置'
  }
  return map[route.path] || ''
})

const displayName = computed(() => authStore.nickname || authStore.realName || authStore.username)

watch(() => route.fullPath, () => {
  const title = currentTitle.value
  if (!title) return
  const existing = tabs.value.find(t => t.path === route.path)
  if (existing) {
    existing.fullPath = route.fullPath
    existing.title = title
  } else {
    tabs.value.push({ path: route.path, fullPath: route.fullPath, title })
  }
}, { immediate: true })

const closeTab = (tab) => {
  const idx = tabs.value.indexOf(tab)
  tabs.value.splice(idx, 1)
  if (tab.path === route.path) {
    const next = tabs.value[idx] || tabs.value[tabs.value.length - 1]
    if (next) router.push(next.fullPath || next.path)
  }
}

const handleUserMenuCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
    return
  }
  if (command === 'profile') {
    router.push('/admin/profile')
    return
  }
  if (command === 'settings') {
    router.push('/admin/settings')
  }
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100vh; }

.layout-aside {
  background: linear-gradient(180deg, var(--app-aside-bg-start) 0%, var(--app-aside-bg-end) 100%);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  box-shadow: var(--app-aside-shadow);
  position: relative;
  z-index: 20;
}

.aside-header {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  border-bottom: 1px solid var(--app-aside-border);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.aside-header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 16px;
  right: 16px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--app-aside-border), transparent);
}

.aside-logo {
  width: 36px; height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--app-aside-logo-start) 0%, var(--app-aside-logo-end) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-text-inverse);
  flex-shrink: 0;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  position: relative;
}
.aside-logo::after {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--app-aside-logo-start), var(--app-aside-logo-end));
  opacity: 0;
  z-index: -1;
  transition: opacity 0.3s ease;
  filter: blur(8px);
}
.aside-header:hover .aside-logo {
  transform: scale(1.08);
}
.aside-header:hover .aside-logo::after {
  opacity: 0.4;
}

.aside-title {
  color: var(--app-aside-text);
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: 1px;
}

.aside-menu {
  border-right: none;
  background: transparent;
  padding: 8px 0;
}
.aside-menu :deep(.el-menu-item) {
  color: var(--app-aside-text-muted);
  margin: 3px 10px;
  border-radius: 10px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  height: 44px;
  line-height: 44px;
  position: relative;
  overflow: hidden;
}
.aside-menu :deep(.el-menu-item::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  border-radius: 2px;
  background: var(--app-aside-item-active-border);
  transition: height 0.25s ease;
}
.aside-menu :deep(.el-menu-item:hover) {
  color: var(--app-aside-text-hover);
  background: var(--app-aside-item-hover);
}
.aside-menu :deep(.el-menu-item:hover::before) {
  height: 20px;
}
.aside-menu :deep(.el-menu-item.is-active) {
  color: var(--app-aside-text);
  background: var(--app-aside-item-active);
  border-left: none;
}
.aside-menu :deep(.el-menu-item.is-active::before) {
  height: 24px;
}

/* --- body --- */
.layout-body { flex-direction: column; }

.layout-header {
  height: 56px;
  background: var(--app-glass-bg);
  backdrop-filter: blur(var(--app-glass-blur));
  -webkit-backdrop-filter: blur(var(--app-glass-blur));
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: var(--app-header-shadow);
  z-index: 10;
  border-bottom: 1px solid var(--app-glass-border);
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: var(--app-text-secondary);
  transition: color 0.2s, transform 0.2s;
  border-radius: 6px;
  padding: 4px;
}
.collapse-btn:hover { color: var(--app-primary); transform: scale(1.1); }

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  transition: box-shadow 0.3s ease;
}
.user-avatar:hover {
  box-shadow: 0 0 12px var(--app-glow-primary);
}

.user-name { font-size: 14px; color: var(--app-text-primary); font-weight: 500; }

.user-menu-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px;
  border-radius: 999px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

.user-menu-trigger:hover {
  background: var(--app-primary-soft);
  border-color: var(--app-border);
}

.arrow-icon {
  font-size: 12px;
  color: var(--app-text-secondary);
}

/* --- 标签页 --- */
.layout-tabs {
  height: 42px;
  background: var(--app-glass-bg);
  backdrop-filter: blur(var(--app-glass-blur));
  -webkit-backdrop-filter: blur(var(--app-glass-blur));
  display: flex;
  align-items: center;
  padding: 0 14px;
  gap: 6px;
  border-bottom: 1px solid var(--app-border);
  overflow-x: auto;
}

.tab-item {
  padding: 5px 14px 5px 16px;
  font-size: 13px;
  color: var(--app-text-secondary);
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--app-tab-item-bg);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
  position: relative;
}
.tab-item:hover {
  color: var(--app-tab-item-hover-text);
  background: var(--app-tab-item-hover-bg);
  transform: translateY(-1px);
}
.tab-item.active {
  color: var(--app-primary);
  background: var(--app-tab-item-active-bg);
  border-color: var(--app-tab-item-active-border);
  font-weight: 500;
  box-shadow: 0 2px 8px var(--app-glow-primary);
}

.tab-close {
  font-size: 12px;
  border-radius: 50%;
  padding: 2px;
  transition: all 0.2s;
}
.tab-close:hover { background: var(--app-primary-soft); color: var(--app-danger); }

/* --- 主内容 --- */
.layout-main {
  background: var(--app-bg);
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
