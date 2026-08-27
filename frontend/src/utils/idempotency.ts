/**
 * 生成全局唯一的下单幂等 Token（长度不超过 64 个字符，无空白符）
 */
export function generateIdempotencyToken(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID().replace(/-/g, '')
  }
  return `ord_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 12)}`
}
