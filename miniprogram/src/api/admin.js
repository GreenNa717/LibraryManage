import { get, post, put } from './request'

export function scanInbound(data) {
  return post('/admin/books/scan-inbound', data)
}

export function updateCopyStatus(copyId, status) {
  return put(`/admin/books/copies/${copyId}/status`, { status })
}

export function lookupCopy(code) {
  return get('/admin/books/copy-lookup', { code })
}

export function lookupIsbn(isbn) {
  return get('/admin/books/isbn-metadata', { isbn })
}

export function getAdminStats() {
  return get('/admin/dashboard/stats')
}
