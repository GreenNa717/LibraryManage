<template>
  <div>
    <div class="page-head">
      <div>
        <h2 class="app-page-title">ISBN 资料库</h2>
        <p class="page-subtitle">维护扫码入库的本地兜底数据，外部源失灵时也能稳稳补全书籍信息。</p>
      </div>
      <el-button type="primary" @click="openAdd">
        <el-icon class="app-inline-icon"><Plus /></el-icon>
        新增资料
      </el-button>
    </div>

    <div class="app-toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索 ISBN / 书名 / 作者 / 出版社"
        clearable
        class="app-control-lg"
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <el-select v-model="source" placeholder="数据来源" clearable class="app-control-sm" @change="handleSearch">
        <el-option label="本地资料库" value="local-metadata" />
        <el-option label="本地图书库" value="local-book" />
        <el-option label="OpenLibrary" value="openlibrary" />
        <el-option label="OpenLibrary Search" value="openlibrary-search" />
        <el-option label="Google Books" value="googlebooks" />
      </el-select>
      <el-button :loading="loading" @click="loadData">
        <el-icon class="app-inline-icon"><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="app-table-card">
      <el-table :data="list" v-loading="loading" class="app-full-width">
        <el-table-column label="封面" width="86" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.coverImage"
              :src="row.coverImage"
              fit="cover"
              class="cover"
              :preview-src-list="[row.coverImage]"
              preview-teleported
            />
            <div v-else class="cover-placeholder">无封面</div>
          </template>
        </el-table-column>
        <el-table-column label="图书信息" min-width="260">
          <template #default="{ row }">
            <div class="book-info">
              <span class="book-title">{{ row.title }}</span>
              <span class="book-meta">{{ row.author || '未知作者' }} · {{ row.publisher || '未知出版社' }}</span>
              <span class="book-isbn">ISBN {{ row.isbn }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="publishDate" label="出版日期" width="130">
          <template #default="{ row }">{{ row.publishDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="来源" width="150">
          <template #default="{ row }">
            <el-tag :type="sourceTag(row.source)" effect="plain">{{ sourceLabel(row.source) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.updateTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <div class="app-action-group">
              <button type="button" class="app-action-link" @click="openEdit(row)">编辑</button>
              <button type="button" class="app-action-link app-action-link--danger" @click="handleDelete(row)">删除</button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑 ISBN 资料' : '新增 ISBN 资料'" class="app-dialog-lg" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="92px">
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="form.isbn" placeholder="支持带横线，保存时会自动归一化">
            <template #append>
              <el-button :loading="fetching" @click="fetchMetadata">自动补全</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="书名" prop="title">
          <el-input v-model="form.title" placeholder="请输入书名" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="form.author" placeholder="可选" />
        </el-form-item>
        <el-form-item label="出版社">
          <el-input v-model="form.publisher" placeholder="可选" />
        </el-form-item>
        <el-form-item label="出版日期">
          <el-input v-model="form.publishDate" placeholder="如：2024-01 或 2024" />
        </el-form-item>
        <el-form-item label="封面URL">
          <el-input v-model="form.coverImage" placeholder="可选，支持 http(s) 图片地址" />
        </el-form-item>
        <el-form-item label="数据来源">
          <el-select v-model="form.source" class="app-full-width">
            <el-option label="本地资料库" value="local-metadata" />
            <el-option label="OpenLibrary" value="openlibrary" />
            <el-option label="OpenLibrary Search" value="openlibrary-search" />
            <el-option label="Google Books" value="googlebooks" />
          </el-select>
        </el-form-item>
      </el-form>

      <div v-if="form.coverImage" class="preview-card">
        <img :src="form.coverImage" alt="cover preview" />
        <div>
          <strong>{{ form.title || '待填写书名' }}</strong>
          <span>{{ form.author || '作者未填写' }}</span>
          <span>{{ form.publisher || '出版社未填写' }}</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  addIsbnMetadata,
  deleteIsbnMetadata,
  getIsbnMetadata,
  getIsbnMetadataList,
  updateIsbnMetadata
} from '../../api/admin'
import { formatDateTime } from '../../utils/formatters'

const route = useRoute()
const list = ref([])
const loading = ref(false)
const saving = ref(false)
const fetching = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const keyword = ref('')
const source = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)

const emptyForm = () => ({
  id: null,
  isbn: '',
  title: '',
  author: '',
  publisher: '',
  publishDate: '',
  coverImage: '',
  source: 'local-metadata'
})

const form = reactive(emptyForm())
const rules = {
  isbn: [{ required: true, message: '请填写 ISBN', trigger: 'blur' }],
  title: [{ required: true, message: '请填写书名', trigger: 'blur' }]
}

const sourceMap = {
  'local-metadata': '本地资料库',
  'local-book': '本地图书库',
  openlibrary: 'OpenLibrary',
  'openlibrary-search': 'OpenLibrary Search',
  googlebooks: 'Google Books'
}

const fmtTime = (value) => formatDateTime(value)
const sourceLabel = (value) => sourceMap[value] || value || '未知来源'
const sourceTag = (value) => {
  if (value === 'local-metadata') return 'success'
  if (value === 'local-book') return 'warning'
  if (value === 'googlebooks') return 'primary'
  return 'info'
}

const resetForm = () => Object.assign(form, emptyForm())

const syncFiltersFromRoute = () => {
  keyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  source.value = typeof route.query.source === 'string' ? route.query.source : ''
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getIsbnMetadataList({
      current: current.value,
      size: size.value,
      keyword: keyword.value?.trim() || undefined,
      source: source.value || undefined
    })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  current.value = 1
  loadData()
}

const handleSizeChange = () => {
  current.value = 1
  loadData()
}

const openAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  Object.assign(form, emptyForm(), row)
  dialogVisible.value = true
}

