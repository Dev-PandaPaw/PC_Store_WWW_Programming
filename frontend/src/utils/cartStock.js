// Distinguish a product that stopped selling / has 0 stock from a cart line whose OLD quantity exceeds current stock.
function isProductUnavailableForAnyQuantity(existing, product, available) {
  if (typeof product?.status === 'string' && product.status.toUpperCase() !== 'ACTIVE') {
    return true
  }
  if (existing && existing.sku === null) {
    return true
  }
  const inCart = existing?.quantity || 0
  // Backend sets purchasable = (status == 'ACTIVE' && quantity <= available).
  // Therefore, if purchasable === false while inCart <= available (or available is unknown),
  // the product itself is inactive/unavailable rather than merely over-stock in the cart.
  if (existing?.purchasable === false && (available == null || inCart <= available)) {
    return true
  }
  return false
}

export function remainingAddableQuantity(items, product) {
  const existing = items.find(item => item.productId === product.id || item.id === product.id)
  const available = existing?.availableQuantity ?? product.availableQuantity ?? product.stockOnHand
  const quantity = existing?.quantity || 0
  if (available === 0 || isProductUnavailableForAnyQuantity(existing, product, available)) {
    return 0
  }
  const maxTotal = available == null ? 99 : Math.min(99, Math.max(0, available))
  return Math.max(0, maxTotal - quantity)
}

export function productCartState(items, product) {
  const existing = items.find(item => item.productId === product.id || item.id === product.id)
  const available = existing?.availableQuantity ?? product.availableQuantity ?? product.stockOnHand
  const quantity = existing?.quantity || 0
  const remaining = remainingAddableQuantity(items, product)
  if (available === 0 || isProductUnavailableForAnyQuantity(existing, product, available)) {
    return { available, quantity, remaining: 0, canAdd: false, status: 'sold-out', label: 'Hết hàng' }
  }
  if (remaining <= 0 || quantity >= Math.min(99, available ?? 99)) {
    const overStock = available != null && quantity > available
    return {
      available,
      quantity,
      remaining: 0,
      canAdd: false,
      status: 'limit',
      label: overStock ? `Vượt tồn khả dụng (còn ${available})` : 'Đã đủ số lượng trong giỏ',
    }
  }
  return {
    available,
    quantity,
    remaining,
    canAdd: true,
    status: 'available',
    label: available == null ? 'Kiểm tra khi đặt hàng' : `Còn ${available} sản phẩm`,
  }
}

export function cartStockError(items, product, quantity, adding = false) {
  if (typeof quantity !== 'number' || !Number.isInteger(quantity)) {
    return 'Số lượng phải là số nguyên từ 1 đến 99, không chấp nhận số thập phân.'
  }
  if (quantity < 1 || quantity > 99) {
    return 'Số lượng phải là số nguyên từ 1 đến 99.'
  }
  const existing = items.find(item => item.productId === product.id || item.id === product.id)
  const inCart = existing?.quantity || 0
  const targetQuantity = adding ? inCart + quantity : quantity
  const available = existing?.availableQuantity ?? product.availableQuantity ?? product.stockOnHand

  if (isProductUnavailableForAnyQuantity(existing, product, available)) {
    return 'Sản phẩm hiện đã ngừng bán hoặc không còn khả dụng.'
  }
  if (available === 0) {
    return adding
      ? 'Sản phẩm hiện đã hết hàng. Số lượng còn có thể thêm: 0.'
      : 'Sản phẩm hiện đã hết hàng (tồn khả dụng: 0). Vui lòng xóa sản phẩm khỏi giỏ hàng.'
  }
  if (available != null && targetQuantity > available) {
    const remaining = Math.max(0, Math.min(99, available) - inCart)
    if (!adding && inCart > available) {
      return `Số lượng yêu cầu vượt quá tồn khả dụng (${available}). Giỏ hàng đang có ${inCart}, vui lòng giảm về tối đa ${available} sản phẩm.`
    }
    if (remaining <= 0) {
      return `Giỏ hàng đang có ${inCart}/${available} sản phẩm khả dụng, không thể thêm nữa (số lượng còn có thể thêm: 0).`
    }
    if (adding && inCart > 0) {
      return `Số lượng yêu cầu vượt quá tồn khả dụng (${available}). Giỏ hàng đang có ${inCart}, còn có thể thêm tối đa ${remaining} sản phẩm.`
    }
    if (adding) {
      return `Số lượng yêu cầu vượt quá tồn khả dụng (${available}). Còn có thể thêm tối đa ${remaining} sản phẩm.`
    }
    return `Số lượng yêu cầu vượt quá tồn khả dụng (${available}). Giỏ hàng đang có ${inCart}, còn có thể thêm tối đa ${remaining} sản phẩm.`
  }
  if (targetQuantity > 99) {
    const remainingByCap = Math.max(0, 99 - inCart)
    return `Mỗi sản phẩm chỉ được đặt tối đa 99 sản phẩm. Giỏ hàng đang có ${inCart}, còn có thể thêm tối đa ${remainingByCap} sản phẩm.`
  }
  return ''
}
