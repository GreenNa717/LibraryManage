const env = 'devtools'

const configs = {
  devtools: {
    apiBaseUrl: 'http://127.0.0.1:8080/api'
  },
  lan: {
    apiBaseUrl: 'http://你的电脑局域网IP:8080/api'
  },
  tunnel: {
    apiBaseUrl: 'https://你的内网穿透域名/api'
  },
  prod: {
    apiBaseUrl: 'https://你的正式API域名/api'
  }
}

export default {
  env,
  ...configs[env]
}
