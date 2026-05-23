<template>
  <div class="my-borrows">
    <div class="page-head">
      <div>
        <h2 class="page-title">我的借阅</h2>
        <p class="sub-title">查看借阅状态，扫码归还图书</p>
      </div>
      <el-button circle type="success" class="scan-btn" @click="openReturnDrawer">
        <el-icon><Camera /></el-icon>
      </el-button>
    </div>

    <el-segmented v-model="statusTab" :options="statusOptions" class="status-tabs" @change="reload" />

    <div class="borrow-list" v-loading="loading">
      <article v-for="item in list" :key="item.id" class="borrow-card app-mobile-card">
        <div class="borrow-card__main">
          <h3>{{ bookTitle(item.bookId) }}</h3>
          <el-tag :type="statusType[item.status]" size="small" effect="dark" round>
            {{ statusLabel[item.status] }}
          </el-tag>
        </div>
        <div class="borrow-meta">
          <span>副本ID {{ item.copyId || '-' }}</span>
          <span>借阅 {{ fmtTime(item.borrowTime) }}</span>
          <span>应还 {{ fmtTime(item.dueTime) }}</span>
          <span v-if="item.returnTime">归还 {{ fmtTime(item.returnTime) }}</span>
        </div>
        <div v-if="item.status !== 1" class="borrow-actions">
          <el-button type="success" size="small" @click="handleReturn(item)">归还</el-button>
        </div>
      </article>
      <el-empty v-if="!loading && !list.length" description="暂无借阅记录" />
    </div>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="current"
        v-model:page-size="size"
        :total="total"
        size="small"
        layout="prev, pager, next"}},{
        @current-change="loadData"
      />
    </div>

    <el-drawer
      v-model="returnVisible"
      direction="btt"
      size="82%"
      :with-header="false"
      class="scan-drawer"
      destroy-on-close
      @close="stopCamera"
    >
      <div class="drawer-head">
        <div>
          <h3>扫码归还</h3>
          <p>扫描图书副本条码完成归还</p>
        </div>
        <el-button text @click="returnVisible = false">关闭</el-button>
      </div>

      <el-tabs v-model="scanMode" stretch @tab-change="handleScanModeChange">
        <el-tab-pane label="摄像头" name="camera">
          <div ref="scannerEl" class="scanner-viewport"></div>
          <div class="scan-actions">
            <el-button v-if="!cameraActive" type="primary" @click="startCamera">开启摄像头</el-button>
            <el-button v-else type="danger" @click="stopCamera">关闭摄像头</el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="手动输入" name="manual">
          <el-input
            ref="copyInputRef"
            v-model="copyCode"
            size="large"
            placeholder="输入或扫码枪录入副本码"
            clearable
            @keyup.enter="submitReturnByCode"
          />
        </el-tab-pane>
      </el-tabs>

      <el-button class="confirm-btn" type="success" size="large" :loading="returning" @click="submitReturnByCode">
        确认归还
      </el-button>
    </el-drawer>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import Quagga from '@ericblade/quagga2'
import { getMyBorrows, getUserBookById, returnMyBorrow, returnMyBorrowByCopyCode } from '../../api/user'
import { formatDateTime } from '../../utils/formatters'

const loading = ref(false)
const returning = ref(false)
const list = ref([])
const current = ref(1)
const size = ref(10)
const total = ref(0)
const statusTab = ref('all')
const bookMap = reactive({})
const returnVisible = ref(false)
const copyCode = ref('')
const copyInputRef = ref()
const scannerEl = ref()
const scanMode = ref('manual')
const cameraActive = ref(false)

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '借阅中', value: '0' },
  { label: '已归还', value: '1' },
  { label: '逾期', value: '2' }
]
const statusLabel = { 0: '借阅中', 1: '已归还', 2: '已逾期' }
const statusType = { 0: 'primary', 1: 'success', 2: 'danger' }

const fmtTime = (value) => formatDateTime(value)
const bookTitle = (bookId) => bookMap[bookId] || `图书 #${bookId}`
const currentStatus = () => statusTab.value === 'all' ? null : Number(statusTab.value)

