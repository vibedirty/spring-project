import { useCallback, useEffect, useState } from 'react'
import {
  CarOutlined,
  ClockCircleOutlined,
  EnvironmentOutlined,
  EyeOutlined,
  HistoryOutlined,
  ReloadOutlined,
  SearchOutlined,
  SendOutlined,
  ShoppingOutlined,
} from '@ant-design/icons'
import {
  Alert,
  App,
  AutoComplete,
  Button,
  Card,
  DatePicker,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Image,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Timeline,
  Typography,
} from 'antd'
import type { TableColumnsType } from 'antd'
import {
  getOrderDetail,
  getOrderPage,
  shipOrder,
  type OrderAddress,
  type OrderDetail,
  type OrderItemSummary,
  type OrderListItem,
  type OrderOperateLog,
  type OrderShipmentInput,
  type OrderStatus,
} from '../services/order'

const { RangePicker } = DatePicker
const { Text, Title } = Typography

interface OrderFilters {
  orderNo?: string
  userId?: number
  status?: OrderStatus
  dateRange?: [string, string]
}

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

const shippingCompanyPrefixes: Record<string, string> = {
  顺丰速运: 'SF',
  中通快递: 'ZTO',
  圆通速递: 'YTO',
  韵达快递: 'YD',
  申通快递: 'STO',
  极兔速递: 'JT',
  邮政EMS: 'EMS',
  京东快递: 'JD',
  德邦快递: 'DB',
}

const commonShippingCompanies = Object.keys(shippingCompanyPrefixes).map(
  (value) => ({ value }),
)

const imageFallback =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='112' height='112'%3E%3Crect width='112' height='112' fill='%23f3f4f6'/%3E%3Cpath d='M32 36h48v40H32z' fill='none' stroke='%239ca3af' stroke-width='4'/%3E%3Ccircle cx='45' cy='48' r='5' fill='%239ca3af'/%3E%3Cpath d='m35 69 13-13 10 10 7-7 12 10' fill='none' stroke='%239ca3af' stroke-width='4'/%3E%3C/svg%3E"

const orderStatusMeta: Record<
  OrderStatus,
  { label: string; color: string }
> = {
  PENDING_PAYMENT: { label: '待付款', color: 'warning' },
  PENDING_SHIPMENT: { label: '待发货', color: 'processing' },
  SHIPPED: { label: '已发货', color: 'blue' },
  COMPLETED: { label: '已完成', color: 'success' },
  CANCELLED: { label: '已取消', color: 'default' },
}

const operationNameMap: Record<string, string> = {
  CREATE: '创建订单',
  PAY: '支付订单',
  SHIP: '订单发货',
  COMPLETE: '确认收货',
  CANCEL: '取消订单',
}

