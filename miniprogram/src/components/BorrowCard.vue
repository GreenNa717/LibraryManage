<template>
  <view class="borrow-card mp-card">
    <image v-if="record.coverImage" class="borrow-cover" :src="record.coverImage" mode="aspectFill" />
    <view class="borrow-body">
      <view class="borrow-header">
        <text class="borrow-title">{{ record.bookTitle || '未知书名' }}</text>
        <text :class="['mp-tag', statusClass]">{{ statusLabel }}</text>
      </view>
      <text class="borrow-meta">{{ record.bookAuthor || '' }}</text>
      <text class="borrow-meta" @longpress="onCopyCode">副本码：{{ record.copyCode || '-' }}</text>
      <text class="borrow-time">借阅：{{ record.borrowTimeText }}</text>
      <text class="borrow-time">应还：{{ record.dueTimeText }}<text v-if="dueStateText" class="due-state"> · {{ dueStateText }}</text></text>
      <text v-if="record.returnTimeText" class="borrow-time">归还：{{ record.returnTimeText }}</text>
      <view v-if="record.status === 0" class="borrow-actions">
        <view class="mp-button mp-button--ghost borrow-btn" @click="$emit('return', record.id)">归还</view>
      </view>
    </view>
  </view>
</template>

<script>
import { statusText, formatDueState } from '@/utils/format'

export default {
  props: {
    record: { type: Object, required: true }
  },
  emits: ['return'],
  computed: {
    statusLabel() {
      return statusText(this.record.status)
    },
    statusClass() {
      const map = { 0: 'mp-tag--borrowing', 1: 'mp-tag--returned', 2: 'mp-tag--overdue' }
      return map[this.record.status] || ''
    },
    dueStateText() {
      return formatDueState(this.record.dueTime, this.record.status)
    }
  },
  methods: {
    onCopyCode() {
      if (!this.record.copyCode) return
      uni.setClipboardData({
        data: this.record.copyCode,
        success() { uni.showToast({ title: '已复制副本码', icon: 'success' }) }
      })
    }
  }
}
</script>

<style lang="scss">
@import '@/styles/tokens.scss';

.borrow-card {
  display: flex;
  flex-direction: row;
}

.borrow-cover {
  width: 100rpx;
  height: 140rpx;
  border-radius: 12rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
  background: $mp-surface-soft;
}

.borrow-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.borrow-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  margin-bottom: 8rpx;
}

.borrow-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $mp-text-main;
  margin-right: 12rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.borrow-meta {
  font-size: 24rpx;
  color: $mp-text-muted;
  margin-bottom: 4rpx;
}

.borrow-time {
  font-size: 24rpx;
  color: $mp-text-sub;
  margin-bottom: 4rpx;
}

.due-state {
  color: $mp-danger;
  font-weight: 600;
}

.borrow-actions {
  margin-top: 12rpx;
}

.borrow-btn {
  height: 60rpx;
  font-size: 26rpx;
  width: 140rpx;
}
</style>
