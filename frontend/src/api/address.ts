import { request } from './http'

export interface AddressItem {
  id: number
  userId: number
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: number
  createdAt: string
  updatedAt: string
}

export interface AddressCreatePayload {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: number
}

export interface AddressUpdatePayload {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: number
}

export function getAddressList(signal?: AbortSignal): Promise<AddressItem[]> {
  return request<AddressItem[]>({
    url: '/addresses',
    method: 'GET',
    signal,
  })
}

export function createAddress(payload: AddressCreatePayload): Promise<AddressItem> {
  return request<AddressItem>({
    url: '/addresses',
    method: 'POST',
    data: payload,
  })
}

export function updateAddress(
  id: number,
  payload: AddressUpdatePayload,
): Promise<AddressItem> {
  return request<AddressItem>({
    url: `/addresses/${id}/update`,
    method: 'POST',
    data: payload,
  })
}

export function setDefaultAddress(id: number): Promise<AddressItem> {
  return request<AddressItem>({
    url: `/addresses/${id}/set-default`,
    method: 'POST',
  })
}

export function deleteAddress(id: number): Promise<void> {
  return request<void>({
    url: `/addresses/${id}/delete`,
    method: 'POST',
  })
}
