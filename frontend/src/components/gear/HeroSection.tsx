import React from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, ChevronRight } from 'lucide-react'
import HeroBanner from '../HeroBanner'
import CategoryIcon from '../CategoryIcon'
import PromoBanner from '../PromoBanner'
import pcI5Banner from '../../assets/promotions/pc-i5-rtx5060.webp'
import buildPcBanner from '../../assets/promotions/build-pc-monitor.webp'

const menu = [
  { id: 'pc-gaming', name: 'PC Gaming', icon: 'Gamepad2' },
  { id: 'pc-office', name: 'PC văn phòng', icon: 'Monitor' },
  { id: 'pc-workstation', name: 'PC đồ họa & làm việc', icon: 'Cpu' },
  { id: 'components', name: 'CPU, Mainboard & VGA', icon: 'CircuitBoard' },
  { id: 'memory', name: 'RAM & ổ cứng SSD', icon: 'MemoryStick' },
  { id: 'build', name: 'Case, nguồn & tản nhiệt', icon: 'Fan' },
  { id: 'monitor', name: 'Màn hình máy tính', icon: 'Monitor' },
  { id: 'gear', name: 'Chuột & bàn phím', icon: 'Keyboard' },
]

export const HeroSection: React.FC = () => (
  <section className="store-hero-layout" aria-label="Khám phá PixelCore">
    <aside className="category-menu">
      <Link to="/catalog" className="category-menu__heading">
        Danh mục sản phẩm <ArrowRight size={16} />
      </Link>
      <nav aria-label="Nhóm sản phẩm">
        {menu.map((item) => (
          <Link key={item.id} to={`/catalog?category=${item.id}`}>
            <CategoryIcon name={item.icon} />
            <span>{item.name}</span>
            <ChevronRight size={14} />
          </Link>
        ))}
      </nav>
    </aside>
    <div className="store-hero-main">
      <div className="store-hero-video">
        <HeroBanner compact />
      </div>
      <div className="hero-promos">
        <PromoBanner
          to="/blog/pc-i5-rtx-5060-tu-23-trieu"
          src={pcI5Banner}
          alt="PC Intel Core i5 và RTX 5060 — xem thông tin chương trình"
          width={1000}
          height={500}
          className="hero-promo"
          priority
        />
        <PromoBanner
          to="/blog/build-pc-tang-man-hinh-240hz"
          src={buildPcBanner}
          alt="Build PC tặng màn hình Gaming 240Hz — xem thông tin chương trình"
          width={1000}
          height={500}
          className="hero-promo"
          priority
        />
      </div>
    </div>
  </section>
)
