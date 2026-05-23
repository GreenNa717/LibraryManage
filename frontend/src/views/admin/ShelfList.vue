<template>
  <div>
    <h2 class="app-page-title">库位管理</h2>

    <div class="app-toolbar">
      <el-button type="primary" @click="openAdd">
        <el-icon class="app-inline-icon"><Plus /></el-icon>
        新增库位
      </el-button>
    </div>

    <div class="app-table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="locationName" label="库位名称" min-width="180" />
        <el-table-column label="关联分类" min-width="140">
          <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column prop="currentCount" label="当前藏书" width="110" align="center" />
        <el-table-column prop="maxCapacity" label="最大容量" width="110" align="center" />
        <el-table-column label="饱和度" min-width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="capacityRate(row)"
              :stroke-width="14"
              :text-inside="true"
              :color="capacityRate(row) > 80 ? 'var(--app-danger)' : capacityRate(row) > 60 ? 'var(--app-warning)' : 'var(--app-success)'"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="app-action-group">
              <button type="button" class="app-action-link" @click="openEdit(row)">编辑</button>
              <button type="button" class="app-action-link app-action-link--danger" @click="handleDelete(row)">删除</button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑库位' : '新增库位'" class="app-dialog-md" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="库位名称" prop="locationName">
          <el-input v-model="form.locationName" placeholder="如：A区计算机架" />
        </el-form-item>
        <el-form-item label="关联分类">
          <el-select v-model="form.categoryId" clearable class="app-full-width">
            <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大容量" prop="maxCapacity">
          <el-input-number v-model="form.maxCapacity" :min="1" class="app-full-width" />
        </el-form-item>
        <el-form-item label="当前藏书">
          <el-input-number v-model="form.currentCount" :min="0" class="app-full-width" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { addShelf, deleteShelf, getCategoryList, getShelfList, updateShelf } from '../../api/admin'
import { formatDateTime } from '../../utils/formatters'

const list = ref([])
const categories = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = reactive({ id: null, locationName: '', categoryId: null, maxCapacity: 100, currentCount: 0 })
const rules = {
  locationName: [{ required: true, message: '请填写库位名称', trigger: 'blur' }],
  maxCapacity: [{ required: true, message: '请填写最大容量', trigger: 'change' }]
}

const fmtTime = (value) => formatDateTime(value)
const categoryName = (id) => categories.value.find(c => c.id === id)?.categoryName || '-'
const capacityRate = (row) => {
  const max = Number(row.maxCapacity || 0)
  return max > 0 ? Math.round(Number(row.currentCount || 0) * 100 / max) : 0
}

const loadData = async () => {
  loading.value = true
  try {
    const [shelfRes, categoryRes] = await Promise.all([getShelfList(), getCategoryList()])
    list.value = shelfRes.data || []
    categories.value = categoryRes.data || []
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  isEdit.value = false
  Object.assign(form, { id: null, locationName: '', categoryId: null, maxCapacity: 100, currentCount: 0 })
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  const payload = {
    locationName: form.locationName,
    categoryId: form.categoryId,
    maxCapacity: form.maxCapacity,
    currentCount: form.currentCount
  }
  if (isEdit.value) await updateShelf(form.id, payload)
  else await addShelf(payload)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除库位 ${row.locationName}？`, '提示', { type: 'warning' })
  await deleteShelf(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>

</style>
