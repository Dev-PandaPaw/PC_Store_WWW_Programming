import type { StoreProduct } from '../data/storeData'
import type { CartItem } from '../utils/cart'

export type CartProduct = Pick<StoreProduct, 'id' | 'name' | 'price'> & Partial<StoreProduct>

export interface CartMutationResult {
  success: boolean
  message?: string
}

export interface ProductCartState {
  available: number | undefined
  quantity: number
  canAdd: boolean
  status: 'available' | 'sold-out' | 'limit'
  label: string
}

export interface CartContextValue {
  items: CartItem[]
  addItem: (product: CartProduct, quantity?: number, onAccepted?: () => void) => Promise<CartMutationResult>
  setQuantity: (id: string, quantity: number) => Promise<CartMutationResult>
  removeItem: (id: string) => Promise<CartMutationResult>
  clearCart: () => Promise<CartMutationResult>
  totalCount: number
  totalAmount: number
  ready: boolean
  syncing: boolean
  refresh: () => Promise<void>
  cartVersion: number
  notice: string
  demo: { userId: string | null } | null
  selectDemo: (userId: string | null) => Promise<CartMutationResult>
  productState: (product: CartProduct) => ProductCartState
  reportCartError: (message?: string) => void
  login: (username: string, password: string) => Promise<unknown>
  mergeCart: () => Promise<CartMutationResult>
}
