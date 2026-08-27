import { useEffect, useState } from 'react'
import {
  Alert,
  App as AntdApp,
  Button,
  Card,
  Empty,
  Input,
  Pagination,
  Select,
  Skeleton,
  Space,
  Tag,
  Typography,
} from 'antd'
import { ShoppingCartOutlined } from '@ant-design/icons'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { addToCart } from '@/api/cart'
import {
  getOnSaleProducts,
  type Product,
  type ProductSort,
} from '@/api/product'
import { ProductImage } from '@/components/product/ProductImage'
import { paths, productDetailPath } from '@/router/paths'
import { useAuthStore } from '@/stores/authStore'

const PRODUCT_PAGE_SIZE = 12

function formatPrice(price: number): string {
  const value = Number(price)
  return Number.isFinite(value) ? value.toFixed(2) : '0.00'
}

interface OnSaleProductListProps {
  categoryId: number | null
  keyword: string
  sort: ProductSort | null
  onKeywordChange: (keyword: string) => void
  onSortChange: (sort: ProductSort | null) => void
}

export function OnSaleProductList({
  categoryId,
  keyword,
  sort,
  onKeywordChange,
  onSortChange,
}: OnSaleProductListProps) {
  const location = useLocation()
  const returnLocation = `${location.pathname}${location.search}`
  const [products, setProducts] = useState<Product[]>([])
  const [searchText, setSearchText] = useState(keyword)
  const [currentPage, setCurrentPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [addingProductId, setAddingProductId] = useState<number | null>(null)

  const { isAuthenticated, role } = useAuthStore()
  const navigate = useNavigate()
  const { message } = AntdApp.useApp()

  const handleQuickAddToCart = async (product: Product) => {
    if (!isAuthenticated || role !== 'USER') {
      void message.info('请先登录账号')
      navigate(paths.login, {
        state: { from: location },
      })
      return
    }

    setAddingProductId(product.id)
    try {
      await addToCart({
        productId: product.id,
        quantity: 1,
      })
      void message.success(`已将 1 件【${product.name}】加入购物车`)
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setAddingProductId(null)
    }
  }

  useEffect(() => {
    const controller = new AbortController()

    getOnSaleProducts(
      {
        page: currentPage,
        size: PRODUCT_PAGE_SIZE,
        ...(categoryId === null ? {} : { categoryId }),
        ...(keyword ? { keyword } : {}),
        ...(sort === null ? {} : { sort }),
      },
      controller.signal,
    )
      .then((response) => {
        setProducts(Array.isArray(response.result) ? response.result : [])
        setTotal(response.total)

        if (response.pages > 0 && currentPage > response.pages) {
          setCurrentPage(response.pages)
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setFailed(true)
          setProducts([])
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      })

    return () => controller.abort()
  }, [categoryId, currentPage, keyword, reloadKey, sort])

  return (
    <section className="product-list-section" aria-labelledby="on-sale-products-title">
      <div className="product-list-heading">
        <div>
          <Typography.Title level={2} id="on-sale-products-title">
            在售商品
          </Typography.Title>
          <Typography.Text type="secondary">
            精选好物，发现适合你的商品
          </Typography.Text>
        </div>
        {!loading && !failed && total > 0 && (
          <Typography.Text type="secondary">共 {total} 件商品</Typography.Text>
        )}
      </div>

      <div className="product-search-bar" role="search">
        <Input.Search
          value={searchText}
          allowClear
          enterButton="搜索"
          maxLength={128}
          placeholder="输入商品名称关键词"
          aria-label="按商品名称搜索"
          onChange={(event) => {
            const value = event.target.value
            setSearchText(value)
            if (value === '' && keyword) {
              onKeywordChange('')
            }
          }}
          onSearch={(value) => {
            const normalizedKeyword = value.trim()
            setSearchText(normalizedKeyword)
            if (normalizedKeyword !== keyword) {
              onKeywordChange(normalizedKeyword)
            }
          }}
        />
        <Select
          className="product-sort-select"
          value={sort ?? 'DEFAULT'}
          aria-label="商品价格排序"
          options={[
            { value: 'DEFAULT', label: '综合排序' },
            { value: 'PRICE_ASC', label: '价格从低到高' },
            { value: 'PRICE_DESC', label: '价格从高到低' },
          ]}
          onChange={(value) =>
            onSortChange(
              value === 'PRICE_ASC' || value === 'PRICE_DESC' ? value : null,
            )
          }
        />
      </div>

      {loading ? (
        <div className="product-grid" aria-label="商品加载中">
          {Array.from({ length: 8 }, (_, index) => (
            <Card key={index} className="product-card product-skeleton-card">
              <Skeleton active paragraph={{ rows: 3 }} />
            </Card>
          ))}
        </div>
      ) : failed ? (
        <Alert
          type="error"
          showIcon
          message="商品加载失败"
          description="请检查网络连接或稍后重试"
          action={
            <Button
              size="small"
              onClick={() => {
                setLoading(true)
                setFailed(false)
                setReloadKey((key) => key + 1)
              }}
            >
              重新加载
            </Button>
          }
        />
      ) : products.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={
            keyword
              ? `没有找到与“${keyword}”相关的商品`
              : categoryId === null
                ? '暂无在售商品'
                : '该分类暂无在售商品'
          }
        />
      ) : (
        <>
          <div className="product-grid">
            {products.map((product) => (
              <Card
                key={product.id}
                className="product-card"
                cover={
                  <Link
                    className="product-image-link"
                    to={productDetailPath(product.id)}
                    state={{ from: returnLocation }}
                    aria-label={`查看${product.name}详情`}
                  >
                    <ProductImage product={product} />
                  </Link>
                }
              >
                <div className="product-card-content">
                  <Tag className="product-category-tag">{product.categoryName}</Tag>
                  <Typography.Title
                    level={4}
                    className="product-name"
                    title={product.name}
                  >
                    <Link
                      className="product-name-link"
                      to={productDetailPath(product.id)}
                      state={{ from: returnLocation }}
                    >
                      {product.name}
                    </Link>
                  </Typography.Title>
                  <Typography.Paragraph
                    type="secondary"
                    className="product-description"
                    ellipsis={{ rows: 2 }}
                  >
                    {product.description || '品质好物，欢迎选购'}
                  </Typography.Paragraph>

                  <div className="product-card-meta">
                    <Typography.Text className="product-price">
                      <small>¥</small>{formatPrice(product.price)}
                    </Typography.Text>
                    <Space size={6} align="center">
                      <Typography.Text type="secondary" className="product-sales">
                        已售 {product.sales}
                      </Typography.Text>
                      {product.stock === 0 && <Tag color="default">缺货</Tag>}
                    </Space>
                  </div>

                  <div className="product-card-actions">
                    <Button
                      type="primary"
                      icon={<ShoppingCartOutlined />}
                      disabled={product.stock <= 0 || addingProductId !== null}
                      loading={addingProductId === product.id}
                      onClick={() => handleQuickAddToCart(product)}
                      block
                      className="product-card-cart-btn"
                    >
                      {product.stock > 0 ? '加入购物车' : '暂时缺货'}
                    </Button>
                  </div>
                </div>
              </Card>
            ))}
          </div>

          {total > PRODUCT_PAGE_SIZE && (
            <Pagination
              className="product-pagination"
              current={currentPage}
              pageSize={PRODUCT_PAGE_SIZE}
              total={total}
              showSizeChanger={false}
              showQuickJumper={total > PRODUCT_PAGE_SIZE * 5}
              onChange={(page) => {
                setLoading(true)
                setCurrentPage(page)
                document
                  .getElementById('on-sale-products-title')
                  ?.scrollIntoView({ behavior: 'smooth', block: 'start' })
              }}
            />
          )}
        </>
      )}
    </section>
  )
}
