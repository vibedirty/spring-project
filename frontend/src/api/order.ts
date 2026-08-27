import { request } from './http'
import type { PageResponse } from './types'

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PENDING_SHIPMENT'
  | 'SHIPPED'
  | 'COMPLETED'
  | 'CANCELLED'

export interface OrderItemSummaryResponse {
  productId: number
  productName: string
  productImageUrl: string | null
  unitPrice: number
  quantity: number
  subtotalAmount: number
}

export interface OrderAddressResponse {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
}

export interface OrderOperateLogResponse {
  operatorType: 'USER' | 'ADMIN' | 'SYSTEM' | string
  operatorName: string
  operation: 'CREATE' | 'PAY' | 'SHIP' | 'COMPLETE' | 'CANCEL' | string
  fromStatus: OrderStatus | null
  toStatus: OrderStatus | null
  reason: string | null
  createdAt: string
}

export interface OrderListResponse {
  orderNo: string
  status: OrderStatus
  statusDescription: string
  totalAmount: number
  createdAt: string
  expireAt: string
  items: OrderItemSummaryResponse[]
}

export interface OrderDetailResponse {
  orderNo: string
  status: OrderStatus
  statusDescription: string
  totalAmount: number
  expireAt: string | null
  paidAt: string | null
  shippingCompany: string | null
  trackingNumber: string | null
  shippedAt: string | null
  completedAt: string | null
  cancelledAt: string | null
  createdAt: string
  items: OrderItemSummaryResponse[]
  address: OrderAddressResponse | null
  operateLogs: OrderOperateLogResponse[]
}

export interface OrderListQuery {
  page?: number
  size?: number
  status?: OrderStatus
}

export interface OrderCreatePayload {
  addressId: number
  idempotencyToken?: string
}

export interface OrderCreateResponse {
  orderNo: string
  totalAmount: number
  status: OrderStatus | string
  expireAt: string
}

export function createOrder(payload: OrderCreatePayload): Promise<OrderCreateResponse> {
  return request<OrderCreateResponse>({
    url: '/orders',
    method: 'POST',
    data: payload,
  })
}

export function getOrderList(
  params?: OrderListQuery,
  signal?: AbortSignal,
): Promise<PageResponse<OrderListResponse>> {
  return request<PageResponse<OrderListResponse>>({
    url: '/orders',
    method: 'GET',
    params,
    signal,
  })
}

export function getOrderDetail(
  orderNo: string,
  signal?: AbortSignal,
): Promise<OrderDetailResponse> {
  return request<OrderDetailResponse>({
    url: `/orders/${encodeURIComponent(orderNo)}`,
    method: 'GET',
    signal,
  })
}

export function cancelOrder(orderNo: string): Promise<void> {
  return request<void>({
    url: `/orders/${encodeURIComponent(orderNo)}/cancel`,
    method: 'POST',
  })
}

export function payOrder(orderNo: string): Promise<void> {
  return request<void>({
    url: `/orders/${encodeURIComponent(orderNo)}/pay`,
    method: 'POST',
  })
}

export function confirmReceipt(orderNo: string): Promise<void> {
  return request<void>({
    url: `/orders/${encodeURIComponent(orderNo)}/confirm-receipt`,
    method: 'POST',
  })
}
