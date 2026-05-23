import request from './request'

// 登录
export const login = (data) => request.post('/admin/login', data)
export const getCurrentAdmin = () => request.get('/admin/me')
export const updateCurrentAdminProfile = (data) => request.put('/admin/me/profile', data)
export const changeCurrentAdminPassword = (data) => request.put('/admin/me/password', data)

// 数据看板
export const getStats = () => request.get('/admin/dashboard/stats')
export const getHotBooks = (limit) => request.get('/admin/dashboard/hot-books', { params: { limit } })
export const getMonthlyBorrows = () => request.get('/admin/dashboard/monthly-borrows')
export const getBookStocks = () => request.get('/admin/dashboard/book-stocks')
export const getActivityFeed = (limit) => request.get('/admin/dashboard/activity-feed', { params: { limit } })
export const getInventoryHealth = () => request.get('/admin/dashboard/inventory-health')
export const getShelfCapacity = () => request.get('/admin/dashboard/shelf-capacity')
export const getExceptions = () => request.get('/admin/dashboard/exceptions')

// 图书管理
export const getBookList = (params) => request.get('/admin/books', { params })
export const getBookById = (id) => request.get(`/admin/books/${id}`)
export const lookupBookByIsbn = (isbn) => request.get('/admin/books/lookup', { params: { isbn } })
export const lookupCopyByCode = (code) => request.get('/admin/books/copy-lookup', { params: { code } })
export const addBook = (data) => request.post('/admin/books', data)
export const updateBook = (id, data) => request.put(`/admin/books/${id}`, data)
export const deleteBook = (id) => request.delete(`/admin/books/${id}`)
export const getBookCopies = (bookId) => request.get(`/admin/books/${bookId}/copies`)
export const addBookCopies = (bookId, data) => request.post(`/admin/books/${bookId}/copies`, data)
export const updateBookCopyStatus = (copyId, status) => request.put(`/admin/books/copies/${copyId}/status`, { status })
export const updateBookCopyLocation = (copyId, locationId) => request.put(`/admin/books/copies/${copyId}/location`, { locationId })
export const deleteBookCopy = (copyId) => request.delete(`/admin/books/copies/${copyId}`)
export const generateBarcode = (code) => request.get('/admin/books/barcode', { params: { code }, responseType: 'blob' })
export const scanInbound = (data) => request.post('/admin/books/scan-inbound', data)
export const getIsbnMetadata = (isbn) => request.get('/admin/books/isbn-metadata', { params: { isbn } })

// ISBN 元数据资料库
export const getIsbnMetadataList = (params) => request.get('/admin/isbn-metadata', { params })
export const addIsbnMetadata = (data) => request.post('/admin/isbn-metadata', data)
export const updateIsbnMetadata = (id, data) => request.put(`/admin/isbn-metadata/${id}`, data)
export const deleteIsbnMetadata = (id) => request.delete(`/admin/isbn-metadata/${id}`)

// 库位管理
export const getShelfList = () => request.get('/admin/shelves')
export const addShelf = (data) => request.post('/admin/shelves', data)
export const updateShelf = (id, data) => request.put(`/admin/shelves/${id}`, data)
export const deleteShelf = (id) => request.delete(`/admin/shelves/${id}`)

// 分类管理
export const getCategoryList = () => request.get('/admin/categories')
export const addCategory = (data) => request.post('/admin/categories', data)
export const updateCategory = (id, data) => request.put(`/admin/categories/${id}`, data)
export const deleteCategory = (id) => request.delete(`/admin/categories/${id}`)

// 用户管理
export const getUserList = (params) => request.get('/admin/users', { params })
export const addUser = (data) => request.post('/admin/users', data)
export const getUserById = (id) => request.get(`/admin/users/${id}`)
export const getUserDetail = (id) => request.get(`/admin/users/${id}/detail`)
export const updateUserProfile = (id, data) => request.put(`/admin/users/${id}/profile`, data)
export const toggleUserStatus = (id, status) => request.put(`/admin/users/${id}/status`, null, { params: { status } })
export const resetPassword = (id, password) => request.put(`/admin/users/${id}/reset-password`, null, { params: { password } })
export const updateUserRole = (id, role) => request.put(`/admin/users/${id}/role`, null, { params: { role } })

// 借阅管理
export const getBorrowList = (params) => request.get('/admin/borrows', { params })
export const createBorrow = (data) => request.post('/admin/borrows', data)
export const doReturn = (id) => request.put(`/admin/borrows/${id}/return`)
export const refreshStatus = () => request.put('/admin/borrows/refresh-status')

// 副本管理
export const copyLookup = (code) => request.get('/admin/books/copy-lookup', { params: { code } })
