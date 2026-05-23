export function scanCode() {
  return new Promise((resolve, reject) => {
    uni.scanCode({
      scanType: ['barCode', 'qrCode'],
      success(res) {
        const code = (res.result || '').trim()
        if (!code) {
          reject({ cancelled: false, msg: '扫码结果为空' })
          return
        }
        resolve(code)
      },
      fail(err) {
        if (err.errMsg && err.errMsg.indexOf('cancel') !== -1) {
          reject({ cancelled: true })
        } else {
          reject({ cancelled: false, msg: '扫码失败' })
        }
      }
    })
  })
}

export function showManualInput() {
  return new Promise((resolve, reject) => {
    uni.showModal({
      title: '手动输入副本码',
      editable: true,
      placeholderText: '请输入副本码',
      success(res) {
        if (res.confirm && res.content) {
          resolve(res.content.trim())
        } else {
          reject({ cancelled: true })
        }
      },
      fail() {
        reject({ cancelled: true })
      }
    })
  })
}
