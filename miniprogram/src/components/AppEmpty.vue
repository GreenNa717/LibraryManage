<template>
  <view class="empty-state">
    <text class="empty-icon">📚</text>
    <text class="empty-msg">{{ message }}</text>
    <view v-if="$slots.action" class="empty-action"><slot name="action" /></view>
  </view>
</template>

<script>
export default {
  props: {
    message: { type: String, default: '暂无数据' }
  }
}
</script>

<style lang="scss">
@import '@/styles/tokens.scss';

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 24rpx;
}

.empty-msg {
  font-size: 28rpx;
  color: $mp-text-muted;
  margin-bottom: 24rpx;
}

.empty-action {
  margin-top: 8rpx;
}
</style>
