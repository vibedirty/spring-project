import { useEffect, useState } from 'react'
import {
  App as AntdApp,
  Breadcrumb,
  Button,
  Card,
  Result,
  Skeleton,
  Space,
  Tag,
  Typography,
} from 'antd'
import { ShoppingCartOutlined } from '@ant-design/icons'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '@/api/types'
import { addToCart } from '@/api/cart'
import { getOnSaleProductDetail, type Product } from '@/api/product'
import { QuantityInput } from '@/components/common/QuantityInput'
import { ProductImage } from '@/components/product/ProductImage'
import { paths } from '@/router/paths'
import { useAuthStore } from '@/stores/authStore'

type DetailState =
  | { status: 'loading' }
  | { status: 'success'; product: Product }
  | { status: 'not-found' }
  | { status: 'failed' }

function parseProductId(value: string | undefined): number | null {
  if (!value || !/^\d+$/.test(value)) {
    return null
  }

  const productId = Number(value)
  return Number.isSafeInteger(productId) && productId > 0 ? productId : null
}

function formatPrice(price: number): string {
  const value = Number(price)
  return Number.isFinite(value) ? value.toFixed(2) : '0.00'
}

function ProductDetailContent({
  productId,
  backTo,
}: {
  productId: number | null
  backTo: string
}) {
  const [state, setState] = useState<DetailState>(
    productId === null ? { status: 'not-found' } : { status: 'loading' },
  )
  const [quantity, setQuantity] = useState<number>(1)
  const [addingToCart, setAddingToCart] = useState<boolean>(false)
  const { isAuthenticated, role } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()
  const { message } = AntdApp.useApp()

  useEffect(() => {
    if (productId === null) {
      return
    }

    const controller = new AbortController()
    getOnSaleProductDetail(productId, controller.signal)
      .then((product) => setState({ status: 'success', product }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setState(
          error instanceof ApiError && error.code === 404
            ? { status: 'not-found' }
            : { status: 'failed' },
        )
      })

    return () => controller.abort()
  }, [productId])

  const handleAddToCart = async (product: Product) => {
    if (!isAuthenticated || role !== 'USER') {
      void message.info('请先登录账号')
      navigate(paths.login, {
        state: { from: location },
      })
      return
    }

    setAddingToCart(true)
    try {
      await addToCart({
        productId: product.id,
        quantity,
      })
      void message.success('已成功加入购物车')
    } catch {
      // 错误由 http 拦截器统一处理
    } finally {
      setAddingToCart(false)
    }
  }

  if (state.status === 'loading') {
    return (
      <Card className="product-detail-card" bordered={false}>
        <Skeleton active paragraph={{ rows: 8 }} />
      </Card>
    )
  }

  if (state.status === 'not-found') {
    return (
      <Card className="product-detail-card" bordered={false}>
        <Result
          status="404"
          title="商品不存在"
          subTitle="该商品可能已下架、删除或不存在。"
          extra={
            <Button type="primary">
              <Link to={backTo}>返回商品列表</Link>
            </Button>
          }
        />
      </Card>
    )
  }

  if (state.status === 'failed') {
    return (
      <Card className="product-detail-card" bordered={false}>
        <Result
          status="error"
          title="商品详情加载失败"
          subTitle="请检查网络连接后重新加载页面。"
          extra={
            <Button type="primary" onClick={() => window.location.reload()}>
              重新加载
            </Button>
          }
        />
      </Card>
    )
  }

  const { product } = state
  const maxAvailable = Math.max(1, Math.min(product.stock, 99))

  return (
    <Card className="product-detail-card" bordered={false}>
      <div className="product-detail-grid">
        <div className="product-detail-media">
          <ProductImage product={product} variant="detail" />
        </div>
        <div className="product-detail-info">
          <Space size={[8, 8]} wrap>
            <Tag color="blue">{product.categoryName}</Tag>
            <Tag color={product.stock > 0 ? 'success' : 'default'}>
              {product.stock > 0 ? '在售' : '缺货'}
            </Tag>
          </Space>
          <Typography.Title level={1}>{product.name}</Typography.Title>
          <Typography.Text className="product-detail-price">
            <small>¥</small>{formatPrice(product.price)}
          </Typography.Text>
          <div className="product-detail-stats">
            <Typography.Text type="secondary">
              已售 {product.sales} 件
            </Typography.Text>
            <Typography.Text type={product.stock > 0 ? undefined : 'danger'}>
              {product.stock > 0 ? `库存 ${product.stock} 件` : '暂时缺货'}
            </Typography.Text>
          </div>

          <div className="product-detail-actions">
            <div className="product-quantity-row">
              <Typography.Text className="product-quantity-label">
                数量
              </Typography.Text>
              <QuantityInput
                min={1}
                max={maxAvailable}
                value={quantity}
                onChange={setQuantity}
                disabled={product.stock <= 0}
                size="middle"
                className="product-quantity-stepper"
              />
              {product.stock > 0 && (
                <Typography.Text type="secondary" className="product-quantity-limit">
                  (限购 {maxAvailable} 件)
                </Typography.Text>
              )}
            </div>

            <Button
              type="primary"
              size="large"
              icon={<ShoppingCartOutlined />}
              className="product-add-cart-btn"
              onClick={() => handleAddToCart(product)}
              loading={addingToCart}
              disabled={product.stock <= 0}
            >
              {product.stock > 0 ? '加入购物车' : '暂时缺货'}
            </Button>
          </div>

          <div className="product-detail-description">
            <Typography.Title level={4}>商品详情</Typography.Title>
            <Typography.Paragraph>
              {product.description || '暂无商品详情描述'}
            </Typography.Paragraph>
          </div>
        </div>
      </div>
    </Card>
  )
}

export function ProductDetailPage() {
  const { productId: rawProductId } = useParams()
  const location = useLocation()
  const locationState = location.state as { from?: unknown } | null
  const backTo =
    typeof locationState?.from === 'string' &&
    locationState.from.startsWith('/') &&
    !locationState.from.startsWith('//')
      ? locationState.from
      : paths.home
  const productId = parseProductId(rawProductId)

  return (
    <div className="product-detail-page">
      <Breadcrumb
        items={[
          { title: <Link to={paths.home}>首页</Link> },
          { title: <Link to={backTo}>在售商品</Link> },
          { title: '商品详情' },
        ]}
      />
      <ProductDetailContent
        key={rawProductId}
        productId={productId}
        backTo={backTo}
      />
    </div>
  )
}
