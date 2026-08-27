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
import { Link, useNavigate } from 'react-router-dom'
import { register, type RegisterRequest } from '@/api/auth'
import { ApiError } from '@/api'
import { paths } from '@/router/paths'
import { useAuthStore } from '@/stores/authStore'

interface RegisterFormValues extends RegisterRequest {
  confirmPassword: string
}

const USERNAME_PATTERN = /^[A-Za-z0-9_]+$/

export function RegisterPage() {
  const [form] = Form.useForm<RegisterFormValues>()
  const [submitting, setSubmitting] = useState(false)
  const setSession = useAuthStore((state) => state.setSession)
  const navigate = useNavigate()
  const { message } = AntdApp.useApp()

  const handleSubmit = async (values: RegisterFormValues) => {
    setSubmitting(true)

    try {
      const result = await register({
        username: values.username.trim(),
        password: values.password,
        nickname: values.nickname.trim(),
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
      void message.success(`注册成功，欢迎你，${result.nickname}`)
      navigate(paths.account, { replace: true })
    } catch (error) {
      if (error instanceof ApiError && error.code === 409) {
        form.setFields([
          {
            name: 'username',
            errors: [error.message],
          },
        ])
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="register-page">
      <aside className="register-intro">
        <Typography.Text className="register-eyebrow">
          CREATE YOUR ACCOUNT
        </Typography.Text>
        <Typography.Title>开始你的购物之旅</Typography.Title>
        <Typography.Paragraph>
          创建普通用户账号后会自动登录，随后即可使用用户中心、购物车和订单等功能。
        </Typography.Paragraph>
        <div className="register-intro-note">
          当前开放普通用户注册，管理员账号由系统初始化。
        </div>
      </aside>

      <Card className="register-card" bordered={false}>
        <Space direction="vertical" size={4} className="register-heading">
          <Typography.Title level={2}>创建账号</Typography.Title>
          <Typography.Text type="secondary">
            请填写以下信息完成注册
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
              placeholder="4–32 位字母、数字或下划线"
              autoComplete="username"
            />
          </Form.Item>

          <Form.Item
            label="昵称"
            name="nickname"
            validateTrigger="onBlur"
            rules={[
              { required: true, whitespace: true, message: '请输入昵称' },
              { max: 32, message: '昵称长度不能超过 32 个字符' },
            ]}
          >
            <Input
              size="large"
              maxLength={32}
              placeholder="请输入昵称"
              autoComplete="nickname"
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
              placeholder="请输入 6–64 位密码"
              autoComplete="new-password"
            />
          </Form.Item>

          <Form.Item
            label="确认密码"
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: '请再次输入密码' },
              ({ getFieldValue }) => ({
                validator(_, value: string | undefined) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'))
                },
              }),
            ]}
          >
            <Input.Password
              size="large"
              maxLength={64}
              placeholder="请再次输入密码"
              autoComplete="new-password"
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
            注册并登录
          </Button>
        </Form>

        <Typography.Paragraph className="register-login-link" type="secondary">
          已有账号？<Link to={paths.login}>前往登录</Link>
        </Typography.Paragraph>
      </Card>
    </section>
  )
}
