import React, { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useCart } from '../contexts/CartContext'
import { useAuth } from '../contexts/AuthContext'
import { CATEGORY_GROUPS } from '../data/storeData'
import BrandLogo from './BrandLogo'
import '../styles/store-header.css'

const navigation = [
  { label: 'Trang chủ', to: '/', icon: 'home' },
  {
    label: 'PC bộ',
    to: '/catalog?category=pc',
    category: 'pc',
    icon: 'monitor',
  },
  {
    label: 'Linh kiện PC',
    to: '/catalog?category=components',
    category: 'components',
    icon: 'graphics',
  },
  {
    label: 'Chuột & bàn phím',
    to: '/catalog?category=gear',
    category: 'gear',
    icon: 'keyboard',
  },
  { label: 'Góc kiến thức', to: '/blog', icon: 'book' },
  { label: 'Tư vấn cấu hình', to: '/advisory', icon: 'tools' },
]

function HeaderIcon({ name, className = '' }) {
  const paths = {
    search: (
      <>
        <circle cx="10.5" cy="10.5" r="6.5" />
        <path d="m16 16 4.5 4.5" />
      </>
    ),
    cart: (
      <>
        <path d="M3 3h2l2.5 12h11l2-8H6M8 18h10" />
        <circle cx="9" cy="21" r=".6" />
        <circle cx="18" cy="21" r=".6" />
      </>
    ),
    user: (
      <>
        <circle cx="12" cy="7" r="3.5" />
        <path d="M4.5 21v-2a7.5 7.5 0 0 1 15 0v2z" />
      </>
    ),
    home: (
      <>
        <path d="m3 10 9-7 9 7M5 9v12h14V9" />
        <path d="M9 21v-7h6v7" />
      </>
    ),
    grid: (
      <>
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </>
    ),
    gamepad: (
      <>
        <path d="M7.5 7h9c2 0 3 1.5 3.5 3.5l1.5 7c.5 3-2 4-4 2L15 17H9l-2.5 2.5c-2 2-4.5 1-4-2l1.5-7C4.5 8.5 5.5 7 7.5 7Z" />
        <path d="M6 12h5M8.5 9.5v5M16 11h.01M18.5 13.5h.01" />
      </>
    ),
    monitor: (
      <>
        <rect x="2" y="3" width="20" height="14" rx="2" />
        <path d="M8 21h8M12 17v4M8 8l-2 2 2 2m8-4 2 2-2 2" />
      </>
    ),
    graphics: (
      <>
        <rect x="3" y="5" width="18" height="13" rx="2" />
        <circle cx="12" cy="11.5" r="4" />
        <path d="m10 9 4 5m0-5-4 5M7 18v3m4-3v3m4-3v3M3 8H1" />
      </>
    ),
    tools: (
      <>
        <path d="m14 6 4 4m-3-7a6 6 0 0 0-7 8L3.5 16.5a2.8 2.8 0 0 0 4 4L13 15a6 6 0 0 0 8-7l-4 4-5-5 4-4Z" />
      </>
    ),
    keyboard: (
      <>
        <rect x="2" y="5" width="20" height="14" rx="2" />
        <path d="M6 9h.01M10 9h.01M14 9h.01M18 9h.01M6 12h.01M10 12h.01M14 12h.01M18 12h.01M7 16h10" />
      </>
    ),
    book: (
      <>
        <path d="M12 5c-3-2-7-2-10-1v15c3-1 7-1 10 1 3-2 7-2 10-1V4c-3-1-7-1-10 1Zm0 0v15" />
      </>
    ),
  }

  return (
    <svg
      className={`store-icon ${className}`}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name]}
    </svg>
  )
}

export default function StoreHeader() {
  const { pathname, search } = useLocation()
  const navigate = useNavigate()
  const { totalCount } = useCart()
  const { user, logout } = useAuth()
  const [loggingOut, setLoggingOut] = useState(false)
  const [keyword, setKeyword] = useState(
    () => new URLSearchParams(search).get('keyword') || '',
  )
  const activeCategory = new URLSearchParams(search).get('category')

  useEffect(() => {
    setKeyword(new URLSearchParams(search).get('keyword') || '')
  }, [pathname, search])

  const handleLogout = async () => {
    setLoggingOut(true)
    try {
      await logout()
    } finally {
      setLoggingOut(false)
    }
  }

  const handleSearch = (event) => {
    event.preventDefault()
    const params = new URLSearchParams()
    if (keyword.trim()) params.set('keyword', keyword.trim())
    navigate(`/catalog${params.size ? `?${params}` : ''}`)
  }

  return (
    <header className="store-header">
      <div className="store-header__top">
        <div className="store-header__inner store-header__toolbar">
          <BrandLogo className="store-brand" />

          <form
            className="store-search"
            role="search"
            aria-label="Tìm sản phẩm"
            onSubmit={handleSearch}
          >
            <input
              type="search"
              name="keyword"
              aria-label="Từ khóa tìm kiếm"
              placeholder="Tìm theo nhu cầu: PC cá nhân, chuột chơi game..."
              maxLength={300}
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
            <button type="submit" aria-label="Tìm kiếm">
              <HeaderIcon name="search" />
            </button>
          </form>

          <div className="store-header__actions">
            <Link
              to="/cart"
              className="store-cart"
              aria-label={`Giỏ hàng (${totalCount} sản phẩm)`}
            >
              <HeaderIcon name="cart" />
              <span className="store-cart__count">{totalCount}</span>
            </Link>
            {user ? (
              <div className="store-account store-account--signed-in">
                {loggingOut ? (
                  <span className="shell-spinner" aria-hidden="true" />
                ) : (
                  <HeaderIcon name="user" />
                )}
                <div>
                  <span className="store-account__name">
                    {user.fullName || user.username}
                  </span>
                  <button type="button" onClick={handleLogout} disabled={loggingOut}>
                    {loggingOut ? 'Đang thoát...' : 'Đăng xuất'}
                  </button>
                </div>
              </div>
            ) : (
              <Link
                to="/login"
                className="store-account"
                aria-label="Đăng nhập"
              >
                <HeaderIcon name="user" />
                <span>Đăng nhập</span>
              </Link>
            )}
          </div>
        </div>
      </div>

      <div className="store-header__bottom">
        <nav
          className="store-header__inner store-nav"
          aria-label="Điều hướng chính"
        >
          {navigation.map((item) => {
            const active = item.category
              ? pathname === '/catalog' &&
                (activeCategory === item.category ||
                  CATEGORY_GROUPS[item.category]?.includes(activeCategory))
              : pathname === item.to ||
                (item.to === '/blog' && pathname.startsWith('/blog/'))
            return (
              <Link
                key={item.to}
                to={item.to}
                className={`store-nav__link${active ? ' store-nav__link--active' : ''}`}
                aria-current={active ? 'page' : undefined}
              >
                <HeaderIcon name={item.icon} />
                <span>{item.label}</span>
              </Link>
            )
          })}
        </nav>
      </div>
    </header>
  )
}
