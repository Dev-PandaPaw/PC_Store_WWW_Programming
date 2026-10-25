import React, { useState } from 'react'
import http from '../services/http'
import { useCart } from '../contexts/CartContext'

export default function AdvisoryPage() {
  const [budget, setBudget] = useState('35000000')
  const [purpose, setPurpose] = useState('GAMING')
  const [recommendations, setRecommendations] = useState([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const { addItem } = useCart()

  const handleConsult = (e) => {
    e.preventDefault()
    setLoading(true)
    setSearched(true)
    http.get('/advisory/recommendations', {
      params: {
        purpose,
        budget: budget || undefined
      }
    })
      .then(res => setRecommendations(res.data))
      .catch(err => console.error('Lỗi tư vấn cấu hình:', err))
      .finally(() => setLoading(false))
  }

  return (
    <div style={{ maxWidth: '850px', margin: '0 auto' }}>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>
        Tư Vấn Cấu Hình PC Thông Minh
      </h1>
      <p style={{ color: 'var(--pc-text-muted)', marginBottom: '2rem' }}>
        Hệ thống tự động tra cứu cơ sở dữ liệu Supabase để tìm kiếm cấu hình máy tính phù hợp nhất với ngân sách và nhu cầu của bạn.
      </p>

      <form onSubmit={handleConsult} className="card" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', marginBottom: '2rem' }}>
        <div>
          <label style={{ display: 'block', fontWeight: 600, marginBottom: '0.5rem' }}>Khoảng ngân sách dự kiến (VNĐ)</label>
          <select value={budget} onChange={e => setBudget(e.target.value)} style={{ width: '100%', padding: '0.65rem', borderRadius: 'var(--pc-radius-sm)', border: '1px solid var(--pc-border)' }}>
            <option value="20000000">Dưới 25 triệu VNĐ (Phổ thông / Esports)</option>
            <option value="35000000">Từ 25 triệu - 45 triệu VNĐ (Chiến game 2K / Đồ họa chuyên nghiệp)</option>
            <option value="55000000">Trên 45 triệu VNĐ (Flagship / Workstation cao cấp)</option>
          </select>
        </div>

        <div>
          <label style={{ display: 'block', fontWeight: 600, marginBottom: '0.5rem' }}>Nhu cầu sử dụng chính</label>
          <select value={purpose} onChange={e => setPurpose(e.target.value)} style={{ width: '100%', padding: '0.65rem', borderRadius: 'var(--pc-radius-sm)', border: '1px solid var(--pc-border)' }}>
            <option value="GAMING">Chơi Game (Esports, AAA Max Settings)</option>
            <option value="DESIGN">Thiết kế đồ họa 3D, Kiến trúc, Render 4K</option>
          </select>
        </div>

        <button type="submit" className="btn btn-primary" style={{ padding: '0.75rem', fontWeight: 600 }}>
          {loading ? 'Đang phân tích cấu hình...' : 'Đề xuất cấu hình tối ưu'}
        </button>
      </form>

      {searched && (
        <div>
          <h2 style={{ fontSize: '1.35rem', fontWeight: 700, marginBottom: '1rem' }}>
            Kết quả đề xuất từ hệ thống ({recommendations.length})
          </h2>

          {loading ? (
            <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--pc-text-muted)' }}>Đang tìm kiếm...</div>
          ) : recommendations.length === 0 ? (
            <div className="card" style={{ textAlign: 'center', padding: '2rem', color: 'var(--pc-text-muted)' }}>
              Chưa có cấu hình phù hợp với mức ngân sách này trong cơ sở dữ liệu. Vui lòng chọn khoảng ngân sách khác.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {recommendations.map(rec => {
                const prod = rec.product
                const price = prod.salePrice || prod.basePrice
                return (
                  <div key={rec.id} className="card" style={{ borderLeft: '4px solid var(--pc-primary)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
                    <div style={{ flex: 1, minWidth: '280px' }}>
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '0.5rem' }}>
                        <span className="badge badge-primary">Điểm phù hợp: {rec.suitabilityScore}/100</span>
                        <span style={{ fontSize: '0.85rem', color: 'var(--pc-text-muted)' }}>{rec.usageTarget}</span>
                      </div>
                      <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '0.35rem' }}>{prod.name}</h3>
                      <p style={{ color: '#047857', fontSize: '0.9rem', marginBottom: '0.5rem', fontWeight: 500 }}>
                        💡 {rec.recommendationNotes}
                      </p>
                      <p style={{ color: 'var(--pc-primary)', fontSize: '1.3rem', fontWeight: 700 }}>
                        {Number(price).toLocaleString('vi-VN')} đ
                      </p>
                    </div>

                    <button
                      onClick={() => addItem({ id: prod.id, name: prod.name, price: Number(price) }, 1)}
                      className="btn btn-primary"
                      style={{ padding: '0.65rem 1.25rem' }}
                    >
                      Chọn mua cấu hình này
                    </button>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
