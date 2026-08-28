import { useEffect, useState } from 'react'
import {
  Alert,
  App as AntdApp,
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Result,
  Row,
  Skeleton,
  Space,
  Switch,
  Tag,
  Typography,
} from 'antd'
import {
  CheckCircleFilled,
  EnvironmentOutlined,
  PlusOutlined,
  ShoppingOutlined,
} from '@ant-design/icons'
import { Link, useNavigate } from 'react-router-dom'
import {
  createAddress,
  getAddressList,
  type AddressCreatePayload,
  type AddressItem,
} from '@/api/address'
import { getCart, type CartResponse } from '@/api/cart'
import { createOrder } from '@/api/order'
import { orderDetailPath, paths, productDetailPath } from '@/router/paths'
import { generateIdempotencyToken } from '@/utils/idempotency'

interface AddressFormFields {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: boolean
}

const PHONE_PATTERN = /^1[3-9]\d{9}$/

function formatPrice(price: number | null | undefined): string {
  const value = Number(price)
  return Number.isFinite(value) ? value.toFixed(2) : '0.00'
}

export function CheckoutPage() {
  const [addresses, setAddresses] = useState<AddressItem[]>([])
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null)
  const [cart, setCart] = useState<CartResponse | null>(null)
  const [idempotencyToken] = useState<string>(() => generateIdempotencyToken())
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [addressSubmitting, setAddressSubmitting] = useState(false)
  const [form] = Form.useForm<AddressFormFields>()

  const navigate = useNavigate()
  const { message } = AntdApp.useApp()

  useEffect(() => {
    const controller = new AbortController()

    Promise.all([
      getAddressList(controller.signal),
      getCart(controller.signal),
    ])
      .then(([addressList, cartData]) => {
        const validAddresses = Array.isArray(addressList) ? addressList : []
        setAddresses(validAddresses)
        setCart(cartData)

        if (validAddresses.length > 0) {
          const defaultAddr = validAddresses.find((a) => a.isDefault === 1)
          setSelectedAddressId(defaultAddr ? defaultAddr.id : validAddresses[0].id)
        }
      })
      .catch(() => {
        // 错误由 http 拦截器处理
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      })

    return () => {
      controller.abort()
    }
  }, [])

  const selectedItems = (cart?.items ?? []).filter(
    (item) => item.valid && item.selected,
  )

  const handleOpenAddressModal = () => {
    form.resetFields()
    form.setFieldsValue({
      isDefault: addresses.length === 0,
    })
    setModalOpen(true)
  }

  const handleCreateAddress = async (values: AddressFormFields) => {
    setAddressSubmitting(true)
    try {
      const payload: AddressCreatePayload = {
        receiverName: values.receiverName.trim(),
        phone: values.phone.trim(),
        province: values.province.trim(),
        city: values.city.trim(),
        district: values.district.trim(),
        detailAddress: values.detailAddress.trim(),
        isDefault: values.isDefault ? 1 : 0,
      }
      const newAddr = await createAddress(payload)
      void message.success('收货地址添加成功')
      setModalOpen(false)
      form.resetFields()

      const latestAddresses = await getAddressList()
      setAddresses(latestAddresses)
      setSelectedAddressId(newAddr.id)
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setAddressSubmitting(false)
    }
  }

  const handleSubmitOrder = async () => {
    if (!selectedAddressId) {
      void message.warning('请选择收货地址')
      return
    }

    if (selectedItems.length === 0) {
      void message.warning('当前没有选中的有效商品')
      return
    }

    setSubmitting(true)
    try {
      const res = await createOrder({
        addressId: selectedAddressId,
        idempotencyToken,
      })
      void message.success('订单提交成功，正在前往订单详情')
      navigate(orderDetailPath(res.orderNo), { replace: true })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="checkout-page">
      <div className="checkout-page-header">
        <Typography.Title level={2} className="checkout-page-title">
          确认订单
        </Typography.Title>
        <Typography.Paragraph type="secondary" className="checkout-page-desc">
          请核对收货地址与结算商品信息
        </Typography.Paragraph>
      </div>

      {loading ? (
        <Card className="checkout-loading-card" bordered={false}>
          <Skeleton active paragraph={{ rows: 8 }} />
        </Card>
      ) : selectedItems.length === 0 ? (
        <Card className="checkout-empty-card" bordered={false}>
          <Result
            status="warning"
            title="购物车中没有选中的商品"
            subTitle="请先前往购物车勾选需要结算的商品。"
            extra={
              <Button type="primary" icon={<ShoppingOutlined />} onClick={() => navigate(paths.cart)}>
                返回购物车
              </Button>
            }
          />
        </Card>
      ) : (
        <Row gutter={[24, 24]}>
          <Col xs={24} lg={16}>
            <div className="checkout-left-sections">
              {/* 1. 收货地址选择 */}
              <Card className="checkout-section-card" bordered={false}>
                <div className="checkout-section-header">
                  <Typography.Title level={4} className="checkout-section-title">
                    <EnvironmentOutlined /> 收货地址
                  </Typography.Title>
                  <Button
                    type="link"
                    icon={<PlusOutlined />}
                    onClick={handleOpenAddressModal}
                  >
                    新增地址
                  </Button>
                </div>

                {addresses.length === 0 ? (
                  <Alert
                    type="warning"
                    showIcon
                    message="暂无收货地址"
                    description="提交订单前请先添加收货地址"
                    action={
                      <Button type="primary" size="small" onClick={handleOpenAddressModal}>
                        立即添加
                      </Button>
                    }
                  />
                ) : (
                  <Row gutter={[12, 12]} className="checkout-address-grid">
                    {addresses.map((addr) => {
                      const isSelected = selectedAddressId === addr.id
                      return (
                        <Col xs={24} sm={12} key={addr.id}>
                          <div
                            className={`checkout-address-item ${isSelected ? 'checkout-address-item-selected' : ''}`}
                            onClick={() => setSelectedAddressId(addr.id)}
                          >
                            <div className="checkout-address-header">
                              <Space size={8}>
                                <span className="checkout-addr-name">{addr.receiverName}</span>
                                <span className="checkout-addr-phone">{addr.phone}</span>
                              </Space>
                              {addr.isDefault === 1 && (
                                <Tag color="blue">默认</Tag>
                              )}
                            </div>
                            <div className="checkout-addr-detail">
                              {addr.province} {addr.city} {addr.district} {addr.detailAddress}
                            </div>
                            {isSelected && (
                              <CheckCircleFilled className="checkout-address-checked-icon" />
                            )}
                          </div>
                        </Col>
                      )
                    })}
                  </Row>
                )}
              </Card>

              {/* 2. 商品清单核对 */}
              <Card className="checkout-section-card" bordered={false}>
                <div className="checkout-section-header">
                  <Typography.Title level={4} className="checkout-section-title">
                    <ShoppingOutlined /> 商品清单 ({selectedItems.length}件)
                  </Typography.Title>
                  <Link to={paths.cart} className="checkout-back-cart-link">
                    返回购物车修改
                  </Link>
                </div>

                <div className="checkout-item-list">
                  {selectedItems.map((item) => {
                    const itemTotal = (item.price ?? 0) * item.quantity
                    return (
                      <div key={item.productId} className="checkout-item-row">
                        <div className="checkout-item-media">
                          {item.imageUrl ? (
                            <img
                              src={item.imageUrl}
                              alt={item.productName || '商品'}
                              className="checkout-item-img"
                            />
                          ) : (
                            <div className="checkout-item-placeholder">暂无图片</div>
                          )}
                        </div>

                        <div className="checkout-item-info">
                          <Typography.Text strong className="checkout-item-name">
                            <Link to={productDetailPath(item.productId)}>
                              {item.productName || `商品 ${item.productId}`}
                            </Link>
                          </Typography.Text>
                          <div className="checkout-item-meta">
                            <Typography.Text type="secondary">
                              ¥{formatPrice(item.price)} × {item.quantity}
                            </Typography.Text>
                          </div>
                        </div>

                        <div className="checkout-item-price-col">
                          <Typography.Text strong className="checkout-item-subtotal">
                            ¥{formatPrice(itemTotal)}
                          </Typography.Text>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </Card>
            </div>
          </Col>

          {/* 3. 结算与提交 */}
          <Col xs={24} lg={8}>
            <Card className="checkout-summary-card" bordered={false}>
              <Typography.Title level={4} className="checkout-summary-title">
                结算明细
              </Typography.Title>

              <div className="checkout-summary-rows">
                <div className="checkout-summary-row">
                  <Typography.Text type="secondary">商品总额</Typography.Text>
                  <Typography.Text strong>
                    ¥{formatPrice(cart?.selectedAmount)}
                  </Typography.Text>
                </div>
                <div className="checkout-summary-row">
                  <Typography.Text type="secondary">运费</Typography.Text>
                  <Typography.Text strong className="checkout-freight-free">
                    包邮 (¥0.00)
                  </Typography.Text>
                </div>
                <div className="checkout-summary-divider" />
                <div className="checkout-summary-row checkout-summary-total">
                  <Typography.Text strong className="checkout-total-label">
                    应付总额
                  </Typography.Text>
                  <Typography.Text className="checkout-total-amount">
                    <small>¥</small>{formatPrice(cart?.selectedAmount)}
                  </Typography.Text>
                </div>
              </div>

              <div className="checkout-summary-action">
                <Button
                  type="primary"
                  size="large"
                  block
                  onClick={() => void handleSubmitOrder()}
                  loading={submitting}
                  disabled={!selectedAddressId || selectedItems.length === 0}
                  className="checkout-submit-btn"
                >
                  提交订单
                </Button>
              </div>

              <div className="checkout-summary-tips">
                <Typography.Text type="secondary">
                  * 点击提交订单后将扣减对应商品库存并生成待付款订单。
                </Typography.Text>
              </div>
            </Card>
          </Col>
        </Row>
      )}

      {/* 新增地址弹窗 */}
      <Modal
        title="新增收货地址"
        open={modalOpen}
        onCancel={() => !addressSubmitting && setModalOpen(false)}
        footer={null}
        destroyOnClose
        centered
        width={560}
      >
        <Form
          form={form}
          layout="vertical"
          requiredMark="optional"
          onFinish={handleCreateAddress}
          disabled={addressSubmitting}
        >
          <Row gutter={16}>
            <Col xs={24} sm={12}>
              <Form.Item
                label="收货人"
                name="receiverName"
                rules={[
                  { required: true, whitespace: true, message: '请输入收货人姓名' },
                  { max: 32, message: '收货人姓名最多 32 个字符' },
                ]}
              >
                <Input placeholder="请填写收货人姓名" maxLength={32} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12}>
              <Form.Item
                label="手机号码"
                name="phone"
                rules={[
                  { required: true, whitespace: true, message: '请输入手机号码' },
                  { pattern: PHONE_PATTERN, message: '请输入正确的 11 位手机号码' },
                  { max: 20, message: '手机号最多 20 个字符' },
                ]}
              >
                <Input placeholder="请填写 11 位手机号" maxLength={20} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={12}>
            <Col xs={24} sm={8}>
              <Form.Item
                label="省/直辖市"
                name="province"
                rules={[
                  { required: true, whitespace: true, message: '请输入省/直辖市' },
                  { max: 64, message: '省份最多 64 个字符' },
                ]}
              >
                <Input placeholder="如：广东省" maxLength={64} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={8}>
              <Form.Item
                label="城市"
                name="city"
                rules={[
                  { required: true, whitespace: true, message: '请输入城市' },
                  { max: 64, message: '城市最多 64 个字符' },
                ]}
              >
                <Input placeholder="如：深圳市" maxLength={64} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={8}>
              <Form.Item
                label="区/县"
                name="district"
                rules={[
                  { required: true, whitespace: true, message: '请输入区/县' },
                  { max: 64, message: '区/县最多 64 个字符' },
                ]}
              >
                <Input placeholder="如：南山区" maxLength={64} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            label="详细地址"
            name="detailAddress"
            rules={[
              { required: true, whitespace: true, message: '请输入详细地址' },
              { max: 255, message: '详细地址最多 255 个字符' },
            ]}
          >
            <Input.TextArea
              rows={3}
              placeholder="请输入详细街道、门牌号等信息"
              maxLength={255}
              showCount
            />
          </Form.Item>

          <Form.Item
            label="设为默认地址"
            name="isDefault"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>

          <div className="address-form-actions">
            <Space size={12}>
              <Button onClick={() => setModalOpen(false)} disabled={addressSubmitting}>
                取消
              </Button>
              <Button type="primary" htmlType="submit" loading={addressSubmitting}>
                保存并使用
              </Button>
            </Space>
          </div>
        </Form>
      </Modal>
    </div>
  )
}
