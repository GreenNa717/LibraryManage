<template>
  <div class="book-hall">
    <div class="page-head">
      <div>
        <h2 class="page-title">图书大厅</h2>
        <p class="sub-title">查书、扫码副本码并完成借阅</p>
      </div>
      <el-button circle type="primary" class="scan-btn" @click="openBorrowDrawer()">
        <el-icon><Camera /></el-icon>
      </el-button>
    </div>

    <el-input
      v-model="keyword"
      class="search-box"
      placeholder="搜索书名或作者..."
      clearable
      prefix-icon="Search"
      @keyup.enter="reload"
      @clear="reload"
    >
      <template #append>
        <el-button @click="reload">搜索</el-button>
      </template>
    </el-input>

    <div class="book-list" v-loading="loading">
      <article v-for="book in list" :key="book.id" class="book-card app-mobile-card">
        <div class="cover">
          <img v-if="book.coverImage" :src="book.coverImage" alt="封面" />
          <span v-else>暂无封面</span>
        </div>
        <div class="book-info">
          <h3>{{ book.title }}</h3>
          <p>{{ book.author || '未知作者' }}</p>
          <div class="meta-row">
            <el-tag :type="book.currentStock > 0 ? 'success' : 'danger'" size="small" round>
              可借 {{ book.currentStock ?? 0 }}
            </el-tag>
            <span>总藏 {{ book.totalStock ?? 0 }}</span>
          </div>
          <div class="actions">
            <el-button size="small" plain @click="openDetail(book)">详情</el-button>
            <el-button size="small" type="primary" :disabled="book.currentStock <= 0" @click="openBorrowDrawer(book)">
              借阅
            </el-button>
          </div>
        </div>
      </article>
      <el-empty v-if="!loading && !list.length" description="没有找到图书" />
    </div>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="current"
        v-model:page-size="size"
        :total="total"
        size="small"
        layout="prev, pager, next"
        @current-change="loadData"
      />
    </div>

    <el-dialog v-model="detailVisible" title="图书详情" width="92%" destroy-on-close>
      <el-descriptions v-if="selectedBook" :column="1" border>
        <el-descriptions-item label="书名">{{ selectedBook.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="作者">{{ selectedBook.author || '-' }}</el-descriptions-item>
        <el-descriptions-item label="ISBN">{{ selectedBook.isbn || '-' }}</el-descriptions-item>
        <el-descriptions-item label="可借库存">{{ selectedBook.currentStock ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="简介">{{ selectedBook.description || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="selectedBook?.currentStock <= 0" @click="openBorrowDrawer(selectedBook)">
          借阅此书
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="borrowVisible"
      direction="btt"
      size="82%"
      :with-header="false"
      class="scan-drawer"
      destroy-on-close
      @close="stopCamera"
    >
      <div class="drawer-head">
        <div>
          <h3>扫码借阅</h3>
          <p v-if="selectedBook">{{ selectedBook.title }}</p>
          <p v-else>扫描书上副本条码</p>
        </div>
        <el-button text @click="borrowVisible = false">关闭</el-button>
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
            @keyup.enter="submitBorrow"
          />
        </el-tab-pane>
      </el-tabs>

      <el-button class="confirm-btn" type="primary" size="large" :loading="borrowing" @click="submitBorrow">
        确认借阅
      </el-button>
    </el-drawer>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import Quagga from '@ericblade/quagga2'
import { createUserBorrow, getUserBookList } from '../../api/user'

const loading = ref(false)
const borrowing = ref(false)
const list = ref([])
const current = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')
const selectedBook = ref(null)
const detailVisible = ref(false)
const borrowVisible = ref(false)
const copyCode = ref('')
const copyInputRef = ref()
const scannerEl = ref()
const scanMode = ref('manual')
const cameraActive = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserBookList({
      current: current.value,
      size: size.value,
      keyword: keyword.value
    })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const reload = () => {
  current.value = 1
  loadData()
}

const openDetail = (book) => {
  selectedBook.value = book
  detailVisible.value = true
}

const openBorrowDrawer = (book = null) => {
  selectedBook.value = book
  copyCode.value = ''
  scanMode.value = 'manual'
  borrowVisible.value = true
  detailVisible.value = false
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
  submitBorrow()
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

const submitBorrow = async () => {
  if (borrowing.value) return
  const code = copyCode.value.trim()
  if (!code) {
    ElMessage.warning('请先扫描或输入副本码')
    return
  }
  borrowing.value = true
  try {
    await createUserBorrow({ copyCode: code })
    ElMessage.success('借阅成功')
    borrowVisible.value = false
    await loadData()
  } catch {
  } finally {
    borrowing.value = false
  }
}

onMounted(loadData)
onBeforeUnmount(stopCamera)
</script>

<style scoped>
.book-hall {
  min-height: 100%;
}

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
  flex-shrink: 0;
  width: 42px;
  height: 42px;
}

.search-box {
  margin-bottom: 14px;
}

.book-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 220px;
}

.book-card {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  gap: 12px;
  padding: 12px;
}

.cover {
  width: 82px;
  height: 112px;
  border-radius: 6px;
  background: var(--app-surface-soft);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-text-muted);
  font-size: 12px;
  text-align: center;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.book-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.book-info h3 {
  margin: 0;
  color: var(--app-text-primary);
  font-size: 16px;
  line-height: 1.35;
  font-weight: 700;
}

.book-info p {
  margin: 0;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--app-text-secondary);
  font-size: 12px;
}

.actions {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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
