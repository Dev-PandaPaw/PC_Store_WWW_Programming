import React from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'

export default function NotFoundPage() {
  return (
    <div className="store-empty">
      <p className="section-eyebrow">404 · PIXELCORE</p>
      <h1>Chưa tìm thấy trang này</h1>
      <p>Hãy trở về cửa hàng để khám phá PC và linh kiện.</p>
      <Link className="store-button" to="/">
        <ArrowLeft size={16} /> Về trang chủ
      </Link>
    </div>
  )
}
