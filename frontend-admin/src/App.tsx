import { useEffect, useState } from 'react'
import {
  AppstoreOutlined,
  DatabaseOutlined,
  FolderOutlined,
  LockOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  OrderedListOutlined,
  PlusOutlined,
  ShoppingOutlined,
  UserOutlined,
} from '@ant-design/icons'
import {
  Alert,
  App as AntdApp,
  Avatar,
  Button,
  Card,
  Dropdown,
  Form,
  Input,
  Layout,
  Menu,
  Space,
  Typography,
} from 'antd'
import {
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from 'react-router-dom'
import CategoryPage from './pages/CategoryPage'
import DashboardPage from './pages/DashboardPage'
import OrderManagePage from './pages/OrderManagePage'
import ProductManagePage from './pages/ProductManagePage'
import ProductPage from './pages/ProductPage'
import StockLogPage from './pages/StockLogPage'
import {
  clearSession,
  getSession,
  login,
  logout,
  type AdminSession,
  type LoginRequest,
} from './services/auth'
import {
  ADMIN_SESSION_CHANGE_EVENT,
  ADMIN_SESSION_STORAGE_KEY,
  ApiError,
} from './services/http'

const { Header, Sider, Content } = Layout
const { Title, Text } = Typography

function LoginPage({ onLogin }: { onLogin: (session: AdminSession) => void }) {
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const handleLogin = async (values: LoginRequest) => {
    setSubmitting(true)
    setErrorMessage('')

    try {
      const session = await login(values)
      onLogin(session)
    } catch (error) {
      setErrorMessage(
        error instanceof ApiError ? error.message : '登录失败，请稍后重试',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-intro" aria-label="管理后台介绍">
        <div className="login-brand">
          <div className="brand-mark">A</div>
          <span>Admin Console</span>
        </div>

        <div className="login-copy">
          <Text className="login-eyebrow">WELCOME BACK</Text>
          <Title>清晰、高效地管理你的业务</Title>
          <Text className="login-description">
            登录管理后台，进入统一工作台。
          </Text>
        </div>

        <Text className="login-copyright">© 2026 Admin Console</Text>
      </section>

      <section className="login-form-panel">
        <Card className="login-card" bordered={false}>
          <div className="login-card-heading">
            <Title level={2}>账号登录</Title>
            <Text type="secondary">请输入管理员账号和密码</Text>
          </div>

          {errorMessage && (
            <Alert
              className="login-alert"
              message={errorMessage}
              type="error"
              showIcon
            />
          )}

          <Form<LoginRequest>
            layout="vertical"
            requiredMark={false}
            onFinish={handleLogin}
            autoComplete="on"
            disabled={submitting}
          >
            <Form.Item
              label="账号"
              name="username"
              rules={[
                { required: true, message: '请输入账号' },
                { min: 4, max: 32, message: '账号长度为 4 到 32 个字符' },
                {
                  pattern: /^[A-Za-z0-9_]+$/,
                  message: '账号只能包含字母、数字和下划线',
                },
              ]}
            >
              <Input
                size="large"
                prefix={<UserOutlined />}
                placeholder="请输入账号"
                autoComplete="username"
              />
            </Form.Item>
            <Form.Item
              label="密码"
              name="password"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, max: 64, message: '密码长度为 6 到 64 个字符' },
              ]}
            >
              <Input.Password
                size="large"
                prefix={<LockOutlined />}
                placeholder="请输入密码"
                autoComplete="current-password"
              />
            </Form.Item>
            <Form.Item className="login-submit-item">
              <Button
                type="primary"
                htmlType="submit"
                size="large"
                loading={submitting}
                block
              >
                登录
              </Button>
            </Form.Item>
          </Form>
        </Card>
      </section>
    </main>
  )
}

