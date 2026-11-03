import React, { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'

export default function TopProgressBar() {
  const location = useLocation()
  const [progress, setProgress] = useState(0)
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    // Bắt đầu chạy tiến trình khi đổi đường dẫn (navigation)
    setVisible(true)
    setProgress(25)

    const timer1 = setTimeout(() => {
      setProgress(75)
    }, 120)

    const timer2 = setTimeout(() => {
      setProgress(100)
    }, 300)

    const timer3 = setTimeout(() => {
      setVisible(false)
      setProgress(0)
    }, 550)

    return () => {
      clearTimeout(timer1)
      clearTimeout(timer2)
      clearTimeout(timer3)
    }
  }, [location.pathname, location.search])

  if (!visible && progress === 0) return null

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100%',
        height: '3px',
        zIndex: 999999,
        pointerEvents: 'none',
        overflow: 'hidden',
      }}
    >
      <div
        style={{
          height: '100%',
          width: `${progress}%`,
          background: 'linear-gradient(90deg, #38bdf8 0%, #2563eb 50%, #e11d48 100%)',
          boxShadow: '0 0 10px rgba(37, 99, 235, 0.7), 0 0 5px rgba(225, 29, 72, 0.5)',
          transition: progress === 100 ? 'width 0.2s ease-out, opacity 0.25s ease' : 'width 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
          opacity: visible ? 1 : 0,
        }}
      />
    </div>
  )
}
