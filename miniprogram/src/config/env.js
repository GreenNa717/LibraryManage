const env = 'lan'

const configs = {
  devtools: {
    apiBaseUrl: 'http://127.0.0.1:8080/api'
  },
  lan: {
    apiBaseUrl: 'http://192.168.194.8:8080/api'
  },
  tunnel: {
    apiBaseUrl: 'https://你的内网穿透域名/api'
  },
  prod: {
    apiBaseUrl: 'https://api.example.com/api'
  }
}

export default {
  env,
  ...configs[env]
}
