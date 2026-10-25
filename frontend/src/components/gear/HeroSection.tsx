import React, { useState, useEffect } from 'react';
import {
  Laptop,
  Gamepad2,
  Cpu,
  CircuitBoard,
  Fan,
  Database,
  Mic,
  Monitor,
  Keyboard,
  Mouse,
  Headphones,
  Armchair,
  Wifi,
  Gamepad,
  RotateCcw,
  Info,
  ChevronRight,
  ChevronLeft,
} from 'lucide-react';
import { CATEGORIES } from '../../data/storeData';

const iconMap: Record<string, React.ReactNode> = {
  Laptop: <Laptop className="h-4 w-4 text-blue-600" />,
  Gamepad2: <Gamepad2 className="h-4 w-4 text-blue-600" />,
  Cpu: <Cpu className="h-4 w-4 text-blue-600" />,
  CircuitBoard: <CircuitBoard className="h-4 w-4 text-blue-600" />,
  Fan: <Fan className="h-4 w-4 text-blue-600" />,
  Database: <Database className="h-4 w-4 text-blue-600" />,
  Mic: <Mic className="h-4 w-4 text-blue-600" />,
  Monitor: <Monitor className="h-4 w-4 text-blue-600" />,
  Keyboard: <Keyboard className="h-4 w-4 text-blue-600" />,
  Mouse: <Mouse className="h-4 w-4 text-blue-600" />,
  Headphones: <Headphones className="h-4 w-4 text-blue-600" />,
  Armchair: <Armchair className="h-4 w-4 text-blue-600" />,
  Wifi: <Wifi className="h-4 w-4 text-blue-600" />,
  Gamepad: <Gamepad className="h-4 w-4 text-blue-600" />,
  RotateCcw: <RotateCcw className="h-4 w-4 text-blue-600" />,
  Info: <Info className="h-4 w-4 text-blue-600" />,
};

const MAIN_SLIDES = [
  {
    id: 1,
    title: 'GIAN HÀNG PC STORE',
    subtitle: 'ƯU ĐÃI LÊN ĐẾN 25.000.000đ',
    badge: 'Tặng màn OLED',
    image: 'https://images.unsplash.com/photo-1587202372775-e229f172b9d7?auto=format&fit=crop&w=1000&q=80',
    accent: 'from-blue-950/95 via-indigo-950/85 to-slate-950/90',
  },
  {
    id: 2,
    title: 'SIÊU SALE LAPTOP GAMING',
    subtitle: 'GIẢM ĐẾN 8.000.000đ + COMBO GEAR',
    badge: 'Quà tặng 15 triệu',
    image: 'https://images.unsplash.com/photo-1593642702821-c8da6771f0c6?auto=format&fit=crop&w=1000&q=80',
    accent: 'from-blue-900/90 via-sky-950/80 to-neutral-900/90',
  },
  {
    id: 3,
    title: 'MÀN HÌNH FAST IPS 2K',
    subtitle: 'TẦN SỐ QUÉT ĐẾN 320Hz SIÊU MƯỢT',
    badge: 'Bảo hành 1 đổi 1',
    image: 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=1000&q=80',
    accent: 'from-slate-950/90 via-blue-950/80 to-slate-900/90',
  },
];