const fetchMetadata = async () => {
  const isbn = form.isbn?.trim()
  if (!isbn) {
    ElMessage.warning('请先输入 ISBN')
    return
  }
  fetching.value = true
  try {
    const res = await getIsbnMetadata(isbn)
    const data = res.data || {}
    form.isbn = data.isbn || isbn
    form.title = data.title || form.title
    form.author = data.author || form.author
    form.publisher = data.publisher || form.publisher
    form.publishDate = data.publishDate || form.publishDate
    form.coverImage = data.coverImage || form.coverImage
    form.source = data.source || 'local-metadata'
    ElMessage.success(`已补全：${sourceLabel(form.source)}`)
  } catch {
    ElMessage.warning('暂未获取到书籍信息，可以手动维护')
  } finally {
    fetching.value = false
  }
}

const handleSave = async () => {
  if (saving.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      isbn: form.isbn,
      title: form.title,
      author: form.author,
      publisher: form.publisher,
      publishDate: form.publishDate,
      coverImage: form.coverImage,
      source: form.source || 'local-metadata'
    }
    if (isEdit.value) await updateIsbnMetadata(form.id, payload)
    else await addIsbnMetadata(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除《${row.title}》的 ISBN 资料？`, '提示', { type: 'warning' })
  await deleteIsbnMetadata(row.id)
  ElMessage.success('删除成功')
  await loadData()
}

watch(() => [route.query.keyword, route.query.source], () => {
  syncFiltersFromRoute()
  current.value = 1
  loadData()
})

onMounted(() => {
  syncFiltersFromRoute()
  loadData()
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.page-subtitle {
  margin: 6px 0 0;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.cover,
.cover-placeholder {
  width: 52px;
  height: 70px;
  border-radius: 8px;
}
.cover {
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.14);
}
.cover-placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--app-surface-soft);
  color: var(--app-text-muted);
  font-size: 12px;
  border: 1px dashed var(--app-border);
}
.book-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.book-title {
  color: var(--app-text-primary);
  font-size: 15px;
  font-weight: 800;
}
.book-meta,
.book-isbn {
  color: var(--app-text-muted);
  font-size: 12px;
}
.pag-wrap { display: flex; justify-content: flex-end; margin-top: 18px; }
.preview-card {
  display: flex;
  gap: 14px;
  margin: 4px 0 4px 92px;
  padding: 12px;
  border-radius: 12px;
  background: var(--app-surface-soft);
  border: 1px solid var(--app-border);
}
.preview-card img {
  width: 54px;
  height: 74px;
  object-fit: cover;
  border-radius: 8px;
}
.preview-card div {
  display: flex;
  flex-direction: column;
  gap: 5px;
  color: var(--app-text-secondary);
  font-size: 13px;
}
.preview-card strong {
  color: var(--app-text-primary);
}
@media (max-width: 760px) {
  .page-head,
  
  .toolbar :deep(.el-input),
  .toolbar :deep(.el-select) {
    width: 100% !important;
  }
  .preview-card {
    margin-left: 0;
  }
}
</style>
