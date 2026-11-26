import type { StoreProduct } from '../data/storeData'
import type { CartItem } from '../utils/cart'

export type CartProduct = Pick<StoreProduct, 'id' | 'name' | 'price'> & Partial<StoreProduct>

export interface CartMutationResult {
  success: boolean
  message?: string
  operationId?: string
  availableQuantity?: number
  remainingAddableQuantity?: number
}

export interface ProductCartState {
  available: number | undefined
  quantity: number
  remaining: number
  canAdd: boolean
  status: 'available' | 'sold-out' | 'limit'
  label: string
}

export interface CartContextValue {
  items: CartItem[]
  addItem: (
    product: CartProduct,
    quantity?: number,
    onAccepted?: () => void,
  ) => Promise<CartMutationResult>
  retryLastAdd: () => Promise<CartMutationResult>
  adding: boolean
  isAdding: (productId: string) => boolean
  setQuantity: (id: string, quantity: number) => Promise<CartMutationResult>
  removeItem: (id: string) => Promise<CartMutationResult>
  clearCart: () => Promise<CartMutationResult>
  totalCount: number
  totalAmount: number
  ready: boolean
  syncing: boolean
  /** Reloads the cart and resolves with the fresh server cart (or null on failure). */
  refresh: () => Promise<{ cartVersion?: number; items?: unknown[] } | null>
  cartVersion: number
  notice: string
  demo: { userId: string | null } | null
  selectDemo: (userId: string | null) => Promise<CartMutationResult>
  productState: (product: CartProduct) => ProductCartState
  reportCartError: (message?: string) => void
  login: (username: string, password: string) => Promise<unknown>
  mergeCart: () => Promise<CartMutationResult>
  applyBuildProposal: (proposalId: string, acknowledged: boolean) => Promise<CartMutationResult>
}
