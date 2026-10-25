import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useCart } from '../contexts/CartContext'
import http from '../services/http'

export default function CatalogPage() {
  const { addItem } = useCart()
  const [categories, setCategories] = useState([])
  const [products, setProducts] = useState([])
  const [activeCategory, setActiveCategory] = useState('ALL')
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    http.get('/catalog/categories')
      .then(res => setCategories(res.data))
      .catch(err => console.error('Lỗi tải danh mục:', err))
  }, [])

  useEffect(() => {
    setLoading(true)
    const params = {}
    if (activeCategory !== 'ALL') {
      params.category = activeCategory
    }
    if (keyword.trim()) {
      params.keyword = keyword.trim()
    }

    http.get('/catalog/products', { params })
      .then(res => setProducts(res.data))
      .catch(err => console.error('Lỗi tải sản phẩm:', err))
      .finally(() => setLoading(false))
  }, [activeCategory, keyword])

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.5rem' }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 700 }}>Danh Mục Sản Phẩm</h1>
          <p style={{ color: 'var(--pc-text-muted)', fontSize: '0.9rem' }}>
            Dữ liệu tải trực tiếp từ cơ sở dữ liệu Supabase PostgreSQL
          </p>
        </div>

        <input
          type="text"
          placeholder="Tìm theo tên sản phẩm, mã SKU..."
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
          style={{ padding: '0.6rem 1rem', border: '1px solid var(--pc-border)', borderRadius: 'var(--pc-radius-sm)', minWidth: '280px' }}
        />
      </div>

      <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '1.5rem' }}>
        <button
          onClick={() => setActiveCategory('ALL')}
          className={`btn ${activeCategory === 'ALL' ? 'btn-primary' : 'btn-outline'}`}
          style={{ fontSize: '0.85rem', padding: '0.4rem 0.85rem' }}
        >
          Tất cả
        </button>
        {categories.map(c => (
          <button
            key={c.id}
            onClick={() => setActiveCategory(c.slug)}
            className={`btn ${activeCategory === c.slug ? 'btn-primary' : 'btn-outline'}`}
            style={{ fontSize: '0.85rem', padding: '0.4rem 0.85rem' }}
          >
            {c.name}
          </button>
        ))}
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--pc-text-muted)' }}>
          Đang tải dữ liệu từ database...
        </div>
      ) : products.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '3rem', color: 'var(--pc-text-muted)' }}>
          Không tìm thấy sản phẩm nào phù hợp với bộ lọc.
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1.5rem' }}>
          {products.map(product => {
            const displayPrice = product.salePrice || product.basePrice
            return (
              <div key={product.id} className="card" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                <div>
                  <div style={{ height: '140px', background: 'var(--pc-surface-subtle)', borderRadius: 'var(--pc-radius-sm)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '3rem', marginBottom: '1rem' }}>
                    🖥️
                  </div>
                  {product.category && (
                    <span className="badge badge-primary" style={{ marginBottom: '0.5rem' }}>
                      {product.category.name}
                    </span>
                  )}
                  <h3 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '0.5rem', lineHeight: 1.35 }}>
                    {product.name}
                  </h3>
                  <p style={{ color: 'var(--pc-text-muted)', fontSize: '0.85rem', marginBottom: '0.75rem', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                    {product.summary || product.description}
                  </p>
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.5rem', marginBottom: '1rem' }}>
                    <span style={{ color: 'var(--pc-primary)', fontSize: '1.25rem', fontWeight: 700 }}>
                      {Number(displayPrice).toLocaleString('vi-VN')} đ
                    </span>
                    {product.salePrice && (
                      <span style={{ textDecoration: 'line-through', color: 'var(--pc-text-muted)', fontSize: '0.9rem' }}>
                        {Number(product.basePrice).toLocaleString('vi-VN')} đ
                      </span>
                    )}
                  </div>
                </div>
                <button
                  onClick={() => addItem({ id: product.id, name: product.name, price: Number(displayPrice) }, 1)}
                  className="btn btn-primary"
                  style={{ width: '100%' }}
                >
                  Thêm vào giỏ
                </button>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
