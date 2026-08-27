import { useState } from 'react'
import type { Product } from '@/api/product'

interface ProductImageProps {
  product: Product
  variant?: 'card' | 'detail'
}

export function ProductImage({
  product,
  variant = 'card',
}: ProductImageProps) {
  const [failed, setFailed] = useState(false)
  const imageClassName =
    variant === 'detail' ? 'product-detail-image' : 'product-image'
  const placeholderClassName =
    variant === 'detail'
      ? 'product-detail-image-placeholder'
      : 'product-image-placeholder'

  if (!product.imageUrl || failed) {
    return (
      <div
        className={placeholderClassName}
        role="img"
        aria-label={`${product.name}暂无图片`}
      >
        <span>Hard Store</span>
      </div>
    )
  }

  return (
    <img
      className={imageClassName}
      src={product.imageUrl}
      alt={product.name}
      loading="lazy"
      onError={() => setFailed(true)}
    />
  )
}
