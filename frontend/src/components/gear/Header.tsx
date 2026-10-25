import React from 'react';
import {
  Menu,
  Search,
  ShoppingCart,
  User,
  Tag,
  Newspaper,
  ShieldCheck,
  Wrench,
  RotateCcw,
  MapPin,
  ChevronDown
} from 'lucide-react';

interface HeaderProps {
  cartCount: number;
  onOpenCart: () => void;
  searchQuery: string;
  setSearchQuery: (q: string) => void;
}

export const Header: React.FC<HeaderProps> = ({
  cartCount,
  onOpenCart,
  searchQuery,
  setSearchQuery,
}) => {
  return (
    <header className="sticky top-0 z-40 w-full bg-[#2563eb] text-white shadow-md">
      {/* Top main navbar */}
      <div className="mx-auto max-w-[1260px] px-3 py-2.5">
        <div className="flex items-center justify-between gap-4">
          {/* Logo */}
          <div className="flex items-center gap-2 cursor-pointer select-none">
            <div className="flex flex-col items-start leading-none">
              <div className="flex items-center gap-1.5">
                {/* Stylized PC STORE geometric logo icon */}
                <div className="flex h-7 w-7 items-center justify-center rounded-sm bg-black p-1 text-blue-600 font-black italic tracking-tighter text-sm border border-red-500 shadow-inner">
                  <span className="text-white font-extrabold text-xs">G</span>
                </div>
                <div className="flex flex-col">
                  <span className="text-2xl font-black italic tracking-wider leading-none text-white drop-shadow">
                    GEAR<span className="text-yellow-400">VN</span>
                  </span>
                  <span className="text-[9px] font-medium tracking-tight text-white/90 uppercase">
                    Hi-End PC & Gaming Gear
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Danh mục Button */}
          <button
            type="button"
            className="flex items-center gap-2 rounded-md bg-[#be151a] hover:bg-[#a61014] px-3 py-2 text-sm font-semibold text-white transition-colors shrink-0 shadow-sm border border-red-400/30"
          >
            <Menu className="h-4 w-4" />
            <span>Danh mục</span>
            <ChevronDown className="h-3.5 w-3.5 opacity-80" />
          </button>

          {/* Search Box */}
          <div className="relative flex-1 max-w-[620px]">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Bạn tìm gì..."
              className="w-full rounded-md bg-white py-2 pl-4 pr-11 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-yellow-400 shadow-sm"
            />
            <button
              type="button"
              className="absolute right-1 top-1/2 -translate-y-1/2 rounded bg-[#2563eb] hover:bg-[#c90015] p-1.5 text-white transition-colors"
              title="Tìm kiếm"
            >
              <Search className="h-4 w-4" />
            </button>
          </div>

          {/* Actions: Cart & Login */}
          <div className="flex items-center gap-3 shrink-0">
            {/* Cart Button */}
            <button
              type="button"
              onClick={onOpenCart}
              className="relative flex items-center gap-2 rounded-md bg-[#be151a] hover:bg-[#a61014] px-3 py-2 text-sm font-medium text-white transition-colors border border-red-400/30"
            >
              <div className="relative">
                <ShoppingCart className="h-4 w-4" />
                <span className="absolute -right-2 -top-2 flex h-4 w-4 items-center justify-center rounded-full bg-yellow-400 text-[10px] font-bold text-red-900 shadow">
                  {cartCount}
                </span>
              </div>
              <span className="hidden sm:inline">Giỏ hàng</span>
            </button>

            {/* Login Button */}
            <button
              type="button"
              className="flex items-center gap-2 rounded-md bg-[#be151a] hover:bg-[#a61014] px-3 py-2 text-sm font-medium text-white transition-colors border border-red-400/30"
            >
              <User className="h-4 w-4" />
              <span>Đăng nhập</span>
            </button>
          </div>
        </div>
      </div>

      {/* Subnav links row */}
      <div className="border-t border-blue-600/60 bg-white text-gray-700 shadow-sm text-xs">
        <div className="mx-auto flex max-w-[1260px] items-center justify-between px-3 py-1.5 overflow-x-auto whitespace-nowrap scrollbar-none font-medium">
          <a
            href="#khuyen-mai"
            className="flex items-center gap-1.5 hover:text-[#2563eb] text-[#2563eb] font-semibold transition-colors px-2 py-0.5"
          >
            <Tag className="h-3.5 w-3.5 text-[#2563eb]" />
            <span>Tất cả khuyến mãi</span>
          </a>
          <span className="text-gray-300">|</span>
          <a
            href="#tin-tuc"
            className="flex items-center gap-1.5 hover:text-[#2563eb] transition-colors px-2 py-0.5"
          >
            <Newspaper className="h-3.5 w-3.5 text-gray-500" />
            <span>Tin tức-Review</span>
          </a>
          <span className="text-gray-300">|</span>
          <a
            href="#bao-hanh"
            className="flex items-center gap-1.5 hover:text-[#2563eb] transition-colors px-2 py-0.5"
          >
            <ShieldCheck className="h-3.5 w-3.5 text-gray-500" />
            <span>Tra Cứu Bảo Hành</span>
          </a>
          <span className="text-gray-300">|</span>
          <a
            href="#dich-vu"
            className="flex items-center gap-1.5 hover:text-[#2563eb] transition-colors px-2 py-0.5"
          >
            <Wrench className="h-3.5 w-3.5 text-gray-500" />
            <span>Dịch vụ tận nhà</span>
          </a>
          <span className="text-gray-300">|</span>
          <a
            href="#thu-cu"
            className="flex items-center gap-1.5 hover:text-[#2563eb] transition-colors px-2 py-0.5"
          >
            <RotateCcw className="h-3.5 w-3.5 text-gray-500" />
            <span>Thu cũ đổi mới</span>
          </a>
          <span className="text-gray-300">|</span>
          <a
            href="#showroom"
            className="flex items-center gap-1.5 hover:text-[#2563eb] transition-colors px-2 py-0.5"
          >
            <MapPin className="h-3.5 w-3.5 text-gray-500" />
            <span>Hệ Thống Showroom</span>
          </a>
        </div>
      </div>
    </header>
  );
};
