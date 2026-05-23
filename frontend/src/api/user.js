import request from './request'

export const getAccountMe = () => request.get('/account/me')
export const updateAccountProfile = (data) => request.put('/account/me/profile', data)

export const getUserBookList = (params) => request.get('/user/books', { params })
export const getUserBookById = (id) => request.get(`/user/books/${id}`)
export const lookupUserBookByIsbn = (isbn) => request.get('/user/books/lookup', { params: { isbn } })
export const lookupUserCopyByCode = (code) => request.get('/user/books/copy-lookup', { params: { code } })

export const createUserBorrow = (data) => request.post('/user/borrows', data)
export const getMyBorrows = (params) => request.get('/user/borrows/me', { params })
export const returnMyBorrow = (id) => request.put(`/user/borrows/${id}/return`)
export const returnMyBorrowByCopyCode = (copyCode) => request.put('/user/borrows/return-by-copy-code', { copyCode })
