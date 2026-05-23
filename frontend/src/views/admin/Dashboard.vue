<template>
  <div class="dashboard-page">
    <h2 class="app-page-title">数据看板</h2>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card stat-card--blue app-card" @click="goCategoryPage">
        <div class="stat-card__inner">
          <div class="stat-card__icon"><el-icon :size="24"><Notebook /></el-icon></div>
          <div class="stat-card__info">
            <div class="stat-card__value">{{ stats.totalBooks }}</div>
            <div class="stat-card__label">图书种类</div>
          </div>
        </div>
      </div>
      <div class="stat-card stat-card--green app-card" @click="goBookPage">
        <div class="stat-card__inner">
          <div class="stat-card__icon"><el-icon :size="24"><Tickets /></el-icon></div>
          <div class="stat-card__info">
            <div class="stat-card__value">{{ stats.totalCopies }}</div>
            <div class="stat-card__label">馆藏册数</div>
          </div>
        </div>
      </div>
      <div class="stat-card stat-card--orange app-card" @click="goBorrowingPage">
        <div class="stat-card__inner">
          <div class="stat-card__icon"><el-icon :size="24"><Timer /></el-icon></div>
          <div class="stat-card__info">
            <div class="stat-card__value">{{ stats.borrowingCount }}</div>
            <div class="stat-card__label">当前借阅</div>
          </div>
        </div>
      </div>
      <div class="stat-card stat-card--red app-card" @click="goOverduePage">
        <div class="stat-card__inner">
          <div class="stat-card__icon"><el-icon :size="24"><WarningFilled /></el-icon></div>
          <div class="stat-card__info">
            <div class="stat-card__value">{{ stats.overdueCount }}</div>
            <div class="stat-card__label">逾期未还</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div v-if="chartsError" class="error-card app-card" style="text-align:center;padding:32px;margin-bottom:24px">
      <p>{{ chartsError }}</p>
      <el-button size="small" @click="onRetryCharts">重试</el-button>
    </div>
    <div v-loading="chartsLoading" :class="['charts-container', chartsLoading ? 'charts-loading' : '']">
    <!-- 图表区 Row 1 -->
    <div class="charts-grid">
      <div class="chart-card app-card">
        <div class="chart-card__header">
          <span>每月借阅趋势</span>
          <el-tag size="small" type="info">近12个月</el-tag>
        </div>
        <div ref="monthlyChartRef" class="chart-card__body"></div>
      </div>
      <div class="chart-card app-card">
        <div class="chart-card__header">
          <span>热门图书 Top 10</span>
          <el-tag size="small" type="info">按借阅次数</el-tag>
        </div>
        <div ref="hotBooksChartRef" class="chart-card__body"></div>
      </div>
    </div>

    <!-- 图表区 Row 2: 库存概览 -->
    <div class="charts-grid charts-grid--spaced">
      <div class="chart-card chart-card--wide app-card">
        <div class="chart-card__header">
          <span>图书库存概览</span>
          <el-tag size="small" type="info">总库存 vs 可借库存</el-tag>
        </div>
        <div ref="stockChartRef" class="chart-card__body"></div>
      </div>
    </div>

    <!-- 新增 Row 3: 资产健康度 + 库位饱和度 -->
    <div class="charts-grid charts-grid--spaced">
      <div class="chart-card app-card">
        <div class="chart-card__header">
          <span>资产健康度</span>
          <el-tag size="small" type="info">实时状态分布</el-tag>
        </div>
        <div ref="healthChartRef" class="chart-card__body"></div>
      </div>
      <div class="chart-card app-card">
        <div class="chart-card__header">
          <span>库位空间饱和度</span>
          <el-tag size="small" type="info">各架位占用率</el-tag>
        </div>
        <div class="shelf-list">
          <div v-for="item in shelfData" :key="item.id" class="shelf-item" @click="goShelfBooks(item)">
            <div class="shelf-item__info">
              <span class="shelf-item__name">{{ item.locationName }}</span>
              <span class="shelf-item__count">{{ item.currentCount }}/{{ item.maxCapacity }}册</span>
            </div>
            <el-progress
              :percentage="item.rate"
              :color="item.rate > 80 ? 'var(--app-danger)' : item.rate > 60 ? 'var(--app-warning)' : 'var(--app-success)'"
              :stroke-width="18"
              :text-inside="true"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 新增 Row 4: 异常待办 + 操作流水线 -->
    <div ref="exceptionSectionRef" class="charts-grid charts-grid--spaced">
      <div class="chart-card app-card">
        <div class="chart-card__header">
          <span>异常处理与待办中心</span>
        </div>
        <el-tabs v-model="exceptionTab" class="exception-tabs">
          <el-tab-pane label="严重逾期" name="overdue">
            <el-table
              :data="exceptions.overdue"
              size="small"
              max-height="260"
              empty-text="暂无逾期记录"
              row-class-name="clickable-row"
              @row-click="goOverdueRecord"
            >
              <el-table-column prop="bookTitle" label="图书" min-width="140" show-overflow-tooltip />
              <el-table-column prop="userName" label="借阅人" width="80" />
              <el-table-column prop="dueTime" label="应还时间" width="110">
                <template #default="{ row }">{{ fmtTime(row.dueTime) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="丢失图书" name="lost">
            <div class="exception-stat exception-stat--clickable" @click="goLostBooks">
              <span class="exception-stat__number">{{ exceptions.lostCount }}</span>
              <span class="exception-stat__label">册图书确认丢失</span>
            </div>
          </el-tab-pane>
          <el-tab-pane label="低库存预警" name="lowStock">
            <el-table
              :data="exceptions.lowStock"
              size="small"
              max-height="260"
              empty-text="库存充足"
              row-class-name="clickable-row"
              @row-click="goLowStockBook"
            >
              <el-table-column prop="title" label="图书" min-width="140" show-overflow-tooltip />
              <el-table-column prop="currentStock" label="可借库存" width="90" align="center">
                <template #default="{ row }">
                  <el-tag type="danger" size="small">{{ row.currentStock }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="totalStock" label="总库存" width="80" align="center" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
      <div class="chart-card app-card">
        <div class="chart-card__header">
          <span>馆务操作流水线</span>
          <el-tag size="small" type="info">最近动态</el-tag>
        </div>
        <div class="activity-feed">
          <el-timeline>
            <el-timeline-item
              v-for="log in activityLogs"
              :key="log.id"
              :timestamp="fmtDateTime(log.createTime)"
              placement="top"
              :color="actionColor[log.actionType] || 'var(--app-text-muted)'"
            >
              <div class="feed-item feed-item--clickable" @click="goActivityDetail(log)">
                <el-tag :type="actionTagType[log.actionType] || 'info'" size="small" effect="plain" class="feed-action-tag">
                  {{ actionLabel[log.actionType] || log.actionType }}
                </el-tag>
                <span>{{ log.targetDesc }}</span>
                <span v-if="log.detail" class="feed-item__detail"> — {{ log.detail }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <div v-if="!activityLogs.length" class="app-empty-state">暂无操作记录</div>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { Notebook, Tickets, Timer, WarningFilled } from '@element-plus/icons-vue'
import {
  getStats, getHotBooks, getMonthlyBorrows, getBookStocks,
  getActivityFeed, getInventoryHealth, getShelfCapacity, getExceptions
} from '../../api/admin'
import { formatDate, formatDateTime } from '../../utils/formatters'

const stats = reactive({
  totalBooks: 0, totalCopies: 0, borrowingCount: 0, overdueCount: 0
})
const router = useRouter()
const route = useRoute()

const monthlyChartRef = ref()
const hotBooksChartRef = ref()
const stockChartRef = ref()
const healthChartRef = ref()
const exceptionSectionRef = ref()
const monthlyData = ref([])
const hotBooksData = ref([])
const stockData = ref([])
let monthlyChart, hotBooksChart, stockChart, healthChart

const shelfData = ref([])
const activityLogs = ref([])
const exceptionTab = ref('overdue')
const exceptions = reactive({ overdue: [], lostCount: 0, lowStock: [] })
const chartsLoading = ref(true)
const chartsError = ref('')

const actionLabel = {
  BORROW: '借出', RETURN: '归还', NEW_BOOK: '入库', ADD_STOCK: '增库存',
  EDIT_BOOK: '编辑', DELETE_BOOK: '删除', LOST_MARK: '标记丢失'
}
const actionColor = {
  BORROW: 'var(--app-primary)', RETURN: 'var(--app-success)', NEW_BOOK: 'var(--app-success)', ADD_STOCK: 'var(--app-warning)',
  EDIT_BOOK: 'var(--app-text-muted)', DELETE_BOOK: 'var(--app-danger)', LOST_MARK: 'var(--app-danger)'
}
const actionTagType = {
  BORROW: 'primary', RETURN: 'success', NEW_BOOK: 'success', ADD_STOCK: 'warning',
  EDIT_BOOK: 'info', DELETE_BOOK: 'danger', LOST_MARK: 'danger'
}

const fmtTime = (val) => formatDate(val)
const fmtDateTime = (val) => formatDateTime(val)
const readVar = (name, fallback) => getComputedStyle(document.documentElement).getPropertyValue(name)?.trim() || fallback
const validExceptionTabs = ['overdue', 'lost', 'lowStock']

const getChartPalette = () => ({
  bg: readVar('--app-surface', '#ffffff'),
  border: readVar('--app-border', '#e4e7ed'),
  textPrimary: readVar('--app-text-primary', '#303133'),
  textSecondary: readVar('--app-text-secondary', '#606266'),
  textMuted: readVar('--app-text-muted', '#909399'),
  primaryStart: readVar('--app-chart-primary-start', '#409eff'),
  primaryEnd: readVar('--app-chart-primary-end', '#337ecc'),
  auxStart: readVar('--app-chart-aux-start', '#67c23a'),
  auxEnd: readVar('--app-chart-aux-end', '#4f9f2d'),
  success: readVar('--app-success', '#67c23a'),
  warning: readVar('--app-warning', '#e6a23c'),
  danger: readVar('--app-danger', '#f56c6c'),
  splitLine: readVar('--app-chart-grid', '#ebeef5')
})

const fetchStats = async () => {
  const res = await getStats()
  Object.assign(stats, res.data)
}

const goBorrowPage = (query = {}) => {
  router.push({ path: '/admin/borrows', query }).catch(() => {})
}

const goBookList = (query = {}) => {
  router.push({ path: '/admin/books', query }).catch(() => {})
}

const goBookDetail = (id, query = {}) => {
  if (!id) return
  router.push({ path: `/admin/books/${id}`, query }).catch(() => {})
}

const goCategoryPage = () => {
  router.push('/admin/books').catch(() => {})
}

const goBookPage = () => {
  goBookList()
}

const goBorrowingPage = () => {
  goBorrowPage({ status: '0' })
}

const goOverduePage = () => {
  goBorrowPage({ status: '2' })
}

const goMonthlyBorrowDetails = (month) => {
  if (!month) return
  goBorrowPage({ month })
}

const goHotBookBorrowDetails = (book) => {
  if (!book?.id) return
  goBorrowPage({ bookId: String(book.id), bookTitle: book.title || '' })
}

const goStockBookDetails = (book) => {
  if (!book) return
  if (book.id) {
    goBookDetail(book.id, { from: route.fullPath })
    return
  }
  if (book.title) {
    goBookList({ keyword: book.title })
  }
}

const goShelfBooks = (item) => {
  if (!item?.id) return
  goBookList({ locationId: String(item.id), locationName: item.locationName || '' })
}

const goLostBooks = () => {
  goBookList({ lostOnly: 'true' })
}

const goOverdueRecord = (row) => {
  if (!row) return
  const query = { status: '2' }
  if (row.userId) query.userId = String(row.userId)
  if (row.bookId) query.bookId = String(row.bookId)
  goBorrowPage(query)
}

const goLowStockBook = (row) => {
  if (!row) return
  if (row.id) {
    goBookDetail(row.id, { from: route.fullPath })
    return
  }
  if (row.title) {
    goBookList({ keyword: row.title })
  }
}

const extractBookTitle = (text) => {
  if (!text) return ''
  const match = /《([^》]+)》/.exec(text)
  return match?.[1] || ''
}

const goActivityDetail = (log) => {
  if (!log) return
  if (log.actionType === 'BORROW') {
    const query = { status: '0' }
    const title = extractBookTitle(log.targetDesc)
    if (title) query.bookTitle = title
    goBorrowPage(query)
    return
  }
  if (log.actionType === 'RETURN') {
    const title = extractBookTitle(log.targetDesc)
    const query = { status: '1' }
    if (title) query.bookTitle = title
    goBorrowPage(query)
    return
  }
  if (log.actionType === 'LOST_MARK') {
    goLostBooks()
    return
  }
  const title = extractBookTitle(log.targetDesc)
  if (title) {
    goBookList({ keyword: title })
    return
  }
  goBookPage()
}

const syncExceptionTabFromRoute = () => {
  const tab = typeof route.query.exceptionTab === 'string' ? route.query.exceptionTab : ''
  exceptionTab.value = validExceptionTabs.includes(tab) ? tab : 'overdue'
}

const jumpToExceptionTab = (tab) => {
  const targetTab = validExceptionTabs.includes(tab) ? tab : 'overdue'
  exceptionTab.value = targetTab
  const query = { ...route.query, exceptionTab: targetTab }
  router.replace({ path: '/admin/dashboard', query }).catch(() => {})
  nextTick(() => {
    exceptionSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

const initMonthlyChart = async () => {
  const res = await getMonthlyBorrows()
  const data = Array.isArray(res.data) ? res.data : []
  monthlyData.value = data
  const p = getChartPalette()
  monthlyChart?.dispose()
  monthlyChart = echarts.init(monthlyChartRef.value)
  monthlyChartRef.value.style.cursor = 'pointer'
  monthlyChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: p.bg, borderColor: p.border, textStyle: { color: p.textPrimary }, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' },
    grid: { top: 20, right: 20, bottom: 24, left: 40 },
    xAxis: {
      type: 'category',
      data: monthlyData.value.map(d => d.month),
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { color: p.textMuted }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: p.splitLine, type: 'dashed' } },
      axisLabel: { color: p.textMuted }
    },
    series: [{
      name: '借阅量',
      type: 'bar',
      data: monthlyData.value.map(d => d.count),
      barWidth: 20,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: p.primaryStart },
          { offset: 1, color: p.primaryEnd }
        ])
      }
    }]
  })
  monthlyChart.off('click')
  monthlyChart.on('click', (params) => {
    const record = monthlyData.value?.[params?.dataIndex]
    if (record?.month) goMonthlyBorrowDetails(record.month)
  })
}

