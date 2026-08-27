import { Button, InputNumber, Space } from 'antd'
import { MinusOutlined, PlusOutlined } from '@ant-design/icons'

export interface QuantityInputProps {
  value: number
  min?: number
  max?: number
  size?: 'small' | 'middle' | 'large'
  disabled?: boolean
  loading?: boolean
  onChange: (value: number) => void
  className?: string
}

export function QuantityInput({
  value,
  min = 1,
  max = 99,
  size = 'middle',
  disabled = false,
  loading = false,
  onChange,
  className,
}: QuantityInputProps) {
  const handleMinus = () => {
    if (value > min && !disabled && !loading) {
      onChange(value - 1)
    }
  }

  const handlePlus = () => {
    if (value < max && !disabled && !loading) {
      onChange(value + 1)
    }
  }

  const handleInputChange = (val: number | null) => {
    if (val === null || Number.isNaN(val)) return
    const clamped = Math.max(min, Math.min(Math.floor(val), max))
    if (clamped !== value) {
      onChange(clamped)
    }
  }

  return (
    <Space.Compact size={size} className={`quantity-input-compact ${className ?? ''}`}>
      <Button
        icon={<MinusOutlined />}
        onClick={handleMinus}
        disabled={disabled || value <= min || loading}
        className="quantity-input-btn quantity-input-minus"
        aria-label="减少数量"
      />
      <InputNumber
        min={min}
        max={max}
        value={value}
        disabled={disabled || loading}
        controls={false}
        onChange={handleInputChange}
        className="quantity-input-field"
      />
      <Button
        icon={<PlusOutlined />}
        onClick={handlePlus}
        disabled={disabled || value >= max || loading}
        className="quantity-input-btn quantity-input-plus"
        aria-label="增加数量"
      />
    </Space.Compact>
  )
}
