<template>
  <view class="books-page mp-page-with-fab">
    <view class="search-bar mp-sticky-top">
      <input class="search-input" placeholder="搜索书名、作者或 ISBN" :value="keyword" @input="onInput" @confirm="onSearch" confirm-type="search" />
      <view v-if="keyword" class="search-clear" @click="onClear"><text>✕</text></view>
    </view>

    <view class="filter-bar">
      <view :class="['filter-chip', availableOnly ? 'filter-chip--active' : '']" @click="onToggleAvailable">
        <text>仅看可借</text>
      </view>
    </view>

    <view v-if="loading && books.length === 0" class="skeleton-wrap">
      <view class="mp-skeleton mp-skeleton-card" style="height:196rpx"></view>
      <view class="mp-skeleton mp-skeleton-card" style="height:196rpx"></view>
      <view class="mp-skeleton mp-skeleton-card" style="height:196rpx"></view>
    </view>

    <view v-else-if="error && books.length === 0" class="error-wrap mp-card">
      <text class="mp-muted">{{ error }}</text>
      <view class="mp-button mp-button--primary mp-btn-inline mp-error-wrap-btn" @click="onRetry">重试</view>
    </view>

    <view v-else>
      <view class="book-list">
        <BookCard v-for="item in books" :key="item.id" :book="item" @tap="onBookTap(item.id)" />
      </view>
      <AppEmpty v-if="isEmpty" message="暂无图书" />
      <view v-if="noMore && books.length > 0" class="mp-no-more"><text>没有更多了</text></view>
    </view>

    <ScanFab icon="扫码" text="借阅" @tap="onScanBorrow" />
  </view>
</template>

<script>
import BookCard from '@/components/BookCard.vue'
import ScanFab from '@/components/ScanFab.vue'
import AppEmpty from '@/components/AppEmpty.vue'
import { getBooks } from '@/api/books'
import { requireLogin } from '@/utils/guard'
import { scanAndBorrow } from '@/utils/borrowFlow'

let searchTimer = null

export default {
  components: { BookCard, ScanFab, AppEmpty },
  data() {
    return {
      keyword: '',
      books: [],
      current: 1,
      size: 10,
      total: 0,
      loading: false,
      noMore: false,
      isEmpty: false,
      error: '',
      availableOnly: false
    }
  },
  onLoad() {
    if (!requireLogin()) return
    this.loadBooks()
  },
  onShow() {
    if (!requireLogin()) return
    if (this.books.length > 0) this.loadBooks()
  },
  onPullDownRefresh() {
    this.current = 1
    this.noMore = false
    this.isEmpty = false
    this.error = ''
    this.loadBooks().then(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    if (this.noMore || this.loading) return
    this.loadMore()
  },
  methods: {
    onInput(e) {
      this.keyword = e.detail.value
      if (searchTimer) clearTimeout(searchTimer)
      const that = this
      searchTimer = setTimeout(() => {
        that.current = 1
        that.noMore = false
        that.isEmpty = false
        that.error = ''
        that.books = []
        that.loadBooks()
      }, 300)
    },
    onSearch() {
      if (searchTimer) clearTimeout(searchTimer)
      this.current = 1
      this.noMore = false
      this.isEmpty = false
      this.error = ''
      this.books = []
      this.loadBooks()
    },
    onClear() {
      if (searchTimer) clearTimeout(searchTimer)
      this.keyword = ''
      this.current = 1
      this.noMore = false
      this.isEmpty = false
      this.error = ''
      this.books = []
      this.loadBooks()
    },
    onToggleAvailable() {
      this.availableOnly = !this.availableOnly
      this.current = 1
      this.noMore = false
      this.isEmpty = false
      this.error = ''
      this.books = []
      this.loadBooks()
    },
    onBookTap(id) {
      uni.navigateTo({ url: `/pages/book-detail/index?id=${id}` })
    },
    onRetry() {
      this.current = 1
      this.noMore = false
      this.isEmpty = false
      this.error = ''
      this.books = []
      this.loadBooks()
    },
    async loadBooks() {
      this.loading = true
      try {
        const res = await getBooks({
          current: 1,
          size: this.size,
          keyword: this.keyword || undefined,
          availableOnly: this.availableOnly || undefined
        })
        const books = res.records || []
        this.books = books
        this.total = res.total || 0
        this.current = 1
        this.noMore = books.length < this.size
        this.isEmpty = books.length === 0
        this.loading = false
        this.error = ''
      } catch (err) {
        this.loading = false
        this.error = err.msg || '加载失败'
      }
    },
    async loadMore() {
      this.loading = true
      try {
        const nextPage = this.current + 1
        const res = await getBooks({
          current: nextPage,
          size: this.size,
          keyword: this.keyword || undefined,
          availableOnly: this.availableOnly || undefined
        })
        const newBooks = res.records || []
        this.books = [...this.books, ...newBooks]
        this.total = res.total || 0
        this.current = nextPage
        this.noMore = newBooks.length < this.size
        this.loading = false
      } catch (err) {
        this.loading = false
      }
    },
    async onScanBorrow() {
      const ok = await scanAndBorrow()
      if (ok) {
        this.current = 1
        this.noMore = false
        this.isEmpty = false
        this.error = ''
        this.books = []
        await this.loadBooks()
      }
    }
  }
}
</script>

<style lang="scss">
@import '@/styles/tokens.scss';

.search-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: $mp-surface;
  position: sticky;
  top: 0;
  z-index: 10;
}

.search-input {
  flex: 1;
  height: 72rpx;
  background: $mp-surface-soft;
  border-radius: 36rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
}

.search-clear {
  padding: 8rpx 16rpx;
  color: $mp-text-muted;
  font-size: 28rpx;
}

.filter-bar {
  display: flex;
  padding: 8rpx 24rpx 16rpx;
}

.filter-chip {
  font-size: 24rpx;
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
  background: $mp-surface-soft;
  color: $mp-text-sub;

  &--active {
    background: $mp-primary-soft;
    color: $mp-primary;
    font-weight: 600;
  }
}

.book-list {
  padding: 0 24rpx;
}

.error-wrap {
  text-align: center;
  padding: 60rpx 40rpx;
  margin: 0 24rpx;
}

.skeleton-wrap {
  padding: 0 24rpx;
}
</style>
