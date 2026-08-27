import { useCallback, useEffect, useState } from 'react'
import {
  ArrowRightOutlined,
  DatabaseOutlined,
  FallOutlined,
  ReloadOutlined,
  RiseOutlined,
} from '@ant-design/icons'
import {
  Alert,
  App,
  Button,
  Card,
  Empty,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { TableColumnsType } from 'antd'
import {
  getStockLogPage,
  type StockLog,
} from '../services/stock'

const { Text, Title } = Typography

interface PaginationState {
  page: number
  size: number
  total: number
}

const initialPagination: PaginationState = {
  page: 1,
  size: 10,
  total: 0,
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '库存记录加载失败，请稍后重试'
}

function formatDateTime(value: string) {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date)
}

function StockLogPage() {
  const { message } = App.useApp()
  const [logs, setLogs] = useState<StockLog[]>([])
  const [pagination, setPagination] = useState(initialPagination)
  const [loading, setLoading] = useState(true)

  const loadLogs = useCallback(async (page: number, size: number) => {
    setLoading(true)

    try {
      const data = await getStockLogPage({ page, size })
      setLogs(data.result)
      setPagination({ page: data.page, size: data.size, total: data.total })
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }, [message])

  useEffect(() => {
    let cancelled = false

    getStockLogPage({ page: 1, size: initialPagination.size })
      .then((data) => {
        if (cancelled) return
        setLogs(data.result)
        setPagination({ page: data.page, size: data.size, total: data.total })
      })
      .catch((error: unknown) => {
        if (!cancelled) message.error(getErrorMessage(error))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [message])

  const columns: TableColumnsType<StockLog> = [
    {
      title: '记录 ID',
      dataIndex: 'id',
      width: 100,
    },
    {
      title: '商品',
      key: 'product',
      minWidth: 210,
      render: (_, log) => (
        <div className="stock-log-product">
          <Text strong ellipsis={{ tooltip: log.productName || '已删除商品' }}>
            {log.productName || '已删除商品'}
          </Text>
          <Text type="secondary">商品 ID：{log.productId}</Text>
        </div>
      ),
    },
    {
      title: '变更类型',
      dataIndex: 'changeQuantity',
      width: 115,
      render: (quantity: number) => quantity > 0 ? (
        <Tag color="success" icon={<RiseOutlined />}>增加</Tag>
      ) : (
        <Tag color="error" icon={<FallOutlined />}>减少</Tag>
      ),
    },
    {
      title: '变更数量',
      dataIndex: 'changeQuantity',
      width: 120,
      render: (quantity: number) => (
        <Text strong className={quantity > 0 ? 'stock-change-increase' : 'stock-change-decrease'}>
          {quantity > 0 ? `+${quantity}` : quantity}
        </Text>
      ),
    },
    {
      title: '库存变化',
      key: 'stockChange',
      width: 180,
      render: (_, log) => (
        <Space size={8}>
          <Text>{log.beforeStock}</Text>
          <ArrowRightOutlined className="stock-log-arrow" />
          <Text strong>{log.afterStock}</Text>
        </Space>
      ),
    },
    {
      title: '变更原因',
      dataIndex: 'reason',
      minWidth: 240,
      render: (reason: string) => (
        <Text ellipsis={{ tooltip: reason }}>{reason || '-'}</Text>
      ),
    },
    {
      title: '业务单号',
      dataIndex: 'businessNo',
      width: 170,
      render: (businessNo: string | null) => businessNo || (
        <Text type="secondary">手动调整</Text>
      ),
    },
    {
      title: '变更时间',
      dataIndex: 'createdAt',
      width: 190,
      render: formatDateTime,
    },
  ]

  return (
    <>
      <div className="page-heading stock-log-heading">
        <div>
          <Title level={2}>库存变更记录</Title>
          <Text type="secondary">查看商品库存的每次增加、减少和变更原因</Text>
        </div>
        <Button
          icon={<ReloadOutlined />}
          loading={loading}
          onClick={() => void loadLogs(pagination.page, pagination.size)}
        >
          刷新记录
        </Button>
      </div>

      <Alert
        className="stock-log-alert"
        type="info"
        showIcon
        icon={<DatabaseOutlined />}
        message="库存审计记录"
        description="记录按变更时间倒序排列；没有业务单号的记录来自管理员手动调整。"
      />

      <Card className="stock-log-table-card">
        <Table<StockLog>
          rowKey="id"
          columns={columns}
          dataSource={logs}
          loading={loading}
          scroll={{ x: 1250 }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无库存变更记录"
              />
            ),
          }}
          pagination={{
            current: pagination.page,
            pageSize: pagination.size,
            total: pagination.total,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条变更记录`,
            pageSizeOptions: [10, 20, 50, 100],
            onChange: (page, size) => {
              void loadLogs(page, size)
            },
          }}
        />
      </Card>
    </>
  )
}

export default StockLogPage
