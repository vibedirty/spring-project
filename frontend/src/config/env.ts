const DEFAULT_API_BASE_URL = 'http://localhost:9000/api'

export const env = Object.freeze({
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL,
})
