<template>
  <div>
    <h2 class="page-title">个人资料</h2>

    <div class="profile-card app-mobile-card" v-loading="loading">
      <el-form :model="form" label-width="86px" class="profile-form">
        <el-form-item label="头像">
          <div class="avatar-row">
            <el-avatar :size="72" :src="form.avatar || ''">
              {{ (form.nickname || form.realName || form.username || '?').slice(0, 1) }}
            </el-avatar>
            <el-upload :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="handleAvatarChange">
              <el-button size="small">上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" maxlength="50" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" maxlength="50" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" maxlength="100" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAccountMe, updateAccountProfile } from '../../api/user'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  username: '',
  realName: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: ''
})

const loadProfile = async () => {
  loading.value = true
  try {
    const res = await getAccountMe()
    Object.assign(form, {
      username: res.data?.username || '',
      realName: res.data?.realName || '',
      nickname: res.data?.nickname || '',
      email: res.data?.email || '',
      phone: res.data?.phone || '',
      avatar: res.data?.avatar || ''
    })
  } finally {
    loading.value = false
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
    form.avatar = String(reader.result || '')
  }
  reader.readAsDataURL(raw)
}

const saveProfile = async () => {
  if (saving.value) return
  const payload = {
    realName: form.realName?.trim() || '',
    nickname: form.nickname?.trim() || '',
    email: form.email?.trim() || '',
    phone: form.phone?.trim() || '',
    avatar: form.avatar || ''
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
  saving.value = true
  try {
    const res = await updateAccountProfile(payload)
    authStore.updateProfile(res.data || payload)
    ElMessage.success('资料已保存')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--app-text-primary);
  margin: 0 0 18px;
}

.profile-card {
  max-width: 640px;
  padding: 22px;
}

.profile-form {
  max-width: 520px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 14px;
}
</style>
