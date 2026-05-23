import env from '@/config/env'

let isRedirecting = false

function request(options) {
  const { url, method = 'GET', data, header = {}, silent = false, timeout = 10000 } = options
  const token = uni.getStorageSync('token') || ''
  if (token) {
    header['Authorization'] = `Bearer ${token}`
  }
  if (!header['content-type']) {
    header['content-type'] = 'application/json'
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: env.apiBaseUrl + url,
      method,
      data,
      header,
      timeout,
      success(res) {
        if (res.statusCode === 401 || res.statusCode === 403) {
          if (!isRedirecting) {
            isRedirecting = true
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            setTimeout(() => { isRedirecting = false }, 2000)
            uni.reLaunch({ url: '/pages/login/index' })
          }
          reject({ code: res.statusCode, msg: '登录状态无效或无权限' })
          return
        }
        if (res.statusCode === 500) {
          if (!silent) uni.showToast({ title: '服务器内部错误', icon: 'none' })
          reject({ code: 500, msg: '服务器内部错误' })
          return
        }
        if (typeof res.data !== 'object') {
          if (!silent) uni.showToast({ title: '响应格式异常', icon: 'none' })
          reject({ code: -1, msg: '响应格式异常' })
          return
        }
        const result = res.data
        if (result.code === 200) {
          resolve(result.data)
        } else {
          if (!silent) {
            uni.showToast({ title: result.msg || '请求失败', icon: 'none' })
          }
          reject({ code: result.code, msg: result.msg || '请求失败' })
        }
      },
      fail(err) {
        if (!silent) uni.showToast({ title: '网络异常，请检查连接', icon: 'none' })
        reject({ code: -1, msg: '网络异常' })
      }
    })
  })
}

export function get(url, params, options = {}) {
  let qs = ''
  if (params) {
    const parts = []
    Object.keys(params).forEach(k => {
      if (params[k] !== undefined && params[k] !== null && params[k] !== '') {
        parts.push(`${k}=${encodeURIComponent(params[k])}`)
      }
    })
    if (parts.length) qs = '?' + parts.join('&')
  }
  return request({ url: url + qs, method: 'GET', ...options })
}

export function post(url, data, options = {}) {
  return request({ url, method: 'POST', data, ...options })
}

export function put(url, data, options = {}) {
  return request({ url, method: 'PUT', data, ...options })
}

export default request
