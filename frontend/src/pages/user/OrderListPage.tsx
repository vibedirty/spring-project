import { useEffect, useState } from 'react'
import {
  Alert,
  App as AntdApp,
  Button,
  Card,
  Empty,
  Modal,
  Pagination,
  Popconfirm,
  Row,
  Skeleton,
  Space,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import {
  CheckCircleOutlined,
  CreditCardOutlined,
  ReloadOutlined,
  ShoppingOutlined,
} from '@ant-design/icons'
import { Link } from 'react-router-dom'
import {
  cancelOrder,
  confirmReceipt,
  getOrderList,
  payOrder,
  type OrderListResponse,
  type OrderStatus,
} from '@/api/order'
import { OrderCountdown } from '@/components/common/OrderCountdown'
import { orderDetailPath, paths, productDetailPath } from '@/router/paths'

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
    case 'PENDING_PAYMENT':
      return <Tag color="gold">{desc || '待付款'}</Tag>
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

const TAB_ITEMS = [
  { key: 'ALL', label: '全部订单' },
  { key: 'PENDING_PAYMENT', label: '待付款' },
  { key: 'PENDING_SHIPMENT', label: '待发货' },
  { key: 'SHIPPED', label: '已发货' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' },
]

export function OrderListPage() {
  const [orders, setOrders] = useState<OrderListResponse[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(10)
  const [statusTab, setStatusTab] = useState<string>('ALL')
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [cancellingOrderNo, setCancellingOrderNo] = useState<string | null>(null)
  const [payingOrderNo, setPayingOrderNo] = useState<string | null>(null)
  const [confirmingOrderNo, setConfirmingOrderNo] = useState<string | null>(null)
  const [payModalOrder, setPayModalOrder] = useState<OrderListResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const { message } = AntdApp.useApp()

  const fetchOrders = async (
    targetPage: number,
    targetSize: number,
    targetStatus: string,
    isManualRefresh = false,
  ) => {
    if (isManualRefresh) {
      setRefreshing(true)
    } else {
      setLoading(true)
    }
    setError(null)

    const queryStatus =
      targetStatus === 'ALL' ? undefined : (targetStatus as OrderStatus)

    try {
      const data = await getOrderList({
        page: targetPage,
        size: targetSize,
        status: queryStatus,
      })
      setOrders(data.result || [])
      setTotal(data.total || 0)
    } catch {
      setError('获取订单列表失败，请稍后重试')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    const controller = new AbortController()
    const queryStatus =
      statusTab === 'ALL' ? undefined : (statusTab as OrderStatus)

    getOrderList(
      {
        page,
        size,
        status: queryStatus,
      },
      controller.signal,
    )
      .then((data) => {
        setOrders(data.result || [])
        setTotal(data.total || 0)
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setError('获取订单列表失败，请稍后重试')
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
  }, [page, size, statusTab])

  const handleTabChange = (key: string) => {
    setStatusTab(key)
    setPage(1)
  }

  const handlePaginationChange = (newPage: number, newSize: number) => {
    setPage(newPage)
    setSize(newSize)
  }

  const handleCancelOrder = async (orderNo: string) => {
    setCancellingOrderNo(orderNo)
    try {
      await cancelOrder(orderNo)
      void message.success('订单已成功取消')
      await fetchOrders(page, size, statusTab)
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setCancellingOrderNo(null)
    }
  }

  const handleConfirmPay = async () => {
    if (!payModalOrder) return
    const orderNo = payModalOrder.orderNo
    setPayingOrderNo(orderNo)
    try {
      await payOrder(orderNo)
      void message.success(`订单 ${orderNo} 模拟支付成功！已转为待发货`)
      setPayModalOrder(null)
      await fetchOrders(page, size, statusTab)
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setPayingOrderNo(null)
    }
  }

  const handleConfirmReceipt = async (orderNo: string) => {
    setConfirmingOrderNo(orderNo)
    try {
      await confirmReceipt(orderNo)
      void message.success(`订单 ${orderNo} 已确认收货，交易完成`)
      await fetchOrders(page, size, statusTab)
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setConfirmingOrderNo(null)
    }
  }

  return (
    <div className="order-list-page">
      <div className="order-page-header">
        <div>
          <Typography.Title level={2} className="order-page-title">
            我的订单
          </Typography.Title>
          <Typography.Paragraph type="secondary" className="order-page-desc">
            查看全部历史订单明细与履约状态
          </Typography.Paragraph>
        </div>
        <Button
          icon={<ReloadOutlined />}
          onClick={() => void fetchOrders(page, size, statusTab, true)}
          loading={refreshing}
        >
          刷新订单
        </Button>
      </div>

      <Card className="order-tabs-card" bordered={false}>
        <Tabs
          activeKey={statusTab}
          items={TAB_ITEMS}
          onChange={handleTabChange}
          className="order-status-tabs"
        />

        {error && (
          <Alert
            type="error"
            showIcon
            message={error}
            action={
              <Button
                size="small"
                onClick={() => void fetchOrders(page, size, statusTab, true)}
              >
                重试
              </Button>
            }
            className="order-error-alert"
          />
        )}

        {loading ? (
          <div className="order-loading-container">
            <Skeleton active paragraph={{ rows: 6 }} />
          </div>
        ) : orders.length === 0 ? (
          <div className="order-empty-container">
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="暂无相关订单"
            >
              <Button
                type="primary"
                icon={<ShoppingOutlined />}
                href={paths.home}
              >
                去挑选好物
              </Button>
            </Empty>
          </div>
        ) : (
          <div className="order-cards-list">
            {orders.map((order) => {
              const totalItemsCount = order.items.reduce(
                (sum, item) => sum + (item.quantity || 0),
                0,
              )
              const isPendingPayment = order.status === 'PENDING_PAYMENT'
              const isShipped = order.status === 'SHIPPED'
              const isCancellingThis = cancellingOrderNo === order.orderNo
              const isPayingThis = payingOrderNo === order.orderNo
              const isConfirmingThis = confirmingOrderNo === order.orderNo

              return (
                <Card
                  key={order.orderNo}
                  className="order-item-card"
                  bordered={false}
                >
                  <div className="order-card-header">
                    <Space size={16} wrap className="order-meta-info">
                      <Typography.Text type="secondary" className="order-time">
                        下单时间: {formatDateTime(order.createdAt)}
                      </Typography.Text>
                      <Typography.Text className="order-no">
                        订单编号: <Link to={orderDetailPath(order.orderNo)}>{order.orderNo}</Link>
                      </Typography.Text>
                    </Space>

                    <div className="order-status-wrapper">
                      {renderStatusTag(order.status, order.statusDescription)}
                    </div>
                  </div>

                  {isPendingPayment && order.expireAt && (
                    <div className="order-expire-notice">
                      <OrderCountdown
                        expireAt={order.expireAt}
                        onFinish={() => {
                          void fetchOrders(page, size, statusTab)
                        }}
                      />
                    </div>
                  )}

                  <div className="order-goods-list">
                    {order.items.map((goods) => (
                      <div key={goods.productId} className="order-goods-row">
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

                  <div className="order-card-footer">
                    <div className="order-footer-summary">
                      <Typography.Text type="secondary">
                        共 {totalItemsCount} 件商品
                      </Typography.Text>
                      <Typography.Text className="order-total-amount">
                        实付总额: <span>¥{formatPrice(order.totalAmount)}</span>
                      </Typography.Text>
                    </div>

                    <div className="order-card-actions">
                      <Space size={8}>
                        {isPendingPayment && (
                          <>
                            <Button
                              type="primary"
                              size="small"
                              icon={<CreditCardOutlined />}
                              onClick={() => setPayModalOrder(order)}
                              disabled={
                                Boolean(cancellingOrderNo) ||
                                Boolean(payingOrderNo) ||
                                Boolean(confirmingOrderNo)
                              }
                              className="order-pay-btn"
                            >
                              立即支付
                            </Button>

                            <Popconfirm
                              title="取消订单"
                              description="确定要取消此待付款订单吗？取消后库存将自动释放。"
                              okText="确定取消"
                              cancelText="暂不取消"
                              okButtonProps={{ danger: true }}
                              onConfirm={() => void handleCancelOrder(order.orderNo)}
                              disabled={
                                Boolean(cancellingOrderNo) ||
                                Boolean(payingOrderNo) ||
                                Boolean(confirmingOrderNo)
                              }
                            >
                              <Button
                                danger
                                size="small"
                                loading={isCancellingThis}
                                disabled={
                                  (Boolean(cancellingOrderNo) && !isCancellingThis) ||
                                  isPayingThis
                                }
                                className="order-cancel-btn"
                              >
                                取消订单
                              </Button>
                            </Popconfirm>
                          </>
                        )}
                        {isShipped && (
                          <Popconfirm
                            title="确认收货"
                            description="请确认已收到商品。确认后订单将完成，且无法撤销此操作。"
                            okText="确认已收货"
                            cancelText="暂不确认"
                            onConfirm={() => void handleConfirmReceipt(order.orderNo)}
                            disabled={Boolean(confirmingOrderNo)}
                          >
                            <Button
                              type="primary"
                              size="small"
                              icon={<CheckCircleOutlined />}
                              loading={isConfirmingThis}
                              disabled={
                                Boolean(confirmingOrderNo) && !isConfirmingThis
                              }
                              className="order-confirm-receipt-btn"
                            >
                              确认收货
                            </Button>
                          </Popconfirm>
                        )}
                        <Button
                          type="default"
                          size="small"
                          href={orderDetailPath(order.orderNo)}
                          className="order-detail-btn"
                        >
                          查看详情
                        </Button>
                      </Space>
                    </div>
                  </div>
                </Card>
              )
            })}

            {total > 0 && (
              <Row justify="end" className="order-pagination-row">
                <Pagination
                  current={page}
                  pageSize={size}
                  total={total}
                  showTotal={(t) => `共 ${t} 条订单`}
                  showSizeChanger
                  pageSizeOptions={['5', '10', '20']}
                  onChange={handlePaginationChange}
                />
              </Row>
            )}
          </div>
        )}
      </Card>

      {/* 模拟支付确认弹窗 */}
      <Modal
        title="模拟支付确认"
        open={Boolean(payModalOrder)}
        onCancel={() => !payingOrderNo && setPayModalOrder(null)}
        footer={[
          <Button
            key="cancel"
            onClick={() => setPayModalOrder(null)}
            disabled={Boolean(payingOrderNo)}
          >
            暂不支付
          </Button>,
          <Button
            key="submit"
            type="primary"
            loading={Boolean(payingOrderNo)}
            onClick={() => void handleConfirmPay()}
          >
            确认支付 (¥{formatPrice(payModalOrder?.totalAmount)})
          </Button>,
        ]}
        destroyOnClose
        centered
        width={480}
      >
        <div className="order-pay-modal-content">
          <Typography.Paragraph type="secondary">
            订单编号：<strong>{payModalOrder?.orderNo}</strong>
          </Typography.Paragraph>
          <div className="order-pay-modal-amount-box">
            <Typography.Text type="secondary">应付金额</Typography.Text>
            <div className="order-pay-modal-amount">
              <small>¥</small>{formatPrice(payModalOrder?.totalAmount)}
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
