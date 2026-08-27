import axios, { type AxiosRequestConfig } from 'axios'

export const ADMIN_SESSION_STORAGE_KEY = 'admin_session'
export const ADMIN_SESSION_CHANGE_EVENT = 'admin-session-changed'

function clearStoredSession() {
  localStorage.removeItem(ADMIN_SESSION_STORAGE_KEY)
  window.dispatchEvent(new Event(ADMIN_SESSION_CHANGE_EVENT))
}

export interface ApiResponse<T> {
  code: number
  data: T
  message: string
}

function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  if (typeof value !== 'object' || value === null) return false

  const response = value as Partial<ApiResponse<unknown>>
  return (
    typeof response.code === 'number'
    && typeof response.message === 'string'
    && 'data' in response
  )
}

export class ApiError extends Error {
  readonly code?: number

  constructor(message: string, code?: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:9000/api',
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const storedSession = localStorage.getItem(ADMIN_SESSION_STORAGE_KEY)

  if (storedSession) {
    try {
      const { token } = JSON.parse(storedSession) as { token?: string }
      if (token) config.headers.Authorization = `Bearer ${token}`
    } catch {
      clearStoredSession()
    }
  }

  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as Partial<ApiResponse<unknown>>

    if (typeof body.code === 'number' && body.code !== 200) {
      if (body.code === 401) clearStoredSession()
      return Promise.reject(new ApiError(body.message ?? '请求失败', body.code))
    }

    return response
  },
  (error: unknown) => {
    if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
      const code = error.response?.data?.code ?? error.response?.status
      if (code === 401) clearStoredSession()
      const message = error.response?.data?.message
        ?? (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络异常，请稍后重试')
      return Promise.reject(new ApiError(message, code))
    }

    return Promise.reject(error)
  },
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResponse<T>>(config)

  if (!isApiResponse(response.data)) {
    throw new ApiError('后端响应格式不符合约定')
  }

  return response.data.data
}
