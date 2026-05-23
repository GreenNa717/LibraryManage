<template>
  <div>
    <h2 class="app-page-title">分类管理</h2>

    <div class="app-toolbar">
      <el-button type="primary" @click="openAdd">
        <el-icon class="app-inline-icon"><Plus /></el-icon>
        新增分类
      </el-button>
    </div>

    <div class="app-table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="100" />
        <el-table-column prop="categoryName" label="分类名称" min-width="280">
          <template #default="{ row }">
            <span class="cat-name">{{ row.categoryName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序值" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.sort }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" :formatter="fmtTime" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <div class="app-action-group">
              <button type="button" class="app-action-link" @click="openEdit(row)">编辑</button>
              <button type="button" class="app-action-link app-action-link--danger" @click="handleDelete(row.id)">删除</button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" class="app-dialog-sm" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="80">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="如：计算机、文学" />
        </el-form-item>
        <el-form-item label="排序值" prop="sort">
          <el-input-number v-model="form.sort" :min="0" class="app-full-width" />
          <div class="form-hint">数值越大排序越靠前</div>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getCategoryList, addCategory, updateCategory, deleteCategory } from '../../api/admin'
import { formatDateTime } from '../../utils/formatters'

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = reactive({ id: null, categoryName: '', sort: 0 })
const formRules = {
  categoryName: [{ required: true, message: '请填写分类名称', trigger: 'blur' }]
}

const fmtTime = (row) => formatDateTime(row.createTime)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCategoryList()
    list.value = res.data
  } finally { loading.value = false }
}

const openAdd = () => {
  isEdit.value = false
  Object.assign(form, { id: null, categoryName: '', sort: 0 })
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
    await updateCategory(form.id, form)
  } else {
    await addCategory(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await loadData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该分类？', '提示', { type: 'warning' })
  await deleteCategory(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>

.cat-name { font-weight: 600; color: var(--app-text-primary); }
.form-hint { font-size: 12px; color: var(--app-text-muted); margin-top: 6px; }
</style>