const initHotBooksChart = async () => {
  const res = await getHotBooks(10)
  const data = Array.isArray(res.data) ? res.data : []
  hotBooksData.value = [...data].reverse()
  const p = getChartPalette()
  const titles = hotBooksData.value.map(d => d.title)
  const counts = hotBooksData.value.map(d => d.borrowCount)
  hotBooksChart?.dispose()
  hotBooksChart = echarts.init(hotBooksChartRef.value)
  hotBooksChartRef.value.style.cursor = 'pointer'
  hotBooksChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: p.bg, borderColor: p.border, textStyle: { color: p.textPrimary }, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' },
    grid: { top: 10, right: 36, bottom: 20, left: 20, containLabel: true },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: p.splitLine, type: 'dashed' } },
      axisLabel: { color: p.textMuted }
    },
    yAxis: {
      type: 'category',
      data: titles,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: p.textSecondary, fontSize: 12 }
    },
    series: [{
      name: '借阅次数',
      type: 'bar',
      data: counts,
      barWidth: 12,
      label: { show: true, position: 'right', color: p.textSecondary },
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: p.primaryStart },
          { offset: 1, color: p.primaryEnd }
        ])
      }
    }]
  })
  hotBooksChart.off('click')
  hotBooksChart.on('click', (params) => {
    const record = hotBooksData.value?.[params?.dataIndex]
    if (record) goHotBookBorrowDetails(record)
  })
}

