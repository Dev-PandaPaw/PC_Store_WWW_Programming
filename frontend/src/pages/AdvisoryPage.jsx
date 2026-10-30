import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, SlidersHorizontal } from 'lucide-react'
import { useCatalog } from '../contexts/CatalogContext'
import { ProductCard } from '../components/gear/ProductCard'
import { formatVND } from '../utils/cart'

export default function AdvisoryPage() {
  const { productsInCategory } = useCatalog()
  const [budget, setBudget] = useState('35000000')
  const [purpose, setPurpose] = useState('pc-gaming')
  const [selection, setSelection] = useState(null)
  const submit = (event) => {
    event.preventDefault()
    setSelection({ budget: Number(budget), purpose })
  }
  const results = selection
    ? productsInCategory(selection.purpose)
        .filter((product) => product.price <= selection.budget)
        .sort((a, b) => b.price - a.price)
    : []
  return (
    <div className="advisory-page">
      <nav className="breadcrumbs" aria-label="Đường dẫn">
        <Link to="/">Trang chủ</Link>
        <span>/</span>
        <span>Tư vấn cấu hình</span>
      </nav>
      <div className="page-heading">
        <p className="section-eyebrow">BẮT ĐẦU TỪ NHU CẦU CỦA BẠN</p>
        <h1>Tìm một bộ PC phù hợp.</h1>
        <p>
          Chọn mục đích sử dụng và ngân sách cho thùng máy. Khám phá các cấu
          hình có trong danh mục PixelCore.
        </p>
      </div>
      <form className="advisory-form" onSubmit={submit}>
        <label>
          Nhu cầu chính
          <select
            value={purpose}
            onChange={(event) => setPurpose(event.target.value)}
          >
            <option value="pc-gaming">Gaming & giải trí</option>
            <option value="pc-office">Văn phòng & học tập</option>
            <option value="pc-workstation">Đồ họa & làm việc</option>
          </select>
        </label>
        <label>
          Ngân sách cho thùng máy (VNĐ)
          <input
            type="number"
            min="1000000"
            max="500000000"
            step="100000"
            required
            value={budget}
            onChange={(event) => setBudget(event.target.value)}
          />
        </label>
        <button className="store-button" type="submit">
          <SlidersHorizontal size={17} /> Tìm cấu hình
        </button>
      </form>
      <p className="advisory-note">
        Màn hình, chuột và bàn phím được lựa chọn riêng theo nhu cầu của bạn.
      </p>
      {selection && (
        <section className="store-section">
          <div className="store-section__heading">
            <div>
              <p className="section-eyebrow">KẾT QUẢ DÀNH CHO BẠN</p>
              <h2>
                {results.length} cấu hình trong {formatVND(selection.budget)}
              </h2>
            </div>
          </div>
          {results.length ? (
            <div className="product-grid">
              {results.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          ) : (
            <div className="store-empty">
              <h2>Chưa có cấu hình trong mức ngân sách này</h2>
              <p>Bạn có thể tăng ngân sách hoặc xem thêm các nhóm PC khác.</p>
              <Link className="store-button" to="/catalog?category=pc">
                Xem tất cả PC bộ <ArrowRight size={16} />
              </Link>
            </div>
          )}
        </section>
      )}
      <div className="advisory-reading">
        <h2>Cần thêm thông tin trước khi chọn?</h2>
        <Link className="text-link" to="/blog/chon-cpu-va-gpu-cho-pc-gaming">
          Đọc hướng dẫn chọn CPU và GPU <ArrowRight size={16} />
        </Link>
      </div>
    </div>
  )
}
