import axios from 'axios'

export type HealthCheckKey = 'overall' | 'application' | 'mysql' | 'redis'
export type HealthStatus =
  | 'CHECKING'
  | 'UP'
  | 'DOWN'
  | 'OUT_OF_SERVICE'
  | 'UNKNOWN'
  | 'UNREACHABLE'
  | string

export interface HealthComponent {
  status: string
}

interface ActuatorHealthResponse {
  status: string
  components?: Record<string, HealthComponent>
}

export interface HealthCheckDefinition {
  key: HealthCheckKey
  name: string
  description: string
  path: string
}

export interface HealthCheckResult extends HealthCheckDefinition {
  status: HealthStatus
  components?: Record<string, HealthComponent>
  responseTimeMs?: number
  checkedAt?: string
  error?: string
}

export const healthCheckDefinitions: HealthCheckDefinition[] = [
  {
    key: 'overall',
    name: '整体状态',
    description: '应用及全部基础依赖的聚合状态',
    path: '/health',
  },
  {
    key: 'application',
    name: '应用服务',
    description: 'Spring Boot 应用进程响应状态',
    path: '/health/application',
  },
  {
    key: 'mysql',
    name: 'MySQL',
    description: '业务数据库连接状态',
    path: '/health/mysql',
  },
  {
    key: 'redis',
    name: 'Redis',
    description: '缓存与队列服务连接状态',
    path: '/health/redis',
  },
]

export const ACTUATOR_BASE_URL = import.meta.env.VITE_ACTUATOR_BASE_URL
  ?? 'http://localhost:9000/management/spring-java/actuator'

const healthHttp = axios.create({
  baseURL: ACTUATOR_BASE_URL,
  timeout: 5_000,
  validateStatus: () => true,
})

function getHealthErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    if (error.code === 'ECONNABORTED') return '健康检查请求超时'
    return error.message || '无法连接健康检查接口'
  }
  return error instanceof Error ? error.message : '无法连接健康检查接口'
}

async function fetchHealthCheck(
  definition: HealthCheckDefinition,
): Promise<HealthCheckResult> {
  const startedAt = performance.now()

  try {
    const response = await healthHttp.get<ActuatorHealthResponse>(definition.path)
    const responseTimeMs = Math.max(0, Math.round(performance.now() - startedAt))
    const body = response.data

    if (!body || typeof body.status !== 'string') {
      throw new Error(`健康检查接口返回异常（HTTP ${response.status}）`)
    }

    return {
      ...definition,
      status: body.status.toUpperCase(),
      components: body.components,
      responseTimeMs,
      checkedAt: new Date().toISOString(),
    }
  } catch (error) {
    return {
      ...definition,
      status: 'UNREACHABLE',
      responseTimeMs: Math.max(0, Math.round(performance.now() - startedAt)),
      checkedAt: new Date().toISOString(),
      error: getHealthErrorMessage(error),
    }
  }
}

export function getHealthChecks(): Promise<HealthCheckResult[]> {
  return Promise.all(healthCheckDefinitions.map(fetchHealthCheck))
}

export function getInitialHealthChecks(): HealthCheckResult[] {
  return healthCheckDefinitions.map((definition) => ({
    ...definition,
    status: 'CHECKING',
  }))
}
