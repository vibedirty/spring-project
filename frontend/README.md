# Hard Frontend

`frontend` 是与 `spring-cloud-backend/spring-java` 独立启动的 React 用户端工程；独立管理端位于同级 `frontend-admin` 项目。当前已接入用户注册、登录、启用分类导航和在售商品分页列表，不包含 mock 数据。

## 技术栈

- React + Vite + TypeScript
- React Router
- Zustand
- Axios
- Ant Design

建议使用 Node.js 20.19+。

## 启动

```bash
npm install
npm run dev
```

开发服务器固定运行在 `http://localhost:5173`。浏览器直接跨域访问 `http://localhost:9000` 的 Gateway，再由 Gateway 通过 Nacos 将请求转发到单体实例；Vite 不代理 API 请求，端口被占用时会直接报错。

如需修改地址，复制 `.env.example` 为 `.env.local` 并调整：

- `VITE_API_BASE_URL`：浏览器访问的 Gateway API 地址，默认 `http://localhost:9000/api`

## 校验

```bash
npm run lint
npm run build
npm run preview
```

## 目录职责

```text
src/
├── api/          # Axios 实例、统一响应类型和后续业务 API
├── components/   # 用户端与管理端可复用组件
├── config/       # 环境配置读取
├── layouts/      # 用户端、管理端基础布局
├── pages/        # 按 user、admin、shared 划分的页面
├── router/       # 路由表、路径常量和角色守卫
├── stores/       # Zustand 全局状态
├── styles/       # 全局样式
├── types/        # 跨模块领域类型
└── utils/        # Token 等无 UI 工具
```

## 已有基础约定

- 所有后端请求通过 `request<T>()` 发送，页面不直接解析 Axios 响应。
- 后端统一响应格式为 `{ code, message, data }`，`code === 200` 表示成功。
- 业务错误由请求层转换为 `ApiError` 并通过 Ant Design 显示消息。
- JWT 只保存在 localStorage，刷新后会恢复；过期 Token 会被清理。
- 请求拦截器自动添加 `Authorization: Bearer <token>`。
- 401 会清除登录态并跳转对应登录入口，403 会进入无权限页面。
- `/account` 仅允许 `USER`，`/admin` 仅允许 `ADMIN`。
- `/register` 提供普通用户注册，成功后保存后端 JWT 并进入 `/account`。
- `/login` 调用普通用户登录接口，成功后恢复原访问页或进入用户端首页。
- 首页通过统一请求工具调用 `GET /api/categories`，匿名用户可查看并切换启用分类；前 10 项之外可通过“更多”进入 `/categories` 查看。
- 首页调用 `GET /api/products` 展示在售商品卡片，并按后端返回的总数进行服务端分页；支持使用 `categoryId`、`keyword` 与价格 `sort` 组合筛选，筛选状态同步到 URL。
- 商品卡片可进入 `/products/:productId`，通过 `GET /api/products/{id}` 展示仍在售商品的详情。
- 注册成功后会同时保存用户 ID、用户名、昵称和角色；刷新页面后顶部仍会显示当前用户信息。

普通用户登录和管理员登录功能后续接入时，应同时保存 Token 与接口返回的用户资料：

```ts
useAuthStore.getState().setSession({ token, user })
```
