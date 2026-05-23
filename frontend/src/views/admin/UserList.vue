<template>
  <div>
    <h2 class="app-page-title">用户管理</h2>

    <div class="toolbar app-toolbar">
      <el-button type="primary" @click="openAddUser">
        <el-icon class="app-inline-icon"><Plus /></el-icon>
        新增读者
      </el-button>
      <el-input
        v-model="keyword"
        placeholder="搜索用户名 / 姓名 / 昵称 / 邮箱 / 手机号..."
        prefix-icon="Search"
        class="app-control-xl"
        clearable
        @keyup.enter="loadData"
      >
        <template #append>
          <el-button @click="loadData">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
    </div>

    <div class="table-card app-table-card">
      <el-table :data="list" v-loading="loading" class="app-full-width">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar
                :size="36"
                :src="row.avatar || ''"
                class="user-avatar"
                @click="openProfile(row)"
              >
                {{ (row.nickname || row.realName || row.username || '?').slice(0, 1) }}
              </el-avatar>
              <div class="user-meta">
                <button type="button" class="app-field-link user-name-link" @click="openProfile(row)">
                  {{ row.username }}
                </button>
                <span class="user-meta__sub">{{ row.nickname || row.realName || '-' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="200">
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="role" label="角色" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small" effect="dark" round>
              {{ roleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="dark" round>
              {{ row.status === 1 ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="170" :formatter="fmtTime" />
        <el-table-column label="操作" min-width="280" fixed="right">
          <template #default="{ row }">
            <div class="app-action-group">
              <button
                v-if="canToggleRole(row)"
                type="button"
                class="app-action-link"
                @click="handleSwitchRole(row)"
              >
                {{ row.role === 1 ? '设为协管员' : row.role === 2 ? '降为普通用户' : '转为普通用户' }}
              </button>
              <button
                type="button"
                class="app-action-link"
                :class="row.status === 1 ? 'app-action-link--warning' : 'app-action-link--success'"
                :disabled="isProtectedAccount(row)"
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '封禁' : '解封' }}
              </button>
              <button
                type="button"
                class="app-action-link app-action-link--muted"
                :disabled="isProtectedAccount(row)"
                @click="openReset(row)"
              >
                重置密码
              </button>
              <button
                type="button"
                class="app-action-link app-action-link--primary"
                @click="goDetail(row)"
              >
                查看详情
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

    <el-dialog v-model="resetVisible" title="重置密码" width="400px" destroy-on-close>
      <p class="reset-desc">将用户 <strong>{{ resetForm.username }}</strong> 的密码重置为：</p>
      <el-input v-model="resetForm.password" placeholder="请输入新密码" show-password size="large" />
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReset">确认重置</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addVisible" title="新增读者" width="520px" destroy-on-close>
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="86px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addForm.username" placeholder="请输入登录用户名" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="addForm.password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="addForm.realName" placeholder="读者姓名" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="addForm.nickname" placeholder="可选" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="addForm.phone" maxlength="11" placeholder="可选" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="addForm.email" placeholder="可选" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="addForm.role" class="app-full-width">
            <el-option label="普通用户" :value="1" />
            <el-option label="游客" :value="3" />
            <el-option v-if="isSuperAdmin" label="协管员" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addSaving" @click="handleAddUser">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="profileVisible" title="用户资料" width="560px" destroy-on-close>
      <div v-loading="profileLoading">
        <el-form label-width="90px">
          <el-form-item label="头像">
            <div class="avatar-editor">
              <el-avatar :size="72" :src="profileForm.avatar || ''">
                {{ (profileForm.nickname || profileForm.realName || profileForm.username || '?').slice(0, 1) }}
              </el-avatar>
              <div class="avatar-editor__ops">
                <el-upload
                  :show-file-list="false"
                  :auto-upload="false"
                  accept="image/*"
                  :disabled="profileReadonly"
                  :on-change="handleAvatarChange"
                >
                  <el-button size="small" :disabled="profileReadonly">上传头像</el-button>
                </el-upload>
                <span class="form-hint">建议方图，2MB以内</span>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="ID">
            <el-input v-model="profileForm.id" disabled />
          </el-form-item>
          <el-form-item label="用户名">
            <el-input v-model="profileForm.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="profileForm.nickname" maxlength="50" :disabled="profileReadonly" />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="profileForm.realName" maxlength="50" :disabled="profileReadonly" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" maxlength="100" :disabled="profileReadonly" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profileForm.phone" maxlength="11" :disabled="profileReadonly" />
          </el-form-item>
          <p v-if="profileReadonly" class="readonly-tip">超级管理员资料仅可查看，不可在此处修改。</p>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="profileVisible = false">关闭</el-button>
        <el-button type="primary" :loading="profileSaving" :disabled="profileReadonly" @click="handleSaveProfile">
          保存资料
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { addUser, getUserList, getUserById, updateUserProfile, toggleUserStatus, resetPassword, updateUserRole } from '../../api/admin'
import { useAuthStore } from '../../stores/auth'
import { formatDateTime } from '../../utils/formatters'

const authStore = useAuthStore()
const router = useRouter()
const isSuperAdmin = computed(() => Number(authStore.role) === 0)

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)
const resetVisible = ref(false)
const resetForm = reactive({ id: null, username: '', password: '' })
const addVisible = ref(false)
const addSaving = ref(false)
const addFormRef = ref()
const addForm = reactive({
  username: '',
  password: '',
  realName: '',
  nickname: '',
  phone: '',
  email: '',
  role: 1
})
const addRules = {
  username: [{ required: true, message: '请填写用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请填写初始密码', trigger: 'blur' }]
}
const profileVisible = ref(false)
const profileLoading = ref(false)
const profileSaving = ref(false)
const currentProfileUser = ref(null)
const profileForm = reactive({
  id: '',
  username: '',
  realName: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: ''
})
const profileReadonly = computed(() => Number(currentProfileUser.value?.role) === 0)

const fmtTime = (row) => formatDateTime(row.createTime)
const roleLabel = (role) => {
  const map = {
    0: '超级管理员',
    1: '普通用户',
    2: '协管员',
    3: '游客'
  }
  return map[role] || '未知'
}
const roleTagType = (role) => {
  const map = {
    0: 'danger',
    1: 'success',
    2: 'warning',
    3: 'info'
  }
  return map[role] || 'info'
}
const isProtectedAccount = (row) => Number(row.role) === 0
const canToggleRole = (row) => isSuperAdmin.value && [1, 2, 3].includes(Number(row.role))

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserList({ current: current.value, size: size.value, keyword: keyword.value })
    list.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const openAddUser = () => {
  Object.assign(addForm, {
    username: '',
    password: '',
    realName: '',
    nickname: '',
    phone: '',
    email: '',
    role: 1
  })
  addVisible.value = true
}

const handleAddUser = async () => {
  if (addSaving.value) return
  const valid = await addFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (addForm.password.length < 6) {
    ElMessage.warning('密码长度不能少于6位')
    return
  }
  if (addForm.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(addForm.email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  if (addForm.phone && !/^\d{11}$/.test(addForm.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  addSaving.value = true
  try {
    await addUser({
      username: addForm.username.trim(),
      password: addForm.password,
      realName: addForm.realName?.trim() || null,
      nickname: addForm.nickname?.trim() || null,
      phone: addForm.phone?.trim() || null,
      email: addForm.email?.trim() || null,
      role: addForm.role,
      status: 1
    })
    ElMessage.success('用户创建成功')
    addVisible.value = false
    await loadData()
  } finally {
    addSaving.value = false
  }
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '封禁' : '解封'
  await ElMessageBox.confirm(`确认${action}用户 ${row.username}？`, '提示', { type: 'warning' })
  await toggleUserStatus(row.id, newStatus)
  ElMessage.success(`${action}成功`)
  await loadData()
}

const handleSwitchRole = async (row) => {
  let targetRole = 1
  let actionText = '转为普通用户'
  if (Number(row.role) === 1) {
    targetRole = 2
    actionText = '设为协管员'
  } else if (Number(row.role) === 2) {
    targetRole = 1
    actionText = '降为普通用户'
  }
  await ElMessageBox.confirm(`确认将用户 ${row.username}${actionText}？`, '提示', { type: 'warning' })
  await updateUserRole(row.id, targetRole)
  ElMessage.success('角色更新成功')
  await loadData()
}

const openReset = (row) => {
  resetForm.id = row.id
  resetForm.username = row.username
  resetForm.password = ''
  resetVisible.value = true
}

const openProfile = async (row) => {
  profileVisible.value = true
  profileLoading.value = true
  currentProfileUser.value = row
  try {
    const res = await getUserById(row.id)
    const user = res.data || {}
    Object.assign(profileForm, {
      id: user.id || '',
      username: user.username || '',
      realName: user.realName || '',
      nickname: user.nickname || '',
      email: user.email || '',
      phone: user.phone || '',
      avatar: user.avatar || ''
    })
  } finally {
    profileLoading.value = false
  }
}

const handleAvatarChange = (uploadFile) => {
  const raw = uploadFile.raw
  if (!raw) return
  if (!raw.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    return
  }
  if (raw.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过2MB')
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    profileForm.avatar = String(reader.result || '')
  }
  reader.readAsDataURL(raw)
}

const handleSaveProfile = async () => {
  if (profileSaving.value) return
  const payload = {
    nickname: profileForm.nickname?.trim() || '',
    realName: profileForm.realName?.trim() || '',
    email: profileForm.email?.trim() || '',
    phone: profileForm.phone?.trim() || '',
    avatar: profileForm.avatar || ''
  }
  if (!payload.realName) {
    ElMessage.warning('姓名不能为空')
    return
  }
  if (payload.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  if (payload.phone && !/^\d{11}$/.test(payload.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  profileSaving.value = true
  try {
    await updateUserProfile(profileForm.id, payload)
    ElMessage.success('用户资料已更新')
    profileVisible.value = false
    await loadData()
  } finally {
    profileSaving.value = false
  }
}

const handleReset = async () => {
  if (!resetForm.password) { ElMessage.warning('请输入新密码'); return }
  if (resetForm.password.length < 6) { ElMessage.warning('新密码长度不能少于6位'); return }
  await resetPassword(resetForm.id, resetForm.password)
  ElMessage.success('密码重置成功')
  resetVisible.value = false
}

const goDetail = (row) => { router.push(`/admin/users/${row.id}`) }

onMounted(loadData)
</script>

<style scoped>
.page-title {
  font-size: 24px; font-weight: 700; color: var(--app-text-primary); margin-bottom: 22px;
  letter-spacing: -0.02em; position: relative; display: inline-block;
}
.page-title::after {
  content: ''; position: absolute; bottom: -6px; left: 0;
  width: 40px; height: 3px; border-radius: 2px;
  background: var(--app-decoration-line);
}
.toolbar {
  background: var(--app-card-bg);
  backdrop-filter: blur(var(--app-glass-blur));
  -webkit-backdrop-filter: blur(var(--app-glass-blur));
  border-radius: 14px; padding: 18px 22px;
  border: var(--app-card-border-width) solid var(--app-card-border);
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 18px; box-shadow: var(--app-card-shadow);
}
.table-card {
  background: var(--app-card-bg);
  backdrop-filter: blur(var(--app-glass-blur));
  -webkit-backdrop-filter: blur(var(--app-glass-blur));
  border-radius: 14px; padding: 22px;
  border: var(--app-card-border-width) solid var(--app-card-border);
  box-shadow: var(--app-card-shadow);
}
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 18px; }
.reset-desc { margin-bottom: 16px; color: var(--app-text-secondary); font-size: 14px; }
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-avatar {
  cursor: pointer;
  background: var(--app-primary-soft);
  color: var(--app-primary);
}
.user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: flex-start;
}
.user-name-link {
  font-weight: 700;
}
.user-meta__sub {
  font-size: 12px;
  color: var(--app-text-muted);
}
.avatar-editor {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avatar-editor__ops {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-hint {
  font-size: 12px;
  color: var(--app-text-muted);
}
.readonly-tip {
  margin: 4px 0 0 0;
  color: var(--app-warning);
  font-size: 12px;
}
</style>
