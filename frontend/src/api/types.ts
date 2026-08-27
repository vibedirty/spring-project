export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  result: T[]
  page: number
  size: number
  total: number
  pages: number
}

export class ApiError extends Error {
  readonly code: number
  readonly cause?: unknown

  constructor(code: number, message: string, cause?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.cause = cause
  }
}

export function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const candidate = value as Partial<ApiResponse<unknown>>
  return typeof candidate.code === 'number' && typeof candidate.message === 'string'
}
