<template>
  <div>
    <h2 class="app-page-title">管理员资料</h2>

    <el-row :gutter="18">
      <el-col :xs="24" :lg="14">
        <div class="card" v-loading="loading">
          <div class="card-title">基础信息</div>
          <el-form label-width="92px">
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
                    :on-change="handleAvatarChange"
                  >
                    <el-button size="small">上传头像</el-button>
                  </el-upload>
                  <span class="form-hint">建议方图，2MB以内</span>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" maxlength="50" placeholder="请输入昵称（可选）" />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="profileForm.realName" maxlength="50" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" maxlength="100" placeholder="请输入邮箱（可选）" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" maxlength="11" placeholder="请输入11位手机号" />
            </el-form-item>
          </el-form>
          <div class="actions">
            <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">保存资料</el-button>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :lg="10">
        <div class="card">
          <div class="card-title">修改密码</div>
          <el-form label-width="92px">
            <el-form-item label="原密码">
              <el-input v-model="passwordForm.oldPassword" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" show-password placeholder="至少6位" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="passwordForm.confirmPassword" show-password placeholder="请再次输入新密码" />
            </el-form-item>
          </el-form>
          <div class="actions">
            <el-button type="warning" :loading="savingPassword" @click="handleChangePassword">更新密码</el-button>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changeCurrentAdminPassword, getCurrentAdmin, updateCurrentAdminProfile } from '../../api/admin'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const savingProfile = ref(false)
const savingPassword = ref(false)

const profileForm = reactive({
  username: '',
  nickname: '',
  realName: '',
  email: '',
  phone: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const loadProfile = async () => {
  loading.value = true
  try {
    const res = await getCurrentAdmin()
    const user = res.data || {}
    profileForm.username = user.username || authStore.username || ''
    profileForm.nickname = user.nickname || ''
    profileForm.realName = user.realName || ''
    profileForm.email = user.email || ''
    profileForm.phone = user.phone || ''
    profileForm.avatar = user.avatar || ''
    authStore.updateProfile({
      username: profileForm.username,
      realName: profileForm.realName,
      nickname: profileForm.nickname,
      email: profileForm.email,
      avatar: profileForm.avatar
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
    profileForm.avatar = String(reader.result || '')
  }
  reader.readAsDataURL(raw)
}

const handleSaveProfile = async () => {
  if (savingProfile.value) return
  const nickname = profileForm.nickname?.trim()
  const realName = profileForm.realName?.trim()
  const email = profileForm.email?.trim()
  const phone = profileForm.phone?.trim()
  if (!realName) {
    ElMessage.warning('姓名不能为空')
    return
  }
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  if (phone && !/^\d{11}$/.test(phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }

  savingProfile.value = true
  try {
    const res = await updateCurrentAdminProfile({
      nickname,
      realName,
      email,
      phone,
      avatar: profileForm.avatar
    })
    const user = res.data || {}
    profileForm.nickname = user.nickname || ''
    profileForm.realName = user.realName || realName
    profileForm.email = user.email || ''
    profileForm.phone = user.phone || ''
    profileForm.avatar = user.avatar || profileForm.avatar || ''
    authStore.updateProfile({
      username: user.username || profileForm.username,
      realName: profileForm.realName,
      nickname: profileForm.nickname,
      email: profileForm.email,
      avatar: profileForm.avatar
    })
    ElMessage.success('资料保存成功')
  } finally {
    savingProfile.value = false
  }
}

const handleChangePassword = async () => {
  if (savingPassword.value) return
  const oldPassword = passwordForm.oldPassword || ''
  const newPassword = passwordForm.newPassword || ''
  const confirmPassword = passwordForm.confirmPassword || ''
  if (!oldPassword || !newPassword || !confirmPassword) {
    ElMessage.warning('请完整填写密码信息')
    return
  }
  if (newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位')
    return
  }
  if (newPassword !== confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (oldPassword === newPassword) {
    ElMessage.warning('新密码不能与原密码相同')
    return
  }

  savingPassword.value = true
  try {
    await changeCurrentAdminPassword({
      oldPassword,
      newPassword
    })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success('密码修改成功')
  } finally {
    savingPassword.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--app-text-primary);
  margin-bottom: 22px;
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

.card {
  background: var(--app-card-bg);
  backdrop-filter: blur(var(--app-glass-blur));
  -webkit-backdrop-filter: blur(var(--app-glass-blur));
  border: var(--app-card-border-width) solid var(--app-card-border);
  box-shadow: var(--app-card-shadow);
  border-radius: 14px;
  padding: 22px;
  margin-bottom: 18px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-bottom: 18px;
}

.actions {
  display: flex;
  justify-content: flex-end;
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
</style>
