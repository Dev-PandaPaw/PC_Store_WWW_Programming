import React from 'react';
import { X, ShoppingBag, Trash2, Plus, Minus, ArrowRight } from 'lucide-react';
import { CartItem, formatVND } from '../../utils/cart';

interface CartDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  items: CartItem[];
  onUpdateQuantity: (id: string, delta: number) => void;
  onRemoveItem: (id: string) => void;
  onClearCart: () => void;
}

export const CartDrawer: React.FC<CartDrawerProps> = ({
  isOpen,
  onClose,
  items,
  onUpdateQuantity,
  onRemoveItem,
  onClearCart,
}) => {
  if (!isOpen) return null;

  const subtotal = items.reduce((sum, item) => sum + item.price * item.quantity, 0);

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/40 backdrop-blur-xs transition-opacity duration-300">
      {/* Background click to dismiss */}
      <div className="absolute inset-0" onClick={onClose} />

      {/* Slide-out Drawer Panel */}
      <div className="relative z-10 flex h-full w-full max-w-sm flex-col bg-white shadow-2xl transition-transform duration-300 animate-in slide-in-from-right">
        {/* Drawer Header */}
        <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3.5">
          <h2 className="text-base font-black text-gray-900">
            Giỏ hàng của bạn
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-gray-500 hover:bg-gray-100 hover:text-black transition-colors"
            aria-label="Đóng giỏ hàng"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Drawer Content */}
        {items.length === 0 ? (
          /* Empty State exactly matching the Figma screenshot */
          <div className="flex flex-1 flex-col items-center justify-center p-6 text-center">
            {/* Shopping Cart Outline Icon with wheels */}
            <div className="mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-gray-50 text-gray-300">
              <ShoppingBag className="h-10 w-10 stroke-[1.5]" />
            </div>

            <h3 className="text-sm font-bold text-gray-800">
              Giỏ hàng trống
            </h3>
            <p className="mt-1 text-xs text-gray-500">
              Hãy thêm sản phẩm vào giỏ hàng nhé!
            </p>

            <button
              type="button"
              onClick={onClose}
              className="mt-5 rounded border border-gray-300 bg-white px-5 py-2 text-xs font-semibold text-gray-700 hover:bg-gray-50 hover:border-gray-400 transition-colors shadow-xs"
            >
              Tiếp tục mua sắm
            </button>
          </div>
        ) : (
          /* Cart items list */
          <div className="flex flex-1 flex-col justify-between overflow-hidden">
            <div className="flex-1 overflow-y-auto p-4 space-y-3 divide-y divide-gray-100">
              {items.map((item) => (
                <div key={item.id} className="pt-3 first:pt-0 flex gap-3 items-center">
                  <img
                    src={item.image}
                    alt={item.name}
                    className="h-14 w-14 rounded object-cover border border-gray-200 shrink-0"
                  />
                  <div className="flex-1 min-w-0">
                    <h4 className="text-xs font-semibold text-gray-900 truncate">
                      {item.name}
                    </h4>
                    <span className="text-xs font-bold text-[#2563eb]">
                      {formatVND(item.price)}
                    </span>
                    <div className="mt-1.5 flex items-center gap-2">
                      <div className="flex items-center rounded border border-gray-200">
                        <button
                          type="button"
                          onClick={() => onUpdateQuantity(item.id, -1)}
                          className="px-1.5 py-0.5 text-gray-600 hover:bg-gray-100"
                          aria-label="Giảm số lượng"
                        >
                          <Minus className="h-3 w-3" />
                        </button>
                        <span className="px-2 text-xs font-bold">{item.quantity}</span>
                        <button
                          type="button"
                          onClick={() => onUpdateQuantity(item.id, 1)}
                          className="px-1.5 py-0.5 text-gray-600 hover:bg-gray-100"
                          aria-label="Tăng số lượng"
                        >
                          <Plus className="h-3 w-3" />
                        </button>
                      </div>

                      <button
                        type="button"
                        onClick={() => onRemoveItem(item.id)}
                        className="text-gray-400 hover:text-blue-600 p-1"
                        title="Xóa"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Footer Summary & Checkout */}
            <div className="border-t border-gray-200 p-4 bg-gray-50 space-y-3">
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-600 font-medium">Tổng tiền:</span>
                <span className="text-base font-black text-[#2563eb]">
                  {formatVND(subtotal)}
                </span>
              </div>

              <div className="space-y-2">
                <a
                  href="/cart"
                  className="w-full rounded bg-[#2563eb] hover:bg-blue-700 py-2.5 text-xs font-bold text-white transition-colors flex items-center justify-center gap-1.5 shadow text-center"
                >
                  <span>Tiến hành đặt hàng</span>
                  <ArrowRight className="h-4 w-4" />
                </a>

                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={onClose}
                    className="flex-1 rounded border border-gray-300 bg-white py-1.5 text-xs font-semibold text-gray-700 hover:bg-gray-100"
                  >
                    Tiếp tục mua sắm
                  </button>
                  <button
                    type="button"
                    onClick={onClearCart}
                    className="rounded border border-blue-200 px-3 py-1.5 text-xs font-semibold text-blue-600 hover:bg-blue-50"
                  >
                    Xóa hết
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
