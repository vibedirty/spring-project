import { Card, Descriptions, Typography } from 'antd'
import { useAuthStore } from '@/stores/authStore'

export function AccountPage() {
  const user = useAuthStore((state) => state.user)

  return (
    <Card className="account-card" bordered={false}>
      <Typography.Title level={2}>用户中心</Typography.Title>
      <Typography.Paragraph type="secondary">
        当前登录账号信息
      </Typography.Paragraph>

      {user ? (
        <Descriptions bordered column={1} className="account-details">
          <Descriptions.Item label="用户 ID">{user.userId}</Descriptions.Item>
          <Descriptions.Item label="用户名">{user.username}</Descriptions.Item>
          <Descriptions.Item label="昵称">{user.nickname}</Descriptions.Item>
          <Descriptions.Item label="角色">普通用户</Descriptions.Item>
        </Descriptions>
      ) : (
        <Typography.Text type="secondary">
          当前 Token 中没有完整用户资料，请重新登录。
        </Typography.Text>
      )}
    </Card>
  )
}
