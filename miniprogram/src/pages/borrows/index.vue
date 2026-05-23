<template>
  <view class="borrows-page mp-page-with-fab">
    <view class="filter-bar">
      <view v-for="f in filters" :key="f.value" :class="['filter-chip', activeFilter === f.value ? 'filter-chip--active' : '']" @click="onFilter(f.value)">
        <text>{{ f.label }}</text>
      </view>
    </view>

    <view v-if="loading && records.length === 0" class="skeleton-wrap">
      <view class="mp-skeleton mp-skeleton-card" style="height:200rpx"></view>
      <view class="mp-skeleton mp-skeleton-card" style="height:200rpx"></view>
      <view class="mp-skeleton mp-skeleton-card" style="height:200rpx"></view>
    </view>

    <view v-else-if="error && records.length === 0" class="error-wrap mp-card">
      <text class="mp-muted">{{ error }}</text>
      <view class="mp-button mp-button--primary mp-btn-inline mp-error-wrap-btn" @click="onRetry">重试</view>
    </view>

    <view v-else>
      <view class="borrow-list">
        <BorrowCard v-for="item in records" :key="item.id" :record="item" @return="onReturn" />
      </view>
      <AppEmpty v-if="isEmpty" message="暂无借阅记录" />
      <view v-if="noMore && records.length > 0" class="mp-no-more"><text>没有更多了</text></view>
    </view>

    <ScanFab icon="扫码" text="归还" variant="success" @tap="onScanReturn" />
  </view>
</template>

<script>
import BorrowCard from '@/components/BorrowCard.vue'
import ScanFab from '@/components/ScanFab.vue'
import AppEmpty from '@/components/AppEmpty.vue'
import { getMyBorrows, returnBorrow } from '@/api/borrows'
import { requireLogin } from '@/utils/guard'
import { scanAndReturn } from '@/utils/borrowFlow'
import { formatDate, formatDateTime } from '@/utils/format'

export default {
  components: { BorrowCard, ScanFab, AppEmpty },
  data() {
    return {
      records: [],
      current: 1,
      size: 10,
      loading: false,
      noMore: false,
      isEmpty: false,
      error: '',
      activeFilter: '',
      filters: [
        { label: '全部', value: '' },
        { label: '借阅中', value: '0' },
        { label: '已归还', value: '1' },
        { label: '已逾期', value: '2' }
      ]
    }
  },
  onLoad() {
    if (!requireLogin()) return
    this.loadRecords()
  },
  onShow() {
    if (!requireLogin()) return
    if (this.records.length > 0) this.loadRecords()
  },
  onPullDownRefresh() {
    this.current = 1
    this.noMore = false
    this.isEmpty = false
    this.error = ''
    this.loadRecords().then(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    if (this.noMore || this.loading) return
    this.loadMore()
  },
  methods: {
    onFilter(val) {
      this.activeFilter = val
      this.current = 1
      this.noMore = false
      this.isEmpty = false
      this.error = ''
      this.records = []
      this.loadRecords()
    },
    onRetry() {
      this.current = 1
      this.noMore = false
      this.isEmpty = false
      this.error = ''
      this.records = []
      this.loadRecords()
    },
    async loadRecords() {
      this.loading = true
      try {
        const params = { current: 1, size: this.size }
        if (this.activeFilter !== '') params.status = Number(this.activeFilter)
        const res = await getMyBorrows(params)
        const records = (res.records || []).map(r => ({
          ...r,
          borrowTimeText: formatDateTime(r.borrowTime),
          dueTimeText: formatDate(r.dueTime),
          returnTimeText: r.returnTime ? formatDateTime(r.returnTime) : ''
        }))
        this.records = records
        this.current = 1
        this.noMore = records.length < this.size
        this.isEmpty = records.length === 0
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
        const params = { current: nextPage, size: this.size }
        if (this.activeFilter !== '') params.status = Number(this.activeFilter)
        const res = await getMyBorrows(params)
        const newRecords = (res.records || []).map(r => ({
          ...r,
          borrowTimeText: formatDateTime(r.borrowTime),
          dueTimeText: formatDate(r.dueTime),
          returnTimeText: r.returnTime ? formatDateTime(r.returnTime) : ''
        }))
        this.records = [...this.records, ...newRecords]
        this.current = nextPage
        this.noMore = newRecords.length < this.size
        this.loading = false
      } catch (err) {
        this.loading = false
      }
    },
    onReturn(id) {
      uni.showModal({
        title: '确认归还',
        content: '确定要归还这本书吗？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await returnBorrow(id)
            uni.showToast({ title: '归还成功', icon: 'success' })
            this.current = 1
            this.noMore = false
            this.isEmpty = false
            this.error = ''
            this.records = []
            await this.loadRecords()
          } catch (err) {}
        }
      })
    },
    async onScanReturn() {
      const ok = await scanAndReturn()
      if (ok) {
        this.current = 1
        this.noMore = false
        this.isEmpty = false
        this.error = ''
        this.records = []
        await this.loadRecords()
      }
    }
  }
}
</script>

<style lang="scss">
@import '@/styles/tokens.scss';

.filter-bar {
  display: flex;
  padding: 16rpx 24rpx;
  gap: 16rpx;
  background: $mp-surface;
  position: sticky;
  top: 0;
  z-index: 10;
}

.filter-chip {
  font-size: 26rpx;
  padding: 8rpx 24rpx;
  border-radius: 24rpx;
  background: $mp-surface-soft;
  color: $mp-text-sub;

  &--active {
    background: $mp-primary-soft;
    color: $mp-primary;
    font-weight: 600;
  }
}

.borrow-list {
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