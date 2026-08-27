import type { PageResponse } from './types'
import { request } from './http'

export type ProductSort = 'PRICE_ASC' | 'PRICE_DESC'

export interface Product {
  id: number
  categoryId: number
  categoryName: string
  name: string
  imageUrl: string | null
  description: string | null
  price: number
  stock: number
  sales: number
  status: 'ON_SALE'
  createdAt: string
  updatedAt: string
}

export interface ProductPageQuery {
  page: number
  size: number
  categoryId?: number
  keyword?: string
  sort?: ProductSort
}

export function getOnSaleProducts(
  query: ProductPageQuery,
  signal?: AbortSignal,
): Promise<PageResponse<Product>> {
  return request<PageResponse<Product>>({
    url: '/products',
    method: 'GET',
    params: query,
    signal,
  })
}

export function getOnSaleProductDetail(
  id: number,
  signal?: AbortSignal,
): Promise<Product> {
  return request<Product>({
    url: `/products/${id}`,
    method: 'GET',
    signal,
  })
}
