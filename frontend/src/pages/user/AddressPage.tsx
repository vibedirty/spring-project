import { useEffect, useState } from 'react'
import {
  App as AntdApp,
  Button,
  Card,
  Col,
  Empty,
  Form,
  Input,
  Modal,
  Popconfirm,
  Row,
  Skeleton,
  Space,
  Switch,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  createAddress,
  deleteAddress,
  getAddressList,
  setDefaultAddress,
  updateAddress,
  type AddressCreatePayload,
  type AddressItem,
  type AddressUpdatePayload,
} from '@/api/address'

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

export function AddressPage() {
  const [addresses, setAddresses] = useState<AddressItem[]>([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingAddress, setEditingAddress] = useState<AddressItem | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)
  const [form] = Form.useForm<AddressFormFields>()
  const { message } = AntdApp.useApp()

  const loadAddresses = async (signal?: AbortSignal) => {
    try {
      const data = await getAddressList(signal)
      setAddresses(Array.isArray(data) ? data : [])
    } catch {
      // 错误由 http 拦截器统一处理
    } finally {
      if (!signal?.aborted) {
        setLoading(false)
      }
    }
  }

  useEffect(() => {
    const controller = new AbortController()

    getAddressList(controller.signal)
      .then((data) => {
        setAddresses(Array.isArray(data) ? data : [])
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

  const handleOpenCreateModal = () => {
    setEditingAddress(null)
    form.resetFields()
    form.setFieldsValue({
      isDefault: addresses.length === 0,
    })
    setModalOpen(true)
  }

  const handleOpenEditModal = (address: AddressItem) => {
    setEditingAddress(address)
    form.resetFields()
    form.setFieldsValue({
      receiverName: address.receiverName,
      phone: address.phone,
      province: address.province,
      city: address.city,
      district: address.district,
      detailAddress: address.detailAddress,
      isDefault: address.isDefault === 1,
    })
    setModalOpen(true)
  }

  const handleCloseModal = () => {
    if (submitting) return
    setModalOpen(false)
    setEditingAddress(null)
    form.resetFields()
  }

  const handleSubmit = async (values: AddressFormFields) => {
    setSubmitting(true)
    try {
      const payload: AddressCreatePayload | AddressUpdatePayload = {
        receiverName: values.receiverName.trim(),
        phone: values.phone.trim(),
        province: values.province.trim(),
        city: values.city.trim(),
        district: values.district.trim(),
        detailAddress: values.detailAddress.trim(),
        isDefault: values.isDefault ? 1 : 0,
      }

      if (editingAddress) {
        await updateAddress(editingAddress.id, payload)
        void message.success('收货地址修改成功')
      } else {
        await createAddress(payload)
        void message.success('收货地址创建成功')
      }

      setModalOpen(false)
      setEditingAddress(null)
      form.resetFields()
      await loadAddresses()
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setSubmitting(false)
    }
  }

  const handleSetDefault = async (id: number) => {
    setActionLoadingId(id)
    try {
      await setDefaultAddress(id)
      void message.success('已设为默认地址')
      await loadAddresses()
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setActionLoadingId(null)
    }
  }

  const handleDelete = async (address: AddressItem) => {
    if (address.isDefault === 1) {
      void message.warning('默认地址不能直接删除，请先将其他地址设为默认')
      return
    }

    setActionLoadingId(address.id)
    try {
      await deleteAddress(address.id)
      void message.success('收货地址已删除')
      await loadAddresses()
    } catch {
      // 错误由 http 拦截器处理
    } finally {
      setActionLoadingId(null)
    }
  }

  return (
    <div className="address-page">
      <div className="address-page-header">
        <div>
          <Typography.Title level={2} className="address-page-title">
            收货地址管理
          </Typography.Title>
          <Typography.Paragraph type="secondary" className="address-page-desc">
            管理您的常用收货地址，下单时可快速选择
          </Typography.Paragraph>
        </div>
        <Button type="primary" size="large" onClick={handleOpenCreateModal}>
          + 新增收货地址
        </Button>
      </div>

      {loading ? (
        <Row gutter={[20, 20]}>
          {[1, 2, 3, 4].map((item) => (
            <Col xs={24} sm={24} md={12} key={item}>
              <Card className="address-card" bordered={false}>
                <Skeleton active paragraph={{ rows: 3 }} />
              </Card>
            </Col>
          ))}
        </Row>
      ) : addresses.length === 0 ? (
        <Card className="address-empty-card" bordered={false}>
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="暂无收货地址，快去添加一个吧"
          >
            <Button type="primary" onClick={handleOpenCreateModal}>
              立即添加
            </Button>
          </Empty>
        </Card>
      ) : (
        <Row gutter={[20, 20]} className="address-grid">
          {addresses.map((address) => (
            <Col xs={24} sm={24} md={12} key={address.id}>
              <Card
                className={`address-card ${address.isDefault === 1 ? 'address-card-default' : ''}`}
                bordered={false}
              >
                <div className="address-card-header">
                  <Space size={10} align="center">
                    <Typography.Text strong className="address-receiver-name">
                      {address.receiverName}
                    </Typography.Text>
                    <Typography.Text type="secondary" className="address-phone">
                      {address.phone}
                    </Typography.Text>
                  </Space>
                  {address.isDefault === 1 && (
                    <Tag color="blue" className="address-default-tag">
                      默认地址
                    </Tag>
                  )}
                </div>

                <div className="address-card-body">
                  <div className="address-region">
                    {address.province} {address.city} {address.district}
                  </div>
                  <div className="address-detail">{address.detailAddress}</div>
                </div>

                <div className="address-card-footer">
                  <Typography.Text type="secondary" className="address-created-at">
                    添加于 {address.createdAt ? address.createdAt.replace('T', ' ').slice(0, 16) : '-'}
                  </Typography.Text>

                  <Space size={4} className="address-card-actions">
                    {address.isDefault !== 1 && (
                      <Button
                        type="link"
                        size="small"
                        className="address-action-btn"
                        onClick={() => handleSetDefault(address.id)}
                        loading={actionLoadingId === address.id}
                        disabled={actionLoadingId !== null}
                      >
                        设为默认
                      </Button>
                    )}
                    <Button
                      type="link"
                      size="small"
                      className="address-action-btn"
                      onClick={() => handleOpenEditModal(address)}
                      disabled={actionLoadingId !== null}
                    >
                      编辑
                    </Button>
                    {address.isDefault === 1 ? (
                      <Tooltip title="默认地址不能直接删除，请先将其他地址设为默认">
                        <Button
                          type="link"
                          size="small"
                          danger
                          className="address-action-btn"
                          disabled
                        >
                          删除
                        </Button>
                      </Tooltip>
                    ) : (
                      <Popconfirm
                        title="删除收货地址"
                        description="确定要删除该收货地址吗？"
                        okText="删除"
                        cancelText="取消"
                        okButtonProps={{ danger: true }}
                        onConfirm={() => handleDelete(address)}
                        disabled={actionLoadingId !== null}
                      >
                        <Button
                          type="link"
                          size="small"
                          danger
                          className="address-action-btn"
                          loading={actionLoadingId === address.id}
                          disabled={actionLoadingId !== null}
                        >
                          删除
                        </Button>
                      </Popconfirm>
                    )}
                  </Space>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      <Modal
        title={editingAddress ? '编辑收货地址' : '新增收货地址'}
        open={modalOpen}
        onCancel={handleCloseModal}
        footer={null}
        destroyOnClose
        centered
        width={560}
      >
        <Form
          form={form}
          layout="vertical"
          requiredMark="optional"
          onFinish={handleSubmit}
          disabled={submitting}
          className="address-form"
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
            className="address-form-switch"
            extra={
              editingAddress?.isDefault === 1
                ? '当前地址为默认地址，如需更换请将其他地址设为默认'
                : undefined
            }
          >
            <Switch disabled={editingAddress?.isDefault === 1} />
          </Form.Item>

          <div className="address-form-actions">
            <Space size={12}>
              <Button onClick={handleCloseModal} disabled={submitting}>
                取消
              </Button>
              <Button type="primary" htmlType="submit" loading={submitting}>
                {editingAddress ? '保存修改' : '保存并使用'}
              </Button>
            </Space>
          </div>
        </Form>
      </Modal>
    </div>
  )
}
