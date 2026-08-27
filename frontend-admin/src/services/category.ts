import { request } from './http'

export type CategoryStatus = 'ENABLED' | 'DISABLED'

export interface Category {
  id: number
  name: string
  sort: number
  status: CategoryStatus
  createdAt: string
  updatedAt: string
}

export interface CategoryQuery {
  page: number
  size: number
  name?: string
  status?: CategoryStatus
}

export interface CategoryInput {
  name: string
  sort: number
  status: CategoryStatus
}

export interface CategoryPageResponse {
  result: Category[]
  page: number
  size: number
  total: number
  pages: number
  nextSort: number
}

export function getCategoryPage(
  query: CategoryQuery,
): Promise<CategoryPageResponse> {
  return request<CategoryPageResponse>({
    url: '/admin/categories',
    method: 'GET',
    params: query,
  })
}

export function createCategory(input: CategoryInput): Promise<Category> {
  return request<Category>({
    url: '/admin/categories',
    method: 'POST',
    data: input,
  })
}

export function updateCategory(
  id: number,
  input: CategoryInput,
): Promise<Category> {
  return request<Category>({
    url: `/admin/categories/${id}/update`,
    method: 'POST',
    data: input,
  })
}

export function deleteCategory(id: number): Promise<void> {
  return request<void>({
    url: `/admin/categories/${id}/delete`,
    method: 'POST',
  })
}
