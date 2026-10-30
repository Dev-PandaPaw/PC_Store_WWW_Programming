/**
 * Currency and price formatting utilities for GEARVN PC Store
 */

export function formatVND(amount: number): string {
  return new Intl.NumberFormat('vi-VN').format(amount) + '₫';
}

export function calculateDiscount(originalPrice: number, discountedPrice: number): number {
  if (originalPrice <= 0 || discountedPrice >= originalPrice) return 0;
  return Math.round(((originalPrice - discountedPrice) / originalPrice) * 100);
}

export interface CartItem {
  id: string;
  name: string;
  price: number;
  originalPrice?: number;
  image: string;
  quantity: number;
}

export function calculateCartTotal(items: CartItem[]): { subtotal: number; totalItems: number } {
  const subtotal = items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const totalItems = items.reduce((sum, item) => sum + item.quantity, 0);
  return { subtotal, totalItems };
}
