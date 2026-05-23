import { DEFAULT_THEME_KEY, THEME_PRESETS, THEME_STORAGE_KEY } from './themes'

const ELEMENT_PLUS_VAR_MAP = {
  '--el-color-primary': '--app-primary',
  '--el-color-success': '--app-success',
  '--el-color-warning': '--app-warning',
  '--el-color-danger': '--app-danger',
  '--el-bg-color-page': '--app-bg',
  '--el-bg-color': '--app-surface',
  '--el-bg-color-overlay': '--app-surface',
  '--el-fill-color-light': '--app-surface-soft',
  '--el-fill-color-lighter': '--app-surface-soft',
  '--el-border-color': '--app-border',
  '--el-border-color-light': '--app-border',
  '--el-border-color-lighter': '--app-border',
  '--el-border-color-dark': '--app-border-strong',
  '--el-text-color-primary': '--app-text-primary',
  '--el-text-color-regular': '--app-text-secondary',
  '--el-text-color-secondary': '--app-text-muted',
  '--el-text-color-placeholder': '--app-text-muted'
}

const getThemeKey = (key) => (THEME_PRESETS[key] ? key : DEFAULT_THEME_KEY)

const writeThemeToDom = (themeKey) => {
  const root = document.documentElement
  const theme = THEME_PRESETS[themeKey]

  root.setAttribute('data-theme', themeKey)
  document.body?.setAttribute('data-theme', themeKey)
  document.getElementById('app')?.setAttribute('data-theme', themeKey)
  root.classList.toggle('dark', theme.mode === 'dark')

  Object.entries(theme.vars).forEach(([name, value]) => {
    root.style.setProperty(name, value)
  })

  Object.entries(ELEMENT_PLUS_VAR_MAP).forEach(([elVar, appVar]) => {
    const appValue = theme.vars[appVar]
    if (appValue) root.style.setProperty(elVar, appValue)
  })

  return theme
}

export const getStoredTheme = () => {
  try {
    return localStorage.getItem(THEME_STORAGE_KEY) || DEFAULT_THEME_KEY
  } catch {
    return DEFAULT_THEME_KEY
  }
}

export const applyTheme = (themeKey) => {
  const safeKey = getThemeKey(themeKey)
  writeThemeToDom(safeKey)
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('app-theme-change', { detail: { themeKey: safeKey } }))
  }
  try {
    localStorage.setItem(THEME_STORAGE_KEY, safeKey)
  } catch {
    // Ignore persistence errors in private mode or restricted environments.
  }
  return safeKey
}

export const initTheme = () => applyTheme(getStoredTheme())
