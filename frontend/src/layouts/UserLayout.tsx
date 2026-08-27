import { useState } from 'react'
import { App as AntdApp, Avatar, Button, Layout, Space, Typography } from 'antd'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { logout } from '@/api/auth'
import { paths } from '@/router/paths'
import { useAuthStore } from '@/stores/authStore'

const { Header, Content, Footer } = Layout

export function UserLayout() {
  const [loggingOut, setLoggingOut] = useState(false)
  const navigate = useNavigate()
  const { isAuthenticated, role, user, clearSession } = useAuthStore()
  const { message } = AntdApp.useApp()

  const handleLogout = async () => {
    setLoggingOut(true)
    try {
      await logout()
      void message.success('已安全退出登录')
    } catch {
      // 接口异常提示由 HTTP 拦截器处理，本地会话仍需清理
    } finally {
      clearSession()
      navigate(paths.home, { replace: true })
      setLoggingOut(false)
    }
  }

  return (
    <Layout className="site-layout">
      <Header className="site-header">
        <Link className="site-brand" to={paths.home}>
          Hard Store
        </Link>
        <Space size="middle" className="site-navigation">
          <Link className="site-nav-link" to={paths.home}>
            首页
          </Link>
          <Link className="site-nav-link" to={paths.cart}>
            购物车
          </Link>
          <Link className="site-nav-link" to={paths.orders}>
            我的订单
          </Link>
          <Link className="site-nav-link" to={paths.addresses}>
            收货地址
          </Link>
          {isAuthenticated ? (
            <>
              <Link
                className="current-user"
                to={role === 'ADMIN' ? paths.adminHome : paths.account}
              >
                <Space size={8}>
                  <Avatar size="small">
                    {(user?.nickname || user?.username || '用户').slice(0, 1)}
                  </Avatar>
                  <Typography.Text>
                    {user?.nickname || user?.username || '已登录用户'}
                  </Typography.Text>
                </Space>
              </Link>
              <Button
                loading={loggingOut}
                disabled={loggingOut}
                onClick={() => void handleLogout()}
              >
                退出登录
              </Button>
            </>
          ) : (
            <>
              <Link className="site-nav-link" to={paths.login}>
                登录
              </Link>
              <Button type="primary" href={paths.register}>
                注册
              </Button>
            </>
          )}
        </Space>
      </Header>
      <Content className="site-content">
        <Outlet />
      </Content>
      <Footer className="site-footer">
        <Typography.Text type="secondary">Hard 学习级电商系统</Typography.Text>
      </Footer>
    </Layout>
  )
}
