import { get, post, put } from './request'

export function createBorrow(data) {
  return post('/user/borrows', data)
}

export function getMyBorrows(params) {
  return get('/user/borrows/me', params)
}

export function returnBorrow(id) {
  return put(`/user/borrows/${id}/return`)
}

export function returnByCopyCode(copyCode) {
  return put('/user/borrows/return-by-copy-code', { copyCode })
}
