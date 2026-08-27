# frontend-admin

基于 React、TypeScript、Ant Design、Axios 和 Vite 的管理端项目。管理员登录调用后端 `POST /api/admin/auth/login`，登录成功后进入工作台。

分类管理已接入以下管理员接口：

- `GET /api/admin/categories`：分页查询与名称、状态筛选
- `POST /api/admin/categories`：新增分类
- `POST /api/admin/categories/{id}/update`：修改分类
- `POST /api/admin/categories/{id}/delete`：逻辑删除无商品引用的分类

商品管理已接入以下管理员接口：

- `GET /api/admin/products`：分页查询与名称、分类、状态筛选
- `GET /api/admin/products/{id}`：查看商品详情
- `POST /api/admin/products`：创建草稿商品，支持分类、名称、图片 URL、描述、价格和初始库存
- `POST /api/admin/products/{id}/update`：修改商品分类、名称、图片、描述和价格
- `POST /api/admin/products/{id}/change-status`：上架或下架商品

分类请求统一通过 `services/http.ts` 的 `request<T>()` 发送并解包；后端业务错误会保留原始提示，由分类页面展示。

页面路由：

- `/login`：管理员登录
- `/dashboard`：工作台
- `/categories`：分类管理
- `/products`：商品列表、筛选、详情、编辑与状态管理
- `/products/create`：商品管理下的创建商品子页面

## 开发

```bash
npm install
npm run dev
```

复制 `.env.example` 为 `.env.local` 后可按需配置：

- `VITE_API_BASE_URL`：浏览器直接访问的 Gateway API 地址，默认 `http://localhost:9000/api`
- `VITE_ACTUATOR_BASE_URL`：Gateway 暴露的单体健康检查入口，默认 `http://localhost:9000/management/spring-java/actuator`

开发服务器固定运行在 `http://localhost:5174`。Vite 不代理 API 或 Actuator 请求，端口被占用时会直接报错。

## 校验

```bash
npm run lint
npm run build
```
