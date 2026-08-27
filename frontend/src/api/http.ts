import { message } from 'antd'
import axios, { type AxiosRequestConfig } from 'axios'
import { env } from '@/config/env'
import { clearAuthSession } from '@/stores/authStore'
import { getToken } from '@/utils/token'
import { ApiError, type ApiResponse, isApiResponse } from './types'

const SUCCESS_CODE = 200
const UNAUTHORIZED_CODE = 401
const FORBIDDEN_CODE = 403
const UNKNOWN_ERROR_CODE = 500

export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

function redirectToLogin(): void {
  const loginPath = window.location.pathname.startsWith('/admin')
    ? '/admin/login'
    : '/login'

  if (window.location.pathname !== loginPath) {
    const returnTo = `${window.location.pathname}${window.location.search}`
    window.location.replace(`${loginPath}?from=${encodeURIComponent(returnTo)}`)
  }
}

function isPublicAuthRequest(requestUrl?: string): boolean {
  return Boolean(
    requestUrl &&
      ['/auth/login', '/auth/register', '/admin/auth/login'].some((path) =>
        requestUrl.endsWith(path),
      ),
  )
}

function handleAuthorizationError(code: number, requestUrl?: string): void {
  if (code === UNAUTHORIZED_CODE) {
    clearAuthSession()
    redirectToLogin()
  } else if (
    code === FORBIDDEN_CODE &&
    !isPublicAuthRequest(requestUrl) &&
    window.location.pathname !== '/403'
  ) {
    window.location.replace('/403')
  }
}

apiClient.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => {
    if (!isApiResponse(response.data)) {
      const error = new ApiError(UNKNOWN_ERROR_CODE, '后端响应格式不符合约定')
      void message.error(error.message)
      return Promise.reject(error)
    }

    if (response.data.code !== SUCCESS_CODE) {
      const error = new ApiError(response.data.code, response.data.message)
      handleAuthorizationError(error.code, response.config.url)
      void message.error(error.message)
      return Promise.reject(error)
    }

    return response
  },
  (cause: unknown) => {
    if (axios.isCancel(cause)) {
      return Promise.reject(cause)
    }

    if (!axios.isAxiosError(cause)) {
      const error = new ApiError(UNKNOWN_ERROR_CODE, '请求处理失败', cause)
      void message.error(error.message)
      return Promise.reject(error)
    }

    const responseBody = cause.response?.data
    const code = isApiResponse(responseBody)
      ? responseBody.code
      : (cause.response?.status ?? UNKNOWN_ERROR_CODE)
    const errorMessage = isApiResponse(responseBody)
      ? responseBody.message
      : cause.response
        ? '服务暂时不可用，请稍后重试'
        : '无法连接到服务器，请检查网络或后端服务'
    const error = new ApiError(code, errorMessage, cause)

    handleAuthorizationError(error.code, cause.config?.url)
    void message.error(error.message)
    return Promise.reject(error)
  },
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await apiClient.request<ApiResponse<T>>(config)
  return response.data.data
}
