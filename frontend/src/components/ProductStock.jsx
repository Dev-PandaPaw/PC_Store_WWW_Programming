import React from 'react'
import { useCart } from '../contexts/CartContext'

export default function ProductStock({ product }) {
  const { productState } = useCart()
  const state = productState(product)
  return <p className={`product-stock product-stock--${state.status}`}>
    <span className="product-stock__dot" aria-hidden="true" />{state.label}
  </p>
}
