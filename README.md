# Library — 图书馆管理系统

## 技术栈

- **管理端 Web：** Vue 3 + Vite + Element Plus + Pinia + ECharts
- **读者端 Web：** Vue 3 + Vite + Element Plus（保留 `/user/**` 路由）
- **小程序/App：** uni-app（Vue 3 + Vite + Pinia + uni-ui）
- **后端：** Java 21 + Spring Boot 3.5.6 + MyBatis-Plus + Spring Security + JWT
- **数据库：** MySQL 8.0
- **接口文档：** Springdoc OpenAPI `/swagger-ui.html`

## 权限模型

| 角色 | role | 管理端 Web | 读者端 Web/小程序 | 小程序管理入口 |
|------|------|------------|-------------------|----------------|
| 超级管理员 | 0 | 全部权限 | 不可访问 | 可访问 |
| 协管员 | 2 | 大部分权限 | 不可访问 | 可访问 |
| 普通用户 | 1 | 不可访问 | 借阅/归还/查书 | 不可访问 |
| 游客 | 3 | 不可访问 | 不可登录 | 不可访问 |

认证：用户名密码 + JWT。`/api/admin/login` 面向管理端 Web；`/api/auth/login` 面向小程序/App，允许普通用户和管理员登录，拒绝游客账号。

## 核心功能

### 管理端（Vue Web，电脑浏览器）

- 数据看板（统计卡片 + 图表 + 异常待办 + 操作流水）
- 图书管理（增删改查、ISBN 兼容查询、分类筛选、条码生成下载）
- 副本管理（补录、标记丢失、恢复在馆、移动库位、删除、条码下载）
- 扫码入库（本地元数据 + OpenLibrary + Google Books）
- 借阅管理（业务化列表、按读者/图书/副本码搜索、扫码借阅）
- 用户管理（角色管理、封禁解封、重置密码、资料编辑）
- 分类管理、库位管理、ISBN 资料库维护
- 主题切换、管理员资料页

### 用户端（uni-app 微信小程序/App）

- 账号密码登录（自动跳转已登录用户，按角色进入读者端或小程序管理入口）
- 图书大厅（搜索防抖、仅看可借筛选、分页、下拉刷新、扫码借阅）
- 图书详情（封面、信息、loading/error/empty 三态、扫码借阅）
- 我的借阅（状态筛选、到期倒计时、按记录归还、扫码归还）
- 个人资料（查看、编辑、退出登录）
- 小程序管理入口（管理员扫码入库、扫码出库、手动入库、手动出库）
- 设计 token 统一视觉（CSS 变量、通用组件样式）

## 启动

### 数据库

```bash
mysql -u root -p < sqlfile/init.sql
```

### 后端

`application.yaml` 通过环境变量读取数据库和 JWT 配置，默认连接本机 `librarymanage` 数据库。首次运行至少建议设置数据库密码和 JWT secret：

```bash
export LIBRARY_DB_PASSWORD='你的本地数据库密码'
export LIBRARY_JWT_SECRET='请替换为至少 32 位的随机密钥'
```

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

### 管理端

```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:3000/admin/dashboard
```

### 用户端

1. `cd miniprogram && npm install`
2. `npm run dev:mp-weixin`（开发）或 `npm run build:mp-weixin`（构建）
3. 微信开发者工具导入 `miniprogram/dist/dev/mp-weixin`（开发）或 `dist/build/mp-weixin`（构建）
4. HBuilderX App 真机调试可运行 App 平台，或导入 `miniprogram/dist/build/app`
5. 先启动后端
6. 电脑开发者工具可用 `env = 'devtools'`，请求 `http://127.0.0.1:8080/api`
7. 手机真机调试使用 `env = 'lan'`，当前请求 `http://192.168.194.8:8080/api`
8. 手机浏览器访问局域网 API 返回 `401` 代表网络已通，只是未登录
9. 使用 `reader001 / user123` 登录测试

## 关键接口

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 小程序/App 登录（role=1/0/2，拒绝游客） |
| POST | `/api/admin/login` | 管理端登录（兼容） |
| GET | `/api/admin/me` | 当前管理员资料 |
| PUT | `/api/admin/me/profile` | 修改管理员资料 |
| PUT | `/api/admin/me/password` | 修改管理员密码 |

### 管理端

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/books` | 图书列表（keyword/categoryId/locationId/lostOnly/author/isbn） |
| GET/POST/PUT/DELETE | `/api/admin/books/{id}` | 图书 CRUD |
| GET/POST | `/api/admin/books/{id}/copies` | 副本列表/新增 |
| PUT | `/api/admin/books/copies/{copyId}/status` | 副本状态变更 |
| PUT | `/api/admin/books/copies/{copyId}/location` | 副本移动库位 |
| DELETE | `/api/admin/books/copies/{copyId}` | 删除副本 |
| GET | `/api/admin/books/barcode` | 条码图片 |
| POST | `/api/admin/books/scan-inbound` | 扫码入库 |
| GET | `/api/admin/borrows` | 借阅列表（支持关键字筛选） |
| POST | `/api/admin/borrows` | 创建借阅 |
| PUT | `/api/admin/borrows/{id}/return` | 归还 |
| PUT | `/api/admin/borrows/refresh-status` | 刷新逾期状态 |
| GET/POST/PUT/DELETE | `/api/admin/users/{id}` | 用户管理 |
| GET/POST/PUT/DELETE | `/api/admin/isbn-metadata/{id}` | ISBN 资料库 |
| GET/POST/PUT/DELETE | `/api/admin/shelves/{id}` | 库位管理 |
| GET | `/api/admin/dashboard/*` | 看板接口 |

### 用户端（小程序/App）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 普通用户进入读者端，管理员进入小程序管理入口，游客拒绝登录 |
| GET | `/api/account/me` | 当前用户资料（需 USER 角色） |
| PUT | `/api/account/me/profile` | 修改个人资料 |
| GET | `/api/user/books` | 图书列表（keyword/categoryId/availableOnly） |
| GET | `/api/user/books/{id}` | 图书详情 |
| GET | `/api/user/books/lookup` | 按 ISBN 查询 |
| GET | `/api/user/books/copy-lookup` | 扫码副本查询（返回 CopyLookupView） |
| POST | `/api/user/borrows` | 创建借阅（返回 BorrowActionResult） |
| GET | `/api/user/borrows/me` | 我的借阅（返回 BorrowRecordView） |
| PUT | `/api/user/borrows/{id}/return` | 按记录归还（返回 ReturnActionResult） |
| PUT | `/api/user/borrows/return-by-copy-code` | 扫码归还（返回 ReturnActionResult） |

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 超级管理员 | admin | admin123 |
| 协管员 | assistant001 | admin123 |
| 普通用户 | reader001 | user123 |
| 普通用户 | reader002 | user123 |
| 游客 | guest001 | user123 |

## 生产部署

管理端打包：

```bash
cd frontend && npm run build
```

Nginx 托管 `frontend/dist`，`/api/` 反向代理到 Spring Boot。

小程序正式版需要：真实 AppID + HTTPS API 域名 + 微信公众平台 request 合法域名。

App 正式包建议同步改为 HTTPS API，并按 Android/iOS 平台要求配置网络安全策略。

## 参考文档

- 技术参考：`document/项目总览.md`
- 小程序运行：`miniprogram/README.md`
