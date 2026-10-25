import React from 'react';
import { ArrowRight, ChevronRight, Play, ChevronLeft } from 'lucide-react';
import { NEWS_FEATURED, NEWS_RECENT } from '../../data/storeData';

export const NewsSection: React.FC = () => {
  return (
    <section id="tin-tuc" className="mx-auto max-w-[1260px] px-3 my-6 pb-4">
      {/* Blue Campaign Banner */}
      <div className="relative overflow-hidden rounded-md bg-gradient-to-r from-[#0d1e3d] via-[#102a5c] to-[#0b172e] p-4 text-white shadow-sm flex flex-wrap items-center justify-between gap-4">
        <div className="space-y-0.5 z-10">
          <h3 className="text-base sm:text-lg md:text-xl font-black tracking-wide">
            Cập nhật <span className="text-blue-400">Tin Tức Công Nghệ</span>
          </h3>
          <p className="text-xs sm:text-sm font-semibold text-gray-300">
            Mới nhất cùng PC STORE
          </p>
        </div>

        <a
          href="#tat-ca-tin-tuc"
          className="z-10 flex items-center gap-2 rounded-full border border-blue-400/50 bg-blue-950/60 px-4 py-1.5 text-xs font-bold text-white hover:bg-blue-600 transition-colors shadow-sm"
        >
          <span>Xem tất cả</span>
          <ArrowRight className="h-3.5 w-3.5" />
        </a>

        {/* Ambient background light */}
        <div className="absolute right-10 top-1/2 -translate-y-1/2 h-32 w-32 rounded-full bg-blue-500/10 blur-2xl pointer-events-none" />
      </div>

      {/* Two columns: Tin Tức Nổi Bật & Bài Viết Mới */}
      <div className="mt-4 grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* Left: Tin Tức Nổi Bật (7 cols) */}
        <div className="lg:col-span-7 flex flex-col">
          <div className="flex items-center justify-between border-b border-gray-200 pb-2">
            <h3 className="text-base font-black text-gray-900">
              Tin Tức Nổi Bật
            </h3>
          </div>

          <div className="mt-3 relative flex-1 overflow-hidden rounded-md border border-gray-200 bg-black group cursor-pointer shadow-sm">
            {/* Thumbnail */}
            <div className="relative h-[220px] sm:h-[260px] w-full overflow-hidden">
              <img
                src={NEWS_FEATURED.image}
                alt={NEWS_FEATURED.title}
                className="h-full w-full object-cover group-hover:scale-105 transition-transform duration-500 opacity-80"
                loading="lazy"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black via-black/50 to-transparent" />

              {/* Tag & Play overlay */}
              <div className="absolute top-3 left-3 flex items-center gap-2">
                <span className="rounded bg-black/60 backdrop-blur-xs border border-white/20 px-2 py-0.5 text-[10px] font-bold text-white">
                  {NEWS_FEATURED.tag}
                </span>
              </div>

              <div className="absolute inset-0 flex items-center justify-center">
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-blue-600/90 text-white shadow-lg group-hover:scale-110 group-hover:bg-blue-600 transition-all">
                  <Play className="h-5 w-5 fill-white ml-0.5" />
                </div>
              </div>

              {/* Slider arrow controls */}
              <div className="absolute left-2 top-1/2 -translate-y-1/2 flex h-7 w-7 items-center justify-center rounded-full bg-black/50 text-white opacity-0 group-hover:opacity-100 transition-opacity">
                <ChevronLeft className="h-4 w-4" />
              </div>
              <div className="absolute right-2 top-1/2 -translate-y-1/2 flex h-7 w-7 items-center justify-center rounded-full bg-black/50 text-white opacity-0 group-hover:opacity-100 transition-opacity">
                <ChevronRight className="h-4 w-4" />
              </div>
            </div>

            {/* Content overlay text */}
            <div className="p-3.5 bg-black/95 text-white space-y-1.5">
              <h4 className="text-sm sm:text-base font-bold leading-snug hover:text-red-400 transition-colors">
                {NEWS_FEATURED.title}
              </h4>
              <p className="text-xs text-gray-300 line-clamp-2 leading-relaxed">
                {NEWS_FEATURED.description}
              </p>
            </div>
          </div>
        </div>

        {/* Right: Bài Viết Mới (5 cols) */}
        <div className="lg:col-span-5 flex flex-col">
          <div className="flex items-center justify-between border-b border-gray-200 pb-2">
            <h3 className="text-base font-black text-gray-900">
              Bài Viết Mới
            </h3>
          </div>

          <div className="mt-3 flex flex-col gap-3 flex-1 justify-between">
            {NEWS_RECENT.map((item) => (
              <div
                key={item.id}
                className="relative overflow-hidden rounded-md border border-gray-200 bg-neutral-900 text-white shadow-sm group cursor-pointer h-[135px] flex items-end p-3 hover:border-gray-400 transition-all"
              >
                <img
                  src={item.image}
                  alt={item.title}
                  className="absolute inset-0 h-full w-full object-cover opacity-60 group-hover:scale-105 group-hover:opacity-75 transition-all duration-300"
                  loading="lazy"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black via-black/60 to-transparent" />

                <div className="relative z-10 flex items-center justify-between w-full gap-2">
                  <h4 className="text-xs sm:text-sm font-bold text-white group-hover:text-red-400 transition-colors leading-snug">
                    {item.title}
                  </h4>
                  <div className="shrink-0 flex h-6 w-6 items-center justify-center rounded-full bg-black/60 text-white border border-white/20">
                    <ChevronRight className="h-3.5 w-3.5" />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
};
