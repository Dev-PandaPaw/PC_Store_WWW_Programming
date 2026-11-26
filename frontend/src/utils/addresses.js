export function normalizeAddress(value) {
  return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim().replace(/\s+/g, ' ')
}

export function filterAddresses(options, query) {
  const words = normalizeAddress(query).split(/\s+/).filter(Boolean)
  return options.filter(option => words.every(word => normalizeAddress(option.name).includes(word)))
}

export function selectedAddress(options, name) {
  return options.find(option => normalizeAddress(option.name) === normalizeAddress(name))
}

export function changeProvince(shipping, province) {
  return { ...shipping, province, district: '', ward: '' }
}

export function changeDistrict(shipping, district) {
  return { ...shipping, district, ward: '' }
}
