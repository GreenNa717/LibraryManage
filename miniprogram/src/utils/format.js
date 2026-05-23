export function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 10)
}

export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

export function statusText(status) {
  if (status === 0) return '借阅中'
  if (status === 1) return '已归还'
  if (status === 2) return '已逾期'
  return '未知'
}

export function formatDueState(dueTime, status) {
  if (status === 1) return '已归还'
  if (status === 2) {
    if (!dueTime) return '已逾期'
    const dueStr = String(dueTime).replace('T', ' ').slice(0, 10)
    const dueDate = new Date(dueStr.replace(/-/g, '/'))
    if (isNaN(dueDate.getTime())) return '已逾期'
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    dueDate.setHours(23, 59, 59, 0)
    const diff = today.getTime() - dueDate.getTime()
    const days = Math.round(diff / 86400000)
    if (days > 0) return '已逾期 ' + days + ' 天'
    return '已逾期'
  }
  if (!dueTime) return ''
  const dueStr = String(dueTime).replace('T', ' ').slice(0, 10)
  const dueDate = new Date(dueStr.replace(/-/g, '/'))
  if (isNaN(dueDate.getTime())) return ''
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  dueDate.setHours(23, 59, 59, 0)
  const diff = dueDate.getTime() - today.getTime()
  const days = Math.round(diff / 86400000)
  if (days < 0) return '已逾期 ' + Math.abs(days) + ' 天'
  if (days === 0) return '今天到期'
  return days + ' 天后到期'
}
