import { request } from './http'

export type ProductStatus = 'DRAFT' | 'ON_SALE' | 'OFF_SALE'

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
  status: ProductStatus
  createdAt: string
  updatedAt: string
}

export interface ProductCreateInput {
  categoryId: number
  name: string
  imageUrl?: string
  description?: string
  price: number
  stock: number
}

export interface ProductUpdateInput {
  categoryId: number
  name: string
  imageUrl?: string
  description?: string
  price: number
}

export interface StockAdjustmentInput {
  changeQuantity: number
  reason: string
}

export interface ProductQuery {
  page: number
  size: number
  name?: string
  categoryId?: number
  status?: ProductStatus
}

export interface ProductPageResponse {
  result: Product[]
  page: number
  size: number
  total: number
  pages: number
}

export function getProductPage(query: ProductQuery): Promise<ProductPageResponse> {
  return request<ProductPageResponse>({
    url: '/admin/products',
    method: 'GET',
    params: query,
  })
}

export function getProductDetail(id: number): Promise<Product> {
  return request<Product>({
    url: `/admin/products/${id}`,
    method: 'GET',
  })
}

export function createProduct(input: ProductCreateInput): Promise<Product> {
  return request<Product>({
    url: '/admin/products',
    method: 'POST',
    data: input,
  })
}

export function updateProduct(
  id: number,
  input: ProductUpdateInput,
): Promise<Product> {
  return request<Product>({
    url: `/admin/products/${id}/update`,
    method: 'POST',
    data: input,
  })
}

export function changeProductStatus(
  id: number,
  status: Exclude<ProductStatus, 'DRAFT'>,
): Promise<Product> {
  return request<Product>({
    url: `/admin/products/${id}/change-status`,
    method: 'POST',
    data: { status },
  })
}

export function deleteProduct(id: number): Promise<void> {
  return request<void>({
    url: `/admin/products/${id}/delete`,
    method: 'POST',
  })
}

export function adjustProductStock(
  id: number,
  input: StockAdjustmentInput,
): Promise<Product> {
  return request<Product>({
    url: `/admin/products/${id}/stock-adjustments`,
    method: 'POST',
    data: input,
  })
}
