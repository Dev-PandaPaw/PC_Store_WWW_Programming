// Include queued operations so rapid clicks cannot exceed the last known stock.
export function productCartState(items, product) {
  const existing = items.find(item => item.productId === product.id || item.id === product.id)
  const available = existing?.availableQuantity ?? product.stockOnHand
  const quantity = existing?.quantity || 0
  if (existing?.purchasable === false || available === 0) {
    return { available, quantity, canAdd: false, status: 'sold-out', label: 'Hết hàng' }
  }
  if (quantity >= Math.min(99, available ?? 99)) {
    return { available, quantity, canAdd: false, status: 'limit', label: 'Đã đủ số lượng trong giỏ' }
  }
  return { available, quantity, canAdd: true, status: 'available',
    label: available == null ? 'Kiểm tra khi đặt hàng' : `Còn ${available} sản phẩm` }
}

export function cartStockError(items, product, quantity, adding = false) {
  const existing = items.find(item => item.productId === product.id)
  const requested = quantity + (adding ? existing?.quantity || 0 : 0)
  const available = existing?.availableQuantity ?? product.stockOnHand
  if (existing?.purchasable === false || available === 0) return 'Sản phẩm hiện đã hết hàng hoặc ngừng bán.'
  if (available != null && requested > available) return `Chỉ còn ${available} sản phẩm có thể đặt. Giỏ hàng đang có ${existing?.quantity || 0} sản phẩm.`
  if (requested > 99) return 'Mỗi sản phẩm chỉ được đặt tối đa 99 sản phẩm.'
  return ''
}
