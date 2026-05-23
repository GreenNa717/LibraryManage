import { get } from './request'

export function getBooks(params) {
  return get('/user/books', params)
}

export function getBookById(id) {
  return get(`/user/books/${id}`)
}

export function lookupCopy(code) {
  return get('/user/books/copy-lookup', { code })
}
