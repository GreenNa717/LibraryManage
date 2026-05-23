# uni-app 用户端现状说明

更新时间：2026-05-17

## 1. 当前结论

项目用户端现在只保留 uni-app 方案。

当前用户端源码目录：

```text
miniprogram/
```

说明：

- `miniprogram/` 是 uni-app 工程源码，不再是原生微信小程序源码。
- 旧的 WXML/WXSS/JS 原生小程序目录已经不作为项目源码保留。
- 微信开发者工具应导入 uni-app 构建产物，而不是直接导入源码目录。

## 2. 目录结构

```text
miniprogram/
  package.json
  package-lock.json
  vite.config.js
  index.html
  public/
  src/
    App.vue
    main.js
    manifest.json
    pages.json
    uni.scss
    api/
    components/
    config/
    pages/
    static/
    stores/
    styles/
    utils/
  dist/
    build/app/
    build/mp-weixin/
    dev/mp-weixin/
```

关键说明：

- `src/` 是唯一需要长期维护的用户端源码。
- `dist/` 是 uni-app 构建产物，可删除后重新构建生成。
- `node_modules/` 是依赖目录，不作为源码维护。
- `src/static/tab/` 中的 tabBar 图标使用 `@dcloudio/uni-ui` 的 `uni-icons` 字体生成 PNG，是 tabBar 实际引用目录。
- `public/tab/` 目前仅作为图标备份目录，不建议让 `pages.json` 直接引用 `public/tab`。

## 3. 技术栈

- Vue 3
- Vite
- uni-app
- Pinia
- uni-ui
- Sass

## 4. 运行方式

安装依赖：

```bash
cd miniprogram
npm install
```

开发构建：

```bash
npm run dev:mp-weixin
```

微信开发者工具导入：

```text
miniprogram/dist/dev/mp-weixin
```

正式构建：

```bash
npm run build:mp-weixin
```

微信开发者工具导入：

```text
miniprogram/dist/build/mp-weixin
```

App 真机调试构建：

```bash
npx uni build -p app
```

HBuilderX 导入或运行：

```text
miniprogram/dist/build/app
```

说明：

- 微信小程序运行时导入 `dist/dev/mp-weixin` 或 `dist/build/mp-weixin`。
- App 真机调试时使用 HBuilderX 运行 App 平台，不要导入微信小程序构建产物。
- HBuilderX 通过 USB 安装到手机只负责调试安装，网络请求仍走手机当前网络。

## 5. 环境配置

配置文件：

```text
miniprogram/src/config/env.js
```

环境说明：

| env | 场景 | API |
|---|---|---|
| `devtools` | 电脑微信开发者工具 | `http://127.0.0.1:8080/api` |
| `lan` | 手机真机局域网调试 | `http://你的电脑局域网IP:8080/api` |
| `tunnel` | HTTPS 内网穿透 | `https://穿透域名/api` |
| `prod` | 正式版 | `https://正式API域名/api` |

注意：

- 真机不能使用 `127.0.0.1`，因为那会指向手机本机。
- 手机浏览器能打开 `http://电脑局域网IP:8080/api` 并返回 `401`，说明网络已经通，401 只是未登录或 token 过期。
- 默认本机调试配置为 `const env = 'devtools'`，请求 `http://127.0.0.1:8080/api`。
- 手机真机调试前需改为 `env = 'lan'`，并同步修改 `lan.apiBaseUrl` 中的电脑局域网 IP。
- Windows 防火墙需要允许后端 8080 端口入站，否则手机可能访问不到电脑后端。
- 体验版和正式版必须使用 HTTPS 合法域名。
- 微信公众平台必须配置 request 合法域名。

## 5.1 底部导航栏配置

配置文件：

```text
miniprogram/src/pages.json
```

当前 tabBar 使用 uni-app 原生配置：

```json
{
  "height": "72px",
  "fontSize": "13px",
  "iconWidth": "38px",
  "spacing": "3px"
}
```

图标资源：

```text
miniprogram/src/static/tab/
  tab-book.png
  tab-book-active.png
  tab-borrow.png
  tab-borrow-active.png
  tab-profile.png
  tab-profile-active.png
```

说明：

- 6 张图标源文件尺寸为 `64x64` PNG。
- App 端实际显示宽度由 `pages.json` 的 `iconWidth: "38px"` 控制。
- 微信小程序原生 tabBar 会忽略部分 App/H5 尺寸字段，构建器会在 `mp-weixin` 产物中自动剥离不适用字段。
- 如果手机上图标不显示，优先检查 `dist/build/app/static/tab/` 或 `dist/build/mp-weixin/static/tab/` 是否存在这些 PNG。
- 不建议使用 `public/tab/...` 作为 tabBar 路径，因为小程序/App 产物不一定按该路径复制资源。

## 6. 当前页面

```text
src/pages/login/index.vue
src/pages/books/index.vue
src/pages/book-detail/index.vue
src/pages/borrows/index.vue
src/pages/profile/index.vue
```

当前能力：

- 读者登录
- 图书大厅
- 图书搜索
- 仅看可借
- 图书详情
- 扫码借阅
- 我的借阅
- 借阅状态筛选
- 到期状态展示
- 按记录归还
- 扫码归还
- 个人资料查看与编辑
- 退出登录

## 7. 当前组件

```text
src/components/AppEmpty.vue
src/components/BookCard.vue
src/components/BorrowCard.vue
src/components/ScanFab.vue
src/components/StatusTag.vue
```

## 8. 需要保留的内容

必须保留：

- `miniprogram/package.json`
- `miniprogram/package-lock.json`
- `miniprogram/vite.config.js`
- `miniprogram/index.html`
- `miniprogram/src/**`

可以重建：

- `miniprogram/node_modules/`
- `miniprogram/dist/`

不再需要：

- 旧原生微信小程序的 `app.js/app.json/app.wxss`
- 旧原生微信小程序的根级 `pages/`
- 旧原生微信小程序的根级 `components/`
- 旧原生微信小程序的根级 `api/`
- 旧原生微信小程序的根级 `utils/`
- 旧原生微信小程序的根级 `config/`

上述旧目录当前已经不在项目源码中。

## 9. 后续维护原则

- 后续用户端功能只改 `miniprogram/src/`。
- 不再新增原生小程序 WXML/WXSS 页面。
- 微信平台能力通过 `uni.*` API 调用。
- tabBar 继续使用 uni-app 原生 `pages.json` 配置。
- tabBar 图标路径统一使用 `static/tab/...`。
- 构建产物只用于微信开发者工具调试和上传，不作为主要源码维护。
