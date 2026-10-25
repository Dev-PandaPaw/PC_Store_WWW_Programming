import React from 'react';
import { Zap, ShoppingCart } from 'lucide-react';
import { StoreProduct } from '../../data/storeData';
import { formatVND } from '../../utils/cart';

interface ProductCardProps {
  product: StoreProduct;
  onAddToCart: (p: StoreProduct) => void;
}

export const ProductCard: React.FC<ProductCardProps> = ({ product, onAddToCart }) => {
  return (
    <div className="group flex flex-col justify-between rounded-md border border-gray-200 bg-white p-2.5 hover:border-blue-500 hover:shadow-md transition-all duration-200 relative">
      {/* Top badges (Flashsale / Giảm xx%) */}
      <div className="flex items-center justify-between gap-1 mb-1 min-h-[20px]">
        {product.badge ? (
          <span className="rounded bg-blue-600 px-1.5 py-0.5 text-[10px] font-bold text-white uppercase tracking-tight">
            {product.badge}
          </span>
        ) : product.discountPercent && product.discountPercent > 0 ? (
          <span className="rounded bg-rose-600 px-1.5 py-0.5 text-[10px] font-bold text-white tracking-tight">
            Giảm {product.discountPercent}%
          </span>
        ) : (
          <span />
        )}

        {product.isHoaToc && (
          <span className="flex items-center gap-0.5 text-[10px] font-black text-amber-500">
            <Zap className="h-3 w-3 fill-amber-500" />
            HỎA TỐC
          </span>
        )}
      </div>

      {/* Product Image */}
      <div className="relative aspect-square w-full overflow-hidden rounded bg-gray-50 flex items-center justify-center p-2">
        <img
          src={product.image}
          alt={product.name}
          className="h-full w-full object-contain group-hover:scale-105 transition-transform duration-300"
          loading="lazy"
        />

        {/* Hover Quick Add to Cart button */}
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onAddToCart(product);
          }}
          className="absolute bottom-2 right-2 flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 text-white shadow-md opacity-0 group-hover:opacity-100 transition-all hover:bg-blue-700"
          title="Thêm vào giỏ"
        >
          <ShoppingCart className="h-4 w-4" />
        </button>
      </div>

      {/* Title */}
      <h3
        className="mt-2 text-xs font-semibold text-gray-800 line-clamp-2 leading-relaxed min-h-[36px] group-hover:text-blue-600 transition-colors"
        title={product.name}
      >
        {product.name}
      </h3>

      {/* Specs Tags */}
      <div className="mt-2 flex flex-wrap gap-1 min-h-[44px] items-start content-start">
        {product.specs.slice(0, 5).map((spec, idx) => (
          <span
            key={idx}
            className="rounded bg-gray-100 px-1.5 py-0.5 text-[9px] font-medium text-gray-600 border border-gray-200/60"
          >
            {spec.label}
          </span>
        ))}
      </div>

      {/* Price block */}
      <div className="mt-2.5 pt-2 border-t border-gray-100">
        <div className="flex items-baseline gap-1.5">
          {product.originalPrice > product.price && (
            <>
              <span className="text-[11px] text-gray-400 line-through">
                {formatVND(product.originalPrice)}
              </span>
              {product.discountPercent !== undefined && product.discountPercent > 0 && (
                <span className="text-[10px] font-bold text-rose-600">
                  -{product.discountPercent}%
                </span>
              )}
            </>
          )}
        </div>

        <div className="text-sm font-black text-rose-600 leading-tight mt-0.5">
          {formatVND(product.price)}
        </div>
      </div>

      {/* Free gift / Bonus tag */}
      {product.bonusText && (
        <div className="mt-2 rounded bg-blue-50 border border-blue-100 p-1 text-[10px] font-medium text-blue-800 line-clamp-1">
          🎁 {product.bonusText}
        </div>
      )}
    </div>
  );
};
