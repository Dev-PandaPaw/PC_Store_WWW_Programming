import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const products = ['catalog-core.json', 'catalog-gear.json'].flatMap((file) =>
  JSON.parse(
    readFileSync(new URL(`../src/data/${file}`, import.meta.url), 'utf8'),
  ),
)
const categories = [
  'pc-gaming',
  'pc-office',
  'pc-workstation',
  'cpu',
  'vga',
  'mainboard',
  'ram',
  'storage',
  'mouse',
  'keyboard',
  'monitor',
  'cooling',
  'case',
  'psu',
]

test('catalog contains unique, sourced PC products in every store category', () => {
  assert.equal(
    new Set(products.map((product) => product.id)).size,
    products.length,
    'Duplicate product IDs',
  )
  assert.equal(
    new Set(products.map((product) => product.slug)).size,
    products.length,
    'Duplicate product slugs',
  )
  for (const category of categories)
    assert.ok(
      products.some((product) => product.category === category),
      `Missing category: ${category}`,
    )
  for (const product of products) {
    assert.ok(
      categories.includes(product.category),
      `Unsupported category: ${product.category}`,
    )
    assert.match(product.slug, /^[a-z0-9]+(?:-[a-z0-9]+)*$/)
    assert.doesNotMatch(
      product.name.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd'),
      /laptop|gaming chair|ghe gaming|ban ghe/,
    )
    assert.ok(
      product.name.trim() && product.brand.trim() && product.description.trim(),
      `Missing content: ${product.slug}`,
    )
    assert.ok(
      product.specs.length > 0 &&
        product.specs.every((spec) => spec.label.trim()),
      `Missing specifications: ${product.slug}`,
    )
    const source = new URL(product.source.url)
    assert.equal(source.protocol, 'https:')
    assert.ok(
      ['gearvn.com', 'phongvu.vn'].includes(source.hostname),
      `Unexpected product source: ${source.hostname}`,
    )
    assert.match(product.source.checkedAt, /^\d{4}-\d{2}-\d{2}$/)
    assert.equal(new URL(product.sourceImageUrl).protocol, 'https:')
  }
})

test('prices are valid snapshots and local product images are image files', () => {
  for (const product of products) {
    assert.ok(
      Number.isFinite(product.price) && product.price > 0,
      `Invalid price: ${product.slug}`,
    )
    assert.ok(
      Number.isFinite(product.originalPrice) &&
        product.originalPrice >= product.price,
      `Invalid original price: ${product.slug}`,
    )
    assert.match(
      product.image,
      /^products\/[a-z0-9-]+\.(png|jpg|jpeg|webp)$/,
    )
    const imageFile = new URL(`../src/assets/${product.image}`, import.meta.url)
    const bytes = readFileSync(imageFile)
    const png = bytes
      .subarray(0, 8)
      .equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))
    const jpeg = bytes[0] === 255 && bytes[1] === 216
    const webp =
      bytes.toString('ascii', 0, 4) === 'RIFF' &&
      bytes.toString('ascii', 8, 12) === 'WEBP'
    assert.ok(
      bytes.length > 128 && (png || jpeg || webp),
      `Invalid image: ${fileURLToPath(imageFile)}`,
    )
  }
})
