<template>
  <div>
    <h2 class="app-page-title">设置</h2>

    <div class="app-card">
      <div class="section-head">
        <div>
          <div class="section-title">主题外观</div>
          <div class="section-desc">选择管理端主题风格，切换后即时生效并自动保存。</div>
        </div>
        <el-tag size="small" type="info" effect="plain">当前：{{ activeThemeLabel }}</el-tag>
      </div>

      <div class="theme-grid">
        <div
          v-for="option in themeOptions"
          :key="option.key"
          :class="['theme-card', { active: option.key === currentTheme }]"
          @click="selectTheme(option.key)"
        >
          <div class="theme-header">
            <span class="theme-label">{{ option.label }}</span>
            <el-tag size="small" :type="option.mode === 'dark' ? 'warning' : 'success'" effect="light">
              {{ option.mode === 'dark' ? '暗色' : '亮色' }}
            </el-tag>
          </div>
          <p class="theme-desc">{{ option.description }}</p>
          <div class="theme-foot">
            <el-tag v-if="option.key === currentTheme" type="primary" effect="dark" round>
              已启用
            </el-tag>
            <span v-else class="theme-hint">点击启用</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useTheme } from '../../composables/useTheme'

const { currentTheme, themeOptions, setTheme } = useTheme()

const activeThemeLabel = computed(() => {
  return themeOptions.find((option) => option.key === currentTheme.value)?.label || '-'
})

const selectTheme = (themeKey) => {
  if (themeKey === currentTheme.value) return
  setTheme(themeKey)
  ElMessage.success('主题切换成功')
}
</script>

<style scoped>

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.section-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--app-text-primary);
  margin-bottom: 6px;
}

.section-desc {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 14px;
}

.theme-card {
  border: 1px solid var(--app-border);
  border-radius: 12px;
  padding: 14px;
  background: var(--app-surface-soft);
  cursor: pointer;
  transition: all 0.22s ease;
}

.theme-card:hover {
  transform: translateY(-2px);
  border-color: var(--app-primary);
  box-shadow: 0 8px 20px var(--app-glow-primary);
}

.theme-card.active {
  border-color: var(--app-primary);
  box-shadow: 0 10px 20px var(--app-glow-primary);
  background: var(--app-primary-soft);
}

.theme-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.theme-label {
  font-size: 15px;
  color: var(--app-text-primary);
  font-weight: 600;
}

.theme-desc {
  margin: 10px 0 14px;
  color: var(--app-text-secondary);
  font-size: 13px;
  line-height: 1.5;
  min-height: 40px;
}

.theme-foot {
  display: flex;
  justify-content: flex-end;
}

.theme-hint {
  font-size: 12px;
  color: var(--app-text-muted);
}
</style>
