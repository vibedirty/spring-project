import { useEffect, useRef, useState } from 'react'
import { ClockCircleOutlined } from '@ant-design/icons'

interface OrderCountdownProps {
  expireAt: string | null | undefined
  onFinish?: () => void
  showIcon?: boolean
  prefixText?: string
  suffixText?: string
  className?: string
}

function parseTargetTime(expireAt: string | null | undefined): number {
  if (!expireAt) return 0
  // 兼容 ISO 8601 及普通空格分隔的时间字符串
  const normalizedStr = expireAt.includes('T') ? expireAt : expireAt.replace(' ', 'T')
  const time = new Date(normalizedStr).getTime()
  return Number.isNaN(time) ? 0 : time
}

function formatCountdown(remainSeconds: number): string {
  if (remainSeconds <= 0) {
    return '00分00秒'
  }
  const hours = Math.floor(remainSeconds / 3600)
  const minutes = Math.floor((remainSeconds % 3600) / 60)
  const seconds = remainSeconds % 60

  const pad = (num: number) => String(num).padStart(2, '0')

  if (hours > 0) {
    return `${hours}小时${pad(minutes)}分${pad(seconds)}秒`
  }
  return `${pad(minutes)}分${pad(seconds)}秒`
}

export function OrderCountdown({
  expireAt,
  onFinish,
  showIcon = true,
  prefixText = '请在',
  suffixText = '内完成支付，超时订单将自动关闭释放库存',
  className = '',
}: OrderCountdownProps) {
  const [now, setNow] = useState(() => Date.now())
  const hasFinishedRef = useRef(false)
  const onFinishRef = useRef(onFinish)

  useEffect(() => {
    onFinishRef.current = onFinish
  }, [onFinish])

  const targetTimeMs = parseTargetTime(expireAt)
  const remainSeconds =
    targetTimeMs > 0 ? Math.max(0, Math.floor((targetTimeMs - now) / 1000)) : 0

  useEffect(() => {
    if (targetTimeMs <= 0) return

    const timer = setInterval(() => {
      setNow(Date.now())
    }, 1000)

    return () => {
      clearInterval(timer)
    }
  }, [targetTimeMs])

  useEffect(() => {
    if (targetTimeMs > 0 && remainSeconds <= 0 && !hasFinishedRef.current) {
      hasFinishedRef.current = true
      onFinishRef.current?.()
    }
  }, [targetTimeMs, remainSeconds])

  const isExpired = remainSeconds <= 0
  const isUrgent = remainSeconds > 0 && remainSeconds <= 60

  return (
    <div
      className={`order-countdown-wrap ${isExpired ? 'is-expired' : ''} ${isUrgent ? 'is-urgent' : ''} ${className}`}
    >
      {showIcon && <ClockCircleOutlined className="order-countdown-icon" />}
      {isExpired ? (
        <span className="order-countdown-text">订单支付已超时，已自动关闭</span>
      ) : (
        <span className="order-countdown-text">
          {prefixText && <span className="order-countdown-prefix">{prefixText} </span>}
          <strong className="order-countdown-timer">{formatCountdown(remainSeconds)}</strong>
          {suffixText && <span className="order-countdown-suffix"> {suffixText}</span>}
        </span>
      )}
    </div>
  )
}
