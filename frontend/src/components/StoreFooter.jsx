import React from 'react'
import { Link } from 'react-router-dom'
import { ArrowUpRight } from 'lucide-react'
import BrandLogo from './BrandLogo'
import '../styles/store-footer.css'

export default function StoreFooter() {
  return (
    <footer className="store-footer">
      <div className="store-footer__inner">
        <div className="store-footer__grid">
          <div className="store-footer__brand">
            <BrandLogo className="store-footer__logo" />
            <p>
              Dành cho những người yêu PC.
              <br />
              Từ linh kiện đầu tiên đến góc máy của riêng bạn.
            </p>
            <Link to="/advisory" className="store-footer__cta">
              Tìm cấu hình phù hợp <ArrowUpRight size={16} />
            </Link>
          </div>
          <div>
            <h2>Khám phá cửa hàng</h2>
            <nav aria-label="Danh mục ở chân trang">
              <Link to="/catalog?category=pc">PC Gaming & máy tính bộ</Link>
              <Link to="/catalog?category=components">Linh kiện máy tính</Link>
              <Link to="/catalog?category=build">Case, nguồn & tản nhiệt</Link>
              <Link to="/catalog?category=gear">Chuột & bàn phím</Link>
              <Link to="/catalog?category=monitor">Màn hình</Link>
            </nav>
          </div>
          <div>
            <h2>Góc kiến thức</h2>
            <nav aria-label="Hướng dẫn PC">
              <Link to="/blog/chon-cpu-va-gpu-cho-pc-gaming">
                Chọn cấu hình PC
              </Link>
              <Link to="/blog/tan-nhiet-khi-hay-tan-nhiet-nuoc-aio">
                Tìm hiểu tản nhiệt
              </Link>
              <Link to="/blog/chon-ram-va-ssd-khi-nang-cap-pc">
                Nâng cấp RAM & SSD
              </Link>
              <Link to="/blog/chon-chuot-va-ban-phim-cho-goc-may">
                Hoàn thiện góc máy
              </Link>
              <Link to="/blog">Tất cả bài viết</Link>
            </nav>
          </div>
          <div className="store-footer__guide">
            <h2>Chọn theo nhu cầu</h2>
            <p>
              Tìm bộ PC và thiết bị phù hợp cho công việc, sáng tạo và giải trí.
            </p>
            <nav aria-label="Chọn PC theo nhu cầu">
              <Link to="/catalog?category=pc-gaming">Chơi game & giải trí</Link>
              <Link to="/catalog?category=pc-office">Văn phòng & học tập</Link>
              <Link to="/catalog?category=pc-workstation">
                Đồ họa & làm việc
              </Link>
              <Link to="/advisory">Tư vấn cấu hình</Link>
            </nav>
          </div>
        </div>
        <div className="store-footer__bottom">
          <p>© 2026 PixelCore. PC, linh kiện và góc máy của bạn.</p>
          <Link to="/catalog">
            Khám phá tất cả sản phẩm <ArrowUpRight size={14} />
          </Link>
        </div>
      </div>
    </footer>
  )
}