const initStockChart = async () => {
  const res = await getBookStocks()
  const data = Array.isArray(res.data) ? res.data : []
  stockData.value = data
  const p = getChartPalette()
  const titles = stockData.value.map(d => d.title.length > 8 ? d.title.slice(0, 8) + '...' : d.title)
  stockChart?.dispose()
  stockChart = echarts.init(stockChartRef.value)
  stockChartRef.value.style.cursor = 'pointer'
  stockChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: p.bg, borderColor: p.border, textStyle: { color: p.textPrimary }, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' },
    legend: { data: ['总库存', '可借库存'], bottom: 0, textStyle: { color: p.textSecondary } },
    grid: { top: 20, right: 20, bottom: 40, left: 40 },
    xAxis: { type: 'category', data: titles, axisLine: { lineStyle: { color: p.border } }, axisTick: { show: false }, axisLabel: { color: p.textMuted } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: p.splitLine, type: 'dashed' } }, axisLabel: { color: p.textMuted } },
    series: [
      { name: '总库存', type: 'bar', data: stockData.value.map(d => d.totalStock), barWidth: 20, itemStyle: { borderRadius: [6, 6, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: p.primaryStart }, { offset: 1, color: p.primaryEnd }]) } },
      { name: '可借库存', type: 'bar', data: stockData.value.map(d => d.currentStock), barWidth: 20, itemStyle: { borderRadius: [6, 6, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: p.auxStart }, { offset: 1, color: p.auxEnd }]) } }
    ]
  })
  stockChart.off('click')
  stockChart.on('click', (params) => {
    const record = stockData.value?.[params?.dataIndex]
    if (record) goStockBookDetails(record)
  })
}

