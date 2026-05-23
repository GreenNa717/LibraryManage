<template>
  <div>
    <h2 class="app-page-title">图书管理</h2>

    <div class="app-toolbar">
      <el-button type="primary" @click="openAdd">
        <el-icon class="app-inline-icon"><Plus /></el-icon>
        新增图书
      </el-button>
      <el-select
        v-model="categoryFilter"
        placeholder="按分类筛选"
        clearable
        class="app-control-sm"
        @change="handleCategoryFilterChange"
      >
        <el-option label="全部分类" :value="0" />
        <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索书名、作者或ISBN..."
        prefix-icon="Search"
        class="app-control-md"
        clearable
        @clear="loadData"
        @keyup.enter="loadData"
      />
    </div>

    <div v-if="locationFilter !== null || lostOnlyFilter || authorFilter || isbnFilter" class="dashboard-filters">
      <span class="dashboard-filters__label">当前筛选：</span>
      <el-tag v-if="authorFilter" type="primary" closable @close="clearAuthorFilter">
        作者 {{ authorFilter }}
      </el-tag>
      <el-tag v-if="isbnFilter" type="info" closable @close="clearIsbnFilter">
        ISBN {{ isbnFilter }}
      </el-tag>
      <el-tag v-if="locationFilter !== null" type="success" closable @close="clearLocationFilter">
        库位 {{ locationName || `#${locationFilter}` }}
      </el-tag>
      <el-tag v-if="lostOnlyFilter" type="danger" closable @close="clearLostOnlyFilter">仅看丢失相关图书</el-tag>
      <el-button link type="primary" @click="clearAllDashboardFilters">清除全部</el-button>
    </div>

    <div class="app-table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="isbn" label="ISBN" width="170">
          <template #default="{ row }">
            <el-popover
              placement="right"
              trigger="hover"
              width="280"
              :show-after="180"
              @show="loadInlineBarcode(row)"
            >
              <template #reference>
                <button type="button" class="app-code-link isbn-link" @click="goIsbnMetadata(row)">
                  {{ row.isbn || '-' }}
                </button>
              </template>
              <div class="app-barcode-popover">
                <div class="app-barcode-popover__title">ISBN 一维码</div>
                <div class="app-barcode-popover__plate">
                  <div v-if="row.isbnBarcodeLoading" class="app-barcode-popover__state">
                    <el-icon class="is-loading"><Loading /></el-icon>
                    <span>生成中...</span>
                  </div>
                  <img v-else-if="row.isbnBarcodeUrl" :src="row.isbnBarcodeUrl" alt="ISBN barcode" />
                  <div v-else class="app-barcode-popover__state">暂无 ISBN</div>
                </div>
                <p class="app-barcode-popover__hint">点击 ISBN 可进入本地 ISBN 资料库。</p>
              </div>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="书名" min-width="180">
          <template #default="{ row }">
            <button
              type="button"
              class="book-title-btn"
              :class="{ 'is-selected': selectedBookId === row.id }"
              @click="goBookDetail(row)"
            >
              <span class="book-title">{{ row.title }}</span>
              <el-icon class="title-arrow"><ArrowRight /></el-icon>
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="160">
          <template #default="{ row }">
            <button
              v-if="row.author"
              type="button"
              class="app-field-link author-link"
              @click="goAuthorBooks(row)"
            >
              {{ row.author }}
            </button>
            <span v-else class="app-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="categoryId" label="分类" width="90" :formatter="categoryFormatter">
          <template #default="{ row }">
            <el-tag
              size="small"
              type="info"
              effect="plain"
              class="category-tag"
              @click="handleCategoryTagClick(row)"
            >
              {{ categoryFormatter(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="locationId" label="库位" width="130">
          <template #default="{ row }">
            <button
              v-if="row.locationId"
              type="button"
              class="app-field-link shelf-link"
              @click="goShelfBooks(row)"
            >
              {{ shelfFormatter(row.locationId) }}
            </button>
            <span v-else class="app-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalStock" label="总库存" width="90" align="center">
          <template #default="{ row }">
            <button type="button" class="stock-pill stock-pill--total" @click="goBookDetail(row)">
              {{ row.totalStock }}
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="currentStock" label="可借" width="80" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.currentStock > 0 ? 'success' : 'danger'"
              size="small"
              effect="dark"
              round
              class="stock-tag"
              @click="goBookDetail(row)"
            >
              {{ row.currentStock }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="录入时间" width="170" :formatter="fmtTime" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="app-action-group">
              <button type="button" class="app-action-link app-action-link--success" @click="showBarcodes(row)">条码</button>
              <button type="button" class="app-action-link" @click="openEdit(row)">编辑</button>
              <button type="button" class="app-action-link app-action-link--danger" @click="handleDelete(row.id)">删除</button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑图书' : '新增图书'" class="app-dialog-md" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="80">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="ISBN" prop="isbn">
              <el-input v-model="form.isbn" placeholder="978-7-xxx-xxxxx-x" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="书名" prop="title">
              <el-input v-model="form.title" placeholder="请输入书名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者" prop="author">
              <el-input v-model="form.author" placeholder="请输入作者" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" class="app-full-width">
                <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库位" prop="locationId">
              <el-select v-model="form.locationId" clearable class="app-full-width">
                <el-option v-for="s in shelves" :key="s.id" :label="s.locationName" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="!isEdit" label="入库数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" class="app-full-width" />
            </el-form-item>
            <el-form-item v-else label="总库存">
              <el-input-number v-model="form.totalStock" :min="0" class="app-full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="封面URL" prop="coverImage">
              <el-input v-model="form.coverImage" placeholder="图片链接（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="简介" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="图书简介（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 条码对话框（批量展示副本条码） -->
    <el-dialog v-model="barcodeVisible" title="图书副本条码" class="app-dialog-lg" destroy-on-close>
      <div v-if="barcodeLoading" class="app-empty-state">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      </div>
      <div v-else-if="barcodeCopies.length" class="barcode-grid">
        <div v-for="copy in barcodeCopies" :key="copy.id" class="barcode-item">
          <img :src="copy.barcodeUrl" alt="条码" class="barcode-img" />
          <p class="barcode-code">{{ copy.copyCode }}</p>
          <el-tag :type="copy.status === 0 ? 'success' : copy.status === 1 ? 'warning' : 'danger'" size="small">
            {{ copy.status === 0 ? '在馆' : copy.status === 1 ? '借出' : '丢失' }}
          </el-tag>
        </div>
      </div>
      <div v-else class="empty-placeholder">暂无副本记录</div>
      <template #footer>
        <el-button @click="downloadAllBarcodes" :disabled="!barcodeCopies.length">批量下载</el-button>
        <el-button type="primary" @click="barcodeVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Loading, ArrowRight } from '@element-plus/icons-vue'
import { getBookList, addBook, updateBook, deleteBook, getCategoryList, getShelfList, getBookCopies, generateBarcode } from '../../api/admin'
import { formatDateTime } from '../../utils/formatters'

const route = useRoute()
const router = useRouter()
const list = ref([])
const loading = ref(false)
const keyword = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)
const categories = ref([])
const shelves = ref([])
const categoryFilter = ref(0)
const locationFilter = ref(null)
const locationName = ref('')
const lostOnlyFilter = ref(false)
const authorFilter = ref('')
const isbnFilter = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const barcodeVisible = ref(false)
const barcodeLoading = ref(false)
const barcodeCopies = ref([])
const selectedBookId = ref(null)
let titleClickTimer = null
const inlineBarcodeUrls = new Set()
const form = reactive({
  isbn: '', title: '', author: '', categoryId: null, locationId: null, coverImage: '', description: '', quantity: 1, totalStock: 1
})
const formRules = {
  isbn: [{ required: true, message: '请填写ISBN', trigger: 'blur' }],
  title: [{ required: true, message: '请填写书名', trigger: 'blur' }]
}

const loadCategories = async () => {
  const res = await getCategoryList()
  categories.value = res.data
}

const loadShelves = async () => {
  const res = await getShelfList()
  shelves.value = res.data || []
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getBookList({
      current: current.value,
      size: size.value,
      keyword: keyword.value,
      categoryId: categoryFilter.value || undefined,
      locationId: locationFilter.value || undefined,
      lostOnly: lostOnlyFilter.value || undefined,
      author: authorFilter.value || undefined,
      isbn: isbnFilter.value || undefined
    })
    list.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const categoryFormatter = (row) => {
  const c = categories.value.find(x => x.id === row.categoryId)
  return c ? c.categoryName : '-'
}

const shelfFormatter = (id) => {
  const shelf = shelves.value.find(x => x.id === id)
  return shelf ? shelf.locationName : '-'
}

const fmtTime = (row) => formatDateTime(row.createTime)

const parseCategoryId = (value) => {
  const num = Number(value)
  return Number.isInteger(num) && num > 0 ? num : 0
}

const parseRouteBool = (value) => {
  return value === true || value === 'true' || value === '1'
}

const syncFiltersFromRoute = () => {
  categoryFilter.value = parseCategoryId(route.query.categoryId)
  locationFilter.value = parseCategoryId(route.query.locationId)
  locationName.value = typeof route.query.locationName === 'string' ? route.query.locationName : ''
  lostOnlyFilter.value = parseRouteBool(route.query.lostOnly)
  authorFilter.value = typeof route.query.author === 'string' ? route.query.author : ''
  isbnFilter.value = typeof route.query.isbn === 'string' ? route.query.isbn : ''
  keyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : ''
}

const handleCategoryFilterChange = (value) => {
  const nextCategoryId = parseCategoryId(value)
  const currentCategoryId = parseCategoryId(route.query.categoryId)
  if (nextCategoryId === currentCategoryId) {
    current.value = 1
    loadData()
    return
  }
  const query = { ...route.query }
  if (nextCategoryId) query.categoryId = String(nextCategoryId)
  else delete query.categoryId
  pushRoute({ path: '/admin/books', query })
}

const handleCategoryTagClick = (row) => {
  if (!row.categoryId) return
  handleCategoryFilterChange(row.categoryId)
}

const clearLocationFilter = () => {
  const query = { ...route.query }
  delete query.locationId
  delete query.locationName
  pushRoute({ path: '/admin/books', query })
}

const clearAuthorFilter = () => {
  const query = { ...route.query }
  delete query.author
  pushRoute({ path: '/admin/books', query })
}

const clearIsbnFilter = () => {
  const query = { ...route.query }
  delete query.isbn
  pushRoute({ path: '/admin/books', query })
}

const clearLostOnlyFilter = () => {
  const query = { ...route.query }
  delete query.lostOnly
  pushRoute({ path: '/admin/books', query })
}

const clearAllDashboardFilters = () => {
  const query = { ...route.query }
  delete query.locationId
  delete query.locationName
  delete query.lostOnly
  delete query.author
  delete query.isbn
  pushRoute({ path: '/admin/books', query })
}

const cancelPendingTitleNavigation = () => {
  if (!titleClickTimer) return
  clearTimeout(titleClickTimer)
  titleClickTimer = null
}

const pushRoute = (target) => {
  cancelPendingTitleNavigation()
  router.push(target).catch(() => {})
}

const goIsbnMetadata = (row) => {
  if (!row?.isbn) return
  pushRoute({ path: '/admin/isbn-metadata', query: { keyword: row.isbn } })
}

const goAuthorBooks = (row) => {
  if (!row?.author) return
  pushRoute({ path: '/admin/books', query: { author: row.author } })
}

const goShelfBooks = (row) => {
  if (!row?.locationId) return
  pushRoute({
    path: '/admin/books',
    query: { locationId: String(row.locationId), locationName: shelfFormatter(row.locationId) }
  })
}

const goBookDetail = (row) => {
  if (!row?.id) return
  cancelPendingTitleNavigation()
  selectedBookId.value = row.id
  titleClickTimer = setTimeout(() => {
    titleClickTimer = null
    const from = route.fullPath
    router.push({ path: `/admin/books/${row.id}`, query: { from } }).catch(() => {})
  }, 220)
}

const openAdd = () => {
  isEdit.value = false
  Object.assign(form, { isbn: '', title: '', author: '', categoryId: null, locationId: null, coverImage: '', description: '', quantity: 1, totalStock: 1 })
  dialogVisible.value = true
}
const openEdit = (row) => {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  if (isEdit.value) {
    await updateBook(form.id, form)
  } else {
    await addBook({ ...form, quantity: form.quantity })
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该图书？', '提示', { type: 'warning' })
  await deleteBook(id)
  ElMessage.success('删除成功')
  loadData()
}

const showBarcodes = async (row) => {
  barcodeVisible.value = true
  barcodeLoading.value = true
  barcodeCopies.value = []
  try {
    const res = await getBookCopies(row.id)
    const copies = res.data || []
    const withBarcodes = await Promise.all(copies.map(async (copy) => {
      try {
        const blob = await generateBarcode(copy.copyCode)
        return { ...copy, barcodeUrl: URL.createObjectURL(blob) }
      } catch {
        return { ...copy, barcodeUrl: '' }
      }
    }))
    barcodeCopies.value = withBarcodes
  } catch {
    ElMessage.error('获取副本列表失败')
  } finally {
    barcodeLoading.value = false
  }
}

const loadInlineBarcode = async (row) => {
  if (!row?.isbn || row.isbnBarcodeUrl || row.isbnBarcodeLoading) return
  row.isbnBarcodeLoading = true
  try {
    const blob = await generateBarcode(row.isbn)
    const url = URL.createObjectURL(blob)
    inlineBarcodeUrls.add(url)
    row.isbnBarcodeUrl = url
  } finally {
    row.isbnBarcodeLoading = false
  }
}

const downloadAllBarcodes = () => {
  barcodeCopies.value.forEach((copy, i) => {
    if (!copy.barcodeUrl) return
    const a = document.createElement('a')
    a.href = copy.barcodeUrl
    a.download = `barcode-${copy.copyCode}.png`
    setTimeout(() => a.click(), i * 200)
  })
}

watch(() => [
  route.query.categoryId,
  route.query.locationId,
  route.query.locationName,
  route.query.lostOnly,
  route.query.keyword,
  route.query.author,
  route.query.isbn
], () => {
  syncFiltersFromRoute()
  current.value = 1
  loadData()
})

onMounted(() => {
  loadCategories()
  loadShelves()
  syncFiltersFromRoute()
  loadData()
})

onBeforeUnmount(() => {
  if (titleClickTimer) clearTimeout(titleClickTimer)
  inlineBarcodeUrls.forEach(url => URL.revokeObjectURL(url))
})
</script>

<style scoped>

.dashboard-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.dashboard-filters__label {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.book-title-btn {
  border: none;
  background: transparent;
  padding: 4px 8px;
  margin-left: -8px;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: transform 0.24s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.24s ease, box-shadow 0.24s ease, color 0.24s ease;
}
.book-title-btn:hover {
  transform: translateX(2px);
  background: var(--app-primary-soft);
}
.book-title-btn.is-selected {
  color: var(--app-primary);
  background: var(--app-primary-soft);
  box-shadow: inset 0 0 0 1px var(--app-border);
  animation: titleSelectPop 0.24s ease-out;
}
.book-title {
  font-weight: 600;
  color: inherit;
}
.title-arrow {
  font-size: 14px;
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.24s ease, transform 0.24s ease;
}
.book-title-btn:hover .title-arrow,
.book-title-btn.is-selected .title-arrow {
  opacity: 1;
  transform: translateX(0);
}

@keyframes titleSelectPop {
  0% { transform: translateX(0) scale(1); }
  55% { transform: translateX(2px) scale(1.02); }
  100% { transform: translateX(0) scale(1); }
}

.pag-wrap { display: flex; justify-content: flex-end; margin-top: 18px; }
.category-tag { cursor: pointer; }
.stock-tag { cursor: pointer; }

.author-link,
.shelf-link {
  max-width: 100%;
}
.stock-pill {
  min-width: 30px;
  border: 1px solid var(--app-border);
  border-radius: 999px;
  padding: 2px 9px;
  background: var(--app-surface-soft);
  color: var(--app-text-primary);
  font-weight: 700;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease, transform 0.2s ease;
}
.stock-pill:hover {
  border-color: var(--app-primary);
  background: var(--app-primary-soft);
  transform: translateY(-1px);
}
.barcode-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  max-height: 480px;
  overflow-y: auto;
}
.barcode-item {
  text-align: center;
  background: var(--app-surface-soft);
  border-radius: 10px;
  padding: 14px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.barcode-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.barcode-img { max-width: 100%; height: 60px; }
.barcode-code { font-size: 11px; color: var(--app-text-secondary); margin: 6px 0; word-break: break-all; }
.empty-placeholder { text-align: center; color: var(--app-text-muted); padding: 40px 0; }
</style>