const loadBookNames = async () => {
  const ids = [...new Set(list.value.map(item => item.bookId).filter(Boolean))]
  await Promise.all(ids.map(async (id) => {
    if (bookMap[id]) return
    try {
      const res = await getUserBookById(id)
      bookMap[id] = res.data?.title || `图书 #${id}`
    } catch {
      bookMap[id] = `图书 #${id}`
    }
  }))
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyBorrows({
      current: current.value,
      size: size.value,
      status: currentStatus()
    })
    list.value = res.data.records || []
    total.value = res.data.total || 0
    await loadBookNames()
  } finally {
    loading.value = false
  }
}

const reload = () => {
  current.value = 1
  loadData()
}

const handleReturn = async (row) => {
  if (returning.value) return
  await ElMessageBox.confirm('确认归还这本图书？', '提示', { type: 'warning' })
  returning.value = true
  try {
    await returnMyBorrow(row.id)
    ElMessage.success('归还成功')
    await loadData()
  } catch {
  } finally {
    returning.value = false
  }
}

const openReturnDrawer = () => {
  copyCode.value = ''
  scanMode.value = 'manual'
  returnVisible.value = true
  nextTick(() => copyInputRef.value?.focus())
}

const handleScanModeChange = (name) => {
  if (name === 'manual') {
    stopCamera()
    nextTick(() => copyInputRef.value?.focus())
  }
}

const onDetected = (result) => {
  const code = result.codeResult?.code
  if (!code) return
  copyCode.value = code
  stopCamera()
  submitReturnByCode()
}

const startCamera = async () => {
  await nextTick()
  if (!scannerEl.value || cameraActive.value) return
  Quagga.init({
    inputStream: {
      name: 'Live',
      type: 'LiveStream',
      target: scannerEl.value,
      constraints: { facingMode: 'environment' }
    },
    decoder: { readers: ['code_128_reader', 'ean_reader'] }
  }, (err) => {
    if (err) {
      ElMessage.error('无法访问摄像头，请检查浏览器权限')
      return
    }
    Quagga.start()
    cameraActive.value = true
    Quagga.onDetected(onDetected)
  })
}

const stopCamera = () => {
  if (!cameraActive.value) return
  Quagga.offDetected(onDetected)
  Quagga.stop()
  cameraActive.value = false
}

const submitReturnByCode = async () => {
  if (returning.value) return
  const code = copyCode.value.trim()
  if (!code) {
    ElMessage.warning('请先扫描或输入副本码')
    return
  }
  returning.value = true
  try {
    await returnMyBorrowByCopyCode(code)
    ElMessage.success('归还成功')
    returnVisible.value = false
    await loadData()
  } catch {
  } finally {
    returning.value = false
  }
}

onMounted(loadData)
onBeforeUnmount(stopCamera)
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--app-text-primary);
  margin: 0;
}

.sub-title {
  margin: 4px 0 0;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.scan-btn {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
}

.status-tabs {
  width: 100%;
  margin-bottom: 14px;
}

.borrow-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 220px;
}

.borrow-card {
  padding: 14px;
}

.borrow-card__main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.borrow-card h3 {
  margin: 0;
  color: var(--app-text-primary);
  font-size: 16px;
  line-height: 1.35;
}

.borrow-meta {
  display: grid;
  gap: 6px;
  margin-top: 10px;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.borrow-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.drawer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.drawer-head h3 {
  margin: 0;
  font-size: 20px;
  color: var(--app-text-primary);
}

.drawer-head p {
  margin: 4px 0 0;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.scanner-viewport {
  width: 100%;
  min-height: 260px;
  border-radius: var(--app-card-radius);
  overflow: hidden;
  background: #000000;
}

.scanner-viewport :deep(video),
.scanner-viewport :deep(canvas) {
  width: 100% !important;
}

.scan-actions {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}

.confirm-btn {
  width: 100%;
  margin-top: 18px;
}

:deep(.scan-drawer) {
  max-width: 480px;
  left: 50% !important;
  transform: translateX(-50%);
  border-radius: 18px 18px 0 0;
}
</style>
