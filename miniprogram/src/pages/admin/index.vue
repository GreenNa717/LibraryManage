<template>
  <view class="admin-page">
    <view class="admin-header mp-card">
      <view class="avatar-wrap">
        <view class="avatar-circle"><text class="avatar-letter">{{ avatarLetter }}</text></view>
      </view>
      <view class="admin-info">
        <text class="admin-name">{{ displayName }}</text>
        <text class="admin-role">{{ roleLabel }}</text>
      </view>
      <view class="logout-btn" @click="onLogout">
        <text class="mp-muted">退出</text>
      </view>
    </view>

    <view class="action-grid">
      <view class="action-card mp-card" @click="onScanInbound">
        <view class="action-icon action-icon--inbound">
          <text class="icon-text">📥</text>
        </view>
        <text class="action-title">扫码入库</text>
        <text class="action-desc">扫描ISBN条码，快速入库新书</text>
      </view>

      <view class="action-card mp-card" @click="onScanOutbound">
        <view class="action-icon action-icon--outbound">
          <text class="icon-text">📤</text>
        </view>
        <text class="action-title">扫码出库</text>
        <text class="action-desc">扫描副本码，标记丢失出库</text>
      </view>
    </view>

    <view class="quick-section mp-card">
      <text class="section-title">快捷操作</text>
      <view class="quick-list">
        <view class="quick-item" @click="onManualInbound">
          <text class="quick-icon">📝</text>
          <text class="quick-text">手动入库</text>
        </view>
        <view class="quick-item" @click="onManualOutbound">
          <text class="quick-icon">📋</text>
          <text class="quick-text">手动出库</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { useAuthStore } from '@/stores/auth'
import { requireAdmin } from '@/utils/guard'
import { scanCode, showManualInput } from '@/utils/scan'
import { scanInbound, updateCopyStatus, lookupCopy, lookupIsbn } from '@/api/admin'

export default {
  data() {
    return {}
  },
  computed: {
    avatarLetter() {
      const auth = useAuthStore()
      return auth.avatarText
    },
    displayName() {
      const auth = useAuthStore()
      return auth.displayName
    },
    roleLabel() {
      const auth = useAuthStore()
      if (!auth.user) return ''
      return auth.user.role === 0 ? '超级管理员' : '协管员'
    }
  },
  onLoad() {
    if (!requireAdmin()) return
  },
  onShow() {
    if (!requireAdmin()) return
  },
  methods: {
    onLogout() {
      uni.showModal({
        title: '确认退出',
        content: '退出后需要重新登录',
        success(res) {
          if (!res.confirm) return
          const auth = useAuthStore()
          auth.logout()
        }
      })
    },

    async onScanInbound() {
      try {
        const isbn = await scanCode()
        this.doInbound(isbn)
      } catch (err) {
        if (err && err.cancelled) return
        uni.showModal({
          title: '扫码失败',
          content: '是否手动输入ISBN？',
          success: async (res) => {
            if (!res.confirm) return
            try {
              const isbn = await showManualInput()
              this.doInbound(isbn)
            } catch (e) {}
          }
        })
      }
    },

    async onManualInbound() {
      try {
        const isbn = await showManualInput()
        this.doInbound(isbn)
      } catch (e) {}
    },

    async doInbound(isbn) {
      if (!isbn) return
      uni.showModal({
        title: '确认入库',
        content: 'ISBN：' + isbn + '\n\n将自动查询书籍信息并入库',
        success: async (res) => {
          if (!res.confirm) return
          uni.showLoading({ title: '入库中...' })
          try {
            let metadata = null
            try {
              metadata = await lookupIsbn(isbn)
            } catch (e) {}
            const result = await scanInbound({
              isbn,
              quantity: 1,
              title: metadata && metadata.title,
              author: metadata && metadata.author,
              coverImage: metadata && metadata.coverImage,
              description: metadata && metadata.description
            })
            uni.hideLoading()
            const title = (result && result.title) || (metadata && metadata.title) || '未知书名'
            uni.showModal({
              title: '入库成功',
              content: '书名：' + title + '\nISBN：' + isbn,
              showCancel: false
            })
          } catch (err) {
            uni.hideLoading()
            uni.showModal({
              title: '入库失败',
              content: (err && err.msg) || '请检查ISBN是否正确',
              showCancel: false
            })
          }
        }
      })
    },

    async onScanOutbound() {
      try {
        const code = await scanCode()
        this.doOutbound(code)
      } catch (err) {
        if (err && err.cancelled) return
        uni.showModal({
          title: '扫码失败',
          content: '是否手动输入副本码？',
          success: async (res) => {
            if (!res.confirm) return
            try {
              const code = await showManualInput()
              this.doOutbound(code)
            } catch (e) {}
          }
        })
      }
    },

    async onManualOutbound() {
      try {
        const code = await showManualInput()
        this.doOutbound(code)
      } catch (e) {}
    },

    async doOutbound(code) {
      if (!code) return
      uni.showLoading({ title: '查询中...' })
      try {
        const copyInfo = await lookupCopy(code)
        uni.hideLoading()
        if (!copyInfo || !copyInfo.copyId) {
          uni.showToast({ title: '未找到该副本', icon: 'none' })
          return
        }
        const bookTitle = copyInfo.bookTitle || copyInfo.title || '未知书名'
        const copyStatus = copyInfo.copyStatus
        if (copyStatus === 1) {
          uni.showModal({ title: '无法出库', content: '该副本当前已借出，请先归还', showCancel: false })
          return
        }
        if (copyStatus === 2) {
          uni.showModal({ title: '无法出库', content: '该副本已标记丢失', showCancel: false })
          return
        }
        uni.showModal({
          title: '确认出库',
          content: '副本码：' + code + '\n图书：' + bookTitle + '\n\n确认将此副本标记为丢失？',
          success: async (res) => {
            if (!res.confirm) return
            uni.showLoading({ title: '处理中...' })
            try {
              await updateCopyStatus(copyInfo.copyId, 2)
              uni.hideLoading()
              uni.showToast({ title: '出库成功', icon: 'success' })
            } catch (err) {
              uni.hideLoading()
              uni.showModal({ title: '出库失败', content: (err && err.msg) || '操作失败', showCancel: false })
            }
          }
        })
      } catch (err) {
        uni.hideLoading()
        uni.showToast({ title: '查询失败', icon: 'none' })
      }
    }
  }
}
</script>

