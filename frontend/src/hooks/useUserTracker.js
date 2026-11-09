import { useEffect, useRef } from 'react'
import { sendTrackingEvent } from '../services/tracking'

/**
 * Nguong thoi gian dung tren trang de xac nhan do quan tam cao (Dwell Time = 30 giay)
 * Theo dung quy trinh phan tich trong bao cao chuyen de.
 */
const DWELL_TIME_THRESHOLD_MS = 30000

/**
 * Hook theo doi hanh vi xem chi tiet san pham cua nguoi dung.
 * - Tu dong gui su kien VIEW_PRODUCT khi san pham duoc render.
 * - Tu dong bat dau timer 30 giay, neu nguoi dung van o lai trang -> gui su kien DWELL_TIME_REACHED.
 * - Tu dong huy timer neu nguoi dung chuyen sang san pham khac hoac roi trang.
 * 
 * @param {Object|null} product - Doi tuong san pham dang xem
 */
export function useProductViewTracker(product) {
  const timerRef = useRef(null)

  useEffect(() => {
    // Neu chua co thong tin san pham thi khong thuc hien
    if (!product || !product.id) return

    // 1. Gui su kien xem san pham ngay khi trang duoc mo
    sendTrackingEvent({
      eventType: 'VIEW_PRODUCT',
      productId: product.id,
      categorySlug: product.category || null,
      metadata: {
        productName: product.name,
        price: product.price,
      },
    })

    // 2. Bat dau dem thoi gian onsite (Dwell Time)
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

    // 3. Cleanup: Huy timer khi nguoi dung chuyen san pham hoac dong trang
    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current)
        timerRef.current = null
      }
    }
  }, [product?.id])
}

/**
 * Hook theo doi hanh vi tim kiem va loc danh muc tren Catalog.
 * - Loc danh muc: Gui su kien FILTER_CATEGORY.
 * - Tim kiem tu khoa: Ap dung Debounce 500ms truoc khi gui su kien SEARCH_KEYWORD de tranh spam mang.
 * 
 * @param {string} category - Danh muc dang loc (vd: 'cpu', 'vga', 'ALL')
 * @param {string} keyword - Tu khoa nguoi dung nhap vao o tim kiem
 */
export function useCatalogTracker(category, keyword) {
  // 1. Theo doi su kien loc danh muc
  useEffect(() => {
    if (!category || category === 'ALL') return

    sendTrackingEvent({
      eventType: 'FILTER_CATEGORY',
      categorySlug: category,
    })
  }, [category])

  // 2. Theo doi su kien tim kiem tu khoa co Debounce 500ms
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

