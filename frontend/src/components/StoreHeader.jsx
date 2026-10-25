import React, { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useCart } from '../contexts/CartContext'
import { useAuth } from '../contexts/AuthContext'
import '../styles/store-header.css'

const categories = [
  { label: 'PC Gaming', slug: 'pc-gaming', icon: 'gamepad' },
  { label: 'PC Workstation', slug: 'pc-workstation', icon: 'monitor' },
  { label: 'Bộ vi xử lý (CPU)', slug: 'cpu', icon: 'cpu' },
  { label: 'Card đồ họa (VGA)', slug: 'vga', icon: 'graphics' },
  { label: 'Bộ nhớ RAM', slug: 'ram', icon: 'memory' },
  { label: 'Ổ cứng SSD', slug: 'storage', icon: 'drive' },
]

const navigation = [
  { label: 'Trang chủ', to: '/', icon: 'home' },
  { label: 'Tất cả sản phẩm', to: '/catalog', icon: 'grid' },
  { label: 'PC Gaming', to: '/catalog?category=pc-gaming', category: 'pc-gaming', icon: 'gamepad' },
  { label: 'PC Workstation', to: '/catalog?category=pc-workstation', category: 'pc-workstation', icon: 'monitor' },
  { label: 'Card đồ họa', to: '/catalog?category=vga', category: 'vga', icon: 'graphics' },
  { label: 'Tư vấn cấu hình', to: '/advisory', icon: 'tools' },
]

