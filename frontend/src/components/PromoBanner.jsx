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
    <Link
      to={to}
      className={`promo-banner ${className}`}
      style={{
        position: 'relative',
        background: '#f1f5f9',
        overflow: 'hidden',
        display: 'block',
      }}
    >
      <img
        src={src}
        alt={alt}
        width={width}
        height={height}
        loading={priority ? 'eager' : 'lazy'}
        decoding="async"
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          display: 'block',
        }}
      />
    </Link>
  )
}
