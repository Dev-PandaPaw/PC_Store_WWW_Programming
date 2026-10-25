import React, { useState } from 'react';
import { ShieldCheck } from 'lucide-react';
import { MONITORS, StoreProduct } from '../../data/storeData';
import { ProductCard } from './ProductCard';

interface ProductSectionMonitorProps {
  onAddToCart: (p: StoreProduct) => void;
}

const MONITOR_FILTERS = ['LG', 'Asus', 'ViewSonic', 'Dell', 'Gigabyte', 'AOC', 'Acer', 'HKC'];

export const ProductSectionMonitor: React.FC<ProductSectionMonitorProps> = ({ onAddToCart }) => {
  const [activeFilter, setActiveFilter] = useState('Asus');

  return (
    <section id="man-hinh" className="mx-auto max-w-[1260px] px-3 my-6">
      {/* Section Header */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b-2 border-gray-200 pb-2">
        <div className="flex flex-wrap items-center gap-2.5">
          <h2 className="text-base sm:text-lg font-black text-gray-900 uppercase tracking-tight">
            Màn hình chính hãng
          </h2>
          <span className="flex items-center gap-1 rounded bg-blue-50 border border-blue-200 px-2 py-0.5 text-xs font-bold text-[#2563eb]">
            <ShieldCheck className="h-3.5 w-3.5" />
            Bảo hành 1 đổi 1
          </span>
        </div>

        {/* Filters */}
        <div className="flex items-center gap-1.5 overflow-x-auto whitespace-nowrap scrollbar-none text-xs">
          {MONITOR_FILTERS.map((filter) => (
            <button
              key={filter}
              type="button"
              onClick={() => setActiveFilter(filter)}
              className={`rounded px-2.5 py-1 font-semibold transition-colors ${
                activeFilter === filter
                  ? 'bg-gray-900 text-white shadow-xs'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {filter}
            </button>
          ))}
          <a
            href="#xem-tat-ca-man-hinh"
            className="ml-2 font-bold text-[#2563eb] hover:underline shrink-0"
          >
            Xem tất cả &gt;
          </a>
        </div>
      </div>

      {/* 10 Monitor Cards Grid (2 rows of 5) */}
      <div className="mt-3.5 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-2.5">
        {MONITORS.map((product) => (
          <ProductCard key={product.id} product={product} onAddToCart={onAddToCart} />
        ))}
      </div>
    </section>
  );
};
