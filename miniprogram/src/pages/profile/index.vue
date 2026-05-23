<template>
  <view class="profile-page">
    <view v-if="loading" class="skeleton-wrap">
      <view class="mp-skeleton" style="height:200rpx;margin-bottom:20rpx;"></view>
      <view class="mp-skeleton" style="height:400rpx;"></view>
    </view>

    <view v-else-if="error" class="error-wrap mp-card">
      <text class="mp-muted">{{ error }}</text>
      <view class="mp-button mp-button--primary mp-btn-inline mp-error-wrap-btn" @click="onRetry">重试</view>
      <view class="mp-button mp-button--ghost mp-btn-inline mp-error-wrap-btn" @click="onLogout">退出登录</view>
    </view>

    <view v-else>
      <view class="profile-header mp-card">
        <view class="avatar-wrap">
          <image v-if="user && user.avatar" class="avatar-img" :src="avatarDataUrl" mode="aspectFill" />
          <view v-else class="avatar-text"><text>{{ avatarLetter }}</text></view>
        </view>
        <view class="user-info">
          <text class="user-name">{{ displayName }}</text>
          <text class="user-role">读者</text>
        </view>
      </view>

      <view class="info-card mp-card">
        <view class="info-row">
          <text class="info-label">用户名</text>
          <text class="info-value">{{ user.username || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">姓名</text>
          <text class="info-value">{{ user.realName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">昵称</text>
          <text class="info-value">{{ user.nickname || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">邮箱</text>
          <text class="info-value">{{ user.email || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">手机</text>
          <text class="info-value">{{ user.phone || '-' }}</text>
        </view>
      </view>

      <view class="actions-card mp-card">
        <view class="action-row" @click="onEditProfile">
          <text>编辑资料</text>
          <text class="action-arrow">›</text>
        </view>
      </view>

      <view v-if="editing" class="edit-card mp-card">
        <view class="form-field">
          <text class="field-label">姓名</text>
          <input class="mp-input" v-model="editForm.realName" placeholder="请输入姓名" />
        </view>
        <view class="form-field">
          <text class="field-label">昵称</text>
          <input class="mp-input" v-model="editForm.nickname" placeholder="请输入昵称" />
        </view>
        <view class="form-field">
          <text class="field-label">邮箱</text>
          <input class="mp-input" v-model="editForm.email" placeholder="请输入邮箱" />
        </view>
        <view class="form-field">
          <text class="field-label">手机号</text>
          <input class="mp-input" v-model="editForm.phone" placeholder="请输入手机号" />
        </view>
        <view class="edit-actions">
          <view class="mp-button mp-button--ghost edit-btn" @click="editing = false">取消</view>
          <view :class="['mp-button mp-button--primary edit-btn', saving ? 'mp-button--disabled' : '']" @click="saveProfile">{{ saving ? '保存中...' : '保存' }}</view>
        </view>
      </view>

      <view class="logout-section">
        <view class="mp-button mp-button--danger mp-btn-full" @click="onLogout">退出登录</view>
      </view>
    </view>
  </view>
</template>

<script>
import { updateProfile } from '@/api/account'
import { useAuthStore } from '@/stores/auth'
import { requireLogin } from '@/utils/guard'

export default {
  data() {
    return {
      loading: false,
      saving: false,
      editing: false,
      error: '',
      editForm: {
        realName: '',
        nickname: '',
        email: '',
        phone: ''
      }
    }
  },
  computed: {
    user() {
      const auth = useAuthStore()
      return auth.user || {}
    },
    displayName() {
      const auth = useAuthStore()
      return auth.displayName
    },
    avatarLetter() {
      const auth = useAuthStore()
      return auth.avatarText
    },
    avatarDataUrl() {
      if (!this.user || !this.user.avatar) return ''
      const a = this.user.avatar
      if (a.startsWith('data:') || a.startsWith('http://') || a.startsWith('https://') || a.startsWith('/')) return a
      return 'data:image/png;base64,' + a
    }
  },
  onLoad() {
    if (!requireLogin()) return
  },
  onShow() {
    if (!requireLogin()) return
    this.loadProfile()
  },
  methods: {
    async loadProfile() {
      this.loading = true
      this.error = ''
      try {
        const auth = useAuthStore()
        await auth.fetchMe()
        this.loading = false
      } catch (err) {
        this.loading = false
        this.error = err.msg || '加载失败'
      }
    },
    onRetry() {
      this.loadProfile()
    },
    onEditProfile() {
      this.editForm.realName = this.user.realName || ''
      this.editForm.nickname = this.user.nickname || ''
      this.editForm.email = this.user.email || ''
      this.editForm.phone = this.user.phone || ''
      this.editing = true
    },
    async saveProfile() {
      if (this.saving) return
      const payload = {
        realName: this.editForm.realName.trim(),
        nickname: this.editForm.nickname.trim(),
        email: this.editForm.email.trim(),
        phone: this.editForm.phone.trim(),
        avatar: this.user.avatar || ''
      }
      if (!payload.realName) {
        uni.showToast({ title: '姓名不能为空', icon: 'none' })
        return
      }
      if (payload.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email)) {
        uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
        return
      }
      if (payload.phone && !/^\d{11}$/.test(payload.phone)) {
        uni.showToast({ title: '手机号格式不正确', icon: 'none' })
        return
      }
      this.saving = true
      try {
        const auth = useAuthStore()
        const user = await updateProfile(payload)
        auth.setUser(user || { ...this.user, ...payload })
        this.editing = false
        uni.showToast({ title: '资料已保存', icon: 'success' })
      } finally {
        this.saving = false
      }
    },
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
    }
  }
}
</script>

<style lang="scss">
@import '@/styles/tokens.scss';

.profile-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 32rpx 24rpx;
}

.avatar-wrap {
  margin-right: 28rpx;
}

.avatar-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  background: $mp-surface-soft;
}

.avatar-text {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  background: $mp-primary-soft;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  font-weight: 700;
  color: $mp-primary;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 34rpx;
  font-weight: 600;
  color: $mp-text-main;
  display: block;
  margin-bottom: 8rpx;
}

.user-role {
  font-size: 26rpx;
  color: $mp-text-muted;
}

.info-card {
  padding: 8rpx 24rpx;
}

.info-row {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid $mp-border;

  &:last-child {
    border-bottom: none;
  }
}

.info-label {
  font-size: 28rpx;
  color: $mp-text-muted;
}

.info-value {
  font-size: 28rpx;
  color: $mp-text-main;
}

.actions-card {
  padding: 0 24rpx;
}

.action-row {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid $mp-border;
  font-size: 28rpx;
  color: $mp-text-main;

  &:last-child {
    border-bottom: none;
  }
}

.action-arrow {
  color: $mp-text-muted;
  font-size: 28rpx;
}

.edit-card {
  padding: 24rpx;
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

.edit-actions {
  display: flex;
  gap: 16rpx;
}

.edit-btn {
  flex: 1;
}

.logout-section {
  margin-top: 40rpx;
  padding: 0 24rpx;
}

.error-wrap {
  text-align: center;
  padding: 60rpx 40rpx;
  margin: 24rpx;
}

.skeleton-wrap {
  padding: 24rpx;
}
</style>