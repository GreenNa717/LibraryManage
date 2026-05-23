import { lookupCopy } from '@/api/books'
import { createBorrow, returnByCopyCode } from '@/api/borrows'
import { scanCode, showManualInput } from '@/utils/scan'

async function scanWithFallback(actionLabel) {
  try {
    const code = await scanCode()
    return code
  } catch (err) {
    if (err && err.cancelled) return null
    return new Promise((resolve) => {
      uni.showModal({
        title: '扫描失败',
        content: '是否手动输入副本码？',
        success: async (res) => {
          if (!res.confirm) { resolve(null); return }
          try {
            const code = await showManualInput()
            resolve(code)
          } catch (e) {
            resolve(null)
          }
        }
      })
    })
  }
}

async function confirmBorrow(code) {
  const copyInfo = await lookupCopy(code)
  if (!copyInfo || !copyInfo.copyId) {
    uni.showToast({ title: '未找到该副本', icon: 'none' })
    return false
  }
  const bookTitle = copyInfo.bookTitle || copyInfo.title || '未知书名'
  const bookAuthor = copyInfo.bookAuthor || copyInfo.author || ''
  let content = '副本码：' + code + '\n图书：' + bookTitle
  if (bookAuthor) content += '\n作者：' + bookAuthor
  return new Promise((resolve) => {
    uni.showModal({
      title: '确认借阅',
      content,
      success: async (res) => {
        if (!res.confirm) { resolve(false); return }
        try {
          await createBorrow({ copyCode: code })
          uni.showToast({ title: '借阅成功', icon: 'success' })
          resolve(true)
        } catch (err) {
          resolve(false)
        }
      }
    })
  })
}

async function confirmReturn(code) {
  const copyInfo = await lookupCopy(code)
  if (!copyInfo || !copyInfo.copyId) {
    uni.showToast({ title: '未找到该副本', icon: 'none' })
    return false
  }
  const bookTitle = copyInfo.bookTitle || copyInfo.title || '未知书名'
  return new Promise((resolve) => {
    uni.showModal({
      title: '确认归还',
      content: '副本码：' + code + '\n图书：' + bookTitle,
      success: async (res) => {
        if (!res.confirm) { resolve(false); return }
        try {
          await returnByCopyCode(code)
          uni.showToast({ title: '归还成功', icon: 'success' })
          resolve(true)
        } catch (err) {
          resolve(false)
        }
      }
    })
  })
}

export async function scanAndBorrow() {
  const code = await scanWithFallback('借阅')
  if (!code) return false
  return confirmBorrow(code)
}

export async function scanAndReturn() {
  const code = await scanWithFallback('归还')
  if (!code) return false
  return confirmReturn(code)
}