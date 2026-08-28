import { useEffect, useState } from 'react'
import {
  Alert,
  App as AntdApp,
  Breadcrumb,
  Button,
  Card,
  Col,
  Modal,
  Popconfirm,
  Result,
  Row,
  Skeleton,
  Space,
  Steps,
  Tag,
  Timeline,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  CopyOutlined,
  CreditCardOutlined,
  EnvironmentOutlined,
  HistoryOutlined,
  ReloadOutlined,
  ShoppingOutlined,
  TruckOutlined,
} from '@ant-design/icons'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '@/api/types'
import {
  cancelOrder,
  confirmReceipt,
  getOrderDetail,
  payOrder,
  type OrderDetailResponse,
  type OrderStatus,
} from '@/api/order'
import { OrderCountdown } from '@/components/common/OrderCountdown'
import { paths, productDetailPath } from '@/router/paths'

function formatPrice(price: number | null | undefined): string {
  const value = Number(price)
  return Number.isFinite(value) ? value.toFixed(2) : '0.00'
}

function formatDateTime(dateStr: string | null | undefined): string {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').slice(0, 19)
}

function renderStatusTag(status: OrderStatus | string, desc?: string) {
  switch (status) {
    case 'PENDING_STOCK':
      return <Tag color="orange">{desc || '库存处理中'}</Tag>
    case 'PENDING_PAYMENT':
      return <Tag color="gold">{desc || '待付款'}</Tag>
    case 'CANCELLING':
      return <Tag color="default">{desc || '取消处理中'}</Tag>
    case 'PENDING_SHIPMENT':
      return <Tag color="blue">{desc || '待发货'}</Tag>
    case 'SHIPPED':
      return <Tag color="cyan">{desc || '已发货'}</Tag>
    case 'COMPLETED':
      return <Tag color="green">{desc || '已完成'}</Tag>
    case 'CANCELLED':
      return <Tag color="default">{desc || '已取消'}</Tag>
    default:
      return <Tag>{desc || status}</Tag>
  }
}

function getStepCurrent(status: OrderStatus): number {
  switch (status) {
    case 'PENDING_PAYMENT':
      return 0
    case 'PENDING_SHIPMENT':
      return 1
    case 'SHIPPED':
      return 2
    case 'COMPLETED':
      return 3
    default:
      return 0
  }
}