const initHealthChart = async () => {
  const res = await getInventoryHealth()
  const d = res.data
  const p = getChartPalette()
  healthChart?.dispose()
  healthChart = echarts.init(healthChartRef.value)
  healthChartRef.value.style.cursor = 'pointer'
  healthChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: p.bg, borderColor: p.border, textStyle: { color: p.textPrimary } },
    legend: { bottom: 0, textStyle: { color: p.textSecondary } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: p.bg, borderWidth: 2 },
      label: {
        show: true,
        formatter: '{b}: {c}册 ({d}%)',
        color: p.textSecondary,
        textShadowColor: 'transparent',
        textShadowBlur: 0,
        textBorderColor: 'transparent',
        textBorderWidth: 0
      },
      data: [
        { value: d.available, name: '在馆可借', itemStyle: { color: p.success } },
        { value: d.borrowed, name: '借出中', itemStyle: { color: p.warning } },
        { value: d.lost, name: '丢失', itemStyle: { color: p.danger } }
      ]
    }]
  })
  healthChart.off('click')
  healthChart.on('click', (params) => {
    if (params?.name === '在馆可借') {
      goBookPage()
      return
    }
    if (params?.name === '借出中') {
      goBorrowingPage()
      return
    }
    if (params?.name === '丢失') {
      goLostBooks()
    }
  })
}

