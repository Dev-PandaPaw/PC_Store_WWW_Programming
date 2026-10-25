import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import http from '../services/http'

export default function HomePage() {
  const [categories, setCategories] = useState([])
  const [featuredProducts, setFeaturedProducts] = useState([])

  useEffect(() => {
    http.get('/catalog/categories')
      .then(res => setCategories(res.data))
      .catch(err => console.error('Lỗi tải danh mục:', err))

    http.get('/catalog/products')
      .then(res => setFeaturedProducts(res.data.slice(0, 4)))
      .catch(err => console.error('Lỗi tải sản phẩm nổi bật:', err))
  }, [])

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2.5rem' }}>
      <section className="card" style={{ padding: '3.5rem 2.5rem', background: 'linear-gradient(135deg, #1e293b, #0f172a)', color: '#fff', borderRadius: 'var(--pc-radius-lg)' }}>
        <h1 style={{ fontSize: '2.25rem', fontWeight: 800, marginBottom: '1rem' }}>
          Hệ Thống PC & Linh Kiện Hiệu Năng Cao
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '1.1rem', maxWidth: '640px', marginBottom: '1.75rem' }}>
          Cung cấp máy tính chơi game, làm việc đồ họa và hệ thống tư vấn cấu hình tự động thông minh theo ngân sách. Toàn bộ dữ liệu kết nối cơ sở dữ liệu Supabase PostgreSQL.
        </p>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          <Link to="/catalog" className="btn btn-primary" style={{ padding: '0.75rem 1.5rem', fontSize: '1rem' }}>
            Xem sản phẩm thật
          </Link>
          <Link to="/advisory" className="btn btn-outline" style={{ padding: '0.75rem 1.5rem', fontSize: '1rem', background: 'rgba(255,255,255,0.1)', borderColor: '#475569', color: '#fff' }}>
            Tư vấn cấu hình PC
          </Link>
        </div>
      </section>

      <section>
        <h2 style={{ fontSize: '1.4rem', fontWeight: 700, marginBottom: '1.25rem' }}>
          Danh mục linh kiện chính hãng ({categories.length})
        </h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
          {categories.map(cat => (
            <Link key={cat.id} to="/catalog" className="card" style={{ display: 'block', textAlign: 'center', padding: '1.5rem', transition: 'transform 0.2s' }}>
              <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>⚡</div>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 600 }}>{cat.name}</h3>
            </Link>
          ))}
        </div>
      </section>

      {featuredProducts.length > 0 && (
        <section>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
            <h2 style={{ fontSize: '1.4rem', fontWeight: 700 }}>Sản phẩm mới lên kệ</h2>
            <Link to="/catalog" style={{ color: 'var(--pc-primary)', fontWeight: 600 }}>Xem tất cả →</Link>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.5rem' }}>
            {featuredProducts.map(p => (
              <div key={p.id} className="card">
                <span className="badge badge-primary" style={{ marginBottom: '0.5rem' }}>{p.category?.name}</span>
                <h4 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '0.5rem' }}>{p.name}</h4>
                <p style={{ color: 'var(--pc-primary)', fontSize: '1.2rem', fontWeight: 700 }}>
                  {Number(p.salePrice || p.basePrice).toLocaleString('vi-VN')} đ
                </p>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
