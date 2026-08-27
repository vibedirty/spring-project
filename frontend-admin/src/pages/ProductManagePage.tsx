import { useCallback, useEffect, useState } from 'react'
import {
  DatabaseOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PictureOutlined,
  PlusOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import {
  Alert,
  App,
  Button,
  Card,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Image,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Radio,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { TableColumnsType } from 'antd'
import { useNavigate } from 'react-router-dom'
import { getCategoryPage, type Category } from '../services/category'
import {
  adjustProductStock,
  changeProductStatus,
  deleteProduct,
  getProductDetail,
  getProductPage,
  updateProduct,
  type Product,
  type ProductStatus,
  type ProductUpdateInput,
} from '../services/product'

const { Paragraph, Text, Title } = Typography
const { TextArea } = Input

interface ProductFilters {
  name?: string
  categoryId?: number
  status?: ProductStatus
}

interface PaginationState {
  page: number
  size: number
  total: number
}

interface StockAdjustmentForm {
  direction: 'INCREASE' | 'DECREASE'
  quantity: number
  reason: string
}

const initialPagination: PaginationState = {
  page: 1,
  size: 10,
  total: 0,
}

const imageFallback = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='112' height='112'%3E%3Crect width='112' height='112' fill='%23f3f4f6'/%3E%3Cpath d='M32 36h48v40H32z' fill='none' stroke='%239ca3af' stroke-width='4'/%3E%3Ccircle cx='45' cy='48' r='5' fill='%239ca3af'/%3E%3Cpath d='m35 69 13-13 10 10 7-7 12 10' fill='none' stroke='%239ca3af' stroke-width='4'/%3E%3C/svg%3E"

const statusMeta: Record<ProductStatus, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: 'default' },
  ON_SALE: { label: '已上架', color: 'success' },
  OFF_SALE: { label: '已下架', color: 'warning' },
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
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
    hour12: false,
  }).format(date)
}

function formatPrice(value: number) {
  return `¥${Number(value).toFixed(2)}`
}

function ProductStatusTag({ status }: { status: ProductStatus }) {
  const meta = statusMeta[status]
  return <Tag color={meta.color}>{meta.label}</Tag>
}

