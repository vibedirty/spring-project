import { useEffect, useState } from 'react'
import { Alert, Skeleton, Tag, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { getCategories, type Category } from '@/api/category'
import { paths } from '@/router/paths'

const HOME_CATEGORY_PAGE_SIZE = 10

interface HomeCategoryNavProps {
  selectedCategoryId: number | null
  onCategoryChange: (categoryId: number | null) => void
}

export function HomeCategoryNav({
  selectedCategoryId,
  onCategoryChange,
}: HomeCategoryNavProps) {
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const controller = new AbortController()

    getCategories(controller.signal)
      .then((response) => {
        const allCategories = Array.isArray(response) ? response : []
        setCategories(allCategories)
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

  const primaryCategories = categories.slice(0, HOME_CATEGORY_PAGE_SIZE)
  const selectedCategory = categories.find(
    (category) => category.id === selectedCategoryId,
  )
  const visibleCategories =
    selectedCategory &&
    !primaryCategories.some((category) => category.id === selectedCategory.id)
      ? [...primaryCategories, selectedCategory]
      : primaryCategories

  return (
    <section className="home-category-section" aria-labelledby="home-category-title">
      <div className="home-category-heading">
        <Typography.Title level={4} id="home-category-title">
          商品分类
        </Typography.Title>
        <Typography.Text type="secondary">快速浏览你感兴趣的分类</Typography.Text>
      </div>

      {loading ? (
        <Skeleton.Button active block className="category-skeleton" />
      ) : failed ? (
        <Alert type="warning" showIcon message="分类加载失败，请稍后刷新页面" />
      ) : categories.length === 0 ? (
        <Typography.Text type="secondary">暂无可用分类</Typography.Text>
      ) : (
        <div className="category-tag-row">
          <Tag.CheckableTag
            checked={selectedCategoryId === null}
            onChange={() => onCategoryChange(null)}
          >
            全部商品
          </Tag.CheckableTag>
          {visibleCategories.map((category) => (
            <Tag.CheckableTag
              key={category.id}
              checked={selectedCategoryId === category.id}
              onChange={(checked) =>
                onCategoryChange(checked ? category.id : null)
              }
            >
              {category.name}
            </Tag.CheckableTag>
          ))}
          {categories.length > HOME_CATEGORY_PAGE_SIZE && (
            <Link className="category-more-tag" to={paths.categories}>
              更多
            </Link>
          )}
        </div>
      )}
    </section>
  )
}
