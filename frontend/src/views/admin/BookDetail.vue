<template>
  <div>
    <div class="page-head">
      <div>
        <h2 class="app-page-title">图书详情</h2>
        <p class="sub-title">查看图书信息与副本状态</p>
      </div>
      <el-button plain @click="goBack">返回图书管理</el-button>
    </div>

    <div class="detail-card app-card" v-loading="loading">
      <template v-if="book">
        <div class="detail-grid">
          <div class="cover-wrap">
            <img v-if="book.coverImage" :src="book.coverImage" alt="封面" class="cover-image" />
            <div v-else class="cover-empty">暂无封面</div>
          </div>

          <el-descriptions title="基础信息" :column="2" border>
            <el-descriptions-item label="书名">{{ book.title || '-' }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ book.author || '-' }}</el-descriptions-item>
            <el-descriptions-item label="ISBN">{{ book.isbn || '-' }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ categoryName }}</el-descriptions-item>
            <el-descriptions-item label="库位">{{ shelfName }}</el-descriptions-item>
            <el-descriptions-item label="总库存">{{ book.totalStock ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="可借库存">{{ book.currentStock ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="录入时间">{{ fmtTime(book.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="最后更新">{{ fmtTime(book.updateTime) }}</el-descriptions-item>
            <el-descriptions-item label="简介" :span="2">{{ book.description || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="copies-block">
          <div class="copies-head">
            <div class="section-title">副本列表（{{ copies.length }}）</div>
            <el-button type="primary" size="small" @click="openAddCopies">新增副本</el-button>
          </div>
          <el-table :data="copies">
            <el-table-column label="副本编码" min-width="180">
              <template #default="{ row }">
                <button type="button" class="app-code-link" @click="downloadBarcode(row)">
                  {{ row.copyCode }}
                </button>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small" effect="dark" round>
                  {{ statusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="入库时间" min-width="170">
              <template #default="{ row }">
                {{ fmtTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="库位" min-width="130">
              <template #default="{ row }">{{ shelfNameById(row.locationId) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="310" fixed="right">
              <template #default="{ row }">
                <div class="app-action-group">
                  <button type="button" class="app-action-link" @click="downloadBarcode(row)">条码</button>
                  <button
                    v-if="row.status === 0"
                    type="button"
                    class="app-action-link app-action-link--warning"
                    @click="handleCopyStatus(row, 2)"
                  >
                    标记丢失
                  </button>
                  <button
                    v-else-if="row.status === 2"
                    type="button"
                    class="app-action-link app-action-link--success"
                    @click="handleCopyStatus(row, 0)"
                  >
                    恢复在馆
                  </button>
                  <button v-else type="button" class="app-action-link app-action-link--muted" disabled>借出中</button>
                  <button type="button" class="app-action-link" @click="openMoveCopy(row)">移动</button>
                  <button
                    type="button"
                    class="app-action-link app-action-link--danger"
                    :disabled="row.status === 1"
                    @click="handleDeleteCopy(row)"
                  >
                    删除
                  </button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="未找到该图书" />
    </div>
  </div>

  <el-dialog v-model="addCopyVisible" title="新增副本" class="app-dialog-sm" destroy-on-close>
    <el-form :model="addCopyForm" label-width="90px">
      <el-form-item label="新增数量">
        <el-input-number v-model="addCopyForm.quantity" :min="1" class="app-full-width" />
      </el-form-item>
      <el-form-item label="入库库位">
        <el-select v-model="addCopyForm.locationId" clearable class="app-full-width">
          <el-option v-for="s in shelves" :key="s.id" :label="s.locationName" :value="s.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="addCopyVisible = false">取消</el-button>
      <el-button type="primary" :loading="copySaving" @click="handleAddCopies">确认新增</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="moveVisible" title="移动副本库位" class="app-dialog-sm" destroy-on-close>
    <el-form label-width="90px">
      <el-form-item label="副本编号">
        <span>{{ currentCopy?.copyCode || '-' }}</span>
      </el-form-item>
      <el-form-item label="目标库位">
        <el-select v-model="moveForm.locationId" clearable class="app-full-width">
          <el-option v-for="s in shelves" :key="s.id" :label="s.locationName" :value="s.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="moveVisible = false">取消</el-button>
      <el-button type="primary" :loading="copySaving" @click="handleMoveCopy">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addBookCopies,
  deleteBookCopy,
  generateBarcode,
  getBookById,
  getBookCopies,
  getCategoryList,
  getShelfList,
  updateBookCopyLocation,
  updateBookCopyStatus
} from '../../api/admin'
import { formatDateTime } from '../../utils/formatters'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const book = ref(null)
const copies = ref([])
const categories = ref([])
const shelves = ref([])
const addCopyVisible = ref(false)
const moveVisible = ref(false)
const copySaving = ref(false)
const currentCopy = ref(null)
const addCopyForm = reactive({ quantity: 1, locationId: null })
const moveForm = reactive({ locationId: null })

const bookId = computed(() => {
  const id = Number(route.params.id)
  return Number.isInteger(id) && id > 0 ? id : null
})

const categoryName = computed(() => {
  if (!book.value?.categoryId) return '-'
  const hit = categories.value.find(c => c.id === book.value.categoryId)
  return hit?.categoryName || '-'
})

const shelfName = computed(() => {
  if (!book.value?.locationId) return '-'
  const hit = shelves.value.find(s => s.id === book.value.locationId)
  return hit?.locationName || '-'
})

const fmtTime = (value) => formatDateTime(value)
const shelfNameById = (id) => shelves.value.find(s => s.id === id)?.locationName || '-'

const statusText = (status) => {
  if (status === 0) return '在馆'
  if (status === 1) return '借出'
  if (status === 2) return '丢失'
  return '未知'
}

const statusTagType = (status) => {
  if (status === 0) return 'success'
  if (status === 1) return 'warning'
  if (status === 2) return 'danger'
  return 'info'
}

const goBack = () => {
  const from = route.query.from
  if (typeof from === 'string' && from.startsWith('/admin/books')) {
    router.push(from).catch(() => {})
    return
  }
  router.push('/admin/books').catch(() => {})
}

const loadData = async () => {
  if (!bookId.value) {
    book.value = null
    copies.value = []
    ElMessage.warning('图书ID无效')
    return
  }
  loading.value = true
  try {
    const [bookRes, copyRes, categoryRes, shelfRes] = await Promise.all([
      getBookById(bookId.value),
      getBookCopies(bookId.value),
      getCategoryList(),
      getShelfList()
    ])
    book.value = bookRes.data || null
    copies.value = copyRes.data || []
    categories.value = categoryRes.data || []
    shelves.value = shelfRes.data || []
  } catch {
    book.value = null
    copies.value = []
    ElMessage.error('加载图书详情失败')
  } finally {
    loading.value = false
  }
}

const openAddCopies = () => {
  addCopyForm.quantity = 1
  addCopyForm.locationId = book.value?.locationId || null
  addCopyVisible.value = true
}

const handleAddCopies = async () => {
  if (!bookId.value) return
  copySaving.value = true
  try {
    await addBookCopies(bookId.value, {
      quantity: addCopyForm.quantity,
      locationId: addCopyForm.locationId
    })
    ElMessage.success('副本已新增')
    addCopyVisible.value = false
    loadData()
  } finally {
    copySaving.value = false
  }
}

const downloadBarcode = async (copy) => {
  const blob = await generateBarcode(copy.copyCode)
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `barcode-${copy.copyCode}.png`
  a.click()
  URL.revokeObjectURL(url)
}

const handleCopyStatus = async (copy, status) => {
  const action = status === 2 ? '标记为丢失' : '恢复为在馆'
  await ElMessageBox.confirm(`确认将副本 ${copy.copyCode}${action}？`, '提示', { type: 'warning' })
  await updateBookCopyStatus(copy.id, status)
  ElMessage.success('副本状态已更新')
  loadData()
}

const openMoveCopy = (copy) => {
  currentCopy.value = copy
  moveForm.locationId = copy.locationId || null
  moveVisible.value = true
}

const handleMoveCopy = async () => {
  if (!currentCopy.value) return
  copySaving.value = true
  try {
    await updateBookCopyLocation(currentCopy.value.id, moveForm.locationId)
    ElMessage.success('副本库位已更新')
    moveVisible.value = false
    loadData()
  } finally {
    copySaving.value = false
  }
}

const handleDeleteCopy = async (copy) => {
  await ElMessageBox.confirm(`确认删除副本 ${copy.copyCode}？`, '提示', { type: 'warning' })
  await deleteBookCopy(copy.id)
  ElMessage.success('副本已删除')
  loadData()
}

watch(() => route.params.id, loadData)

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.sub-title {
  margin: 6px 0 0;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.cover-wrap {
  width: 160px;
  height: 220px;
  border-radius: 12px;
  border: 1px solid var(--app-border);
  overflow: hidden;
  background: var(--app-surface-soft);
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-text-muted);
  font-size: 13px;
}

.copies-block {
  margin-top: 20px;
}

.copies-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .cover-wrap {
    width: 140px;
    height: 190px;
  }
}
</style>
