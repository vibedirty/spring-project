import type { AuthUser } from '@/types/auth'

const TOKEN_STORAGE_KEY = 'hard.auth.token'
const USER_STORAGE_KEY = 'hard.auth.user'

export function saveToken(token: string): void {
  window.localStorage.setItem(TOKEN_STORAGE_KEY, token)
}

export function saveAuthUser(user: AuthUser): void {
  window.localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
}

export function clearAuthUser(): void {
  window.localStorage.removeItem(USER_STORAGE_KEY)
}

export function getAuthUser(): AuthUser | null {
  const value = window.localStorage.getItem(USER_STORAGE_KEY)
  if (!value) {
    return null
  }

  try {
    return JSON.parse(value) as AuthUser
  } catch {
    return null
  }
}

export function getToken(): string | null {
  return window.localStorage.getItem(TOKEN_STORAGE_KEY)
}

export function clearToken(): void {
  window.localStorage.removeItem(TOKEN_STORAGE_KEY)
  clearAuthUser()
}