const fetchShelfCapacity = async () => {
  const res = await getShelfCapacity()
  shelfData.value = res.data
}

const fetchExceptions = async () => {
  const res = await getExceptions()
  exceptions.overdue = res.data.overdue || []
  exceptions.lostCount = res.data.lost || 0
  exceptions.lowStock = res.data.lowStock || []
}

const fetchActivityFeed = async () => {
  const res = await getActivityFeed(20)
  activityLogs.value = res.data
}

const handleThemeChange = async () => {
  if (!monthlyChartRef.value) return
  await Promise.all([
    initMonthlyChart(),
    initHotBooksChart(),
    initStockChart(),
    initHealthChart()
  ])
}

const onRetryCharts = async () => {
  chartsLoading.value = true
  chartsError.value = ''
  await nextTick()
  try {
    await Promise.all([
      initMonthlyChart(),
      initHotBooksChart(),
      initStockChart(),
      initHealthChart(),
      fetchShelfCapacity(),
      fetchExceptions(),
      fetchActivityFeed()
    ])
  } catch (e) { chartsError.value = e.message || '加载图表数据失败' }
  chartsLoading.value = false
}

const handleResize = () => {
  monthlyChart?.resize()
  hotBooksChart?.resize()
  stockChart?.resize()
  healthChart?.resize()
}

