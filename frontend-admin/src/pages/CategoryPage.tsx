import { useCallback, useEffect, useState } from 'react'
import { DeleteOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import {
  App,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { TableColumnsType } from 'antd'
import {
  createCategory,
  deleteCategory,
  getCategoryPage,
  updateCategory,
  type Category,
  type CategoryInput,
  type CategoryStatus,
} from '../services/category'

const { Title, Text } = Typography

interface CategoryFilters {
  name?: string
  status?: CategoryStatus
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

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

function CategoryPage() {
  const { message } = App.useApp()
  const [searchForm] = Form.useForm<CategoryFilters>()
  const [categoryForm] = Form.useForm<CategoryInput>()
  const [categories, setCategories] = useState<Category[]>([])
  const [filters, setFilters] = useState<CategoryFilters>({})
  const [pagination, setPagination] = useState(initialPagination)
  const [nextSort, setNextSort] = useState(1)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [deletingCategoryId, setDeletingCategoryId] = useState<number | null>(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState<Category | null>(null)

  const loadCategories = useCallback(async (
    page: number,
    size: number,
    nextFilters: CategoryFilters,
  ) => {
    setLoading(true)

    try {
      const data = await getCategoryPage({
        page,
        size,
        name: nextFilters.name,
        status: nextFilters.status,
      })
      setCategories(data.result)
      setPagination({ page: data.page, size: data.size, total: data.total })
      setNextSort(data.nextSort ?? 1)
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }, [message])

  useEffect(() => {
    let cancelled = false

    getCategoryPage({ page: 1, size: initialPagination.size })
      .then((data) => {
        if (cancelled) return
        setCategories(data.result)
        setPagination({ page: data.page, size: data.size, total: data.total })
        setNextSort(data.nextSort ?? 1)
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

  const handleSearch = (values: CategoryFilters) => {
    const nextFilters = {
      name: values.name?.trim() || undefined,
      status: values.status,
    }
    setFilters(nextFilters)
    void loadCategories(1, pagination.size, nextFilters)
  }

  const handleReset = () => {
    searchForm.resetFields()
    setFilters({})
    void loadCategories(1, pagination.size, {})
  }

  const openCreateModal = () => {
    setEditingCategory(null)
    categoryForm.resetFields()
    categoryForm.setFieldsValue({ name: '', sort: nextSort, status: 'ENABLED' })
    setModalOpen(true)
  }

  const openEditModal = (category: Category) => {
    setEditingCategory(category)
    categoryForm.setFieldsValue({
      name: category.name,
      sort: category.sort,
      status: category.status,
    })
    setModalOpen(true)
  }

  const handleSave = async () => {
    let values: CategoryInput
    try {
      values = await categoryForm.validateFields()
    } catch {
      return
    }

    setSaving(true)

    try {
      const input = { ...values, name: values.name.trim() }
      if (editingCategory) {
        await updateCategory(editingCategory.id, input)
        message.success('分类修改成功')
      } else {
        await createCategory(input)
        message.success('分类新增成功')
      }

      setModalOpen(false)
      setEditingCategory(null)
      categoryForm.resetFields()
      await loadCategories(pagination.page, pagination.size, filters)
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (category: Category) => {
    setDeletingCategoryId(category.id)

    try {
      await deleteCategory(category.id)
      message.success(`分类“${category.name}”已删除`)
      const targetPage = categories.length === 1 && pagination.page > 1
        ? pagination.page - 1
        : pagination.page
      await loadCategories(targetPage, pagination.size, filters)
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setDeletingCategoryId(null)
    }
  }

  const columns: TableColumnsType<Category> = [
    { title: 'ID', dataIndex: 'id', width: 90 },
    { title: '分类名称', dataIndex: 'name', minWidth: 160 },
    { title: '排序', dataIndex: 'sort', width: 100 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (status: CategoryStatus) => (
        <Tag color={status === 'ENABLED' ? 'success' : 'default'}>
          {status === 'ENABLED' ? '启用' : '停用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
      render: formatDateTime,
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 180,
      render: formatDateTime,
    },
    {
      title: '操作',
      key: 'actions',
      width: 160,
      fixed: 'right',
      render: (_, category) => (
        <Space size={0}>
          <Button type="link" onClick={() => openEditModal(category)}>
            编辑
          </Button>
          <Popconfirm
            title="确认删除该分类？"
            description="删除后分类将不再展示；分类下存在商品时无法删除。"
            okText="确认删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleDelete(category)}
          >
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              loading={deletingCategoryId === category.id}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <>
      <div className="page-heading category-heading">
        <div>
          <Title level={2}>分类管理</Title>
          <Text type="secondary">维护商品分类、排序和启用状态</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
          新增分类
        </Button>
      </div>

      <Card className="search-card">
        <Form<CategoryFilters>
          form={searchForm}
          layout="inline"
          onFinish={handleSearch}
          className="category-search-form"
        >
          <Form.Item name="name" label="分类名称">
            <Input
              allowClear
              maxLength={64}
              placeholder="请输入分类名称"
              onPressEnter={() => searchForm.submit()}
            />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              allowClear
              placeholder="全部状态"
              options={[
                { value: 'ENABLED', label: '启用' },
                { value: 'DISABLED', label: '停用' },
              ]}
            />
          </Form.Item>
          <Form.Item className="category-search-actions">
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                搜索
              </Button>
              <Button onClick={handleReset}>重置</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card className="category-table-card">
        <Table<Category>
          rowKey="id"
          columns={columns}
          dataSource={categories}
          loading={loading}
          scroll={{ x: 1020 }}
          pagination={{
            current: pagination.page,
            pageSize: pagination.size,
            total: pagination.total,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            pageSizeOptions: [10, 20, 50, 100],
            onChange: (page, size) => {
              void loadCategories(page, size, filters)
            },
          }}
        />
      </Card>

      <Modal
        title={editingCategory ? '修改分类' : '新增分类'}
        open={modalOpen}
        confirmLoading={saving}
        okText="保存"
        cancelText="取消"
        onOk={handleSave}
        onCancel={() => {
          setModalOpen(false)
          setEditingCategory(null)
          categoryForm.resetFields()
        }}
        forceRender
      >
        <Form<CategoryInput>
          form={categoryForm}
          layout="vertical"
          requiredMark={false}
          className="category-modal-form"
        >
          <Form.Item
            name="name"
            label="分类名称"
            rules={[
              { required: true, whitespace: true, message: '请输入分类名称' },
              { max: 64, message: '分类名称不能超过 64 个字符' },
            ]}
          >
            <Input placeholder="请输入分类名称" maxLength={64} showCount />
          </Form.Item>
          <Form.Item
            name="sort"
            label="排序值"
            tooltip="数值越小，展示位置越靠前"
            rules={[{ required: true, message: '请输入排序值' }]}
          >
            <InputNumber min={0} precision={0} className="full-width-control" />
          </Form.Item>
          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择分类状态' }]}
          >
            <Select
              options={[
                { value: 'ENABLED', label: '启用' },
                { value: 'DISABLED', label: '停用' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}

export default CategoryPage
