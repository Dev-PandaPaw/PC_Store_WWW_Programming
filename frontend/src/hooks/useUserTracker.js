import { useEffect, useRef } from 'react'
import { sendTrackingEvent } from '../services/tracking'

// Nguong thoi gian o lai trang de xac nhan do quan tam cao (Dwell Time = 30 giay)
const DWELL_TIME_THRESHOLD_MS = 30000

/**
 * Hook theo doi luot xem chi tiet san pham va thoi gian dung trang
 * - Gui su kien VIEW_PRODUCT khi san pham duoc render
 * - Gui su kien DWELL_TIME_REACHED neu o lai trang tu 30 giay tro len
 */
export function useProductViewTracker(product) {
  const timerRef = useRef(null)

  useEffect(() => {
    if (!product || !product.id) return

    // 1. Gui su kien xem san pham ngay khi mount
    const viewTimer = setTimeout(() => sendTrackingEvent({
      eventType: 'VIEW_PRODUCT',
      productId: product.id,
      categorySlug: product.category || null,
      metadata: {
        productName: product.name,
        price: product.price,
      },
    }), 0)

    // 2. Hen gio 30s de ghi nhan dwell time
    timerRef.current = setTimeout(() => {
      sendTrackingEvent({
        eventType: 'DWELL_TIME_REACHED',
        productId: product.id,
        categorySlug: product.category || null,
        metadata: {
          dwellSeconds: DWELL_TIME_THRESHOLD_MS / 1000,
          productName: product.name,
        },
      })
    }, DWELL_TIME_THRESHOLD_MS)

    // 3. Clear timer khi unmount hoac chuyen sang san pham khac
    return () => {
      clearTimeout(viewTimer)
      if (timerRef.current) {
        clearTimeout(timerRef.current)
        timerRef.current = null
      }
    }
  }, [product?.id])
}

/**
 * Hook theo doi hanh vi loc danh muc va tim kiem tren Catalog
 * - FILTER_CATEGORY: Gui khi nguoi dung chon danh muc
 * - SEARCH_KEYWORD: Debounce 500ms tranh spam request trong luc go phim
 */
export function useCatalogTracker(category, keyword) {
  // 1. Theo doi thay doi danh muc
  useEffect(() => {
    if (!category || category === 'ALL') return

    sendTrackingEvent({
      eventType: 'FILTER_CATEGORY',
      categorySlug: category,
    })
  }, [category])

  // 2. Theo doi tim kiem voi debounce 500ms
  useEffect(() => {
    const trimmed = (keyword || '').trim()
    if (trimmed.length < 2) return

    const debounceTimer = setTimeout(() => {
      sendTrackingEvent({
        eventType: 'SEARCH_KEYWORD',
        metadata: {
          keyword: trimmed,
        },
      })
    }, 500)

    return () => clearTimeout(debounceTimer)
  }, [keyword])
}

