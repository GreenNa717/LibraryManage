export const formatDateTime = (value, fallback = '-') => {
  if (!value) return fallback
  const text = String(value).trim()
  if (!text) return fallback
  return text.replace('T', ' ').slice(0, 16)
}

export const formatDate = (value, fallback = '-') => {
  if (!value) return fallback
  const text = String(value).trim()
  if (!text) return fallback
  return text.replace('T', ' ').slice(0, 10)
}
