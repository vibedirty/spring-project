import { request } from './http'
import type { UserRole } from '@/types/auth'

export interface LoginRequest {
  username: string
  password: string
}

export interface AuthResponse {
  userId: number
  username: string
  nickname: string
  role: UserRole
  token: string
}

export type LoginResponse = AuthResponse

export interface RegisterRequest {
  username: string
  password: string
  nickname: string
}

export type RegisterResponse = AuthResponse

export function login(payload: LoginRequest): Promise<LoginResponse> {
  return request<LoginResponse>({
    url: '/auth/login',
    method: 'POST',
    data: payload,
  })
}

export function register(payload: RegisterRequest): Promise<RegisterResponse> {
  return request<RegisterResponse>({
    url: '/auth/register',
    method: 'POST',
    data: payload,
  })
}

export function logout(): Promise<void> {
  return request<void>({
    url: '/auth/logout',
    method: 'POST',
  })
}
