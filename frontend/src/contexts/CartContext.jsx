import React, { createContext, useContext, useState, useEffect } from 'react'

const CartContext = createContext(null)

export function CartProvider({ children }) {
  const [items, setItems] = useState(() => {
    try {
      const saved = localStorage.getItem('pc_cart')
      return saved ? JSON.parse(saved) : []
    } catch {
      return []
    }
  })

  useEffect(() => {
    try {
      localStorage.setItem('pc_cart', JSON.stringify(items))
    } catch (e) {
      console.error(e)
    }
  }, [items])

  const addItem = (product, quantity = 1) => {
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
