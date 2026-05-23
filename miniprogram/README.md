# 图书馆读者端（uni-app）

这是项目唯一保留的用户端源码工程，使用 uni-app + Vue3 + Vite + Pinia。

当前既可以构建微信小程序，也可以通过 HBuilderX 运行到 Android/iOS 真机 App。

## 功能范围

- 普通用户登录后进入图书大厅、我的借阅、个人资料。
- 超级管理员和协管员登录后进入小程序管理入口，可扫码/手动入库和扫码/手动出库。
- 游客账号不可登录。
- `pages/bind` 目前是微信绑定预留页，未在 tabBar 或个人资料页暴露入口。

## 运行

```bash
npm install
npm run dev:mp-weixin
```

微信开发者工具导入：

```text
dist/dev/mp-weixin
```

## 构建

```bash
npm run build:mp-weixin
```

微信开发者工具导入：

```text
dist/build/mp-weixin
```

## App 真机调试

通过 HBuilderX 连接手机运行 App 平台时，网络请求走手机当前网络，不会因为 USB 连接自动访问电脑的 `127.0.0.1`。

App 构建校验：

```bash
npx uni build -p app
```

HBuilderX 运行或导入：

```text
dist/build/app
```

## 源码位置

```text
src/
```

后续用户端功能只维护 `src/`，不再新增原生小程序 WXML/WXSS/JS 页面。

## 环境配置

```text
src/config/env.js
```

- `devtools`：电脑微信开发者工具，默认请求 `http://127.0.0.1:8080/api`
- `lan`：手机真机调试，使用电脑局域网 IP，当前为 `http://192.168.194.8:8080/api`
- `tunnel`：HTTPS 内网穿透
- `prod`：正式 HTTPS API 域名

真机调试不能使用 `127.0.0.1`，正式版必须配置 HTTPS 合法域名。

如果手机浏览器打开 `http://192.168.194.8:8080/api` 返回 `401 登录过期`，说明手机已经能访问后端；401 只是未登录或 token 过期，不是网络异常。

## 底部导航栏

配置文件：

```text
src/pages.json
```

当前 tabBar 使用原生 uni-app 配置：

```json
{
  "height": "72px",
  "fontSize": "13px",
  "iconWidth": "38px",
  "spacing": "3px"
}
```

图标资源位于：

```text
src/static/tab/
```

说明：

- 6 张图标源文件为 `64x64` PNG。
- `pages.json` 必须使用 `static/tab/...` 路径，避免真机或小程序产物找不到资源。
- `public/tab/` 只作为备份目录，不建议作为 tabBar 直接引用路径。