function AdminLayout({
  session,
  onLogout,
  loggingOut,
}: {
  session: AdminSession
  onLogout: () => Promise<void>
  loggingOut: boolean
}) {
  const [collapsed, setCollapsed] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  let activeMenuKey = '/dashboard'
  if (location.pathname.startsWith('/categories')) {
    activeMenuKey = '/categories'
  } else if (location.pathname.startsWith('/orders')) {
    activeMenuKey = '/orders'
  } else if (location.pathname.startsWith('/products/create')) {
    activeMenuKey = '/products/create'
  } else if (location.pathname.startsWith('/products/stock-logs')) {
    activeMenuKey = '/products/stock-logs'
  } else if (location.pathname.startsWith('/products')) {
    activeMenuKey = '/products'
  }

  return (
    <Layout className="app-shell">
      <Sider
        className="app-sider"
        collapsible
        collapsed={collapsed}
        trigger={null}
        breakpoint="lg"
        onBreakpoint={setCollapsed}
      >
        <div className="brand">
          <div className="brand-mark">A</div>
          {!collapsed && <span>Admin Console</span>}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          defaultOpenKeys={['products']}
          selectedKeys={[activeMenuKey]}
          onClick={({ key }) => navigate(key)}
          items={[
            {
              key: '/dashboard',
              icon: <AppstoreOutlined />,
              label: '工作台',
            },
            {
              key: '/categories',
              icon: <FolderOutlined />,
              label: '分类管理',
            },
            {
              key: '/orders',
              icon: <OrderedListOutlined />,
              label: '订单管理',
            },
            {
              key: 'products',
              icon: <ShoppingOutlined />,
              label: '商品管理',
              children: [
                {
                  key: '/products',
                  label: '商品列表',
                },
                {
                  key: '/products/create',
                  icon: <PlusOutlined />,
                  label: '创建商品',
                },
                {
                  key: '/products/stock-logs',
                  icon: <DatabaseOutlined />,
                  label: '库存变更记录',
                },
              ],
            },
          ]}
        />
      </Sider>

      <Layout>
        <Header className="app-header">
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed((value) => !value)}
            aria-label={collapsed ? '展开侧边栏' : '收起侧边栏'}
          />
          <Dropdown
            menu={{
              items: [
                {
                  key: 'logout',
                  icon: <LogoutOutlined />,
                  label: loggingOut ? '正在退出...' : '退出登录',
                  danger: true,
                  disabled: loggingOut,
                  onClick: () => void onLogout(),
                },
              ],
            }}
          >
            <Space className="user-menu">
              <Avatar icon={<UserOutlined />} />
              <Text>{session.user.nickname || session.user.username}</Text>
            </Space>
          </Dropdown>
        </Header>

        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

function App() {
  const [session, setSession] = useState<AdminSession | null>(getSession)
  const [loggingOut, setLoggingOut] = useState(false)
  const { message } = AntdApp.useApp()

  useEffect(() => {
    const syncStoredSession = (event: StorageEvent) => {
      if (
        event.key === ADMIN_SESSION_STORAGE_KEY ||
        event.key === null
      ) {
        setSession(getSession())
      }
    }
    const syncCurrentSession = () => setSession(getSession())

    window.addEventListener('storage', syncStoredSession)
    window.addEventListener(ADMIN_SESSION_CHANGE_EVENT, syncCurrentSession)
    return () => {
      window.removeEventListener('storage', syncStoredSession)
      window.removeEventListener(ADMIN_SESSION_CHANGE_EVENT, syncCurrentSession)
    }
  }, [])

  const handleLogout = async () => {
    setLoggingOut(true)
    try {
      await logout()
      message.success('已安全退出登录')
    } catch (error) {
      message.error(
        error instanceof Error
          ? `${error.message}，本地登录状态已清除`
          : '退出接口调用失败，本地登录状态已清除',
      )
    } finally {
      clearSession()
      setSession(null)
      setLoggingOut(false)
    }
  }

  return (
    <Routes>
      <Route
        path="/login"
        element={
          session
            ? <Navigate to="/dashboard" replace />
            : <LoginPage onLogin={setSession} />
        }
      />

      <Route
        element={
          session
            ? (
                <AdminLayout
                  session={session}
                  onLogout={handleLogout}
                  loggingOut={loggingOut}
                />
              )
            : <Navigate to="/login" replace />
        }
      >
        <Route path="/dashboard" element={<DashboardPage session={session!} />} />
        <Route path="/categories" element={<CategoryPage />} />
        <Route path="/orders" element={<OrderManagePage />} />
        <Route path="/products" element={<ProductManagePage />} />
        <Route path="/products/create" element={<ProductPage />} />
        <Route path="/products/stock-logs" element={<StockLogPage />} />
      </Route>

      <Route
        path="*"
        element={<Navigate to={session ? '/dashboard' : '/login'} replace />}
      />
    </Routes>
  )
}

export default App
