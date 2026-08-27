import { useEffect, useState } from 'react'
import {
  ApiOutlined,
  CheckCircleFilled,
  CloudServerOutlined,
  CloseCircleFilled,
  DatabaseOutlined,
  DisconnectOutlined,
  HeartOutlined,
  LoadingOutlined,
  QuestionCircleFilled,
  ReloadOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Col,
  Row,
  Space,
  Tag,
  Typography,
} from 'antd'
import type { ReactNode } from 'react'
import type { AdminSession } from '../services/auth'
import {
  getHealthChecks,
  getInitialHealthChecks,
  type HealthCheckKey,
  type HealthCheckResult,
  type HealthStatus,
} from '../services/health'

const { Text, Title } = Typography
const REFRESH_INTERVAL_MS = 30_000

interface StatusMeta {
  label: string
  color: string
  tone: 'healthy' | 'warning' | 'danger' | 'neutral'
  icon: ReactNode
}

const healthIcons: Record<HealthCheckKey, ReactNode> = {
  overall: <HeartOutlined />,
  application: <ApiOutlined />,
  mysql: <DatabaseOutlined />,
  redis: <CloudServerOutlined />,
}

function getStatusMeta(status: HealthStatus): StatusMeta {
  switch (status) {
    case 'UP':
      return {
        label: '运行正常',
        color: 'success',
        tone: 'healthy',
        icon: <CheckCircleFilled />,
      }
    case 'DOWN':
      return {
        label: '服务异常',
        color: 'error',
        tone: 'danger',
        icon: <CloseCircleFilled />,
      }
    case 'OUT_OF_SERVICE':
      return {
        label: '停止服务',
        color: 'warning',
        tone: 'warning',
        icon: <CloseCircleFilled />,
      }
    case 'UNREACHABLE':
      return {
        label: '无法连接',
        color: 'error',
        tone: 'danger',
        icon: <DisconnectOutlined />,
      }
    case 'CHECKING':
      return {
        label: '检查中',
        color: 'processing',
        tone: 'neutral',
        icon: <LoadingOutlined spin />,
      }
    default:
      return {
        label: status === 'UNKNOWN' ? '状态未知' : status,
        color: 'default',
        tone: 'neutral',
        icon: <QuestionCircleFilled />,
      }
  }
}

function formatCheckedAt(value?: string) {
  if (!value) return '等待首次检查'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date)
}

function HealthCard({ check }: { check: HealthCheckResult }) {
  const meta = getStatusMeta(check.status)
  const components = Object.entries(check.components ?? {})

  return (
    <Card className={`health-card health-card--${meta.tone}`} bordered={false}>
      <div className="health-card-header">
        <div className={`health-card-icon health-card-icon--${meta.tone}`}>
          {healthIcons[check.key]}
        </div>
        <Tag color={meta.color} icon={meta.icon}>{meta.label}</Tag>
      </div>

      <div className="health-card-copy">
        <Title level={4}>{check.name}</Title>
        <Text type="secondary">{check.description}</Text>
      </div>

      {components.length > 0 && (
        <div className="health-components">
          {components.map(([name, component]) => (
            <Tag
              key={name}
              color={component.status === 'UP' ? 'success' : 'error'}
            >
              {name} · {component.status}
            </Tag>
          ))}
        </div>
      )}

      {check.error && (
        <Text className="health-card-error" type="danger" ellipsis={{ tooltip: check.error }}>
          {check.error}
        </Text>
      )}

      <div className="health-card-footer">
        <Text type="secondary">{formatCheckedAt(check.checkedAt)}</Text>
        <Text type="secondary">
          {check.responseTimeMs === undefined ? '-- ms' : `${check.responseTimeMs} ms`}
        </Text>
      </div>
    </Card>
  )
}

function DashboardPage({ session }: { session: AdminSession }) {
  const [checks, setChecks] = useState<HealthCheckResult[]>(getInitialHealthChecks)
  const [refreshing, setRefreshing] = useState(true)

  useEffect(() => {
    let cancelled = false

    const loadChecks = async (showRefreshing: boolean) => {
      if (showRefreshing && !cancelled) setRefreshing(true)
      const nextChecks = await getHealthChecks()
      if (cancelled) return
      setChecks(nextChecks)
      setRefreshing(false)
    }

    void loadChecks(false)
    const timer = window.setInterval(() => {
      void loadChecks(true)
    }, REFRESH_INTERVAL_MS)

    return () => {
      cancelled = true
      window.clearInterval(timer)
    }
  }, [])

  const handleRefresh = async () => {
    if (refreshing) return
    setRefreshing(true)
    setChecks(await getHealthChecks())
    setRefreshing(false)
  }

  const pending = checks.some((check) => check.status === 'CHECKING')
  const unhealthyChecks = checks.filter((check) => (
    check.status !== 'UP' && check.status !== 'CHECKING'
  ))
  const allHealthy = !pending && unhealthyChecks.length === 0
  const latestCheckedAt = checks
    .map((check) => check.checkedAt)
    .filter((value): value is string => Boolean(value))
    .sort()
    .at(-1)

  return (
    <main className="dashboard-page">
      <div className="page-heading dashboard-heading">
        <div>
          <Title level={2}>工作台</Title>
          <Text type="secondary">
            欢迎回来，{session.user.nickname || session.user.username}
          </Text>
        </div>
        <Button
          icon={<ReloadOutlined />}
          loading={refreshing}
          onClick={() => void handleRefresh()}
        >
          立即检查
        </Button>
      </div>

      <section
        className={`health-overview ${allHealthy ? 'health-overview--healthy' : 'health-overview--warning'}`}
        aria-live="polite"
      >
        <div className="health-overview-copy">
          <Text className="health-overview-eyebrow">SYSTEM HEALTH</Text>
          <Title level={3}>
            {pending
              ? '正在检查系统运行状态'
              : allHealthy
                ? '所有服务运行正常'
                : `检测到 ${unhealthyChecks.length} 项异常`}
          </Title>
          <Text>
            每 30 秒自动检查一次 · 最近检查 {formatCheckedAt(latestCheckedAt)}
          </Text>
        </div>
        <div className="health-overview-status">
          {pending
            ? <LoadingOutlined spin />
            : allHealthy
              ? <CheckCircleFilled />
              : <CloseCircleFilled />}
        </div>
      </section>

      {unhealthyChecks.length > 0 && (
        <Alert
          className="health-warning-alert"
          type="warning"
          showIcon
          message="部分健康检查未通过"
          description={`请关注：${unhealthyChecks.map((check) => check.name).join('、')}`}
        />
      )}

      <Row gutter={[18, 18]}>
        {checks.map((check) => (
          <Col key={check.key} xs={24} sm={12} xl={6}>
            <HealthCard check={check} />
          </Col>
        ))}
      </Row>

      <div className="health-endpoints">
        <Text type="secondary">监测端点</Text>
        <Space wrap size={[6, 6]}>
          {checks.map((check) => (
            <Tag key={check.key}>/actuator{check.path}</Tag>
          ))}
        </Space>
      </div>
    </main>
  )
}

export default DashboardPage
