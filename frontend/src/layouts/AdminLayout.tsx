import { Layout, Menu, Typography } from 'antd'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { paths } from '@/router/paths'

const { Header, Sider, Content } = Layout

export function AdminLayout() {
  const location = useLocation()

  return (
    <Layout className="admin-layout">
      <Sider breakpoint="lg" collapsedWidth="0">
        <Link className="admin-brand" to={paths.adminHome}>
          Hard Admin
        </Link>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={[
            {
              key: paths.adminHome,
              label: <Link to={paths.adminHome}>工作台</Link>,
            },
          ]}
        />
      </Sider>
      <Layout>
        <Header className="admin-header">
          <Typography.Text strong>管理端</Typography.Text>
        </Header>
        <Content className="admin-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
