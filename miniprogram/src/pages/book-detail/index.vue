<template>
  <view class="detail-page mp-page-with-bottom-bar">
    <view v-if="loading" class="skeleton-wrap">
      <view class="mp-skeleton mp-skeleton-card" style="height:400rpx"></view>
      <view class="mp-skeleton mp-skeleton-card" style="height:48rpx;width:60%"></view>
      <view class="mp-skeleton mp-skeleton-card" style="height:36rpx;width:40%"></view>
      <view class="mp-skeleton mp-skeleton-card" style="height:200rpx"></view>
    </view>

    <view v-else-if="error" class="error-wrap mp-card">
      <text class="mp-muted">{{ error }}</text>
      <view class="mp-button mp-button--primary mp-btn-inline mp-error-wrap-btn" @click="onRetry">重试</view>
      <view class="mp-button mp-button--ghost mp-btn-inline mp-error-wrap-btn" @click="goBack">返回</view>
    </view>

    <view v-else-if="book">
      <view class="detail-header">
        <image v-if="book.coverImage" class="detail-cover" :src="book.coverImage" mode="aspectFill" />
        <view v-else class="detail-cover detail-cover--empty"><text>无封面</text></view>
        <view class="detail-brief">
          <text class="detail-title">{{ book.title }}</text>
          <text class="detail-author">{{ book.author || '未知作者' }}</text>
          <text class="detail-isbn">ISBN：{{ book.isbn || '-' }}</text>
        </view>
      </view>

      <view class="detail-meta mp-card">
        <view class="meta-row"><text class="meta-label">总库存</text><text class="meta-value">{{ book.totalStock || 0 }}</text></view>
        <view class="meta-row"><text class="meta-label">可借</text><text :class="['meta-value', book.currentStock > 0 ? 'meta-ok' : 'meta-empty']">{{ book.currentStock || 0 }}</text></view>
      </view>

      <view class="detail-desc mp-card">
        <text class="desc-title">简介</text>
        <text class="desc-text">{{ book.description || '暂无简介' }}</text>
      </view>
    </view>

    <view v-else class="error-wrap mp-card">
      <text class="mp-muted">图书不存在</text>
      <view class="mp-button mp-button--ghost mp-btn-inline mp-error-wrap-btn" @click="goBack">返回</view>
    </view>

    <view v-if="book" class="mp-bottom-bar">
      <view :class="['mp-button mp-btn-full', canBorrow ? 'mp-button--primary' : 'mp-button--disabled']" @click="onScanBorrow">
        {{ canBorrow ? '扫码借阅' : '暂无可借' }}
      </view>
    </view>
  </view>
</template>

<script>
import { getBookById } from '@/api/books'
import { requireLogin } from '@/utils/guard'
import { scanAndBorrow } from '@/utils/borrowFlow'

export default {
  data() {
    return {
      id: null,
      book: null,
      loading: false,
      error: ''
    }
  },
  computed: {
    canBorrow() {
      return this.book && this.book.currentStock > 0
    }
  },
  onLoad(options) {
    if (!requireLogin()) return
    if (options.id) {
      this.id = options.id
      this.loadBook(options.id)
    } else {
      this.error = '缺少图书ID'
    }
  },
  methods: {
    async loadBook(id) {
      this.loading = true
      this.error = ''
      try {
        this.book = await getBookById(id)
        this.loading = false
      } catch (err) {
        this.loading = false
        this.error = err.msg || '加载失败'
      }
    },
    onRetry() {
      if (this.id) this.loadBook(this.id)
    },
    goBack() {
      uni.navigateBack({ delta: 1 })
    },
    async onScanBorrow() {
      if (!this.canBorrow) return
      const ok = await scanAndBorrow()
      if (ok && this.id) await this.loadBook(this.id)
    }
  }
}
</script>

<style lang="scss">
@import '@/styles/tokens.scss';

.detail-header {
  display: flex;
  flex-direction: row;
  margin-bottom: 24rpx;
}

.detail-cover {
  width: 200rpx;
  height: 280rpx;
  border-radius: 16rpx;
  margin-right: 28rpx;
  flex-shrink: 0;
  background: $mp-surface-soft;

  &--empty {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26rpx;
    color: $mp-text-muted;
  }
}

.detail-brief {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.detail-title {
  font-size: 36rpx;
  font-weight: 700;
  color: $mp-text-main;
  margin-bottom: 12rpx;
}

.detail-author {
  font-size: 28rpx;
  color: $mp-text-sub;
  margin-bottom: 8rpx;
}

.detail-isbn {
  font-size: 24rpx;
  color: $mp-text-muted;
}

.detail-meta {
  display: flex;
  flex-direction: row;
  justify-content: space-around;
}

.meta-row {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.meta-label {
  font-size: 24rpx;
  color: $mp-text-muted;
  margin-bottom: 8rpx;
}

.meta-value {
  font-size: 36rpx;
  font-weight: 700;
  color: $mp-text-main;
}

.meta-ok {
  color: $mp-success;
}

.meta-empty {
  color: $mp-danger;
}

.detail-desc {
}

.desc-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $mp-text-main;
  margin-bottom: 12rpx;
  display: block;
}

.desc-text {
  font-size: 28rpx;
  color: $mp-text-sub;
  line-height: 1.6;
}

.error-wrap {
  text-align: center;
  padding: 60rpx 40rpx;
}

.skeleton-wrap {
  padding: 24rpx;
}
</style>