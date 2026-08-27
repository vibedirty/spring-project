import { useEffect, useState } from 'react'
import {
  CheckCircleFilled,
  InboxOutlined,
  LinkOutlined,
  PlusOutlined,
  ReloadOutlined,
  ShoppingOutlined,
} from '@ant-design/icons'
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Form,
  Image,
  Input,
  InputNumber,
  Result,
  Row,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
} from 'antd'
import { getCategoryPage, type Category } from '../services/category'
import {
  createProduct,
  type Product,
  type ProductCreateInput,
} from '../services/product'

const { Text, Title } = Typography
const { TextArea } = Input

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

function formatDateTime(value: string) {
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

function ProductPage() {
  const { message } = App.useApp()
  const [form] = Form.useForm<ProductCreateInput>()
  const imageUrl = Form.useWatch('imageUrl', form)
  const [categories, setCategories] = useState<Category[]>([])
  const [loadingCategories, setLoadingCategories] = useState(true)
  const [saving, setSaving] = useState(false)
  const [createdProduct, setCreatedProduct] = useState<Product | null>(null)

  const loadCategories = async () => {
    setLoadingCategories(true)

    try {
      const data = await getCategoryPage({ page: 1, size: 100 })
      setCategories(data.result)
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setLoadingCategories(false)
    }
  }

  useEffect(() => {
    let cancelled = false

    getCategoryPage({ page: 1, size: 100 })
      .then((data) => {
        if (!cancelled) setCategories(data.result)
      })
      .catch((error: unknown) => {
        if (!cancelled) message.error(getErrorMessage(error))
      })
      .finally(() => {
        if (!cancelled) setLoadingCategories(false)
      })

    return () => {
      cancelled = true
    }
  }, [message])

  const handleSubmit = async (values: ProductCreateInput) => {
    setSaving(true)

    try {
      const product = await createProduct({
        ...values,
        name: values.name.trim(),
        imageUrl: values.imageUrl?.trim() || undefined,
        description: values.description?.trim() || undefined,
      })
      setCreatedProduct(product)
      message.success('商品创建成功，已保存为草稿')
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setSaving(false)
    }
  }

  const resetForNextProduct = () => {
    form.resetFields()
    setCreatedProduct(null)
  }

  const hasCategories = categories.length > 0

  return (
    <>
      <div className="page-heading product-heading">
        <div>
          <Title level={2}>创建商品</Title>
          <Text type="secondary">录入商品基础信息、售价和初始库存</Text>
        </div>
        <Tag color="processing" icon={<InboxOutlined />}>新商品默认为草稿</Tag>
      </div>

      <Row gutter={[20, 20]} align="top">
        <Col xs={24} xl={createdProduct ? 15 : 18} xxl={createdProduct ? 16 : 18}>
          <Card
            className="product-form-card"
            title={
              <Space>
                <ShoppingOutlined />
                <span>创建商品</span>
              </Space>
            }
            extra={<Text type="secondary">带 * 的字段为必填项</Text>}
          >
            {!loadingCategories && !hasCategories && (
              <Alert
                className="product-category-alert"
                type="warning"
                showIcon
                message="暂无可选分类"
                description="创建商品前，请先在分类管理中新增一个商品分类。"
                action={
                  <Button size="small" icon={<ReloadOutlined />} onClick={loadCategories}>
                    重新加载
                  </Button>
                }
              />
            )}

            <Spin spinning={loadingCategories} tip="正在加载分类">
              <Form<ProductCreateInput>
                form={form}
                layout="vertical"
                requiredMark
                initialValues={{ stock: 0 }}
                disabled={saving}
                onFinish={handleSubmit}
                scrollToFirstError={{ behavior: 'smooth', block: 'center' }}
              >
                <section className="product-form-section">
                  <div className="product-section-heading">
                    <Text strong>基础信息</Text>
                    <Text type="secondary">选择分类并填写便于识别的商品名称</Text>
                  </div>

                  <Row gutter={16}>
                    <Col xs={24} md={10}>
                      <Form.Item
                        name="categoryId"
                        label="商品分类"
                        rules={[{ required: true, message: '请选择商品分类' }]}
                      >
                        <Select
                          showSearch
                          optionFilterProp="label"
                          placeholder="请选择商品分类"
                          notFoundContent={<Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无分类" />}
                          options={categories.map((category) => ({
                            value: category.id,
                            label: category.status === 'ENABLED'
                              ? category.name
                              : `${category.name}（已停用）`,
                          }))}
                        />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={14}>
                      <Form.Item
                        name="name"
                        label="商品名称"
                        rules={[
                          { required: true, whitespace: true, message: '请输入商品名称' },
                          { max: 128, message: '商品名称不能超过 128 个字符' },
                        ]}
                      >
                        <Input placeholder="例如：轻量保温杯 500ml" maxLength={128} showCount />
                      </Form.Item>
                    </Col>
                  </Row>
                </section>

                <section className="product-form-section">
                  <div className="product-section-heading">
                    <Text strong>销售与库存</Text>
                    <Text type="secondary">金额最多保留两位小数，库存仅支持整数</Text>
                  </div>

                  <Row gutter={16}>
                    <Col xs={24} md={12}>
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
                          placeholder="0.00"
                        />
                      </Form.Item>
                    </Col>
                    <Col xs={24} md={12}>
                      <Form.Item
                        name="stock"
                        label="初始库存"
                        rules={[{ required: true, message: '请输入初始库存' }]}
                      >
                        <InputNumber
                          className="full-width-control"
                          min={0}
                          max={2_147_483_647}
                          precision={0}
                          addonAfter="件"
                        />
                      </Form.Item>
                    </Col>
                  </Row>
                </section>

                <section className="product-form-section">
                  <div className="product-section-heading">
                    <Text strong>图文信息</Text>
                    <Text type="secondary">第一版使用在线图片 URL，无需上传文件</Text>
                  </div>

                  <Form.Item
                    name="imageUrl"
                    label="商品主图 URL"
                    rules={[
                      { max: 512, message: '图片 URL 不能超过 512 个字符' },
                      { type: 'url', message: '请输入以 http:// 或 https:// 开头的有效 URL' },
                    ]}
                  >
                    <Input
                      prefix={<LinkOutlined />}
                      placeholder="https://example.com/product.jpg"
                      maxLength={512}
                      allowClear
                    />
                  </Form.Item>

                  {imageUrl?.trim() && (
                    <div className="product-image-preview">
                      <Image
                        width={112}
                        height={112}
                        src={imageUrl.trim()}
                        alt="商品主图预览"
                        fallback="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='112' height='112'%3E%3Crect width='112' height='112' fill='%23f3f4f6'/%3E%3Ctext x='56' y='58' text-anchor='middle' fill='%239ca3af' font-size='12'%3E无法预览%3C/text%3E%3C/svg%3E"
                      />
                      <Text type="secondary">主图预览</Text>
                    </div>
                  )}

                  <Form.Item
                    name="description"
                    label="商品描述"
                    rules={[{ max: 16000, message: '商品描述不能超过 16000 个字符' }]}
                  >
                    <TextArea
                      rows={7}
                      placeholder="请输入商品卖点、材质、规格或使用说明"
                      maxLength={16000}
                      showCount
                    />
                  </Form.Item>
                </section>

                <div className="product-form-actions">
                  <Button onClick={resetForNextProduct} disabled={saving}>
                    重置
                  </Button>
                  <Button
                    type="primary"
                    htmlType="submit"
                    icon={<PlusOutlined />}
                    loading={saving}
                    disabled={!hasCategories}
                  >
                    创建商品
                  </Button>
                </div>
              </Form>
            </Spin>
          </Card>
        </Col>

        <Col xs={24} xl={createdProduct ? 9 : 6} xxl={createdProduct ? 8 : 6}>
          {createdProduct ? (
            <Card className="product-result-card">
              <Result
                status="success"
                icon={<CheckCircleFilled />}
                title="商品创建成功"
                subTitle={`商品 ID：${createdProduct.id} · 当前状态：草稿`}
              />
              <Descriptions column={1} size="small" bordered>
                <Descriptions.Item label="商品名称">{createdProduct.name}</Descriptions.Item>
                <Descriptions.Item label="所属分类">{createdProduct.categoryName}</Descriptions.Item>
                <Descriptions.Item label="销售价格">¥ {Number(createdProduct.price).toFixed(2)}</Descriptions.Item>
                <Descriptions.Item label="初始库存">{createdProduct.stock} 件</Descriptions.Item>
                <Descriptions.Item label="创建时间">{formatDateTime(createdProduct.createdAt)}</Descriptions.Item>
              </Descriptions>
              <Button
                className="product-create-another"
                type="primary"
                block
                onClick={resetForNextProduct}
              >
                继续创建商品
              </Button>
            </Card>
          ) : (
            <Card className="product-guide-card" title="创建说明">
              <ol>
                <li>先选择已经存在的商品分类。</li>
                <li>价格与库存创建后会直接保存。</li>
                <li>新商品统一保存为草稿，不会立即展示给用户。</li>
                <li>图片仅保存 URL，请确保地址可公开访问。</li>
              </ol>
            </Card>
          )}
        </Col>
      </Row>
    </>
  )
}

export default ProductPage
