import React from 'react';
import { ArrowRight, Sparkles } from 'lucide-react';

export const MidPromoBanners: React.FC = () => {
  return (
    <section className="mx-auto max-w-[1260px] px-3 my-6">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-3">
        {/* Left Big Monitor Banner (7 cols) */}
        <div className="lg:col-span-7 relative overflow-hidden rounded-md bg-neutral-950 border border-neutral-800 p-5 text-white flex flex-col justify-between shadow-sm group cursor-pointer">
          <div className="relative z-10 space-y-2">
            <span className="inline-flex items-center gap-1 rounded bg-blue-600 px-2 py-0.5 text-[11px] font-black uppercase text-white shadow">
              <Sparkles className="h-3 w-3" />
              Siêu Giảm Giá
            </span>
            <h3 className="text-xl sm:text-2xl font-black tracking-wide leading-tight">
              MÀN HÌNH CHÍNH HÃNG
            </h3>
            <p className="text-sm sm:text-base font-extrabold text-red-500">
              GIẢM SỐC ĐẾN 53%
            </p>
            <div className="text-xs text-gray-300">
              Chỉ từ <span className="text-yellow-400 font-extrabold text-base">1.590.000đ</span>
            </div>
          </div>

          <div className="relative z-10 mt-4 flex items-center gap-1 text-xs font-bold text-gray-300 group-hover:text-white transition-colors">
            <span>Săn deal ngay</span>
            <ArrowRight className="h-3.5 w-3.5 group-hover:translate-x-1 transition-transform" />
          </div>

          {/* Background illustration */}
          <div className="absolute right-0 bottom-0 top-0 w-1/2 overflow-hidden opacity-40 group-hover:opacity-50 transition-opacity">
            <img
              src="https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=600&q=80"
              alt="Màn hình gaming"
              className="h-full w-full object-cover"
              loading="lazy"
            />
            <div className="absolute inset-0 bg-gradient-to-r from-neutral-950 via-neutral-950/80 to-transparent" />
          </div>
        </div>

        {/* Right 2 Stacked Promo Cards (5 cols) */}
        <div className="lg:col-span-5 flex flex-col gap-3 justify-between">
          {/* Card 1: Combo Đẹp Cả Đôi */}
          <div className="flex-1 rounded-md bg-gradient-to-r from-emerald-100 to-teal-50 border border-emerald-200 p-3.5 flex items-center justify-between cursor-pointer hover:shadow-sm transition-all group">
            <div className="space-y-1">
              <span className="rounded bg-emerald-600 px-1.5 py-0.5 text-[9px] font-bold text-white uppercase">
                Khuyến Mãi Hot
              </span>
              <h4 className="text-sm font-extrabold text-emerald-950 leading-snug">
                Combo Đẹp Cả Đôi
              </h4>
              <p className="text-xs text-emerald-800 font-medium">
                Mua Bàn Phím Tặng Chuột Chuẩn Pro
              </p>
            </div>
            <img
              src="https://images.unsplash.com/photo-1595225476474-87563907a212?auto=format&fit=crop&w=160&q=80"
              alt="Combo gear"
              className="h-14 w-16 object-cover rounded shadow-xs group-hover:scale-105 transition-transform"
              loading="lazy"
            />
          </div>

          {/* Card 2: Deal Bàn Ghế Thả Ga Lựa Chọn */}
          <div className="flex-1 rounded-md bg-gradient-to-r from-sky-100 to-blue-50 border border-sky-200 p-3.5 flex items-center justify-between cursor-pointer hover:shadow-sm transition-all group">
            <div className="space-y-1">
              <span className="rounded bg-sky-600 px-1.5 py-0.5 text-[9px] font-bold text-white uppercase">
                Gaming Setup
              </span>
              <h4 className="text-sm font-extrabold text-sky-950 leading-snug">
                Deal Bàn Ghế Thả Ga Lựa Chọn
              </h4>
              <p className="text-xs font-black text-blue-600">
                Giảm đến 40%
              </p>
            </div>
            <img
              src="https://images.unsplash.com/photo-1598550476439-6847785fcea6?auto=format&fit=crop&w=160&q=80"
              alt="Ghế gaming"
              className="h-14 w-16 object-cover rounded shadow-xs group-hover:scale-105 transition-transform"
              loading="lazy"
            />
          </div>
        </div>
      </div>
    </section>
  );
};
