import { useEffect, useState } from 'react'
import { Alert, Card, Empty, Pagination, Skeleton, Tag, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'
import { getCategories, type Category } from '@/api/category'
import { paths } from '@/router/paths'

const CATEGORY_PAGE_SIZE = 20

export function CategoriesPage() {
  const navigate = useNavigate()
  const [categories, setCategories] = useState<Category[]>([])
  const [currentPage, setCurrentPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const controller = new AbortController()

    getCategories(controller.signal)
      .then((response) => {
        setCategories(Array.isArray(response) ? response : [])
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setFailed(true)
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      })

    return () => controller.abort()
  }, [])

  const pageStart = (currentPage - 1) * CATEGORY_PAGE_SIZE
  const visibleCategories = categories.slice(
    pageStart,
    pageStart + CATEGORY_PAGE_SIZE,
  )

  return (
    <Card className="categories-page-card" bordered={false}>
      <div className="categories-page-heading">
        <Typography.Title level={2}>全部分类</Typography.Title>
        <Typography.Paragraph type="secondary">
          选择分类，查看对应的在售商品
        </Typography.Paragraph>
      </div>

      {loading ? (
        <Skeleton active paragraph={{ rows: 4 }} />
      ) : failed ? (
        <Alert type="error" showIcon message="分类加载失败，请稍后重试" />
      ) : categories.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无可用分类" />
      ) : (
        <>
          <div className="all-category-grid">
            {visibleCategories.map((category) => (
              <Tag.CheckableTag
                key={category.id}
                checked={false}
                onChange={() =>
                  navigate(`${paths.home}?categoryId=${category.id}`)
                }
              >
                {category.name}
              </Tag.CheckableTag>
            ))}
          </div>

          {categories.length > CATEGORY_PAGE_SIZE && (
            <Pagination
              className="category-pagination"
              current={currentPage}
              pageSize={CATEGORY_PAGE_SIZE}
              total={categories.length}
              showSizeChanger={false}
              onChange={setCurrentPage}
            />
          )}
        </>
      )}
    </Card>
  )
}