const operatorTypeMap: Record<string, string> = {
  USER: '用户',
  ADMIN: '管理员',
  SYSTEM: '系统',
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

function formatDateTime(value?: string | null) {
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

function formatCurrency(amount?: number | null) {
  if (amount == null) return '¥0.00'
  return `¥${Number(amount).toFixed(2)}`
}

function formatFullAddress(address?: OrderAddress | null) {
  if (!address) return '-'
  return `${address.province} ${address.city} ${address.district} ${address.detailAddress}`
}

function generateTrackingNumber(shippingCompany: string) {
  const prefix = shippingCompanyPrefixes[shippingCompany.trim()]
  if (!prefix) return undefined

  const digits = new Uint32Array(10)
  crypto.getRandomValues(digits)
  const suffix = Array.from(digits, (value) => value % 10).join('')
  return `${prefix}${suffix}`
}

function OrderManagePage() {
  const { message } = App.useApp()
  const [form] = Form.useForm<OrderFilters>()
  const [shipForm] = Form.useForm<OrderShipmentInput>()
  const [orders, setOrders] = useState<OrderListItem[]>([])
  const [pagination, setPagination] = useState<PaginationState>(initialPagination)
  const [loading, setLoading] = useState(true)
  const [activeFilters, setActiveFilters] = useState<OrderFilters>({})

  // 详情抽屉状态
  const [drawerVisible, setDrawerVisible] = useState(false)
  const [selectedOrderNo, setSelectedOrderNo] = useState<string | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [orderDetail, setOrderDetail] = useState<OrderDetail | null>(null)

  // 发货弹窗状态
  const [shipModalVisible, setShipModalVisible] = useState(false)
  const [shippingOrderNo, setShippingOrderNo] = useState<string | null>(null)
  const [shipSubmitting, setShipSubmitting] = useState(false)

  const loadOrders = useCallback(
    async (page: number, size: number, filters: OrderFilters) => {
      setLoading(true)
      try {
        const data = await getOrderPage({
          page,
          size,
          orderNo: filters.orderNo?.trim() || undefined,
          userId: filters.userId || undefined,
          status: filters.status || undefined,
          startTime: filters.dateRange?.[0]
            ? new Date(filters.dateRange[0]).toISOString().slice(0, 19)
            : undefined,
          endTime: filters.dateRange?.[1]
            ? new Date(filters.dateRange[1]).toISOString().slice(0, 19)
            : undefined,
        })
        setOrders(data.result)
        setPagination({
          page: data.page,
          size: data.size,
          total: data.total,
        })
      } catch (error) {
        message.error(getErrorMessage(error))
      } finally {
        setLoading(false)
      }
    },
    [message],
  )

  useEffect(() => {
    let cancelled = false

    getOrderPage({ page: 1, size: initialPagination.size })
      .then((data) => {
        if (cancelled) return
        setOrders(data.result)
        setPagination({
          page: data.page,
          size: data.size,
          total: data.total,
        })
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

  const handleSearch = (values: OrderFilters) => {
    setActiveFilters(values)
    loadOrders(1, pagination.size, values)
  }

  const handleReset = () => {
    form.resetFields()
    setActiveFilters({})
    loadOrders(1, pagination.size, {})
  }

  const handleViewDetail = async (orderNo: string) => {
    setSelectedOrderNo(orderNo)
    setDrawerVisible(true)
    setDetailLoading(true)
    setOrderDetail(null)

    try {
      const detail = await getOrderDetail(orderNo)
      setOrderDetail(detail)
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setDetailLoading(false)
    }
  }

  const handleOpenShipModal = (orderNo: string) => {
    setShippingOrderNo(orderNo)
    shipForm.resetFields()
    setShipModalVisible(true)
  }

  const handleShippingCompanyChange = (shippingCompany: string) => {
    shipForm.setFieldValue(
      'trackingNumber',
      generateTrackingNumber(shippingCompany),
    )
  }

  const handleShipSubmit = async (values: OrderShipmentInput) => {
    if (!shippingOrderNo) return

    setShipSubmitting(true)
    try {
      await shipOrder(shippingOrderNo, {
        shippingCompany: values.shippingCompany.trim(),
        trackingNumber: values.trackingNumber.trim(),
      })
      message.success(`订单 ${shippingOrderNo} 发货成功`)
      setShipModalVisible(false)
      shipForm.resetFields()

      // 刷新列表数据
      loadOrders(pagination.page, pagination.size, activeFilters)

      // 若当前详情抽屉展示的是该订单，刷新抽屉中的详情
      if (drawerVisible && selectedOrderNo === shippingOrderNo) {
        const updatedDetail = await getOrderDetail(shippingOrderNo)
        setOrderDetail(updatedDetail)
      }
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setShipSubmitting(false)
    }
  }

  const columns: TableColumnsType<OrderListItem> = [
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      render: (orderNo: string) => <Text copyable strong>{orderNo}</Text>,
    },
    {
      title: '用户 ID',
      dataIndex: 'userId',
      key: 'userId',
      width: 100,
    },
    {
      title: '订单金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      render: (amount: number) => (
        <Text strong type="danger">
          {formatCurrency(amount)}
        </Text>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      render: (status: OrderStatus) => {
        const meta = orderStatusMeta[status] ?? {
          label: status,
          color: 'default',
        }
        return <Tag color={meta.color}>{meta.label}</Tag>
      },
    },
    {
      title: '下单时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '支付时间',
      dataIndex: 'paidAt',
      key: 'paidAt',
      width: 180,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '操作',
      key: 'action',
      width: 130,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record.orderNo)}
          >
            查看
          </Button>
          {record.status === 'PENDING_SHIPMENT' && (
            <Button
              type="link"
              size="small"
              icon={<SendOutlined />}
              onClick={() => handleOpenShipModal(record.orderNo)}
            >
              发货
            </Button>
          )}
        </Space>
      ),
    },
  ]

  const itemColumns: TableColumnsType<OrderItemSummary> = [
    {
      title: '商品',
      key: 'product',
      render: (_, item) => (
        <Space orientation="horizontal" size="middle">
          <Image
            src={item.productImageUrl || undefined}
            fallback={imageFallback}
            alt={item.productName}
            width={48}
            height={48}
            style={{ objectFit: 'cover', borderRadius: 4 }}
          />
          <Space direction="vertical" size={2}>
            <Text strong>{item.productName}</Text>
            <Text type="secondary" style={{ fontSize: 12 }}>
              ID: {item.productId}
            </Text>
          </Space>
        </Space>
      ),
    },
    {
      title: '单价',
      dataIndex: 'unitPrice',
      key: 'unitPrice',
      width: 100,
      render: (price: number) => formatCurrency(price),
    },
    {
      title: '数量',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 80,
      render: (qty: number) => `x${qty}`,
    },
    {
      title: '小计',
      dataIndex: 'subtotalAmount',
      key: 'subtotalAmount',
      width: 100,
      render: (subtotal: number) => (
        <Text strong>{formatCurrency(subtotal)}</Text>
      ),
    },
  ]

  return (
    <>
      <div className="page-heading">
        <Title level={2}>订单管理</Title>
        <Text type="secondary">
          查看系统全部订单列表，支持按订单号、用户、状态与时间范围检索明细及发货操作。
        </Text>
      </div>

      <Card className="filter-card">
        <Form<OrderFilters>
          form={form}
          layout="horizontal"
          onFinish={handleSearch}
          autoComplete="off"
        >
          <Space wrap size={[16, 16]}>
            <Form.Item name="orderNo" label="订单号" noStyle>
              <Input
                placeholder="请输入订单号"
                allowClear
                style={{ width: 200 }}
              />
            </Form.Item>

            <Form.Item name="userId" label="用户ID" noStyle>
              <InputNumber
                placeholder="用户ID"
                min={1}
                precision={0}
                style={{ width: 140 }}
              />
            </Form.Item>

            <Form.Item name="status" label="订单状态" noStyle>
              <Select
                placeholder="全部状态"
                allowClear
                style={{ width: 130 }}
                options={[
                  { value: 'PENDING_PAYMENT', label: '待付款' },
                  { value: 'PENDING_SHIPMENT', label: '待发货' },
                  { value: 'SHIPPED', label: '已发货' },
                  { value: 'COMPLETED', label: '已完成' },
                  { value: 'CANCELLED', label: '已取消' },
                ]}
              />
            </Form.Item>

            <Form.Item name="dateRange" label="创建时间" noStyle>
              <RangePicker
                showTime
                style={{ width: 340 }}
                placeholder={['开始时间', '结束时间']}
              />
            </Form.Item>

            <Space>
              <Button
                type="primary"
                htmlType="submit"
                icon={<SearchOutlined />}
                loading={loading}
              >
                查询
              </Button>
              <Button
                icon={<ReloadOutlined />}
                onClick={handleReset}
                disabled={loading}
              >
                重置
              </Button>
            </Space>
          </Space>
        </Form>
      </Card>

      <Card className="table-card">
        <Table<OrderListItem>
          rowKey="id"
          columns={columns}
          dataSource={orders}
          loading={loading}
          pagination={{
            current: pagination.page,
            pageSize: pagination.size,
            total: pagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条订单`,
            onChange: (page, size) => {
              loadOrders(page, size, activeFilters)
            },
          }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无订单数据"
              />
            ),
          }}
        />
      </Card>

      {/* 订单详情抽屉 */}
      <Drawer
        title={
          <Space>
            <span>订单详情</span>
            {orderDetail && (
              <Tag
                color={
                  orderStatusMeta[orderDetail.status]?.color ?? 'default'
                }
              >
                {orderDetail.statusDescription ||
                  orderStatusMeta[orderDetail.status]?.label}
              </Tag>
            )}
          </Space>
        }
        extra={
          orderDetail?.status === 'PENDING_SHIPMENT' && (
            <Button
              type="primary"
              size="small"
              icon={<SendOutlined />}
              onClick={() => handleOpenShipModal(orderDetail.orderNo)}
            >
              立即发货
            </Button>
          )
        }
        width={720}
        open={drawerVisible}
        onClose={() => {
          setDrawerVisible(false)
          setSelectedOrderNo(null)
        }}
      >
        {detailLoading ? (
          <div style={{ textAlign: 'center', padding: '60px 0' }}>
            <Spin size="large" tip="加载订单详情中..." />
          </div>
        ) : orderDetail ? (
          <Space orientation="vertical" size="large" style={{ width: '100%' }}>
            {/* 基本信息 */}
            <Card
              size="small"
              title={
                <Space>
                  <ClockCircleOutlined />
                  <span>基本信息</span>
                </Space>
              }
            >
              <Descriptions size="small" column={2} bordered>
                <Descriptions.Item label="订单号">
                  <Text copyable strong>
                    {orderDetail.orderNo}
                  </Text>
                </Descriptions.Item>
                <Descriptions.Item label="用户 ID">
                  <Text strong>{orderDetail.userId ?? '-'}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="订单总金额">
                  <Text strong type="danger" style={{ fontSize: 16 }}>
                    {formatCurrency(orderDetail.totalAmount)}
                  </Text>
                </Descriptions.Item>
                <Descriptions.Item label="订单状态">
                  <Tag
                    color={
                      orderStatusMeta[orderDetail.status]?.color ?? 'default'
                    }
                  >
                    {orderDetail.statusDescription}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="下单时间">
                  {formatDateTime(orderDetail.createdAt)}
                </Descriptions.Item>
                <Descriptions.Item label="付款过期时间">
                  {formatDateTime(orderDetail.expireAt)}
                </Descriptions.Item>
                <Descriptions.Item label="支付时间">
                  {formatDateTime(orderDetail.paidAt)}
                </Descriptions.Item>
                <Descriptions.Item label="发货时间">
                  {formatDateTime(orderDetail.shippedAt)}
                </Descriptions.Item>
                <Descriptions.Item label="完成时间">
                  {formatDateTime(orderDetail.completedAt)}
                </Descriptions.Item>
                <Descriptions.Item label="取消时间">
                  {formatDateTime(orderDetail.cancelledAt)}
                </Descriptions.Item>
              </Descriptions>
            </Card>

            {/* 物流信息 */}
            {(orderDetail.shippingCompany ||
              orderDetail.trackingNumber ||
              orderDetail.shippedAt) && (
              <Card
                size="small"
                title={
                  <Space>
                    <CarOutlined />
                    <span>物流信息</span>
                  </Space>
                }
              >
                <Descriptions size="small" column={2} bordered>
                  <Descriptions.Item label="快递公司">
                    <Text strong>{orderDetail.shippingCompany || '-'}</Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="快递单号">
                    {orderDetail.trackingNumber ? (
                      <Text copyable strong>
                        {orderDetail.trackingNumber}
                      </Text>
                    ) : (
                      '-'
                    )}
                  </Descriptions.Item>
                  <Descriptions.Item label="发货时间" span={2}>
                    {formatDateTime(orderDetail.shippedAt)}
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            )}

            {/* 收货地址快照 */}
            <Card
              size="small"
              title={
                <Space>
                  <EnvironmentOutlined />
                  <span>收货地址快照</span>
                </Space>
              }
            >
              {orderDetail.address ? (
                <Descriptions size="small" column={2} bordered>
                  <Descriptions.Item label="收货人">
                    {orderDetail.address.receiverName}
                  </Descriptions.Item>
                  <Descriptions.Item label="联系电话">
                    {orderDetail.address.phone}
                  </Descriptions.Item>
                  <Descriptions.Item label="收货地址" span={2}>
                    {formatFullAddress(orderDetail.address)}
                  </Descriptions.Item>
                </Descriptions>
              ) : (
                <Text type="secondary">无收货地址信息</Text>
              )}
            </Card>

            {/* 商品快照明细 */}
            <Card
              size="small"
              title={
                <Space>
                  <ShoppingOutlined />
                  <span>商品明细 ({orderDetail.items.length})</span>
                </Space>
              }
            >
              <Table<OrderItemSummary>
                rowKey="productId"
                columns={itemColumns}
                dataSource={orderDetail.items}
                pagination={false}
                size="small"
              />
            </Card>

            {/* 订单流转日志 */}
            <Card
              size="small"
              title={
                <Space>
                  <HistoryOutlined />
                  <span>流转日志 ({orderDetail.operateLogs.length})</span>
                </Space>
              }
            >
              {orderDetail.operateLogs.length === 0 ? (
                <Text type="secondary">暂无流转日志</Text>
              ) : (
                <Timeline
                  style={{ marginTop: 12 }}
                  items={orderDetail.operateLogs.map((log: OrderOperateLog) => {
                    const opName =
                      operationNameMap[log.operation] || log.operation
                    const roleName =
                      operatorTypeMap[log.operatorType] || log.operatorType
                    return {
                      children: (
                        <Space direction="vertical" size={2}>
                          <Space>
                            <Text strong>{opName}</Text>
                            <Tag color="cyan">
                              {roleName}: {log.operatorName}
                            </Tag>
                            {log.toStatus && (
                              <Tag
                                color={
                                  orderStatusMeta[log.toStatus]?.color ??
                                  'default'
                                }
                              >
                                {orderStatusMeta[log.toStatus]?.label ??
                                  log.toStatus}
                              </Tag>
                            )}
                          </Space>
                          {log.reason && (
                            <Text type="secondary">备注: {log.reason}</Text>
                          )}
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            {formatDateTime(log.createdAt)}
                          </Text>
                        </Space>
                      ),
                    }
                  })}
                />
              )}
            </Card>
          </Space>
        ) : (
          <Alert
            message="未获取到订单详情"
            description={`订单号 ${selectedOrderNo} 暂无详细记录`}
            type="warning"
            showIcon
          />
        )}
      </Drawer>

      {/* 发货弹窗 */}
      <Modal
        title={`订单发货 - ${shippingOrderNo}`}
        open={shipModalVisible}
        onOk={() => shipForm.submit()}
        onCancel={() => {
          if (!shipSubmitting) {
            setShipModalVisible(false)
            shipForm.resetFields()
          }
        }}
        confirmLoading={shipSubmitting}
        okText="确认发货"
        cancelText="取消"
        destroyOnClose
      >
        <Form<OrderShipmentInput>
          form={shipForm}
          layout="vertical"
          onFinish={handleShipSubmit}
          autoComplete="off"
          style={{ marginTop: 16 }}
        >
          <Form.Item
            label="快递公司"
            name="shippingCompany"
            rules={[
              { required: true, message: '请选择或输入快递公司' },
              { max: 64, message: '快递公司名称长度不能超过64个字符' },
              {
                pattern: /.*\S.*/,
                message: '快递公司不能为空白字符',
              },
            ]}
          >
            <AutoComplete
              options={commonShippingCompanies}
              placeholder="请选择或输入快递公司（如：顺丰速运）"
              onChange={handleShippingCompanyChange}
              filterOption={(inputValue, option) =>
                (option?.value ?? '')
                  .toUpperCase()
                  .includes(inputValue.toUpperCase())
              }
            />
          </Form.Item>

          <Form.Item
            label="快递单号"
            name="trackingNumber"
            rules={[
              { required: true, message: '请输入快递单号' },
              { max: 64, message: '快递单号长度不能超过64个字符' },
              {
                pattern: /^[A-Za-z0-9_-]+$/,
                message: '快递单号只能包含字母、数字和连接符',
              },
            ]}
          >
            <Input placeholder="选择快递公司后自动生成，也可手动修改" allowClear />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}

export default OrderManagePage
