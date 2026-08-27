import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import type { UserRole } from '@/types/auth'
import { paths } from './paths'

interface RoleGuardProps {
  allowedRoles: UserRole[]
  loginPath: string
}

export function RoleGuard({ allowedRoles, loginPath }: RoleGuardProps) {
  const location = useLocation()
  const { isAuthenticated, role } = useAuthStore()

  if (!isAuthenticated) {
    return <Navigate to={loginPath} replace state={{ from: location }} />
  }

  if (!role || !allowedRoles.includes(role)) {
    return <Navigate to={paths.forbidden} replace />
  }

  return <Outlet />
}