onMounted(async () => {
  syncExceptionTabFromRoute()
  try { await fetchStats() } catch (e) { stats.totalBooks = -1 }
  await nextTick()
  try {
    await Promise.all([
      initMonthlyChart(),
      initHotBooksChart(),
      initStockChart(),
      initHealthChart(),
      fetchShelfCapacity(),
      fetchExceptions(),
      fetchActivityFeed()
    ])
  } catch (e) { chartsError.value = e.message || '加载图表数据失败' }
  chartsLoading.value = false
  window.addEventListener('resize', handleResize)
  window.addEventListener('app-theme-change', handleThemeChange)
})

watch(() => route.query.exceptionTab, () => {
  syncExceptionTabFromRoute()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('app-theme-change', handleThemeChange)
  monthlyChart?.dispose()
  hotBooksChart?.dispose()
  stockChart?.dispose()
  healthChart?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  background: var(--app-bg);
  min-height: 100%;
  padding: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--app-card-bg);
  backdrop-filter: blur(var(--app-glass-blur));
  -webkit-backdrop-filter: blur(var(--app-glass-blur));
  border: var(--app-card-border-width) solid var(--app-card-border);
  border-radius: 16px;
  padding: 22px;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--app-card-shadow);
  opacity: 0;
  animation: fadeInUp 0.5s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}
