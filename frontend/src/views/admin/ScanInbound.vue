<template>
  <div>
    <h2 class="app-page-title">扫码入库</h2>
    <div class="hint">用扫码枪扫描一维码后，回车会先尝试获取书籍信息，确认无误后再入库。</div>

    <div class="card app-card">
      <el-form :model="form" label-width="100px">
        <el-form-item label="扫码内容">
          <el-input
            ref="scanInputRef"
            v-model="form.scannedCode"
            placeholder="请用扫码枪扫描ISBN条码"
            clearable
            @keyup.enter="handleScanEnter"
          />
        </el-form-item>
        <el-form-item label="ISBN">
          <el-input v-model="form.isbn" placeholder="默认使用扫码内容">
            <template #append>
              <el-button :loading="fetchingMeta" @click="handleFetchMetadata">获取书籍信息</el-button>
            </template>
          </el-input>
          <div v-if="metadataSource" class="meta-source">数据来源：{{ metadataSource }}</div>
        </el-form-item>
        <el-form-item label="书名">
          <el-input v-model="form.title" placeholder="新书首次入库必填" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="form.author" placeholder="可选" />
        </el-form-item>
        <el-form-item label="封面URL">
          <el-input v-model="form.coverImage" placeholder="可选，自动获取后会填充" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" clearable class="app-full-width">
            <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="库位">
          <el-select v-model="form.locationId" clearable class="app-full-width">
            <el-option v-for="s in shelves" :key="s.id" :label="s.locationName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量">
          <el-input-number v-model="form.quantity" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleScanSubmit">确认入库</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card app-card" v-if="lastResult">
      <h3 class="result-title">本次结果</h3>
      <p>状态：{{ lastResult.created ? '新书已创建并入库' : '已有图书已增加库存' }}</p>
      <p>图书：{{ lastResult.title }}（{{ lastResult.isbn }}）</p>
      <p>数量：{{ lastResult.quantity }}，总库存：{{ lastResult.totalStock }}，可借：{{ lastResult.currentStock }}</p>
      <p>副本码：</p>
      <div class="codes">
        <el-tag v-for="code in lastResult.copyCodes || []" :key="code" class="code-tag">{{ code }}</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategoryList, getIsbnMetadata, getShelfList, scanInbound } from '../../api/admin'

const scanInputRef = ref()
const submitting = ref(false)
const fetchingMeta = ref(false)
const categories = ref([])
const shelves = ref([])
const lastResult = ref(null)
const metadataSource = ref('')
const form = reactive({
  scannedCode: '',
  isbn: '',
  title: '',
  author: '',
  coverImage: '',
  categoryId: null,
  locationId: null,
  quantity: 1
})

const focusScanInput = () => nextTick(() => scanInputRef.value?.focus())

const loadCategories = async () => {
  const res = await getCategoryList()
  categories.value = res.data || []
}

const loadShelves = async () => {
  const res = await getShelfList()
  shelves.value = res.data || []
}

const resetForm = () => {
  form.scannedCode = ''
  form.isbn = ''
  form.title = ''
  form.author = ''
  form.coverImage = ''
  form.categoryId = null
  form.locationId = null
  form.quantity = 1
  metadataSource.value = ''
  focusScanInput()
}

const handleFetchMetadata = async () => {
  const realIsbn = (form.isbn || form.scannedCode || '').trim()
  if (!realIsbn) {
    ElMessage.warning('请先扫描或输入ISBN')
    focusScanInput()
    return
  }
  fetchingMeta.value = true
  try {
    const res = await getIsbnMetadata(realIsbn)
    const data = res.data || {}
    form.isbn = data.isbn || realIsbn
    form.title = data.title || form.title
    form.author = data.author || form.author
    form.coverImage = data.coverImage || form.coverImage
    metadataSource.value = data.source === 'googlebooks'
      ? 'Google Books'
      : data.source === 'openlibrary' || data.source === 'openlibrary-search'
        ? 'OpenLibrary'
        : data.source === 'local-book'
          ? '本地图书库'
          : data.source === 'local-metadata'
            ? '本地ISBN资料库'
            : '未知来源'
    ElMessage.success(`已自动填充书籍信息（${metadataSource.value}）`)
  } catch {
    metadataSource.value = ''
    ElMessage.warning('未查到书籍信息，请手动补录后入库')
  } finally {
    fetchingMeta.value = false
  }
}

const handleScanEnter = async () => {
  if (!form.isbn && form.scannedCode) {
    form.isbn = form.scannedCode.trim()
  }
  await handleFetchMetadata()
}

const handleScanSubmit = async () => {
  if (submitting.value) return
  const realIsbn = (form.isbn || form.scannedCode || '').trim()
  if (!realIsbn) {
    ElMessage.warning('请先扫描或输入ISBN')
    focusScanInput()
    return
  }
  submitting.value = true
  try {
    const res = await scanInbound({
      scannedCode: form.scannedCode,
      isbn: realIsbn,
      title: form.title,
      author: form.author,
      coverImage: form.coverImage,
      categoryId: form.categoryId,
      locationId: form.locationId,
      quantity: form.quantity
    })
    lastResult.value = res.data
    ElMessage.success('入库成功')
    form.scannedCode = ''
    form.isbn = ''
    form.title = ''
    form.author = ''
    form.coverImage = ''
    form.quantity = 1
    focusScanInput()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadCategories(), loadShelves()])
  focusScanInput()
})
</script>

<style scoped>
.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--app-text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.02em;
  position: relative;
  display: inline-block;
}
.page-title::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 0;
  width: 40px;
  height: 3px;
  border-radius: 2px;
  background: var(--app-decoration-line);
}
.hint {
  color: var(--app-text-secondary);
  margin-bottom: 16px;
  font-size: 13px;
}
.card {
  background: var(--app-card-bg);
  backdrop-filter: blur(var(--app-glass-blur));
  -webkit-backdrop-filter: blur(var(--app-glass-blur));
  border-radius: 14px;
  border: var(--app-card-border-width) solid var(--app-card-border);
  padding: 22px 24px;
  box-shadow: var(--app-card-shadow);
  margin-bottom: 18px;
  transition: box-shadow 0.3s ease;
}
.card:hover {
  box-shadow: var(--app-card-shadow-hover);
}
.result-title {
  margin: 0 0 12px;
  color: var(--app-text-primary);
  font-weight: 600;
  position: relative;
  padding-left: 14px;
}
.result-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  border-radius: 2px;
  background: var(--app-decoration-line);
}
.codes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.code-tag {
  margin-right: 0;
  border-radius: 6px;
}
.meta-source {
  margin-top: 6px;
  color: var(--app-text-muted);
  font-size: 12px;
}
</style>