export function OrderDetailPage() {
  const { orderNo } = useParams<{ orderNo: string }>()
  const [order, setOrder] = useState<OrderDetailResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [paying, setPaying] = useState(false)
  const [confirmingReceipt, setConfirmingReceipt] = useState(false)
  const [payModalOpen, setPayModalOpen] = useState(false)
  const [isExpired, setIsExpired] = useState(false)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const { message } = AntdApp.useApp()

  const fetchDetail = async (isManual = false) => {
    if (!orderNo) return
    if (isManual) {
      setRefreshing(true)
    } else {
      setLoading(true)
    }
    setError(null)
    setNotFound(false)

    try {
      const data = await getOrderDetail(orderNo)
      setOrder(data)
    } catch (err) {
      if (err instanceof ApiError && (err.code === 404 || err.code === 400)) {
        setNotFound(true)
      } else {
        setError('获取订单详情失败，请检查网络后重试')
      }
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    if (!orderNo) return
    const controller = new AbortController()

    getOrderDetail(orderNo, controller.signal)
      .then((data) => {
        setOrder(data)
      })
      .catch((err: unknown) => {
        if (!controller.signal.aborted) {
          if (err instanceof ApiError && (err.code === 404 || err.code === 400)) {
            setNotFound(true)
          } else {
            setError('获取订单详情失败，请稍后重试')
          }
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      })

    return () => {
      controller.abort()
    }
  }, [orderNo])

  const handleCancelOrder = async () => {
    if (!orderNo) return
    setCancelling(true)
    try {
      await cancelOrder(orderNo)
      void message.success('订单已成功取消')
      await fetchDetail()
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setCancelling(false)
    }
  }

  const handleConfirmPay = async () => {
    if (!orderNo) return
    setPaying(true)
    try {
      await payOrder(orderNo)
      void message.success('模拟支付成功！订单状态已更新为待发货')
      setPayModalOpen(false)
      await fetchDetail()
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setPaying(false)
    }
  }

  const handleConfirmReceipt = async () => {
    if (!orderNo) return
    setConfirmingReceipt(true)
    try {
      await confirmReceipt(orderNo)
      void message.success('确认收货成功，订单已完成')
      await fetchDetail()
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setConfirmingReceipt(false)
    }
  }

  const handleCopyTrackingNumber = (text: string) => {
    if (!text) return
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text).then(
        () => {
          void message.success('运单号已复制到剪贴板')
        },
        () => {
          void message.info(`运单号: ${text}`)
        },
      )
    } else {
      void message.info(`运单号: ${text}`)
    }
  }

  if (loading) {
    return (
      <div className="order-detail-page">
        <Breadcrumb
          items={[
            { title: <Link to={paths.home}>首页</Link> },
            { title: <Link to={paths.orders}>我的订单</Link> },
            { title: '订单详情' },
          ]}
        />
        <Card className="order-detail-card" bordered={false}>
          <Skeleton active paragraph={{ rows: 10 }} />
        </Card>
      </div>
    )
  }

  if (notFound || !order) {
    return (
      <div className="order-detail-page">
        <Breadcrumb
          items={[
            { title: <Link to={paths.home}>首页</Link> },
            { title: <Link to={paths.orders}>我的订单</Link> },
            { title: '订单详情' },
          ]}
        />
        <Card className="order-detail-card" bordered={false}>
          <Result
            status="404"
            title="订单不存在"
            subTitle="未找到该订单信息，可能已被取消或无权访问。"
            extra={
              <Button type="primary" href={paths.orders}>
                返回我的订单
              </Button>
            }
          />
        </Card>
      </div>
    )
  }

  const isCancelled = order.status === 'CANCELLED'
  const isPendingPayment = order.status === 'PENDING_PAYMENT'
  const isShipped = order.status === 'SHIPPED'
  const totalItemCount = order.items.reduce((sum, item) => sum + (item.quantity || 0), 0)

  return (
    <div className="order-detail-page">
      <div className="order-detail-top-nav">
        <Breadcrumb
          items={[
            { title: <Link to={paths.home}>首页</Link> },
            { title: <Link to={paths.orders}>我的订单</Link> },
            { title: `订单详情 (${order.orderNo})` },
          ]}
        />
        <Space size={12}>
          {isShipped && (
            <Popconfirm
              title="确认收货"
              description="请确认已收到商品。确认后订单将完成，且无法撤销此操作。"
              okText="确认已收货"
              cancelText="暂不确认"
              onConfirm={() => void handleConfirmReceipt()}
              disabled={confirmingReceipt}
            >
              <Button
                type="primary"
                size="small"
                icon={<CheckCircleOutlined />}
                loading={confirmingReceipt}
              >
                确认收货
              </Button>
            </Popconfirm>
          )}
          {isPendingPayment && (
            <Popconfirm
              title="取消订单"
              description="确定要取消此待付款订单吗？取消后库存将自动释放。"
              okText="确定取消"
              cancelText="暂不取消"
              okButtonProps={{ danger: true }}
              onConfirm={() => void handleCancelOrder()}
              disabled={cancelling || paying || confirmingReceipt}
            >
              <Button danger size="small" loading={cancelling}>
                取消订单
              </Button>
            </Popconfirm>
          )}
          <Button
            icon={<ReloadOutlined />}
            onClick={() => void fetchDetail(true)}
            loading={refreshing}
            disabled={confirmingReceipt}
            size="small"
          >
            刷新
          </Button>
        </Space>
      </div>

      {error && (
        <Alert
          type="error"
          showIcon
          message={error}
          action={
            <Button size="small" onClick={() => void fetchDetail(true)}>
              重试
            </Button>
          }
          className="order-detail-alert"
        />
      )}

      {/* 1. 顶部状态横幅与步骤条 */}
      <Card className="order-status-banner-card" bordered={false}>
        <div className="order-status-banner-header">
          <div className="order-status-main-info">
            <Typography.Title level={3} className="order-status-headline">
              {order.statusDescription || order.status}
            </Typography.Title>
            <div className="order-status-sub-desc">
              <Space size={16} wrap>
                <Typography.Text type="secondary">
                  订单编号: <strong>{order.orderNo}</strong>
                </Typography.Text>
                <Typography.Text type="secondary">
                  下单时间: {formatDateTime(order.createdAt)}
                </Typography.Text>
                {renderStatusTag(order.status, order.statusDescription)}
              </Space>
            </div>
          </div>
        </div>

        {isPendingPayment && order.expireAt && (
          <div className="order-detail-expire-banner">
            <OrderCountdown
              expireAt={order.expireAt}
              onFinish={() => {
                setIsExpired(true)
                void message.warning('订单支付已超时，已自动关闭')
                void fetchDetail()
              }}
            />
          </div>
        )}

        {isCancelled && (
          <div className="order-detail-cancelled-banner">
            <CloseCircleOutlined />
            <span>该订单已取消或关闭。取消时间：{formatDateTime(order.cancelledAt)}</span>
          </div>
        )}

        {!isCancelled && (
          <div className="order-steps-container">
            <Steps
              current={getStepCurrent(order.status)}
              items={[
                {
                  title: '提交订单',
                  description: formatDateTime(order.createdAt),
                  icon: <ShoppingOutlined />,
                },
                {
                  title: '买家付款',
                  description: order.paidAt ? formatDateTime(order.paidAt) : '待支付',
                  icon: <CheckCircleOutlined />,
                },
                {
                  title: '商家发货',
                  description: order.shippedAt
                    ? `${formatDateTime(order.shippedAt)}${
                        order.shippingCompany ? ` (${order.shippingCompany})` : ''
                      }`
                    : '待发货',
                  icon: <TruckOutlined />,
                },
                {
                  title: '交易完成',
                  description: order.completedAt ? formatDateTime(order.completedAt) : '待完成',
                  icon: <CheckCircleOutlined />,
                },
              ]}
            />
          </div>
        )}
      </Card>

      <Row gutter={[20, 20]} className="order-detail-main-row">
        <Col xs={24} lg={16}>
          <div className="order-detail-left-col">
            {/* 2. 物流配送信息 */}
            {(order.shippingCompany ||
              order.trackingNumber ||
              order.status === 'SHIPPED' ||
              order.status === 'COMPLETED' ||
              order.status === 'PENDING_SHIPMENT') && (
              <Card className="order-section-card order-delivery-card" bordered={false}>
                <Typography.Title level={5} className="order-section-title">
                  <TruckOutlined /> 物流配送信息
                </Typography.Title>
                <div className="order-delivery-content">
                  {order.shippingCompany || order.trackingNumber ? (
                    <div className="order-delivery-info-grid">
                      <div className="order-delivery-field">
                        <span className="order-delivery-label">物流公司：</span>
                        <span className="order-delivery-value highlight-company">
                          {order.shippingCompany || '标准快递'}
                        </span>
                      </div>
                      <div className="order-delivery-field">
                        <span className="order-delivery-label">快递单号：</span>
                        <span className="order-delivery-value tracking-num">
                          {order.trackingNumber || '-'}
                        </span>
                        {order.trackingNumber && (
                          <Button
                            type="link"
                            size="small"
                            icon={<CopyOutlined />}
                            onClick={() => handleCopyTrackingNumber(order.trackingNumber || '')}
                            className="order-copy-tracking-btn"
                          >
                            复制
                          </Button>
                        )}
                      </div>
                      {order.shippedAt && (
                        <div className="order-delivery-field">
                          <span className="order-delivery-label">发货时间：</span>
                          <span className="order-delivery-value">
                            {formatDateTime(order.shippedAt)}
                          </span>
                        </div>
                      )}
                    </div>
                  ) : order.status === 'PENDING_SHIPMENT' ? (
                    <div className="order-delivery-pending-tip">
                      <Typography.Text type="secondary">
                        买家已付款，商家正在拣货并安排物流揽件，请耐心等待发货。
                      </Typography.Text>
                    </div>
                  ) : (
                    <div className="order-delivery-pending-tip">
                      <Typography.Text type="secondary">暂无物流运单信息</Typography.Text>
                    </div>
                  )}
                </div>
              </Card>
            )}

            {/* 3. 收货地址快照 */}
            {order.address && (
              <Card className="order-section-card" bordered={false}>
                <Typography.Title level={5} className="order-section-title">
                  <EnvironmentOutlined /> 收货人信息
                </Typography.Title>
                <div className="order-detail-address-content">
                  <div className="order-detail-addr-receiver">
                    <span className="order-addr-name">{order.address.receiverName}</span>
                    <span className="order-addr-phone">{order.address.phone}</span>
                  </div>
                  <div className="order-detail-addr-full">
                    {order.address.province} {order.address.city} {order.address.district} {order.address.detailAddress}
                  </div>
                </div>
              </Card>
            )}

            {/* 4. 商品清单 */}
            <Card className="order-section-card" bordered={false}>
              <Typography.Title level={5} className="order-section-title">
                <ShoppingOutlined /> 商品明细 ({totalItemCount}件)
              </Typography.Title>

              <div className="order-detail-goods-list">
                {order.items.map((goods) => (
                  <div key={goods.productId} className="order-detail-goods-item">
                    <div className="order-goods-media">
                      {goods.productImageUrl ? (
                        <img
                          src={goods.productImageUrl}
                          alt={goods.productName || '商品图片'}
                          className="order-goods-image"
                        />
                      ) : (
                        <div className="order-goods-placeholder">暂无图片</div>
                      )}
                    </div>

                    <div className="order-goods-info">
                      <Typography.Text strong className="order-goods-name">
                        <Link to={productDetailPath(goods.productId)}>
                          {goods.productName || `商品 ID: ${goods.productId}`}
                        </Link>
                      </Typography.Text>
                      <div className="order-goods-meta">
                        <Typography.Text type="secondary">
                          单价: ¥{formatPrice(goods.unitPrice)}
                        </Typography.Text>
                        <Typography.Text type="secondary">
                          数量: × {goods.quantity}
                        </Typography.Text>
                      </div>
                    </div>

                    <div className="order-goods-price-col">
                      <Typography.Text className="order-goods-subtotal">
                        小计: <strong>¥{formatPrice(goods.subtotalAmount)}</strong>
                      </Typography.Text>
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          </div>
        </Col>

        <Col xs={24} lg={8}>
          <div className="order-detail-right-col">
            {/* 4. 费用明细与实付总计 */}
            <Card className="order-section-card order-fee-card" bordered={false}>
              <Typography.Title level={5} className="order-section-title">
                费用明细
              </Typography.Title>

              <div className="order-fee-rows">
                <div className="order-fee-row">
                  <Typography.Text type="secondary">商品总额</Typography.Text>
                  <Typography.Text strong>¥{formatPrice(order.totalAmount)}</Typography.Text>
                </div>
                <div className="order-fee-row">
                  <Typography.Text type="secondary">运费</Typography.Text>
                  <Typography.Text strong className="order-freight-free">包邮 (¥0.00)</Typography.Text>
                </div>
                <div className="order-fee-divider" />
                <div className="order-fee-row order-fee-total-row">
                  <Typography.Text strong className="order-fee-total-label">
                    实付金额
                  </Typography.Text>
                  <Typography.Text className="order-fee-total-amount">
                    <small>¥</small>{formatPrice(order.totalAmount)}
                  </Typography.Text>
                </div>

                {isPendingPayment && (
                  <div className="order-fee-pay-action">
                    <Button
                      type="primary"
                      size="large"
                      block
                      icon={<CreditCardOutlined />}
                      onClick={() => setPayModalOpen(true)}
                      disabled={cancelling || paying || isExpired}
                      className="order-detail-pay-now-btn"
                    >
                      {isExpired ? '订单已超时关闭' : '立即支付'}
                    </Button>
                  </div>
                )}
              </div>
            </Card>

            {/* 5. 订单流转操作记录 */}
            {order.operateLogs && order.operateLogs.length > 0 && (
              <Card className="order-section-card order-log-card" bordered={false}>
                <Typography.Title level={5} className="order-section-title">
                  <HistoryOutlined /> 订单流转日志
                </Typography.Title>

                <Timeline
                  className="order-logs-timeline"
                  items={order.operateLogs.map((log) => ({
                    color: log.operation === 'CANCEL' ? 'gray' : log.operation === 'PAY' ? 'green' : 'blue',
                    children: (
                      <div className="order-log-item">
                        <div className="order-log-header">
                          <span className="order-log-operator">
                            {log.operatorName || log.operatorType}
                          </span>
                          <span className="order-log-action">
                            {log.reason || log.operation}
                          </span>
                        </div>
                        <div className="order-log-time">
                          {formatDateTime(log.createdAt)}
                        </div>
                      </div>
                    ),
                  }))}
                />
              </Card>
            )}
          </div>
        </Col>
      </Row>

      {/* 模拟支付确认弹窗 */}
      <Modal
        title="模拟支付确认"
        open={payModalOpen}
        onCancel={() => !paying && setPayModalOpen(false)}
        footer={[
          <Button
            key="cancel"
            onClick={() => setPayModalOpen(false)}
            disabled={paying}
          >
            暂不支付
          </Button>,
          <Button
            key="submit"
            type="primary"
            loading={paying}
            onClick={() => void handleConfirmPay()}
          >
            确认支付 (¥{formatPrice(order.totalAmount)})
          </Button>,
        ]}
        destroyOnClose
        centered
        width={480}
      >
        <div className="order-pay-modal-content">
          <Typography.Paragraph type="secondary">
            订单编号：<strong>{order.orderNo}</strong>
          </Typography.Paragraph>
          <div className="order-pay-modal-amount-box">
            <Typography.Text type="secondary">应付金额</Typography.Text>
            <div className="order-pay-modal-amount">
              <small>¥</small>{formatPrice(order.totalAmount)}
            </div>
          </div>
          <Typography.Paragraph type="secondary" className="order-pay-modal-tips">
            * 学习级电商系统演示环境：点击确认支付将调用后端模拟支付接口，自动扣款并变更订单为「待发货」状态。
          </Typography.Paragraph>
        </div>
      </Modal>
    </div>
  )
}
