import React, { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import performanceTitle from '../assets/hero/performance-neon.png'
import pcShowcase from '../assets/hero/pc-showcase.webm'
import '../styles/hero.css'

export default function HeroBanner() {
  const videoRef = useRef(null)
  const [isPaused, setIsPaused] = useState(() =>
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  )

  useEffect(() => {
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)')
    const updatePreference = event => setIsPaused(event.matches)
    preference.addEventListener('change', updatePreference)
    return () => preference.removeEventListener('change', updatePreference)
  }, [])

  useEffect(() => {
    const video = videoRef.current
    if (isPaused) {
      video.pause()
    } else {
      video.play().catch(() => setIsPaused(true))
    }
  }, [isPaused])

  return (
    <section className="pc-hero" aria-labelledby="hero-title">
      <div className="pc-hero__inner">
        <div className="pc-hero__content">
          <p className="pc-hero__eyebrow">Thiết kế cho đam mê. Tạo nên khác biệt.</p>
          <h1 id="hero-title" className="pc-hero__title">
            <img src={performanceTitle} alt="Hiệu năng đỉnh cao" width="2170" height="725" fetchPriority="high" />
          </h1>
          <div className="pc-hero__actions">
            <Link to="/catalog" className="pc-hero__button pc-hero__button--primary">
              <span>Khám phá PC</span>
              <span className="pc-hero__arrow" aria-hidden="true">↗</span>
            </Link>
            <Link to="/advisory" className="pc-hero__button pc-hero__button--secondary">
              <span>Tự xây cấu hình</span>
              <span className="pc-hero__arrow" aria-hidden="true">→</span>
            </Link>
          </div>
        </div>
        <div className="pc-hero__showcase">
          <video
            ref={videoRef}
            className="pc-hero__video"
            src={pcShowcase}
            autoPlay={!isPaused}
            muted
            loop
            playsInline
            preload="auto"
            aria-hidden="true"
          />
        </div>
      </div>
    </section>
  )
}
