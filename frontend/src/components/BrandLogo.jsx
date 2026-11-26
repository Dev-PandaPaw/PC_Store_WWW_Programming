import React from 'react'
import { Link } from 'react-router-dom'
import pixelCoreLogo from '../assets/branding/pixel-core.png'
import '../styles/brand-logo.css'

export default function BrandLogo({ className = '' }) {
  return (
    <Link
      to="/"
      className={`pixelcore-brand ${className}`}
      aria-label="PixelCore — Trang chủ"
    >
      <span className="pixelcore-brand__art">
        <img
          src={pixelCoreLogo}
          alt="PixelCore"
          width="1981"
          height="793"
          decoding="async"
        />
      </span>
    </Link>
  )
}
