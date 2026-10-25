import React from 'react';
import { Monitor, Gift, ShoppingBag, Shield } from 'lucide-react';

export const PromoBanners: React.FC = () => {
  return (
    <section className="mx-auto max-w-[1260px] px-3 my-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {/* Banner Left: Build PC TẶNG Màn Hình */}
        <div className="relative overflow-hidden rounded-md bg-gradient-to-r from-cyan-600 via-teal-600 to-emerald-700 p-4 text-white shadow-sm flex items-center justify-between cursor-pointer group hover:shadow-md transition-shadow">
          <div className="space-y-1.5 z-10">
            <h3 className="text-base sm:text-lg font-black tracking-wide leading-tight">
              Build PC TẶNG Màn Hình
            </h3>
            <p className="text-xs sm:text-sm font-extrabold text-yellow-300">
              Giá chỉ từ <span className="text-base sm:text-lg">11.990.000đ</span>
            </p>
            <div className="flex flex-wrap items-center gap-1.5 pt-1">
              <span className="flex items-center gap-1 rounded bg-black/40 px-2 py-0.5 text-[10px] font-bold text-cyan-200 border border-cyan-400/30">
                <Monitor className="h-2.5 w-2.5" />
                Tặng Màn hình 240Hz
              </span>
              <span className="flex items-center gap-1 rounded bg-yellow-400 px-2 py-0.5 text-[10px] font-black text-black">
                Giảm 15% gói build PC
              </span>
            </div>
          </div>

          {/* Decorative Visual */}
          <div className="relative shrink-0 flex items-center justify-center h-20 w-24 sm:w-28 opacity-90 group-hover:scale-105 transition-transform">
            <div className="absolute inset-0 rounded-full bg-white/10 blur-xl"></div>
            <img
              src="https://images.unsplash.com/photo-1587202372775-e229f172b9d7?auto=format&fit=crop&w=300&q=80"
              alt="Build PC"
              className="h-16 w-20 object-cover rounded shadow"
              loading="lazy"
            />
          </div>
        </div>

        {/* Banner Right: Pack Laptop Tặng Gear */}
        <div className="relative overflow-hidden rounded-md bg-gradient-to-r from-blue-700 via-indigo-700 to-purple-800 p-4 text-white shadow-sm flex items-center justify-between cursor-pointer group hover:shadow-md transition-shadow">
          <div className="space-y-1.5 z-10">
            <h3 className="text-base sm:text-lg font-black tracking-wide leading-tight">
              Pack Laptop Tặng Gear
            </h3>
            <p className="text-xs sm:text-sm font-extrabold text-yellow-300">
              Quà tặng lên đến <span className="text-base sm:text-lg">15.000.000đ</span>
            </p>
            <div className="flex flex-wrap items-center gap-1.5 pt-1">
              <span className="flex items-center gap-1 rounded bg-yellow-400 px-2 py-0.5 text-[10px] font-black text-black">
                <ShoppingBag className="h-2.5 w-2.5" />
                Mua 1 được 3
              </span>
              <span className="flex items-center gap-1 rounded bg-black/40 px-2 py-0.5 text-[10px] font-bold text-blue-200 border border-blue-400/30">
                <Gift className="h-2.5 w-2.5" />
                GAMING FOR LIFE
              </span>
            </div>
          </div>

          {/* Decorative Visual */}
          <div className="relative shrink-0 flex items-center justify-center h-20 w-24 sm:w-28 opacity-90 group-hover:scale-105 transition-transform">
            <div className="absolute inset-0 rounded-full bg-white/10 blur-xl"></div>
            <img
              src="https://images.unsplash.com/photo-1593642702821-c8da6771f0c6?auto=format&fit=crop&w=300&q=80"
              alt="Laptop Gear"
              className="h-16 w-20 object-cover rounded shadow"
              loading="lazy"
            />
          </div>
        </div>
      </div>
    </section>
  );
};
