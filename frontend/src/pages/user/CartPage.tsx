import { useEffect, useState } from 'react'
import {
  Alert,
  App as AntdApp,
  Button,
  Card,
  Checkbox,
  Col,
  Empty,
  Popconfirm,
  Row,
  Skeleton,
  Space,
  Tag,
  Typography,
} from 'antd'
import { DeleteOutlined, ReloadOutlined, ShoppingOutlined } from '@ant-design/icons'
import { Link, useNavigate } from 'react-router-dom'
import {
  deleteCartItem,
  getCart,
  updateCartItem,
  type CartItemResponse,
  type CartResponse,
} from '@/api/cart'
import { QuantityInput } from '@/components/common/QuantityInput'
import { paths, productDetailPath } from '@/router/paths'

function formatPrice(price: number | null | undefined): string {
  const value = Number(price)
  return Number.isFinite(value) ? value.toFixed(2) : '0.00'
}

export function CartPage() {
  const [cart, setCart] = useState<CartResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)
  const [batchLoading, setBatchLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()
  const { message } = AntdApp.useApp()

  const loadCartData = async (isManualRefresh = false) => {
    if (isManualRefresh) {
      setRefreshing(true)
    }
    setError(null)

    try {
      const data = await getCart()
      setCart(data)
    } catch {
      setError('获取购物车数据失败，请稍后重试')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    const controller = new AbortController()

    getCart(controller.signal)
      .then((data) => {
        setCart(data)
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setError('获取购物车数据失败，请稍后重试')
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
  }, [])

  const items = cart?.items ?? []
  const validItems = items.filter((item) => item.valid)
  const invalidItems = items.filter((item) => !item.valid)
  const totalItemCount = items.reduce((sum, item) => sum + (item.quantity || 0), 0)
  const selectedValidItems = validItems.filter((item) => item.selected)
  const allSelected =
    validItems.length > 0 && selectedValidItems.length === validItems.length
  const isIndeterminate =
    selectedValidItems.length > 0 && selectedValidItems.length < validItems.length

  const handleToggleSelect = async (item: CartItemResponse) => {
    if (!item.valid) return
    setActionLoadingId(item.productId)
    try {
      await updateCartItem(item.productId, {
        selected: !item.selected,
      })
      await loadCartData()
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setActionLoadingId(null)
    }
  }

  const handleToggleAllSelect = async (checked: boolean) => {
    const targets = validItems.filter((item) => item.selected !== checked)
    if (targets.length === 0) return

    setBatchLoading(true)
    try {
      await Promise.all(
        targets.map((item) =>
          updateCartItem(item.productId, { selected: checked }),
        ),
      )
      await loadCartData()
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setBatchLoading(false)
    }
  }

  const handleQuantityChange = async (
    item: CartItemResponse,
    newVal: number,
  ) => {
    if (!newVal || newVal === item.quantity || newVal < 1) return
    const maxStock = Math.max(1, Math.min(item.stock ?? 99, 99))
    const validQuantity = Math.min(Math.max(1, Math.floor(newVal)), maxStock)

    setActionLoadingId(item.productId)
    try {
      await updateCartItem(item.productId, {
        quantity: validQuantity,
      })
      await loadCartData()
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setActionLoadingId(null)
    }
  }

  const handleDeleteItem = async (productId: number) => {
    setActionLoadingId(productId)
    try {
      await deleteCartItem(productId)
      void message.success('商品已从购物车移除')
      await loadCartData()
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setActionLoadingId(null)
    }
  }

  const handleClearInvalidItems = async () => {
    if (invalidItems.length === 0) return
    setBatchLoading(true)
    try {
      await Promise.all(
        invalidItems.map((item) => deleteCartItem(item.productId)),
      )
      void message.success(`已成功清理 ${invalidItems.length} 件失效商品`)
      await loadCartData()
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setBatchLoading(false)
    }
  }

  return (
    <div className="cart-page">
      <div className="cart-page-header">
        <div>
          <Typography.Title level={2} className="cart-page-title">
            我的购物车
          </Typography.Title>
          <Typography.Paragraph type="secondary" className="cart-page-desc">
            查看并管理购物车中的商品清单
          </Typography.Paragraph>
        </div>
        <Space size={12}>
          {invalidItems.length > 0 && (
            <Button
              danger
              icon={<DeleteOutlined />}
              onClick={() => void handleClearInvalidItems()}
              loading={batchLoading}
            >
              清理失效商品 ({invalidItems.length})
            </Button>
          )}
          <Button
            icon={<ReloadOutlined />}
            onClick={() => void loadCartData(true)}
            loading={refreshing || batchLoading}
          >
            刷新清单
          </Button>
        </Space>
      </div>

      {error && (
        <Alert
          type="error"
          showIcon
          message={error}
          action={
            <Button size="small" onClick={() => void loadCartData(true)}>
              重试
            </Button>
          }
        />
      )}

      {loading ? (
        <Card className="cart-card" bordered={false}>
          <Skeleton active paragraph={{ rows: 6 }} />
        </Card>
      ) : items.length === 0 ? (
        <Card className="cart-empty-card" bordered={false}>
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="购物车还是空的，快去挑选心仪的商品吧"
          >
            <Button type="primary" icon={<ShoppingOutlined />} href={paths.home}>
              去挑选好物
            </Button>
          </Empty>
        </Card>
      ) : (
        <Row gutter={[24, 24]}>
          <Col xs={24} lg={16}>
            <div className="cart-list-header-bar">
              <Checkbox
                checked={allSelected}
                indeterminate={isIndeterminate}
                disabled={validItems.length === 0 || batchLoading}
                onChange={(e) => void handleToggleAllSelect(e.target.checked)}
              >
                <span className="cart-select-all-text">
                  全选 ({selectedValidItems.length}/{validItems.length})
                </span>
              </Checkbox>
            </div>

            <div className="cart-item-list">
              {items.map((item) => {
                const isInvalid = !item.valid
                const itemTotal = (item.price ?? 0) * item.quantity
                const isItemLoading =
                  actionLoadingId === item.productId || batchLoading
                const maxStock = Math.max(1, Math.min(item.stock ?? 99, 99))

                return (
                  <Card
                    key={item.productId}
                    className={`cart-item-card ${isInvalid ? 'cart-item-card-invalid' : ''}`}
                    bordered={false}
                  >
                    <div className="cart-item-content">
                      <div className="cart-item-checkbox-wrapper">
                        <Checkbox
                          checked={item.selected}
                          disabled={isInvalid || isItemLoading}
                          onChange={() => void handleToggleSelect(item)}
                        />
                      </div>

                      <div className="cart-item-media">
                        {item.imageUrl ? (
                          <img
                            src={item.imageUrl}
                            alt={item.productName || '商品图片'}
                            className="cart-item-image"
                          />
                        ) : (
                          <div className="cart-item-image-placeholder">暂无图片</div>
                        )}
                      </div>

                      <div className="cart-item-info">
                        <div className="cart-item-header">
                          <Typography.Title
                            level={5}
                            className="cart-item-name"
                            ellipsis={{ rows: 2 }}
                          >
                            <Link to={productDetailPath(item.productId)}>
                              {item.productName || `商品编号: ${item.productId}`}
                            </Link>
                          </Typography.Title>

                          {isInvalid ? (
                            <Tag color="error" className="cart-item-status-tag">
                              {item.invalidReason || '商品不可用'}
                            </Tag>
                          ) : (
                            <Tag color="blue" className="cart-item-status-tag">
                              在售中
                            </Tag>
                          )}
                        </div>

                        <div className="cart-item-meta">
                          <Typography.Text type="secondary" className="cart-item-price">
                            单价: <span className="cart-price-num">¥{formatPrice(item.price)}</span>
                          </Typography.Text>
                          {typeof item.stock === 'number' && !isInvalid && (
                            <Typography.Text type="secondary" className="cart-stock-text">
                              库存: {item.stock} 件
                            </Typography.Text>
                          )}
                        </div>

                        <div className="cart-item-footer">
                          <div className="cart-item-quantity-ctrl">
                            <Typography.Text type="secondary" className="cart-qty-label">
                              数量:
                            </Typography.Text>
                            <QuantityInput
                              min={1}
                              max={maxStock}
                              value={item.quantity}
                              disabled={isInvalid || isItemLoading}
                              loading={actionLoadingId === item.productId}
                              onChange={(val) =>
                                void handleQuantityChange(item, val)
                              }
                              size="small"
                            />
                          </div>

                          <div className="cart-item-actions">
                            <Typography.Text className="cart-item-subtotal">
                              小计: <strong>¥{formatPrice(itemTotal)}</strong>
                            </Typography.Text>

                            <Popconfirm
                              title="删除商品"
                              description="确定要将该商品从购物车中移除吗？"
                              okText="删除"
                              cancelText="取消"
                              okButtonProps={{ danger: true }}
                              onConfirm={() => void handleDeleteItem(item.productId)}
                              disabled={isItemLoading}
                            >
                              <Button
                                type="link"
                                danger
                                size="small"
                                loading={actionLoadingId === item.productId}
                                disabled={isItemLoading}
                                className="cart-delete-btn"
                              >
                                删除
                              </Button>
                            </Popconfirm>
                          </div>
                        </div>
                      </div>
                    </div>
                  </Card>
                )
              })}
            </div>
          </Col>

          <Col xs={24} lg={8}>
            <Card className="cart-summary-card" bordered={false}>
              <Typography.Title level={4} className="cart-summary-title">
                金额总计
              </Typography.Title>

              <div className="cart-summary-rows">
                <div className="cart-summary-row">
                  <Typography.Text type="secondary">商品总件数</Typography.Text>
                  <Typography.Text strong>{totalItemCount} 件</Typography.Text>
                </div>
                <div className="cart-summary-row">
                  <Typography.Text type="secondary">已勾选商品</Typography.Text>
                  <Typography.Text strong>{selectedValidItems.length} 种</Typography.Text>
                </div>
                <div className="cart-summary-divider" />
                <div className="cart-summary-row cart-summary-total-row">
                  <Typography.Text strong className="cart-total-label">
                    应付总额
                  </Typography.Text>
                  <Typography.Text className="cart-total-amount">
                    <small>¥</small>{formatPrice(cart?.selectedAmount)}
                  </Typography.Text>
                </div>
              </div>

              <div className="cart-summary-tips">
                <Typography.Text type="secondary">
                  * 仅有效且已勾选的商品计入应付总额。
                </Typography.Text>
              </div>

              <Button
                type="primary"
                size="large"
                block
                disabled={selectedValidItems.length === 0}
                onClick={() => navigate(paths.checkout)}
                className="cart-checkout-btn"
              >
                {selectedValidItems.length > 0
                  ? `去结算 (${selectedValidItems.length}件)`
                  : '请勾选商品'}
              </Button>
            </Card>
          </Col>
        </Row>
      )}
    </div>
  )
}
