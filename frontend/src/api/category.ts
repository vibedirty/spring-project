import { request } from './http'

export interface Category {
  id: number
  name: string
  sort: number
  status: 'ENABLED'
  createdAt: string
  updatedAt: string
}

export function getCategories(
  signal?: AbortSignal,
): Promise<Category[]> {
  return request<Category[]>({
    url: '/categories',
    method: 'GET',
    signal,
  })
}