function ProductManagePage() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [searchForm] = Form.useForm<ProductFilters>()
  const [editForm] = Form.useForm<ProductUpdateInput>()
  const [stockForm] = Form.useForm<StockAdjustmentForm>()
  const editImageUrl = Form.useWatch('imageUrl', editForm)
  const stockDirection = Form.useWatch('direction', stockForm)
  const stockQuantity = Form.useWatch('quantity', stockForm)
  const [categories, setCategories] = useState<Category[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [filters, setFilters] = useState<ProductFilters>({})
  const [pagination, setPagination] = useState(initialPagination)
  const [loading, setLoading] = useState(true)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailProduct, setDetailProduct] = useState<Product | null>(null)
  const [editOpen, setEditOpen] = useState(false)
  const [editLoading, setEditLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [stockModalOpen, setStockModalOpen] = useState(false)
  const [adjustingStock, setAdjustingStock] = useState(false)
  const [stockProduct, setStockProduct] = useState<Product | null>(null)
  const [editingProduct, setEditingProduct] = useState<Product | null>(null)
  const [changingStatusId, setChangingStatusId] = useState<number | null>(null)
  const [deletingProductId, setDeletingProductId] = useState<number | null>(null)

  const loadProducts = useCallback(async (
    page: number,
    size: number,
    nextFilters: ProductFilters,
  ) => {
    setLoading(true)

    try {
      const data = await getProductPage({
        page,
        size,
        name: nextFilters.name,
        categoryId: nextFilters.categoryId,
        status: nextFilters.status,
      })
      setProducts(data.result)
      setPagination({ page: data.page, size: data.size, total: data.total })
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }, [message])

  useEffect(() => {
    let cancelled = false

    Promise.all([
      getProductPage({ page: 1, size: initialPagination.size }),
      getCategoryPage({ page: 1, size: 100 }),
    ])
      .then(([productData, categoryData]) => {
        if (cancelled) return
        setProducts(productData.result)
        setPagination({
          page: productData.page,
          size: productData.size,
          total: productData.total,
        })
        setCategories(categoryData.result)
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

  const handleSearch = (values: ProductFilters) => {
    const nextFilters = {
      name: values.name?.trim() || undefined,
      categoryId: values.categoryId,
      status: values.status,
    }
    setFilters(nextFilters)
    void loadProducts(1, pagination.size, nextFilters)
  }

  const handleReset = () => {
    searchForm.resetFields()
    setFilters({})
    void loadProducts(1, pagination.size, {})
  }

  const openDetail = async (id: number) => {
    setDetailProduct(null)
    setDetailOpen(true)
    setDetailLoading(true)

    try {
      setDetailProduct(await getProductDetail(id))
    } catch (error) {
      message.error(getErrorMessage(error))
      setDetailOpen(false)
    } finally {
      setDetailLoading(false)
    }
  }

  const openEdit = async (id: number) => {
    setEditingProduct(null)
    editForm.resetFields()
    setEditOpen(true)
    setEditLoading(true)

    try {
      const product = await getProductDetail(id)
      setEditingProduct(product)
      editForm.setFieldsValue({
        categoryId: product.categoryId,
        name: product.name,
        imageUrl: product.imageUrl ?? undefined,
        description: product.description ?? undefined,
        price: Number(product.price),
      })
    } catch (error) {
      message.error(getErrorMessage(error))
      setEditOpen(false)
    } finally {
      setEditLoading(false)
    }
  }

  const handleUpdate = async () => {
    if (!editingProduct) return

    let values: ProductUpdateInput
    try {
      values = await editForm.validateFields()
    } catch {
      return
    }

    setSaving(true)
    try {
      await updateProduct(editingProduct.id, {
        ...values,
        name: values.name.trim(),
        imageUrl: values.imageUrl?.trim() || undefined,
        description: values.description?.trim() || undefined,
      })
      message.success('商品信息修改成功')
      setEditOpen(false)
      setEditingProduct(null)
      editForm.resetFields()
      await loadProducts(pagination.page, pagination.size, filters)
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setSaving(false)
    }
  }

  const openStockAdjustment = (product: Product) => {
    setStockProduct(product)
    stockForm.resetFields()
    stockForm.setFieldsValue({
      direction: 'INCREASE',
      quantity: 1,
      reason: '',
    })
    setStockModalOpen(true)
  }

  const handleStockAdjustment = async () => {
    if (!stockProduct) return

    let values: StockAdjustmentForm
    try {
      values = await stockForm.validateFields()
    } catch {
      return
    }

    const changeQuantity = values.direction === 'INCREASE'
      ? values.quantity
      : -values.quantity
    setAdjustingStock(true)

    try {
      const updatedProduct = await adjustProductStock(stockProduct.id, {
        changeQuantity,
        reason: values.reason.trim(),
      })
      message.success(`库存调整成功，当前库存 ${updatedProduct.stock} 件`)
      setStockModalOpen(false)
      setStockProduct(null)
      stockForm.resetFields()
      await loadProducts(pagination.page, pagination.size, filters)
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setAdjustingStock(false)
    }
  }

  const handleChangeStatus = async (
    product: Product,
    targetStatus: 'ON_SALE' | 'OFF_SALE',
  ) => {
    setChangingStatusId(product.id)

    try {
      await changeProductStatus(product.id, targetStatus)
      message.success(targetStatus === 'ON_SALE' ? '商品已上架' : '商品已下架')
      await loadProducts(pagination.page, pagination.size, filters)
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setChangingStatusId(null)
    }
  }

  const handleDelete = async (product: Product) => {
    setDeletingProductId(product.id)

    try {
      await deleteProduct(product.id)
      message.success(`商品“${product.name}”已删除`)
      const targetPage = products.length === 1 && pagination.page > 1
        ? pagination.page - 1
        : pagination.page
      await loadProducts(targetPage, pagination.size, filters)
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setDeletingProductId(null)
    }
  }

  const columns: TableColumnsType<Product> = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    {
      title: '商品信息',
      key: 'product',
      minWidth: 250,
      render: (_, product) => (
        <Space size={12}>
          {product.imageUrl ? (
            <Image
              className="product-table-image"
              width={48}
              height={48}
              src={product.imageUrl}
              fallback={imageFallback}
              preview={false}
              alt={product.name}
            />
          ) : (
            <div className="product-table-image product-table-image-empty">
              <PictureOutlined />
            </div>
          )}
          <div className="product-table-name">
            <Text strong ellipsis={{ tooltip: product.name }}>{product.name}</Text>
            <Text type="secondary">{product.categoryName || `分类 ID：${product.categoryId}`}</Text>
          </div>
        </Space>
      ),
    },
    {
      title: '价格',
      dataIndex: 'price',
      width: 125,
      render: (price: number) => <Text strong>{formatPrice(price)}</Text>,
    },
    { title: '库存', dataIndex: 'stock', width: 90 },
    { title: '销量', dataIndex: 'sales', width: 90 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 105,
      render: (status: ProductStatus) => <ProductStatusTag status={status} />,
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 170,
      render: formatDateTime,
    },
    {
      title: '操作',
      key: 'actions',
      width: 350,
      fixed: 'right',
      render: (_, product) => {
        const targetStatus = product.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'
        const isTakingOffSale = targetStatus === 'OFF_SALE'
        return (
          <Space size={0}>
            <Button type="link" icon={<EyeOutlined />} onClick={() => void openDetail(product.id)}>
              详情
            </Button>
            <Button type="link" icon={<EditOutlined />} onClick={() => void openEdit(product.id)}>
              编辑
            </Button>
            <Button
              type="link"
              icon={<DatabaseOutlined />}
              onClick={() => openStockAdjustment(product)}
            >
              库存
            </Button>
            <Popconfirm
              title={isTakingOffSale ? '确认下架该商品？' : '确认上架该商品？'}
              description={isTakingOffSale
                ? '下架后用户端将不再展示该商品。'
                : '上架前请确认分类已启用且商品信息完整。'}
              okText={isTakingOffSale ? '确认下架' : '确认上架'}
              cancelText="取消"
              onConfirm={() => handleChangeStatus(product, targetStatus)}
            >
              <Button
                type="link"
                danger={isTakingOffSale}
                loading={changingStatusId === product.id}
              >
                {product.status === 'OFF_SALE' ? '重新上架' : isTakingOffSale ? '下架' : '上架'}
              </Button>
            </Popconfirm>
            <Popconfirm
              title="确认删除该商品？"
              description="删除后商品将从管理列表和用户端移除，此操作不能在页面中撤销。"
              okText="确认删除"
              cancelText="取消"
              okButtonProps={{ danger: true }}
              onConfirm={() => handleDelete(product)}
            >
              <Button
                type="link"
                danger
                icon={<DeleteOutlined />}
                loading={deletingProductId === product.id}
              >
                删除
              </Button>
            </Popconfirm>
          </Space>
        )
      },
    },
  ]

  return (
    <>
      <div className="page-heading product-heading">
        <div>
          <Title level={2}>商品管理</Title>
          <Text type="secondary">查询全部商品并维护商品信息和销售状态</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/products/create')}>
          创建商品
        </Button>
      </div>

      <Card className="search-card">
        <Form<ProductFilters>
          form={searchForm}
          layout="inline"
          className="product-search-form"
          onFinish={handleSearch}
        >
          <Form.Item name="name" label="商品名称">
            <Input
              allowClear
              maxLength={128}
              placeholder="请输入商品名称"
              onPressEnter={() => searchForm.submit()}
            />
          </Form.Item>
          <Form.Item name="categoryId" label="商品分类">
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="全部分类"
              options={categories.map((category) => ({
                value: category.id,
                label: category.status === 'ENABLED'
                  ? category.name
                  : `${category.name}（已停用）`,
              }))}
            />
          </Form.Item>
          <Form.Item name="status" label="商品状态">
            <Select
              allowClear
              placeholder="全部状态"
              options={Object.entries(statusMeta).map(([value, meta]) => ({
                value,
                label: meta.label,
              }))}
            />
          </Form.Item>
          <Form.Item className="product-search-actions">
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                搜索
              </Button>
              <Button onClick={handleReset}>重置</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card className="product-table-card">
        <Table<Product>
          rowKey="id"
          columns={columns}
          dataSource={products}
          loading={loading}
          scroll={{ x: 1300 }}
          locale={{ emptyText: <Empty description="暂无商品" /> }}
          pagination={{
            current: pagination.page,
            pageSize: pagination.size,
            total: pagination.total,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 件商品`,
            pageSizeOptions: [10, 20, 50, 100],
            onChange: (page, size) => {
              void loadProducts(page, size, filters)
            },
          }}
        />
      </Card>

      <Drawer
        className="product-detail-drawer"
        title="商品详情"
        width={620}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        extra={detailProduct && (
          <Space>
            <Button icon={<DatabaseOutlined />} onClick={() => {
              setDetailOpen(false)
              openStockAdjustment(detailProduct)
            }}>
              调整库存
            </Button>
            <Button icon={<EditOutlined />} onClick={() => {
              setDetailOpen(false)
              void openEdit(detailProduct.id)
            }}>
              编辑商品
            </Button>
          </Space>
        )}
      >
        <Spin spinning={detailLoading}>
          {detailProduct && (
            <div className="product-detail-content">
              <div className="product-detail-hero">
                {detailProduct.imageUrl ? (
                  <Image
                    width={136}
                    height={136}
                    src={detailProduct.imageUrl}
                    fallback={imageFallback}
                    alt={detailProduct.name}
                  />
                ) : (
                  <div className="product-detail-image-empty">
                    <PictureOutlined />
                    <Text type="secondary">暂无主图</Text>
                  </div>
                )}
                <div>
                  <Title level={3}>{detailProduct.name}</Title>
                  <Space wrap>
                    <ProductStatusTag status={detailProduct.status} />
                    <Text type="secondary">商品 ID：{detailProduct.id}</Text>
                  </Space>
                  <div className="product-detail-price">{formatPrice(detailProduct.price)}</div>
                </div>
              </div>

              <Descriptions bordered column={2} size="small">
                <Descriptions.Item label="所属分类" span={2}>
                  {detailProduct.categoryName || `分类 ID：${detailProduct.categoryId}`}
                </Descriptions.Item>
                <Descriptions.Item label="当前库存">{detailProduct.stock} 件</Descriptions.Item>
                <Descriptions.Item label="累计销量">{detailProduct.sales} 件</Descriptions.Item>
                <Descriptions.Item label="创建时间" span={2}>
                  {formatDateTime(detailProduct.createdAt)}
                </Descriptions.Item>
                <Descriptions.Item label="更新时间" span={2}>
                  {formatDateTime(detailProduct.updatedAt)}
                </Descriptions.Item>
                <Descriptions.Item label="图片 URL" span={2}>
                  {detailProduct.imageUrl || '-'}
                </Descriptions.Item>
              </Descriptions>

              <div className="product-detail-description">
                <Text strong>商品描述</Text>
                <Paragraph>{detailProduct.description || '暂无商品描述'}</Paragraph>
              </div>
            </div>
          )}
        </Spin>
      </Drawer>

      <Modal
        title="调整商品库存"
        open={stockModalOpen}
        confirmLoading={adjustingStock}
        okText="确认调整"
        cancelText="取消"
        onOk={handleStockAdjustment}
        onCancel={() => {
          setStockModalOpen(false)
          setStockProduct(null)
          stockForm.resetFields()
        }}
        forceRender
      >
        {stockProduct && (
          <div className="stock-adjustment-summary">
            <div>
              <Text type="secondary">商品</Text>
              <Text strong>{stockProduct.name}</Text>
            </div>
            <div>
              <Text type="secondary">当前库存</Text>
              <Text strong>{stockProduct.stock} 件</Text>
            </div>
            <div>
              <Text type="secondary">调整后库存</Text>
              <Text strong className="stock-adjustment-result">
                {stockDirection === 'DECREASE'
                  ? stockProduct.stock - (stockQuantity || 0)
                  : stockProduct.stock + (stockQuantity || 0)} 件
              </Text>
            </div>
          </div>
        )}

        <Form<StockAdjustmentForm>
          form={stockForm}
          layout="vertical"
          requiredMark={false}
          disabled={adjustingStock}
          className="stock-adjustment-form"
        >
          <Form.Item
            name="direction"
            label="调整方式"
            rules={[{ required: true, message: '请选择调整方式' }]}
          >
            <Radio.Group buttonStyle="solid">
              <Radio.Button value="INCREASE">增加库存</Radio.Button>
              <Radio.Button value="DECREASE">减少库存</Radio.Button>
            </Radio.Group>
          </Form.Item>
          <Form.Item
            name="quantity"
            label="调整数量"
            dependencies={['direction']}
            rules={[
              { required: true, message: '请输入调整数量' },
              {
                validator: (_, value?: number) => {
                  if (!stockProduct || value === undefined || value === null) {
                    return Promise.resolve()
                  }
                  if (!Number.isInteger(value) || value <= 0) {
                    return Promise.reject(new Error('调整数量必须为大于 0 的整数'))
                  }
                  if (stockDirection === 'DECREASE' && value > stockProduct.stock) {
                    return Promise.reject(new Error('减少数量不能超过当前库存'))
                  }
                  if (
                    stockDirection === 'INCREASE'
                    && value > 2_147_483_647 - stockProduct.stock
                  ) {
                    return Promise.reject(new Error('增加后的库存超出允许范围'))
                  }
                  return Promise.resolve()
                },
              },
            ]}
          >
            <InputNumber
              className="full-width-control"
              min={1}
              max={stockDirection === 'DECREASE'
                ? stockProduct?.stock
                : stockProduct
                  ? 2_147_483_647 - stockProduct.stock
                  : 2_147_483_647}
              precision={0}
              addonAfter="件"
              placeholder="请输入库存变动数量"
            />
          </Form.Item>
          <Form.Item
            name="reason"
            label="调整原因"
            rules={[
              { required: true, whitespace: true, message: '请输入库存调整原因' },
              { max: 255, message: '库存调整原因不能超过 255 个字符' },
            ]}
          >
            <TextArea
              rows={4}
              maxLength={255}
              showCount
              placeholder="例如：采购入库、盘点差异调整"
            />
          </Form.Item>
          <Alert
            type="info"
            showIcon
            message="库存调整会被记录"
            description="每次调整都会保存调整前后库存、变动数量和原因。"
          />
        </Form>
      </Modal>

      <Modal
        title="修改商品信息"
        width={720}
        open={editOpen}
        confirmLoading={saving}
        okText="保存修改"
        cancelText="取消"
        onOk={handleUpdate}
        onCancel={() => {
          setEditOpen(false)
          setEditingProduct(null)
          editForm.resetFields()
        }}
        forceRender
      >
        <Spin spinning={editLoading}>
          {editingProduct && (
            <Alert
              className="product-edit-alert"
              type="info"
              showIcon
              message={`正在修改商品 #${editingProduct.id}`}
              description="库存不属于商品基础信息，请通过后续库存管理功能调整。"
            />
          )}
          <Form<ProductUpdateInput>
            form={editForm}
            layout="vertical"
            requiredMark={false}
            disabled={editLoading || saving}
            className="product-edit-form"
          >
            <Form.Item
              name="categoryId"
              label="商品分类"
              rules={[{ required: true, message: '请选择商品分类' }]}
            >
              <Select
                showSearch
                optionFilterProp="label"
                placeholder="请选择商品分类"
                options={categories.map((category) => ({
                  value: category.id,
                  label: category.status === 'ENABLED'
                    ? category.name
                    : `${category.name}（已停用）`,
                }))}
              />
            </Form.Item>
            <Form.Item
              name="name"
              label="商品名称"
              rules={[
                { required: true, whitespace: true, message: '请输入商品名称' },
                { max: 128, message: '商品名称不能超过 128 个字符' },
              ]}
            >
              <Input maxLength={128} showCount placeholder="请输入商品名称" />
            </Form.Item>
            <Form.Item
              name="price"
              label="商品价格"
              rules={[{ required: true, message: '请输入商品价格' }]}
            >
              <InputNumber
                className="full-width-control"
                min={0}
                max={9_999_999_999.99}
                precision={2}
                prefix="¥"
              />
            </Form.Item>
            <Form.Item
              name="imageUrl"
              label="商品主图 URL"
              rules={[
                { max: 512, message: '图片 URL 不能超过 512 个字符' },
                { type: 'url', message: '请输入有效的图片 URL' },
              ]}
            >
              <Input allowClear maxLength={512} placeholder="https://example.com/product.jpg" />
            </Form.Item>
            {editImageUrl?.trim() && (
              <Image
                className="product-edit-image"
                width={96}
                height={96}
                src={editImageUrl.trim()}
                fallback={imageFallback}
                alt="商品主图预览"
              />
            )}
            <Form.Item
              name="description"
              label="商品描述"
              rules={[{ max: 16000, message: '商品描述不能超过 16000 个字符' }]}
            >
              <TextArea rows={5} maxLength={16000} showCount placeholder="请输入商品描述" />
            </Form.Item>
          </Form>
        </Spin>
      </Modal>
    </>
  )
}

export default ProductManagePage
