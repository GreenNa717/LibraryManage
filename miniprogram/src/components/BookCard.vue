<template>
  <view class="book-card mp-card" hover-class="mp-card--pressed" @click="$emit('tap')">
    <image v-if="book.coverImage" class="book-cover" :src="book.coverImage" mode="aspectFill" />
    <view v-else class="book-cover book-cover--empty">
      <text class="book-cover-text">无封面</text>
    </view>
    <view class="book-info">
      <text class="book-title">{{ book.title }}</text>
      <text class="book-author">{{ book.author || '未知作者' }}</text>
      <view class="book-stock">
        <text :class="['stock-badge', book.currentStock > 0 ? 'stock-badge--ok' : 'stock-badge--empty']">
          {{ book.currentStock > 0 ? '可借 ' + book.currentStock : '暂无可借' }}
        </text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  props: {
    book: { type: Object, required: true }
  },
  emits: ['tap']
}
</script>

<style lang="scss">
@import '@/styles/tokens.scss';

.book-card {
  display: flex;
  flex-direction: row;
}

.mp-card--pressed {
  opacity: 0.85;
}

.book-cover {
  width: 140rpx;
  height: 190rpx;
  border-radius: 12rpx;
  margin-right: 24rpx;
  flex-shrink: 0;

  &--empty {
    background: $mp-surface-soft;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &-text {
    font-size: 22rpx;
    color: $mp-text-muted;
  }
}

.book-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.book-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $mp-text-main;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 8rpx;
}

.book-author {
  font-size: 26rpx;
  color: $mp-text-sub;
  margin-bottom: 16rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stock-badge {
  font-size: 22rpx;
  font-weight: 600;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;

  &--ok {
    color: $mp-success;
    background: $mp-success-soft;
  }

  &--empty {
    color: $mp-danger;
    background: $mp-danger-soft;
  }
}
</style>
