// Vite bundles every checked-in product photo and rewrites URLs for dev and production.
// Database image keys remain stable across machines and builds.
const images = import.meta.glob('./products/*.{png,jpg,jpeg,webp}', {
  eager: true,
  query: '?url',
  import: 'default',
})

export function resolveProductImage(key) {
  const logicalKey = key?.replace(/^\/images\//, '')
  const image = images[`./${logicalKey}`]
  if (!image) throw new Error(`Missing bundled product image: ${logicalKey}`)
  return image
}
