import { create } from 'zustand'
import type { AuthUser, UserRole } from '@/types/auth'
import {
  clearToken,
  getAuthUser,
  getToken,
  saveAuthUser,
  saveToken,
} from '@/utils/token'

interface AuthSession {
  token: string
  user: AuthUser
}

interface AuthState {
  token: string | null
  role: UserRole | null
  user: AuthUser | null
  isAuthenticated: boolean
  setSession: (session: AuthSession) => void
  clearSession: () => void
}

const storedToken = getToken()
const storedUser = storedToken ? getAuthUser() : null

export const useAuthStore = create<AuthState>((set) => ({
  token: storedToken,
  role: storedUser?.role ?? null,
  user: storedUser,
  isAuthenticated: Boolean(storedToken),
  setSession: ({ token, user }) => {
    saveToken(token)
    saveAuthUser(user)
    set({
      token,
      role: user.role,
      user,
      isAuthenticated: true,
    })
  },
  clearSession: () => {
    clearToken()
    set({ token: null, role: null, user: null, isAuthenticated: false })
  },
}))

export function clearAuthSession(): void {
  useAuthStore.getState().clearSession()
}
