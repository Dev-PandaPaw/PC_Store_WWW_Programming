import React, { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronLeft, ChevronRight, Zap } from 'lucide-react'
import { useCatalog } from '../../contexts/CatalogContext'
import type { StoreProduct } from '../../data/storeData'
import { ProductCard } from './ProductCard'

function endOfVietnamDay(now: number) {
  const day = 24 * 60 * 60 * 1000
  const offset = 7 * 60 * 60 * 1000
  return Math.floor((now + offset) / day) * day + day - offset
}

export function FlashDeal({
  onAddToCart,
}: {
  onAddToCart?: (product: StoreProduct) => void
}) {
  const { FLASH_DEAL_PRODUCTS } = useCatalog()
  const [endsAt] = useState(() => endOfVietnamDay(Date.now()))
  const [secondsLeft, setSecondsLeft] = useState(() =>
    Math.max(0, Math.ceil((endsAt - Date.now()) / 1000)),
  )
  const railRef = useRef<HTMLDivElement>(null)
  const [canGoBack, setCanGoBack] = useState(false)
  const [canGoNext, setCanGoNext] = useState(false)
  const expired = secondsLeft === 0

  useEffect(() => {
    const update = () =>
      setSecondsLeft(Math.max(0, Math.ceil((endsAt - Date.now()) / 1000)))
    const timer = window.setInterval(update, 1000)
    document.addEventListener('visibilitychange', update)
    return () => {
      window.clearInterval(timer)
      document.removeEventListener('visibilitychange', update)
    }
  }, [endsAt])

  useEffect(() => {
    const rail = railRef.current
    if (!rail) return
    const update = () => {
      setCanGoBack(rail.scrollLeft > 2)
      setCanGoNext(rail.scrollLeft + rail.clientWidth < rail.scrollWidth - 2)
    }
    update()
    rail.addEventListener('scroll', update, { passive: true })
    const observer = new ResizeObserver(update)
    observer.observe(rail)
    return () => {
      rail.removeEventListener('scroll', update)
      observer.disconnect()
    }
  }, [expired])

  const move = (direction: number) => {
    const rail = railRef.current
    if (!rail?.firstElementChild) return
    const distance =
      rail.firstElementChild.getBoundingClientRect().width +
      parseFloat(getComputedStyle(rail).columnGap)
    rail.scrollBy({
      left: direction * distance,
      behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches
        ? 'instant'
        : 'smooth',
    })
  }
  const hours = String(Math.floor(secondsLeft / 3600)).padStart(2, '0')
  const minutes = String(Math.floor((secondsLeft % 3600) / 60)).padStart(2, '0')
  const seconds = String(secondsLeft % 60).padStart(2, '0')

  return (
    <section className="flash-deal" aria-labelledby="flash-deal-title">
      <div className="flash-deal__heading">
        <div className="flash-deal__title">
          <Zap size={24} fill="currentColor" aria-hidden="true" />
          <h2 id="flash-deal-title">
            FLASH DEAL <span>PC & LINH KIỆN</span>
          </h2>
        </div>
        {expired ? (
          <span className="flash-deal__status">Đã kết thúc</span>
        ) : (
          <div
            className="flash-deal__countdown"
            role="timer"
            aria-live="off"
            aria-label={`Còn ${hours} giờ ${minutes} phút ${seconds} giây`}
          >
            <span>{hours}</span>
            <b aria-hidden="true">:</b>
            <span>{minutes}</span>
            <b aria-hidden="true">:</b>
            <span>{seconds}</span>
          </div>
        )}
        <span className="flash-deal__circuit" aria-hidden="true" />
        <Link className="flash-deal__all" to="/catalog?deals=1">
          Xem tất cả <ChevronRight size={17} />
        </Link>
      </div>
      {expired ? (
        <div className="flash-deal__ended">
          <h3>Hẹn bạn ở đợt Flash Deal tiếp theo!</h3>
          <Link to="/catalog">
            Tiếp tục khám phá sản phẩm <ChevronRight size={16} />
          </Link>
        </div>
      ) : (
        <div className="flash-deal__carousel">
          <div
            className="flash-deal__rail"
            ref={railRef}
            role="region"
            aria-label="Sản phẩm Flash Deal"
            tabIndex={0}
          >
            {FLASH_DEAL_PRODUCTS.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                onAddToCart={onAddToCart}
              />
            ))}
          </div>
          <button
            type="button"
            className="flash-deal__arrow flash-deal__arrow--prev"
            aria-label="Sản phẩm trước"
            disabled={!canGoBack}
            onClick={() => move(-1)}
          >
            <ChevronLeft size={21} />
          </button>
          <button
            type="button"
            className="flash-deal__arrow flash-deal__arrow--next"
            aria-label="Sản phẩm tiếp theo"
            disabled={!canGoNext}
            onClick={() => move(1)}
          >
            <ChevronRight size={21} />
          </button>
        </div>
      )}
    </section>
  )
}
