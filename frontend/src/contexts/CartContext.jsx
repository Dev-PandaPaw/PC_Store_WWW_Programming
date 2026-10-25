import React, { createContext, useContext, useState, useEffect } from 'react'
import { useCatalog } from './CatalogContext'

const CartContext = createContext(null)

export function CartProvider({ children }) {
  const { PRODUCTS } = useCatalog()
  const [items, setItems] = useState(() => {
    try {
      const saved = localStorage.getItem('pc_cart')
      return saved ? JSON.parse(saved) : []
    } catch {
      return []
    }
  })

  // Refresh old carts with current database IDs, prices and portable bundled photos.
  useEffect(() => {
    if (!PRODUCTS.length) return
    setItems(previous => previous.map(item => {
      const current = PRODUCTS.find(product => product.id === item.id || product.catalogKey === item.id)
      return current ? { ...item, id: current.id, name: current.name, price: current.price,
        image: current.image, imageKey: current.imageKey } : item
    }))
  }, [PRODUCTS])

  useEffect(() => {
    try {
      localStorage.setItem('pc_cart', JSON.stringify(items))
    } catch (e) {
      console.error(e)
    }
  }, [items])

  const addItem = (product, quantity = 1) => {
    const current = PRODUCTS.find(item => item.id === product.id)
    if (current) product = { ...product, image: current.image, imageKey: current.imageKey }
    setItems(prev => {
      const existing = prev.find(i => i.id === product.id)
      if (existing) {
        return prev.map(i => i.id === product.id ? { ...i, quantity: i.quantity + quantity } : i)
      }
      return [...prev, { ...product, quantity }]
    })
  }

  const removeItem = (productId) => {
    setItems(prev => prev.filter(i => i.id !== productId))
  }

  const clearCart = () => setItems([])

  const totalCount = items.reduce((sum, i) => sum + i.quantity, 0)
  const totalAmount = items.reduce((sum, i) => sum + (i.price || 0) * i.quantity, 0)

  return (
    <CartContext.Provider value={{ items, addItem, removeItem, clearCart, totalCount, totalAmount }}>
      {children}
    </CartContext.Provider>
  )
}

export function useCart() {
  return useContext(CartContext)
}
