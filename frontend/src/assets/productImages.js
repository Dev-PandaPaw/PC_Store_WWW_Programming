// Vite bundles every checked-in product photo and rewrites URLs for dev and production.
// Database image keys remain stable across machines and builds.
const images = import.meta.glob('./products/*.{png,jpg,jpeg,webp}', {
  eager: true,
  query: '?url',
  import: 'default',
})

export function resolveProductImage(key) {
  if (!key || typeof key !== 'string') {
    return images['./products/pc-gvn-i5-12400f-rtx-3050.png'] || ''
  }

  // Nếu đã là URL tuyệt đối hoặc asset Vite đã bundle
  if (
    key.startsWith('http://') ||
    key.startsWith('https://') ||
    key.startsWith('data:') ||
    key.startsWith('/src/') ||
    key.startsWith('/assets/') ||
    key.startsWith('/@')
  ) {
    return key
  }

  // Chuẩn hóa key: loại bỏ /images/ hoặc dấu gạch đầu
  const clean = key.replace(/^\/?images\//, '').replace(/^\//, '')

  // 1. Khớp trực tiếp (ví dụ: products/pc-gvn-i5-12400f-rtx-3050.png)
  let image = images[`./${clean}`]

  // 2. Thử thêm products/ nếu chưa có
  if (!image && !clean.startsWith('products/')) {
    image = images[`./products/${clean}`]
  }

  // 3. Khớp theo tên file nếu có đường dẫn lồng nhau
  if (!image) {
    const filename = clean.split('/').pop()
    const matchKey = Object.keys(images).find(k => k.endsWith(`/${filename}`))
    if (matchKey) {
      image = images[matchKey]
    }
  }

  // 4. Fallback ảnh mặc định hợp lệ để không bị 404
  if (!image) {
    return images['./products/pc-gvn-i5-12400f-rtx-3050.png'] || ''
  }

  return image
}
