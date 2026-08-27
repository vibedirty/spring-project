import { request } from './http'

export interface StockLog {
  id: number
  productId: number
  productName: string | null
  changeQuantity: number
  beforeStock: number
  afterStock: number
  reason: string
  businessNo: string | null
  createdAt: string
}

export interface StockLogQuery {
  page: number
  size: number
}

export interface StockLogPageResponse {
  result: StockLog[]
  page: number
  size: number
  total: number
  pages: number
}

export function getStockLogPage(
  query: StockLogQuery,
): Promise<StockLogPageResponse> {
  return request<StockLogPageResponse>({
    url: '/admin/stock',
    method: 'GET',
    params: query,
  })
}
