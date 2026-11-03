export function projectCart(confirmed, operations) {
  let items = confirmed.items.map(item => ({ ...item }))
  for (const operation of operations) {
    const { type, id, quantity, product } = operation
    const existing = items.find(item => item.productId === id)
    if (type === 'add') {
      if (existing) existing.quantity += quantity
      else items.push({ productId: id, name: product.name, sku: product.sku,
        unitPrice: product.price, quantity, availableQuantity: product.stockOnHand ?? 99,
        purchasable: true, image: product.image || '' })
    } else if (type === 'quantity' && existing) existing.quantity = quantity
    else if (type === 'remove') items = items.filter(item => item.productId !== id)
    else if (type === 'clear') items = []
  }
  return { ...confirmed, items,
    totalQuantity: items.reduce((sum, item) => sum + item.quantity, 0),
    subtotal: items.reduce((sum, item) => sum + Number(item.unitPrice) * item.quantity, 0) }
}