<style lang="scss">
@import '@/styles/tokens.scss';

.admin-page {
  padding: 24rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: $mp-bg;
  min-height: 100vh;
}

.admin-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 28rpx 24rpx;
}

.avatar-wrap {
  margin-right: 20rpx;
}

.avatar-circle {
  width: 80rpx;
  height: 80rpx;
  border-radius: 40rpx;
  background: $mp-primary-soft;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-letter {
  font-size: 36rpx;
  font-weight: 700;
  color: $mp-primary;
}

.admin-info {
  flex: 1;
}

.admin-name {
  font-size: 32rpx;
  font-weight: 600;
  color: $mp-text-main;
  display: block;
  margin-bottom: 4rpx;
}

.admin-role {
  font-size: 24rpx;
  color: $mp-text-muted;
}

.logout-btn {
  padding: 8rpx 16rpx;
}

.action-grid {
  display: flex;
  flex-direction: row;
  gap: 20rpx;
  margin-top: 24rpx;
}

.action-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32rpx 16rpx;
}

.action-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;

  &--inbound {
    background: $mp-success-soft;
  }

  &--outbound {
    background: $mp-danger-soft;
  }
}

.icon-text {
  font-size: 40rpx;
}

.action-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $mp-text-main;
  margin-bottom: 8rpx;
}

.action-desc {
  font-size: 22rpx;
  color: $mp-text-muted;
  text-align: center;
}

.quick-section {
  margin-top: 24rpx;
  padding: 24rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $mp-text-main;
  margin-bottom: 20rpx;
  display: block;
}

.quick-list {
  display: flex;
  flex-direction: row;
  gap: 20rpx;
}

.quick-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24rpx;
  background: $mp-surface-soft;
  border-radius: 16rpx;
}

.quick-icon {
  font-size: 36rpx;
  margin-bottom: 8rpx;
}

.quick-text {
  font-size: 26rpx;
  color: $mp-text-sub;
}
</style>