export const HeroSection: React.FC = () => {
  const [currentSlide, setCurrentSlide] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % MAIN_SLIDES.length);
    }, 4500);
    return () => clearInterval(timer);
  }, []);

  const prevSlide = () => {
    setCurrentSlide((prev) => (prev - 1 + MAIN_SLIDES.length) % MAIN_SLIDES.length);
  };

  const nextSlide = () => {
    setCurrentSlide((prev) => (prev + 1) % MAIN_SLIDES.length);
  };

  return (
    <section className="mx-auto max-w-[1260px] px-3 pt-3 mb-6">
      <div className="grid grid-cols-1 lg:grid-cols-[250px_1fr] gap-3">
        {/* Left Category Menu (Đã phóng to chữ, padding rộng rãi, cân xứng hoàn hảo) */}
        <aside className="hidden lg:block bg-white rounded-md shadow-sm border border-gray-200 py-2 text-[13px] text-gray-700">
          <ul className="flex flex-col divide-y divide-gray-100">
            {CATEGORIES.map((item) => (
              <li key={item.id}>
                <a
                  href={`/catalog?category=${item.id}`}
                  className="group flex items-center justify-between px-3 py-2 hover:bg-blue-50 hover:text-blue-600 transition-colors"
                >
                  <div className="flex items-center gap-2.5">
                    <span className="shrink-0">{iconMap[item.icon] || <Cpu className="h-4 w-4 text-blue-600" />}</span>
                    <span className="font-semibold truncate max-w-[170px] group-hover:translate-x-0.5 transition-transform">
                      {item.name}
                    </span>
                  </div>
                  <ChevronRight className="h-3.5 w-3.5 text-gray-400 group-hover:text-blue-600 group-hover:translate-x-0.5 transition-transform shrink-0" />
                </a>
              </li>
            ))}
          </ul>
        </aside>

        {/* Center & Right Banner System */}
        <div className="flex flex-col gap-2.5 justify-between">
          {/* Top Banners Grid (Main Carousel Banner + 2 Side Promo Banners) */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2.5">
            {/* Main Carousel Banner Nguyên Bản (Không chế chữ đè lên, giữ nguyên slider sang trọng) */}
            <div className="md:col-span-2 relative h-[210px] md:h-[220px] rounded-md overflow-hidden shadow-sm group select-none">
              <div
                className="absolute inset-0 bg-cover bg-center transition-all duration-700 transform scale-105"
                style={{ backgroundImage: `url(${MAIN_SLIDES[currentSlide].image})` }}
              >
                <div
                  className={`absolute inset-0 bg-gradient-to-r ${MAIN_SLIDES[currentSlide].accent}`}
                />
              </div>

              {/* Banner Content nguyên bản */}
              <div className="relative z-10 flex h-full flex-col justify-between p-5 text-white">
                <div className="space-y-1">
                  <span className="inline-block rounded bg-yellow-400 px-2 py-0.5 text-[10px] font-black text-black uppercase tracking-wide">
                    {MAIN_SLIDES[currentSlide].badge}
                  </span>
                  <h2 className="text-xl md:text-2xl font-black italic tracking-wide text-white drop-shadow">
                    {MAIN_SLIDES[currentSlide].title}
                  </h2>
                  <p className="text-sm md:text-base font-extrabold text-yellow-300 drop-shadow">
                    {MAIN_SLIDES[currentSlide].subtitle}
                  </p>
                </div>

                <div className="flex items-center justify-between">
                  <a
                    href="/catalog"
                    className="rounded-full bg-blue-600 hover:bg-blue-700 px-4 py-1.5 text-xs font-bold text-white shadow-md transition-all transform hover:scale-105"
                  >
                    Khám phá ngay
                  </a>

                  {/* Slide Indicators */}
                  <div className="flex gap-1.5">
                    {MAIN_SLIDES.map((_, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => setCurrentSlide(idx)}
                        aria-label={`Chuyển tới slide ${idx + 1}`}
                        className={`h-1.5 rounded-full transition-all cursor-pointer ${
                          idx === currentSlide ? 'w-6 bg-yellow-400' : 'w-2 bg-white/50'
                        }`}
                      />
                    ))}
                  </div>
                </div>
              </div>

              {/* Left/Right Navigation Arrows */}
              <button
                type="button"
                onClick={prevSlide}
                aria-label="Slide trước"
                className="absolute left-2 top-1/2 -translate-y-1/2 flex h-7 w-7 items-center justify-center rounded-full bg-black/40 text-white opacity-0 group-hover:opacity-100 transition-opacity hover:bg-black/70 cursor-pointer"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              <button
                type="button"
                onClick={nextSlide}
                aria-label="Slide tiếp theo"
                className="absolute right-2 top-1/2 -translate-y-1/2 flex h-7 w-7 items-center justify-center rounded-full bg-black/40 text-white opacity-0 group-hover:opacity-100 transition-opacity hover:bg-black/70 cursor-pointer"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>

            {/* Right 2 Side Promo Banners */}
            <div className="flex flex-col gap-2 justify-between">
              {/* Promo Banner 1: Build PC */}
              <div className="relative h-[105px] rounded-md overflow-hidden shadow-sm bg-gradient-to-r from-blue-900 to-indigo-800 p-3 text-white flex flex-col justify-between group cursor-pointer hover:shadow-md transition-shadow">
                <div>
                  <span className="text-[10px] uppercase font-bold tracking-wider text-cyan-200">
                    Chương trình hot
                  </span>
                  <h3 className="text-sm font-black leading-tight mt-0.5">
                    Build PC TẶNG MÀN HÌNH
                  </h3>
                  <span className="text-xs font-extrabold text-yellow-300">
                    Gaming 240Hz
                  </span>
                </div>
                <div className="text-[11px] font-medium text-cyan-100 flex items-center justify-between">
                  <span>Xem chi tiết</span>
                  <ChevronRight className="h-3.5 w-3.5 group-hover:translate-x-1 transition-transform" />
                </div>
              </div>

              {/* Promo Banner 2: Gaming Gear */}
              <div className="relative h-[105px] rounded-md overflow-hidden shadow-sm bg-gradient-to-r from-slate-900 to-blue-950 p-3 text-white flex flex-col justify-between group cursor-pointer hover:shadow-md transition-shadow border border-slate-800">
                <div>
                  <span className="text-[10px] uppercase font-bold tracking-wider text-blue-400">
                    Phụ kiện cao cấp
                  </span>
                  <h3 className="text-sm font-black leading-tight mt-0.5">
                    Bàn Phím Cơ &amp; Chuột
                  </h3>
                  <span className="text-xs font-extrabold text-yellow-300">
                    Giảm đến 26%
                  </span>
                </div>
                <div className="text-[11px] font-medium text-slate-300 flex items-center justify-between">
                  <span>Sắm ngay</span>
                  <ChevronRight className="h-3.5 w-3.5 group-hover:translate-x-1 transition-transform" />
                </div>
              </div>
            </div>
          </div>

          {/* Middle Long Horizontal Banner */}
          <div className="relative rounded-md overflow-hidden shadow-sm bg-gradient-to-r from-blue-950 via-slate-900 to-blue-900 px-4 py-2.5 text-white flex items-center justify-between border border-blue-900/50">
            <div>
              <span className="text-[10px] text-cyan-300 font-medium">Chương trình diễn ra đến hết tháng</span>
              <h3 className="text-sm md:text-base font-black tracking-wide text-white">
                PC STORE TỰ HÀO ĐỒNG HÀNH CÙNG GAME THỦ &amp; CREATORS
              </h3>
            </div>
            <div className="flex items-center gap-3">
              <span className="hidden sm:inline-block rounded bg-blue-600 px-2.5 py-1 text-xs font-black uppercase text-white shadow">
                GIẢM NGAY ĐẾN 8 TRIỆU
              </span>
              <ChevronRight className="h-5 w-5 text-cyan-400 cursor-pointer hover:translate-x-1 transition-transform" />
            </div>
          </div>

          {/* 4 Mini Cards under Hero */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
            <div className="rounded-md border border-gray-200 bg-white p-2 text-center shadow-xs hover:border-blue-400 transition-colors cursor-pointer">
              <div className="text-[11px] font-black text-gray-900">Bắt Beat Cực Chuẩn</div>
              <div className="text-[10px] text-gray-500">Tai nghe Corsair &amp; Logitech</div>
              <span className="mt-1 inline-block rounded bg-blue-50 px-2 py-0.5 text-[10px] font-bold text-blue-700">
                GIẢM ĐẾN 40%
              </span>
            </div>

            <div className="rounded-md border border-gray-200 bg-white p-2 text-center shadow-xs hover:border-blue-400 transition-colors cursor-pointer">
              <div className="text-[11px] font-black text-gray-900">Monitor Gaming</div>
              <div className="text-[10px] text-gray-500">Màn hình Fast IPS 180Hz</div>
              <span className="mt-1 inline-block rounded bg-amber-50 px-2 py-0.5 text-[10px] font-bold text-amber-700">
                TẶNG MOUSE
              </span>
            </div>

            <div className="rounded-md border border-gray-200 bg-white p-2 text-center shadow-xs hover:border-blue-400 transition-colors cursor-pointer">
              <div className="text-[11px] font-black text-gray-900">Gaming Mouse</div>
              <div className="text-[10px] text-gray-500">Chuột công thái học siêu nhẹ</div>
              <span className="mt-1 inline-block rounded bg-emerald-50 px-2 py-0.5 text-[10px] font-bold text-emerald-700">
                CHỈ TỪ 890.000đ
              </span>
            </div>

            <div className="rounded-md border border-gray-200 bg-white p-2 text-center shadow-xs hover:border-blue-400 transition-colors cursor-pointer">
              <div className="text-[11px] font-black text-gray-900">PC TẶNG MÀN HÌNH</div>
              <div className="text-[10px] text-gray-500">PC Store RTX 40 Series</div>
              <span className="mt-1 inline-block rounded bg-purple-50 px-2 py-0.5 text-[10px] font-bold text-purple-700">
                MÀN 240Hz OLED
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
