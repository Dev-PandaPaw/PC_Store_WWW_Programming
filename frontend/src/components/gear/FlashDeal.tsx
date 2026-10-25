import React, { useState, useEffect } from 'react';
import { Flame, ChevronLeft, ChevronRight, Zap } from 'lucide-react';
import { FLASH_DEAL_LAPTOPS, StoreProduct } from '../../data/storeData';
import { formatVND } from '../../utils/cart';

interface FlashDealProps {
  onAddToCart: (product: StoreProduct) => void;
}

export const FlashDeal: React.FC<FlashDealProps> = ({ onAddToCart }) => {
  // Timer state
  const [timeLeft, setTimeLeft] = useState({ hours: 9, minutes: 1, seconds: 44 });

  useEffect(() => {
    const interval = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev.seconds > 0) {
          return { ...prev, seconds: prev.seconds - 1 };
        } else if (prev.minutes > 0) {
          return { ...prev, minutes: 59, seconds: 59 };
        } else if (prev.hours > 0) {
          return { hours: prev.hours - 1, minutes: 59, seconds: 59 };
        }
        return { hours: 9, minutes: 0, seconds: 0 };
      });
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  const formatNumber = (num: number) => String(num).padStart(2, '0');

  return (
    <section className="mx-auto max-w-[1260px] px-3 my-6">
      <div className="rounded-md border-2 border-[#ff6600] bg-gradient-to-b from-[#fff6ed] to-white p-3 shadow-sm">
        {/* Header */}
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-orange-200 pb-2.5">
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-1.5 text-[#ff4d00]">
              <Flame className="h-5 w-5 fill-[#ff4d00] animate-bounce" />
              <h2 className="text-base md:text-lg font-black tracking-wide text-neutral-900 uppercase">
                FLASH DEAL LAPTOP
              </h2>
            </div>

            {/* Countdown Timer */}
            <div className="flex items-center gap-1 text-xs font-bold">
              <span className="flex h-6 min-w-[24px] items-center justify-center rounded bg-black px-1.5 text-white">
                {formatNumber(timeLeft.hours)}
              </span>
              <span className="font-extrabold text-black">:</span>
              <span className="flex h-6 min-w-[24px] items-center justify-center rounded bg-black px-1.5 text-white">
                {formatNumber(timeLeft.minutes)}
              </span>
              <span className="font-extrabold text-black">:</span>
              <span className="flex h-6 min-w-[24px] items-center justify-center rounded bg-black px-1.5 text-white">
                {formatNumber(timeLeft.seconds)}
              </span>
            </div>
          </div>

          <a
            href="#xem-tat-ca"
            className="text-xs font-semibold text-[#2563eb] hover:underline flex items-center gap-0.5"
          >
            Xem tất cả &gt;
          </a>
        </div>

        {/* 6 Laptop Cards Container */}
        <div className="relative pt-3 group">
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-2.5">
            {FLASH_DEAL_LAPTOPS.map((item) => (
              <div
                key={item.id}
                className="group/card flex flex-col justify-between rounded-md bg-white p-2 border border-orange-100 hover:border-orange-400 hover:shadow-md transition-all relative"
              >
                {/* Product Image */}
                <div className="relative aspect-square w-full overflow-hidden rounded bg-gray-50 flex items-center justify-center p-2">
                  <img
                    src={item.image}
                    alt={item.name}
                    className="h-full w-full object-contain group-hover/card:scale-105 transition-transform duration-300"
                    loading="lazy"
                  />
                  <span className="absolute top-1 left-1 flex items-center gap-0.5 rounded bg-blue-600 px-1 py-0.5 text-[9px] font-bold text-white uppercase">
                    <Zap className="h-2.5 w-2.5 fill-yellow-300 text-yellow-300" />
                    Deal
                  </span>
                </div>

                {/* Details */}
                <div className="mt-2 flex flex-col flex-1 justify-between">
                  <div>
                    <h3
                      className="text-xs font-medium text-gray-800 line-clamp-2 hover:text-[#2563eb] transition-colors cursor-pointer"
                      title={item.name}
                    >
                      {item.name}
                    </h3>
                  </div>

                  {/* Price */}
                  <div className="mt-2 pt-1 border-t border-gray-100">
                    <span className="block text-sm font-black text-[#2563eb]">
                      {formatVND(item.price)}
                    </span>
                    <span className="text-[11px] text-gray-400 line-through">
                      {formatVND(item.originalPrice)}
                    </span>

                    {/* Quick Add Button */}
                    <button
                      type="button"
                      onClick={() => onAddToCart(item)}
                      className="mt-2 w-full rounded bg-orange-500 hover:bg-orange-600 py-1 text-[11px] font-bold text-white transition-colors flex items-center justify-center gap-1 shadow-xs"
                    >
                      Mua ngay
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Navigation arrow buttons */}
          <button
            type="button"
            aria-label="Previous"
            className="absolute -left-2 top-1/2 -translate-y-1/2 flex h-7 w-7 items-center justify-center rounded-full bg-white text-gray-700 shadow-md border border-gray-200 hover:bg-gray-50"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
          <button
            type="button"
            aria-label="Next"
            className="absolute -right-2 top-1/2 -translate-y-1/2 flex h-7 w-7 items-center justify-center rounded-full bg-white text-gray-700 shadow-md border border-gray-200 hover:bg-gray-50"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    </section>
  );
};
