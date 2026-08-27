import { useState } from 'react'
import {
  App as AntdApp,
  Button,
  Card,
  Form,
  Input,
  Space,
  Typography,
} from 'antd'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '@/api'
import { login, type LoginRequest } from '@/api/auth'
import { paths } from '@/router/paths'
import { useAuthStore } from '@/stores/authStore'

const USERNAME_PATTERN = /^[A-Za-z0-9_]+$/

interface RedirectLocation {
  pathname: string
  search?: string
  hash?: string
}

interface LoginLocationState {
  from?: RedirectLocation
}

export function UserLoginPage() {
  const [form] = Form.useForm<LoginRequest>()
  const [submitting, setSubmitting] = useState(false)
  const setSession = useAuthStore((state) => state.setSession)
  const location = useLocation()
  const navigate = useNavigate()
  const { message } = AntdApp.useApp()

  const handleSubmit = async (values: LoginRequest) => {
    setSubmitting(true)

    try {
      const result = await login({
        username: values.username.trim(),
        password: values.password,
      })

      setSession({
        token: result.token,
        user: {
          userId: result.userId,
          username: result.username,
          nickname: result.nickname,
          role: result.role,
        },
      })

      void message.success(`欢迎回来，${result.nickname}`)
      const state = location.state as LoginLocationState | null
      const from = state?.from
      const destination = from
        ? `${from.pathname}${from.search ?? ''}${from.hash ?? ''}`
        : paths.home
      navigate(destination, { replace: true })
    } catch (error) {
      if (error instanceof ApiError && (error.code === 401 || error.code === 403)) {
        form.setFields([
          {
            name: 'password',
            errors: [error.message],
          },
        ])
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="register-page login-page">
      <aside className="register-intro login-intro">
        <Typography.Text className="register-eyebrow">
          WELCOME BACK
        </Typography.Text>
        <Typography.Title>欢迎回到 Hard Store</Typography.Title>
        <Typography.Paragraph>
          登录后可继续访问用户中心，并在后续功能中管理购物车、收货地址和订单。
        </Typography.Paragraph>
        <div className="register-intro-note">
          此入口仅供普通用户使用，管理员请从管理端入口登录。
        </div>
      </aside>

      <Card className="register-card login-card" bordered={false}>
        <Space direction="vertical" size={4} className="register-heading">
          <Typography.Title level={2}>用户登录</Typography.Title>
          <Typography.Text type="secondary">
            使用你的普通用户账号登录
          </Typography.Text>
        </Space>

        <Form
          form={form}
          layout="vertical"
          requiredMark={false}
          autoComplete="on"
          onFinish={handleSubmit}
          disabled={submitting}
        >
          <Form.Item
            label="用户名"
            name="username"
            validateTrigger="onBlur"
            rules={[
              { required: true, whitespace: true, message: '请输入用户名' },
              { min: 4, max: 32, message: '用户名长度必须在 4 到 32 个字符之间' },
              {
                pattern: USERNAME_PATTERN,
                message: '用户名只能包含字母、数字和下划线',
              },
            ]}
          >
            <Input
              size="large"
              maxLength={32}
              placeholder="请输入用户名"
              autoComplete="username"
              autoFocus
            />
          </Form.Item>

          <Form.Item
            label="密码"
            name="password"
            rules={[
              { required: true, message: '请输入密码' },
              { min: 6, max: 64, message: '密码长度必须在 6 到 64 个字符之间' },
            ]}
          >
            <Input.Password
              size="large"
              maxLength={64}
              placeholder="请输入密码"
              autoComplete="current-password"
              onPressEnter={() => form.submit()}
            />
          </Form.Item>

          <Button
            type="primary"
            htmlType="submit"
            size="large"
            block
            loading={submitting}
          >
            登录
          </Button>
        </Form>

        <Typography.Paragraph className="register-login-link" type="secondary">
          还没有账号？<Link to={paths.register}>立即注册</Link>
        </Typography.Paragraph>
      </Card>
    </section>
  )
}
