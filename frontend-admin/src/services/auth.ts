import {
  ADMIN_SESSION_STORAGE_KEY,
  http,
  type ApiResponse,
} from './http'

export interface LoginRequest {
  username: string
  password: string
}

export interface AdminUser {
  userId: number
  username: string
  nickname: string | null
  role: 'ADMIN'
}

export interface AdminSession {
  token: string
  user: AdminUser
}

interface LoginResponse extends AdminUser {
  token: string
}

export async function login(credentials: LoginRequest): Promise<AdminSession> {
  const response = await http.post<ApiResponse<LoginResponse>>(
    '/admin/auth/login',
    credentials,
  )
  const { token, ...user } = response.data.data
  const session = { token, user }

  localStorage.setItem(ADMIN_SESSION_STORAGE_KEY, JSON.stringify(session))
  return session
}

export async function logout(): Promise<void> {
  await http.post<ApiResponse<void>>('/admin/auth/logout')
}

export function getSession(): AdminSession | null {
  const value = localStorage.getItem(ADMIN_SESSION_STORAGE_KEY)
  if (!value) return null

  try {
    const session = JSON.parse(value) as AdminSession
    if (
      !session.token ||
      session.user?.role !== 'ADMIN'
    ) {
      clearSession()
      return null
    }
    return session
  } catch {
    clearSession()
    return null
  }
}

export function clearSession() {
  localStorage.removeItem(ADMIN_SESSION_STORAGE_KEY)
}