function HeaderIcon({ name, className = '' }) {
  const paths = {
    menu: <><path d="M4 6h16M4 12h16M4 18h16" /></>,
    search: <><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 4.5 4.5" /></>,
    cart: <><path d="M3 3h2l2.5 12h11l2-8H6M8 18h10" /><circle cx="9" cy="21" r=".6" /><circle cx="18" cy="21" r=".6" /></>,
    user: <><circle cx="12" cy="7" r="3.5" /><path d="M4.5 21v-2a7.5 7.5 0 0 1 15 0v2z" /></>,
    chevron: <path d="m7 10 5 5 5-5" />,
    home: <><path d="m3 10 9-7 9 7M5 9v12h14V9" /><path d="M9 21v-7h6v7" /></>,
    grid: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
    gamepad: <><path d="M7.5 7h9c2 0 3 1.5 3.5 3.5l1.5 7c.5 3-2 4-4 2L15 17H9l-2.5 2.5c-2 2-4.5 1-4-2l1.5-7C4.5 8.5 5.5 7 7.5 7Z" /><path d="M6 12h5M8.5 9.5v5M16 11h.01M18.5 13.5h.01" /></>,
    monitor: <><rect x="2" y="3" width="20" height="14" rx="2" /><path d="M8 21h8M12 17v4M8 8l-2 2 2 2m8-4 2 2-2 2" /></>,
    cpu: <><rect x="5" y="5" width="14" height="14" rx="2" /><rect x="9" y="9" width="6" height="6" rx="1" /><path d="M9 2v3m6-3v3M9 19v3m6-3v3M2 9h3m-3 6h3m14-6h3m-3 6h3" /></>,
    graphics: <><rect x="3" y="5" width="18" height="13" rx="2" /><circle cx="12" cy="11.5" r="4" /><path d="m10 9 4 5m0-5-4 5M7 18v3m4-3v3m4-3v3M3 8H1" /></>,
    memory: <><rect x="2" y="6" width="20" height="12" rx="1" /><path d="M5 10h3v4H5zm6 0h3v4h-3zm6 0h2v4h-2zM5 18v3m4-3v3m4-3v3m4-3v3" /></>,
    drive: <><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M3 14h18M7 17h.01M11 17h.01M7 8h10" /></>,
    tools: <><path d="m14 6 4 4m-3-7a6 6 0 0 0-7 8L3.5 16.5a2.8 2.8 0 0 0 4 4L13 15a6 6 0 0 0 8-7l-4 4-5-5 4-4Z" /></>,
  }

  return (
    <svg className={`store-icon ${className}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name]}
    </svg>
  )
}

export default function StoreHeader() {
  const { pathname, search } = useLocation()
  const navigate = useNavigate()
  const { totalCount } = useCart()
  const { user, logout } = useAuth()
  const categoryMenu = useRef(null)
  const [keyword, setKeyword] = useState(() => new URLSearchParams(search).get('keyword') || '')
  const activeCategory = new URLSearchParams(search).get('category')

  useEffect(() => {
    setKeyword(new URLSearchParams(search).get('keyword') || '')
    categoryMenu.current.open = false
  }, [pathname, search])

  useEffect(() => {
    const closeOutside = event => {
      if (!categoryMenu.current.contains(event.target)) categoryMenu.current.open = false
    }
    const closeOnEscape = event => {
      if (event.key === 'Escape' && categoryMenu.current.open) {
        categoryMenu.current.open = false
        categoryMenu.current.querySelector('summary').focus()
      }
    }
    document.addEventListener('pointerdown', closeOutside)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOutside)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [])

  const handleSearch = event => {
    event.preventDefault()
    const params = new URLSearchParams()
    if (keyword.trim()) params.set('keyword', keyword.trim())
    navigate(`/catalog${params.size ? `?${params}` : ''}`)
  }

  return (
    <header className="store-header">
      <div className="store-header__top">
        <div className="store-header__inner store-header__toolbar">
          <Link to="/" className="store-brand" aria-label="PC Store — Trang chủ">
            <span className="store-brand__mark"><HeaderIcon name="monitor" /></span>
            <span className="store-brand__wordmark">
              <span>PC<span className="store-brand__accent">STORE</span></span>
              <small>HIỆU NĂNG TẠO KHÁC BIỆT</small>
            </span>
          </Link>

          <details className="store-categories" ref={categoryMenu}>
            <summary className="store-categories__trigger">
              <HeaderIcon name="menu" />
              <span>Danh mục</span>
              <HeaderIcon name="chevron" className="store-categories__chevron" />
            </summary>
            <nav className="store-categories__dropdown" aria-label="Danh mục sản phẩm">
              <p>Khám phá sản phẩm</p>
              <Link to="/catalog" onClick={() => { categoryMenu.current.open = false }}>
                <HeaderIcon name="grid" />Tất cả sản phẩm
              </Link>
              {categories.map(category => (
                <Link key={category.slug} to={`/catalog?category=${category.slug}`} onClick={() => { categoryMenu.current.open = false }}>
                  <HeaderIcon name={category.icon} />{category.label}
                </Link>
              ))}
            </nav>
          </details>

          <form className="store-search" role="search" aria-label="Tìm sản phẩm" onSubmit={handleSearch}>
            <input type="search" name="keyword" aria-label="Từ khóa tìm kiếm" placeholder="Bạn đang tìm PC, linh kiện nào?" value={keyword} onChange={event => setKeyword(event.target.value)} />
            <button type="submit" aria-label="Tìm kiếm"><HeaderIcon name="search" /></button>
          </form>

          <div className="store-header__actions">
            <Link to="/cart" className="store-cart" aria-label={`Giỏ hàng (${totalCount} sản phẩm)`}>
              <HeaderIcon name="cart" />
              <span className="store-cart__count">{totalCount}</span>
            </Link>
            {user ? (
              <div className="store-account store-account--signed-in">
                <HeaderIcon name="user" />
                <div>
                  <span className="store-account__name">{user.fullName || user.username}</span>
                  <button type="button" onClick={logout}>Đăng xuất</button>
                </div>
              </div>
            ) : (
              <Link to="/login" className="store-account" aria-label="Đăng nhập">
                <HeaderIcon name="user" /><span>Đăng nhập</span>
              </Link>
            )}
          </div>
        </div>
      </div>

      <div className="store-header__bottom">
        <nav className="store-header__inner store-nav" aria-label="Điều hướng chính">
          {navigation.map(item => {
            const active = item.category
              ? pathname === '/catalog' && activeCategory === item.category
              : pathname === item.to && (pathname !== '/catalog' || !activeCategory)
            return (
              <Link key={item.to} to={item.to} className={`store-nav__link${active ? ' store-nav__link--active' : ''}`} aria-current={active ? 'page' : undefined}>
                <HeaderIcon name={item.icon} /><span>{item.label}</span>
              </Link>
            )
          })}
        </nav>
      </div>
    </header>
  )
}