.stat-card:nth-child(1) { animation-delay: 0.05s; }
.stat-card:nth-child(2) { animation-delay: 0.1s; }
.stat-card:nth-child(3) { animation-delay: 0.15s; }
.stat-card:nth-child(4) { animation-delay: 0.2s; }

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  border-radius: 16px 16px 0 0;
  opacity: 0;
  transition: opacity 0.35s ease;
}
.stat-card--blue::before { background: linear-gradient(90deg, var(--app-primary), #a855f7); }
.stat-card--green::before { background: linear-gradient(90deg, var(--app-success), #06b6d4); }
.stat-card--orange::before { background: linear-gradient(90deg, var(--app-warning), #f97316); }
.stat-card--red::before { background: linear-gradient(90deg, var(--app-danger), #ec4899); }

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--app-card-shadow-hover);
}
.stat-card:hover::before {
  opacity: 1;
}

.stat-card__inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card__icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.stat-card:hover .stat-card__icon {
  transform: scale(1.08);
}
.stat-card--blue .stat-card__icon {
  background: linear-gradient(135deg, var(--app-primary-soft), rgba(64, 158, 255, 0.18));
  color: var(--app-primary);
  box-shadow: 0 4px 12px var(--app-glow-primary);
}
.stat-card--green .stat-card__icon {
  background: linear-gradient(135deg, var(--app-success-soft), rgba(103, 194, 58, 0.18));
  color: var(--app-success);
  box-shadow: 0 4px 12px var(--app-glow-success);
}
.stat-card--orange .stat-card__icon {
  background: linear-gradient(135deg, var(--app-warning-soft), rgba(230, 162, 60, 0.18));
  color: var(--app-warning);
  box-shadow: 0 4px 12px var(--app-glow-warning);
}
.stat-card--red .stat-card__icon {
  background: linear-gradient(135deg, var(--app-danger-soft), rgba(245, 108, 108, 0.18));
  color: var(--app-danger);
  box-shadow: 0 4px 12px var(--app-glow-danger);
}

.stat-card__value {
  font-size: 30px;
  font-weight: 700;
  color: var(--app-text-primary);
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.stat-card__label {
  font-size: 13px;
  color: var(--app-text-muted);
  margin-top: 6px;
  font-weight: 500;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.charts-grid--spaced {
  margin-top: 20px;
}

.chart-card {
  background: var(--app-card-bg);
  backdrop-filter: blur(var(--app-glass-blur));
  -webkit-backdrop-filter: blur(var(--app-glass-blur));
  border: var(--app-card-border-width) solid var(--app-card-border);
  border-radius: 16px;
  padding: 22px;
  box-shadow: var(--app-card-shadow);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  animation: fadeInUp 0.5s cubic-bezier(0.4, 0, 0.2, 1) forwards;
  animation-delay: 0.25s;
}

.chart-card--wide {
  grid-column: 1 / -1;
}
.chart-card:hover {
  box-shadow: var(--app-card-shadow-hover);
  transform: translateY(-2px);
}

.chart-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-primary);
  position: relative;
  padding-left: 14px;
}
.chart-card__header::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  border-radius: 2px;
  background: var(--app-decoration-line);
}

.chart-card__body {
  height: 300px;
}

/* 库位饱和度 */
.shelf-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
}
.shelf-item {
  padding: 8px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.24s ease, transform 0.24s ease;
}
.shelf-item:hover {
  background: var(--app-surface-soft);
  transform: translateY(-1px);
}
.shelf-item__info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}
.shelf-item__name {
  font-size: 13px;
  font-weight: 500;
  color: var(--app-text-primary);
}
.shelf-item__count {
  font-size: 12px;
  color: var(--app-text-muted);
}

/* 异常待办 */
.exception-tabs {
  margin-top: 4px;
}
.exception-stat {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 40px 0;
  justify-content: center;
}
.exception-stat--clickable {
  cursor: pointer;
  border-radius: 12px;
  transition: background-color 0.24s ease, transform 0.24s ease;
}
.exception-stat--clickable:hover {
  background: var(--app-danger-soft);
  transform: translateY(-1px);
}
.exception-stat__number {
  font-size: 52px;
  font-weight: 800;
  color: var(--app-danger);
  letter-spacing: -0.03em;
}
.exception-stat__label {
  font-size: 15px;
  color: var(--app-text-secondary);
}

/* 操作流水线 */
.activity-feed {
  max-height: 340px;
  overflow-y: auto;
  padding: 4px 0;
}
.feed-item {
  font-size: 13px;
  color: var(--app-text-primary);
  line-height: 1.6;
}

.feed-action-tag {
  margin-right: 6px;
}
.feed-item--clickable {
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.24s ease;
}
.feed-item--clickable:hover {
  background: var(--app-surface-soft);
}
.feed-item__detail {
  color: var(--app-text-muted);
  font-size: 12px;
}

:deep(.clickable-row) {
  cursor: pointer;
}

:deep(.clickable-row:hover > td.el-table__cell) {
  background: var(--app-primary-soft) !important;
}

@media (max-width: 1200px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .charts-grid { grid-template-columns: 1fr; }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
