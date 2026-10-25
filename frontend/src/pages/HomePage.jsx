import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import http from '../services/http'
import HeroBanner from '../components/HeroBanner'

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
    <div className="home-page">
      <HeroBanner />

      <section className="home-section">
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
        <section className="home-section">
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
