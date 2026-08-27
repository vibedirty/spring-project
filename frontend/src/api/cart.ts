import { request } from './http'

export interface CartItem {
  productId: number | string
  quantity: number
  selected: boolean
  addedAt: string
}

export interface CartItemAddPayload {
  productId: number
  quantity: number
}

export interface CartItemUpdatePayload {
  quantity?: number
  selected?: boolean
}

export interface CartItemResponse {
  productId: number
  productName: string | null
  imageUrl: string | null
  price: number | null
  stock: number | null
  productStatus: 'ON_SALE' | 'OFF_SALE' | string | null
  quantity: number
  selected: boolean
  valid: boolean
  invalidReason: string | null
}

export interface CartResponse {
  items: CartItemResponse[]
  selectedAmount: number
}

export function addToCart(payload: CartItemAddPayload): Promise<CartItem> {
  return request<CartItem>({
    url: '/cart/items',
    method: 'POST',
    data: payload,
  })
}

export function getCart(signal?: AbortSignal): Promise<CartResponse> {
  return request<CartResponse>({
    url: '/cart',
    method: 'GET',
    signal,
  })
}

export function updateCartItem(
  productId: number,
  payload: CartItemUpdatePayload,
): Promise<CartItem> {
  return request<CartItem>({
    url: `/cart/items/${productId}/update`,
    method: 'POST',
    data: payload,
  })
}

export function deleteCartItem(productId: number): Promise<void> {
  return request<void>({
    url: `/cart/items/${productId}/delete`,
    method: 'POST',
  })
}
