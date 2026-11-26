import { createContext, useContext } from 'react'

/** @type {import('react').Context<import('./cartContextTypes').CartContextValue | null>} */
export const CartContext = createContext(null)

/** @returns {import('./cartContextTypes').CartContextValue} */
export function useCart() {
  const context = useContext(CartContext)
  if (!context) throw new Error('useCart must be used within CartProvider')
  return context
}
