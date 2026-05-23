<template>
  <view class="bind-page mp-page">
    <view class="bind-card mp-card">
      <text class="bind-title">绑定账号</text>
      <text class="bind-desc">此功能即将开放，敬请期待。</text>
    </view>
  </view>
</template>

<script>
export default {
  onLoad() {
    uni.setNavigationBarTitle({ title: '绑定账号' })
  }
}
</script>

<style lang="scss">
@import '@/styles/tokens.scss';

.bind-card {
  text-align: center;
  padding: 80rpx 40rpx;
}

.bind-title {
  font-size: 34rpx;
  font-weight: 600;
  color: $mp-text-main;
  display: block;
  margin-bottom: 16rpx;
}

.bind-desc {
  font-size: 28rpx;
  color: $mp-text-muted;
}
</style>