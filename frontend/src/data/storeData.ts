export interface StoreProduct {
  id: string
  slug: string
  name: string
  category: string
  brand: string
  price: number
  originalPrice: number
  image: string
  specs: { label: string }[]
  description: string
  imageKey?: string
  stockOnHand: number
}

export const CATEGORIES = [
  { id: 'pc-gaming', name: 'PC Gaming', icon: 'Gamepad2' },
  { id: 'pc-office', name: 'PC văn phòng', icon: 'Monitor' },
  { id: 'pc-workstation', name: 'PC đồ họa', icon: 'Cpu' },
  { id: 'cpu', name: 'Bộ vi xử lý', icon: 'Cpu' },
  { id: 'mainboard', name: 'Bo mạch chủ', icon: 'CircuitBoard' },
  { id: 'vga', name: 'Card đồ họa', icon: 'CircuitBoard' },
  { id: 'ram', name: 'Bộ nhớ RAM', icon: 'MemoryStick' },
  { id: 'storage', name: 'Ổ cứng SSD', icon: 'HardDrive' },
  { id: 'cooling', name: 'Tản nhiệt', icon: 'Fan' },
  { id: 'case', name: 'Vỏ case', icon: 'PcCase' },
  { id: 'psu', name: 'Nguồn máy tính', icon: 'Zap' },
  { id: 'monitor', name: 'Màn hình', icon: 'Monitor' },
  { id: 'keyboard', name: 'Bàn phím', icon: 'Keyboard' },
  { id: 'mouse', name: 'Chuột máy tính', icon: 'Mouse' },
]

export const CATEGORY_GROUPS: Record<string, string[]> = {
  pc: ['pc-gaming', 'pc-office', 'pc-workstation'],
  components: [
    'cpu',
    'mainboard',
    'vga',
    'ram',
    'storage',
    'cooling',
    'case',
    'psu',
  ],
  memory: ['ram', 'storage'],
  build: ['cooling', 'case', 'psu'],
  gear: ['keyboard', 'mouse'],
}

export const CATEGORY_LABELS: Record<string, string> = {
  ALL: 'Tất cả sản phẩm',
  pc: 'PC bộ',
  components: 'Linh kiện PC',
  memory: 'RAM & SSD',
  build: 'Case, nguồn & tản nhiệt',
  gear: 'Chuột & bàn phím',
  ...Object.fromEntries(
    CATEGORIES.map((category) => [category.id, category.name]),
  ),
}

const dealCategories = [
  'pc-gaming',
  'vga',
  'cooling',
  'keyboard',
  'mouse',
  'monitor',
  'storage',
  'psu',
  'ram',
  'case',
]

export const selectFlashDeals = (products: StoreProduct[]): StoreProduct[] => dealCategories.flatMap(
  (category) => {
    const deals = products.filter(
      (product) =>
        product.category === category && product.originalPrice > product.price,
    ).sort(
      (a, b) => 1 - b.price / b.originalPrice - (1 - a.price / a.originalPrice),
    )
    return deals.slice(0, 1)
  },
)

export function matchesCategory(
  product: StoreProduct,
  category: string,
): boolean {
  return (
    !category ||
    category === 'ALL' ||
    product.category === category ||
    Boolean(CATEGORY_GROUPS[category]?.includes(product.category))
  )
}

export function normalizeSearch(value: string): string {
  return value
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
}
