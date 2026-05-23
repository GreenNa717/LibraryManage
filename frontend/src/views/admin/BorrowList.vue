<template>
  <div>
    <h2 class="app-page-title">借阅管理</h2>

    <div class="app-toolbar">
      <el-select
        v-model="filterStatus"
        placeholder="状态筛选"
        clearable
        class="app-control-xs"
        @change="loadData"
      >
        <el-option label="全部" :value="-1" />
        <el-option label="借阅中" :value="0" />
        <el-option label="已归还" :value="1" />
        <el-option label="已逾期" :value="2" />
      </el-select>
      <el-input
        v-model="filterKeyword"
        placeholder="读者 / 书名 / 副本码 / ID"
        class="app-control-md"
        clearable
        @keyup.enter="loadData"
      >
        <template #append>
          <el-button @click="loadData">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <div class="toolbar-spacer" />
      <el-button type="primary" @click="openBorrow">
        <el-icon class="app-inline-icon"><Camera /></el-icon>
        扫码借阅
      </el-button>
      <el-button @click="refreshOverdue" :loading="refreshing" type="warning" plain>
        <el-icon class="app-inline-icon"><Refresh /></el-icon>
        刷新逾期状态
      </el-button>
    </div>

    <div v-if="routeMonth || routeUserId !== null || routeBookId !== null || routeBookTitle" class="route-filters">
      <span class="route-filters__label">当前筛选：</span>
      <el-tag v-if="routeMonth" closable @close="clearRouteFilter('month')">月份 {{ routeMonth }}</el-tag>
      <el-tag v-if="routeBookId !== null" type="success" closable @close="clearRouteFilter('bookId')">
        图书ID {{ routeBookId }}<span v-if="routeBookTitle">（{{ routeBookTitle }}）</span>
      </el-tag>
      <el-tag v-else-if="routeBookTitle" type="success" closable @close="clearRouteFilter('bookTitle')">
        书名 {{ routeBookTitle }}
      </el-tag>
      <el-tag v-if="routeUserId !== null" type="warning" closable @close="clearRouteFilter('userId')">
        读者 {{ routeReaderName || `用户ID ${routeUserId}` }}
      </el-tag>
      <el-button link type="primary" @click="clearAllRouteFilters">清除全部</el-button>
    </div>

    <div class="app-table-card">
      <el-table :data="list" v-loading="loading" class="app-full-width">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="读者" min-width="180">
          <template #default="{ row }">
            <div class="business-cell">
              <button type="button" class="app-field-link business-cell__title" @click="goReaderBorrows(row)">
                {{ readerName(row) }}
              </button>
              <span class="business-cell__meta">{{ readerMeta(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="图书" min-width="240">
          <template #default="{ row }">
            <div class="business-cell">
              <button class="app-field-link book-title-link" type="button" @click="goBookDetail(row)">
                {{ row.bookTitle || `图书 #${row.bookId || '-'}` }}
              </button>
              <span class="business-cell__meta">{{ bookMeta(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="副本码" min-width="210">
          <template #default="{ row }">
            <el-popover
              v-if="row.copyCode"
              placement="right"
              trigger="hover"
              width="300"
              :show-after="180"
              @show="loadCopyBarcode(row)"
            >
              <template #reference>
                <button type="button" class="app-code-link copy-code-link" @click="goBookDetail(row)">
                  {{ row.copyCode }}
                </button>
              </template>
              <div class="app-barcode-popover">
                <div class="app-barcode-popover__title">副本条码</div>
                <div class="app-barcode-popover__plate">
                  <div v-if="row.copyBarcodeLoading" class="app-barcode-popover__state">
                    <el-icon class="is-loading"><Refresh /></el-icon>
                    <span>生成中...</span>
                  </div>
                  <img v-else-if="row.copyBarcodeUrl" :src="row.copyBarcodeUrl" alt="copy barcode" />
                  <div v-else class="app-barcode-popover__state">暂无条码</div>
                </div>
                <div class="app-barcode-popover__meta">
                  <span>副本：{{ row.copyCode }}</span>
                  <span>ISBN：{{ row.isbn || '-' }}</span>
                </div>
                <p class="app-barcode-popover__hint">点击副本码可进入图书详情。</p>
              </div>
            </el-popover>
            <span v-else class="business-cell__meta">副本 #{{ row.copyId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="borrowTime" label="借阅时间" min-width="170" :formatter="fmtTimeCol('borrowTime')" />
        <el-table-column prop="dueTime" label="应还时间" min-width="170" :formatter="fmtTimeCol('dueTime')" />
        <el-table-column prop="returnTime" label="实际归还" min-width="170">
          <template #default="{ row }">
            <span v-if="row.returnTime" class="return-time">{{ fmtVal(row.returnTime) }}</span>
            <span v-else class="app-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="112" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]" size="small" effect="dark" round class="borrow-status-tag">
              {{ statusLabel[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" class-name="borrow-action-column">
          <template #default="{ row }">
            <div class="app-action-group">
              <span v-if="row.status === 1" class="return-done-indicator">
                <el-icon class="action-icon"><Check /></el-icon>
                已归还
              </span>
              <button
                v-else
                type="button"
                class="app-action-link app-action-link--success"
                @click="handleReturn(row)"
              >
                <el-icon class="action-icon"><Check /></el-icon>
                <span>归还</span>
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="borrowDialogVisible" title="扫码借阅" class="app-dialog-md" destroy-on-close @close="stopCamera">
      <el-steps :active="borrowStep" finish-status="success" align-center class="borrow-steps">
        <el-step title="选择读者" />
        <el-step title="扫描图书" />
      </el-steps>

      <div v-if="borrowStep === 0">
        <el-input
          v-model="readerKeyword"
          placeholder="输入用户名 / 姓名搜索读者..."
          clearable
          @input="onReaderSearch"
        />
        <el-table
          :data="readerList"
          v-loading="readerLoading"
          size="small"
          highlight-current-row
          class="reader-picker-table"
          @current-change="selectReader"
        >
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="realName" label="姓名" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '正常' : '封禁' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="borrowStep === 1">
        <div class="selected-reader-line">
          <strong>读者：</strong>{{ selectedReader.username }}（{{ selectedReader.realName || '-' }}）
          <el-button size="small" link type="primary" class="switch-reader-btn" @click="borrowStep = 0">更换</el-button>
        </div>

        <el-tabs v-model="scanMode" type="card">
          <el-tab-pane label="摄像头扫描" name="camera">
            <div v-if="scanMode === 'camera'" ref="scannerContainer" class="scanner-container">
              <div ref="scannerEl" class="scanner-viewport"></div>
            </div>
            <div class="scan-tab-actions">
              <el-button v-if="!cameraActive" type="primary" size="small" @click="startCamera">开启摄像头</el-button>
              <el-button v-else type="danger" size="small" @click="stopCamera">关闭摄像头</el-button>
            </div>
          </el-tab-pane>
          <el-tab-pane label="手动输入 / 扫码枪" name="manual">
            <el-input
              ref="manualInputRef"
              v-model="manualIsbn"
              placeholder="请用扫码枪扫描或手动输入ISBN，按回车确认"
              clearable
              @keyup.enter="lookupIsbn(manualIsbn)"
            >
              <template #append>
                <el-button @click="lookupIsbn(manualIsbn)">查询</el-button>
              </template>
            </el-input>
          </el-tab-pane>
        </el-tabs>

        <div v-if="scannedBook" class="scanned-book-card">
          <el-descriptions title="图书信息" :column="2" size="small" border>
            <el-descriptions-item label="书名">{{ scannedBook.title }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ scannedBook.author || '-' }}</el-descriptions-item>
            <el-descriptions-item label="副本编号">{{ scannedBook.copyCode }}</el-descriptions-item>
            <el-descriptions-item label="可借库存">
              <el-tag :type="scannedBook.currentStock > 0 ? 'success' : 'danger'" size="small">
                {{ scannedBook.currentStock }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <div v-if="lookupError" class="lookup-error">{{ lookupError }}</div>
      </div>

      <template #footer>
        <el-button @click="borrowDialogVisible = false">取消</el-button>
        <el-button
          v-if="borrowStep === 1 && scannedBook"
          type="primary"
          :loading="borrowSubmitting"
          :disabled="scannedBook.currentStock <= 0"
          @click="submitBorrow"
        >
          确认借阅
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Check, Camera } from '@element-plus/icons-vue'
import Quagga from '@ericblade/quagga2'
import { getBorrowList, doReturn, refreshStatus, createBorrow, getUserList, lookupCopyByCode, generateBarcode } from '../../api/admin'
import { formatDateTime } from '../../utils/formatters'

const route = useRoute()
const router = useRouter()
const list = ref([])
const loading = ref(false)
const refreshing = ref(false)
const filterStatus = ref(null)
const filterKeyword = ref('')
const routeUserId = ref(null)
const routeReaderName = ref('')
const routeBookId = ref(null)
const routeBookTitle = ref('')
const routeMonth = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)

const statusLabel = { 0: '借阅中', 1: '已归还', 2: '已逾期' }
const statusType = { 0: 'primary', 1: 'success', 2: 'danger' }
const copyBarcodeUrls = new Set()

const fmtVal = (val) => formatDateTime(val)
const fmtTimeCol = (key) => (row) => fmtVal(row[key])
const readerName = (row) => row.realName || row.nickname || row.username || `用户 #${row.userId || '-'}`
const readerMeta = (row) => {
  const parts = []
  if (row.username && row.username !== readerName(row)) parts.push(row.username)
  if (row.phone) parts.push(row.phone)
  if (row.userId) parts.push(`ID ${row.userId}`)
  return parts.join(' / ') || '-'
}
const bookMeta = (row) => {
  const parts = []
  if (row.bookAuthor) parts.push(row.bookAuthor)
  if (row.isbn) parts.push(row.isbn)
  if (row.bookId) parts.push(`ID ${row.bookId}`)
  return parts.join(' / ') || '-'
}
const goBookDetail = (row) => {
  if (!row.bookId) return
  router.push(`/admin/books/${row.bookId}`)
}
const loadCopyBarcode = async (row) => {
  if (!row?.copyCode || row.copyBarcodeUrl || row.copyBarcodeLoading) return
  row.copyBarcodeLoading = true
  try {
    const blob = await generateBarcode(row.copyCode)
    const url = URL.createObjectURL(blob)
    copyBarcodeUrls.add(url)
    row.copyBarcodeUrl = url
  } finally {
    row.copyBarcodeLoading = false
  }
}
const parseStatus = (value) => {
  const num = Number(value)
  return [0, 1, 2].includes(num) ? num : null
}
const parsePositiveId = (value) => {
  const num = Number(value)
  return Number.isInteger(num) && num > 0 ? num : null
}
const parseMonth = (value) => {
  if (typeof value !== 'string') return ''
  return /^\d{4}-\d{2}$/.test(value) ? value : ''
}
const syncFiltersFromRoute = () => {
  filterStatus.value = parseStatus(route.query.status)
  routeUserId.value = parsePositiveId(route.query.userId)
  routeReaderName.value = typeof route.query.readerName === 'string' ? route.query.readerName : ''
  routeBookId.value = parsePositiveId(route.query.bookId)
  routeBookTitle.value = typeof route.query.bookTitle === 'string' ? route.query.bookTitle : ''
  routeMonth.value = parseMonth(route.query.month)
}

const clearRouteFilter = (key) => {
  const query = { ...route.query }
  if (key === 'bookId') {
    delete query.bookId
    delete query.bookTitle
  } else if (key === 'userId') {
    delete query.userId
    delete query.readerName
  } else {
    delete query[key]
  }
  router.replace({ path: '/admin/borrows', query }).catch(() => {})
}

const clearAllRouteFilters = () => {
  const query = { ...route.query }
  delete query.userId
  delete query.readerName
  delete query.bookId
  delete query.bookTitle
  delete query.month
  router.replace({ path: '/admin/borrows', query }).catch(() => {})
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value, status: filterStatus.value }
    if (routeUserId.value !== null) params.userId = routeUserId.value
    if (routeBookId.value !== null) params.bookId = routeBookId.value
    if (routeBookId.value === null && routeBookTitle.value?.trim()) params.bookTitle = routeBookTitle.value.trim()
    if (routeMonth.value) params.month = routeMonth.value

    if (filterKeyword.value?.trim()) {
      const kw = filterKeyword.value.trim()
      const userMatch = /^u:(\d+)$/i.exec(kw)
      const bookMatch = /^b:(\d+)$/i.exec(kw)
      if (userMatch && routeUserId.value === null) params.userId = parseInt(userMatch[1], 10)
      else if (bookMatch && routeBookId.value === null) params.bookId = parseInt(bookMatch[1], 10)
      else params.keyword = kw
    }
    const res = await getBorrowList(params)
    list.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const goReaderBorrows = (row) => {
  if (!row?.userId) return
  router.push({
    path: '/admin/borrows',
    query: {
      userId: String(row.userId),
      readerName: readerName(row)
    }
  }).catch(() => {})
}

const handleReturn = async (row) => {
  if (row.status === 1) return
  await ElMessageBox.confirm('确认该图书已归还？', '提示', { type: 'warning' })
  await doReturn(row.id)
  ElMessage.success('归还成功')
  await loadData()
}

const refreshOverdue = async () => {
  refreshing.value = true
  try {
    await refreshStatus()
    ElMessage.success('逾期状态已刷新')
    await loadData()
  } finally { refreshing.value = false }
}

// --- 扫码借阅 ---
const borrowDialogVisible = ref(false)
const borrowStep = ref(0)
const readerKeyword = ref('')
const readerList = ref([])
const readerLoading = ref(false)
const selectedReader = ref(null)
const scanMode = ref('manual')
const manualIsbn = ref('')
const scannedBook = ref(null)
const lookupError = ref('')
const borrowSubmitting = ref(false)
const cameraActive = ref(false)
const scannerEl = ref(null)
const manualInputRef = ref(null)

let readerSearchTimer = null

const openBorrow = () => {
  borrowDialogVisible.value = true
  borrowStep.value = 0
  readerKeyword.value = ''
  readerList.value = []
  selectedReader.value = null
  scanMode.value = 'manual'
  manualIsbn.value = ''
  scannedBook.value = null
  lookupError.value = ''
  cameraActive.value = false
  searchReaders('')
}

const searchReaders = async (keyword) => {
  readerLoading.value = true
  try {
    const res = await getUserList({ current: 1, size: 20, keyword })
    readerList.value = (res.data.records || []).filter(u => u.role === 1)
  } finally { readerLoading.value = false }
}

const onReaderSearch = () => {
  clearTimeout(readerSearchTimer)
  readerSearchTimer = setTimeout(() => searchReaders(readerKeyword.value), 300)
}

const selectReader = (row) => {
  if (!row) return
  if (row.status === 0) {
    ElMessage.warning('该用户已被封禁，无法借阅')
    return
  }
  selectedReader.value = row
  borrowStep.value = 1
  nextTick(() => {
    if (scanMode.value === 'manual' && manualInputRef.value) {
      manualInputRef.value.focus()
    }
  })
}

watch(scanMode, (val) => {
  scannedBook.value = null
  lookupError.value = ''
  if (val === 'manual') {
    stopCamera()
    nextTick(() => manualInputRef.value?.focus())
  } else {
    manualIsbn.value = ''
  }
})

const lookupIsbn = async (code) => {
  if (!code || !code.trim()) return
  lookupError.value = ''
  scannedBook.value = null
  try {
    const res = await lookupCopyByCode(code.trim())
    scannedBook.value = res.data
  } catch {
    lookupError.value = '未找到该编号对应的副本'
  }
}

const onDetected = (result) => {
  const code = result.codeResult?.code
  if (code) {
    stopCamera()
    lookupIsbn(code)
  }
}

const startCamera = async () => {
  try {
    await Quagga.init({
      inputStream: {
        name: 'Live',
        type: 'LiveStream',
        target: scannerEl.value,
        constraints: { facingMode: 'environment' }
      },
      decoder: { readers: ['code_128_reader', 'ean_reader'] }
    }, (err) => {
      if (err) {
        ElMessage.error('无法访问摄像头，请检查权限')
        return
      }
      Quagga.start()
      cameraActive.value = true
    })
    Quagga.onDetected(onDetected)
  } catch {
    ElMessage.error('摄像头初始化失败')
  }
}

const stopCamera = () => {
  if (cameraActive.value) {
    Quagga.stop()
    Quagga.offDetected(onDetected)
    cameraActive.value = false
  }
}

const submitBorrow = async () => {
  if (!selectedReader.value || !scannedBook.value) return
  if (borrowSubmitting.value) return
  borrowSubmitting.value = true
  try {
    await createBorrow({ userId: selectedReader.value.id, copyId: scannedBook.value.copyId })
    ElMessage.success('借阅成功')
    borrowDialogVisible.value = false
    await loadData()
  } finally { borrowSubmitting.value = false }
}

watch(() => [
  route.query.status,
  route.query.userId,
  route.query.readerName,
  route.query.bookId,
  route.query.bookTitle,
  route.query.month
], () => {
  syncFiltersFromRoute()
  current.value = 1
  loadData()
})

onMounted(() => {
  syncFiltersFromRoute()
  loadData()
})

onBeforeUnmount(() => {
  copyBarcodeUrls.forEach(url => URL.revokeObjectURL(url))
  copyBarcodeUrls.clear()
})
</script>

<style scoped>

.toolbar-spacer { flex: 1; }
.route-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}
.route-filters__label {
  color: var(--app-text-secondary);
  font-size: 13px;
}
.borrow-steps {
  margin-bottom: 24px;
}
.reader-picker-table {
  margin-top: 12px;
  cursor: pointer;
}
.selected-reader-line {
  margin-bottom: 12px;
}
.switch-reader-btn {
  margin-left: 8px;
}
.scan-tab-actions {
  margin-top: 8px;
  text-align: center;
}
.lookup-error {
  margin-top: 8px;
  color: var(--app-danger);
  font-size: 13px;
}
.business-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.business-cell__title {
  color: var(--app-text-primary);
  font-weight: 700;
  line-height: 1.25;
}
.business-cell__meta {
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.2;
}
.book-title-link {
  font-weight: 800;
}
.copy-code-link {
  display: inline-flex;
}
.return-time {
  color: var(--app-success);
}
.borrow-status-tag {
  min-width: 64px;
  justify-content: center;
}
.return-done-indicator {
  min-width: 70px;
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-radius: 7px;
  color: var(--app-success);
  background: var(--app-success-soft);
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
}
.action-icon {
  font-size: 13px;
  margin-right: 3px;
}

.pag-wrap { display: flex; justify-content: flex-end; margin-top: 18px; }
.scanner-container {
  width: 100%; max-width: 480px; margin: 0 auto;
  border: 2px solid var(--app-border-strong); border-radius: 10px; overflow: hidden;
  background: #000000;
}
.scanner-viewport {
  width: 100%; min-height: 240px;
}
.scanner-viewport video,
.scanner-viewport canvas {
  width: 100% !important;
}
.scanned-book-card {
  margin-top: 16px;
  background: var(--app-surface-soft); border-radius: 10px; padding: 14px;
}
</style>
