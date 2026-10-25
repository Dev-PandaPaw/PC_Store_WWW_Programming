import React from 'react'
import { Link } from 'react-router-dom'

export default function PromoBanner({
  to,
  src,
  alt,
  width,
  height,
  className = '',
  priority = false,
}) {
  return (
    <Link to={to} className={`promo-banner ${className}`}>
      <img
        src={src}
        alt={alt}
        width={width}
        height={height}
        loading={priority ? 'eager' : 'lazy'}
        decoding="async"
      />
    </Link>
  )
}
