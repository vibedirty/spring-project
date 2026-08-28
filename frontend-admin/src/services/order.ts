import { request } from './http'

export type OrderStatus =
  | 'PENDING_STOCK'
  | 'PENDING_PAYMENT'
  | 'CANCELLING'
  | 'PENDING_SHIPMENT'
  | 'SHIPPED'
  | 'COMPLETED'
  | 'CANCELLED'

export interface OrderItemSummary {
  productId: number
  productName: string
  productImageUrl: string | null
  unitPrice: number
  quantity: number
  subtotalAmount: number
}

export interface OrderAddress {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
}

export interface OrderOperateLog {
  operatorType: 'USER' | 'ADMIN' | 'SYSTEM' | string
  operatorName: string
  operation: 'CREATE' | 'PAY' | 'SHIP' | 'COMPLETE' | 'CANCEL' | string
  fromStatus: OrderStatus | null
  toStatus: OrderStatus | null
  reason: string | null
  createdAt: string
}

export interface OrderListItem {
  id: number
  orderNo: string
  userId: number
  totalAmount: number
  status: OrderStatus
  shippingCompany: string | null
  trackingNumber: string | null
  expireAt: string | null
  paidAt: string | null
  shippedAt: string | null
  completedAt: string | null
  cancelledAt: string | null
  createdAt: string
  updatedAt: string
}

export interface OrderDetail {
  orderNo: string
  userId: number
  status: OrderStatus
  statusDescription: string
  totalAmount: number
  shippingCompany: string | null
  trackingNumber: string | null
  expireAt: string | null
  paidAt: string | null
  shippedAt: string | null
  completedAt: string | null
  cancelledAt: string | null
  createdAt: string
  items: OrderItemSummary[]
  address: OrderAddress | null
  operateLogs: OrderOperateLog[]
}

export interface OrderShipmentInput {
  shippingCompany: string
  trackingNumber: string
}

export interface OrderQuery {
  page: number
  size: number
  orderNo?: string
  userId?: number
  status?: OrderStatus
  startTime?: string
  endTime?: string
}

export interface OrderPageResponse {
  result: OrderListItem[]
  page: number
  size: number
  total: number
  pages: number
}

export function getOrderPage(query: OrderQuery): Promise<OrderPageResponse> {
  return request<OrderPageResponse>({
    url: '/admin/orders',
    method: 'GET',
    params: query,
  })
}

export function getOrderDetail(orderNo: string): Promise<OrderDetail> {
  return request<OrderDetail>({
    url: `/admin/orders/${encodeURIComponent(orderNo)}`,
    method: 'GET',
  })
}

export function shipOrder(
  orderNo: string,
  input: OrderShipmentInput,
): Promise<OrderListItem> {
  return request<OrderListItem>({
    url: `/admin/orders/${encodeURIComponent(orderNo)}/ship`,
    method: 'POST',
    data: input,
  })
}
