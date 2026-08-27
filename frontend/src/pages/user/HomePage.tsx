import { useSearchParams } from 'react-router-dom'
import { HomeCategoryNav } from '@/components/category/HomeCategoryNav'
import { OnSaleProductList } from '@/components/product/OnSaleProductList'
import type { ProductSort } from '@/api/product'

function parseCategoryId(value: string | null): number | null {
  if (value === null || !/^\d+$/.test(value)) {
    return null
  }

  const categoryId = Number(value)
  return Number.isSafeInteger(categoryId) && categoryId > 0 ? categoryId : null
}

function parseKeyword(value: string | null): string {
  return (value ?? '').trim().slice(0, 128)
}

function parseSort(value: string | null): ProductSort | null {
  return value === 'PRICE_ASC' || value === 'PRICE_DESC' ? value : null
}

export function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const selectedCategoryId = parseCategoryId(searchParams.get('categoryId'))
  const keyword = parseKeyword(searchParams.get('keyword'))
  const sort = parseSort(searchParams.get('sort'))

  const updateProductFilters = (
    categoryId: number | null,
    nextKeyword: string,
    nextSort: ProductSort | null,
  ) => {
    const nextSearchParams = new URLSearchParams()
    if (categoryId !== null) {
      nextSearchParams.set('categoryId', String(categoryId))
    }
    if (nextKeyword) {
      nextSearchParams.set('keyword', nextKeyword)
    }
    if (nextSort !== null) {
      nextSearchParams.set('sort', nextSort)
    }
    setSearchParams(nextSearchParams)
  }

  const handleCategoryChange = (categoryId: number | null) => {
    updateProductFilters(categoryId, keyword, sort)
  }

  return (
    <div className="home-page">
      <HomeCategoryNav
        selectedCategoryId={selectedCategoryId}
        onCategoryChange={handleCategoryChange}
      />
      <OnSaleProductList
        key={`${selectedCategoryId ?? 'all'}:${keyword}:${sort ?? 'default'}`}
        categoryId={selectedCategoryId}
        keyword={keyword}
        sort={sort}
        onKeywordChange={(nextKeyword) =>
          updateProductFilters(selectedCategoryId, nextKeyword, sort)
        }
        onSortChange={(nextSort) =>
          updateProductFilters(selectedCategoryId, keyword, nextSort)
        }
      />
    </div>
  )
}
