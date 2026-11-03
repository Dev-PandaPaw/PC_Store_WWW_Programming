export function shippingFromCustomer(customer) {
  return {
    recipient: customer?.fullName || customer?.name || '',
    phone: customer?.phone || '', province: '', district: '', ward: '', addressLine: '',
  }
}

export function loginDestination(from) {
  if (from && typeof from === 'object') from = `${from.pathname || '/'}${from.search || ''}`
  return typeof from === 'string' && /^\/(?!\/|\\)/.test(from) && !from.startsWith('/login') ? from : '/'
}

// A late account response must never replace what the customer is typing.
export function mergeCustomerShipping(shipping, customer, edited) {
  const latest = shippingFromCustomer(customer)
  return {
    ...shipping,
    recipient: edited.has('recipient') ? shipping.recipient : latest.recipient,
    phone: edited.has('phone') ? shipping.phone : latest.phone,
  }
}

export function shortOrderCode(code) {
  if (!code || code.length <= 16) return code || ''
  const reference = code.replace(/^PC-/i, '').replace(/-/g, '')
  return `PC-${reference.slice(0, 4).toUpperCase()}-${reference.slice(-4).toUpperCase()}`
}
