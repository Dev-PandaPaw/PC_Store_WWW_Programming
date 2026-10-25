import React, { useState } from 'react';
import {
  Facebook,
  Youtube,
  Instagram,
  ChevronUp,
  MessageCircle,
  Phone,
  X,
  CreditCard,
  ShieldCheck
} from 'lucide-react';

export const Footer: React.FC = () => {
  const [showSupportPill, setShowSupportPill] = useState(true);

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <footer className="w-full bg-[#161616] text-gray-300 text-xs mt-10 border-t border-neutral-800 relative">
      <div className="mx-auto max-w-[1260px] px-4 py-8">
        {/* Main 4 Columns Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-7">
          {/* Column 1: Hotline & Contact */}
          <div className="space-y-4">
            {/* Logo & Brand */}
            <div>
              <div className="flex items-center gap-1.5 cursor-pointer">
                <div className="flex h-6 w-6 items-center justify-center rounded-sm bg-black p-1 text-blue-600 font-black italic text-xs border border-red-500 shadow-inner">
                  <span className="text-white font-extrabold text-[10px]">G</span>
                </div>
                <span className="text-xl font-black italic tracking-wider text-white">
                  GEAR<span className="text-yellow-400">VN</span>
                </span>
              </div>
              <p className="text-[10px] text-gray-400 font-medium uppercase tracking-wider mt-0.5">
                Hi-End PC & Gaming Gear
              </p>
            </div>

            {/* Socials */}
            <div>
              <p className="text-xs font-bold text-white uppercase tracking-wider mb-2">
                KẾT NỐI VỚI CHÚNG TÔI
              </p>
              <div className="flex items-center gap-2">
                <a
                  href="#facebook"
                  aria-label="Facebook"
                  className="flex h-7 w-7 items-center justify-center rounded-full bg-neutral-800 text-gray-300 hover:bg-blue-600 hover:text-white transition-colors"
                >
                  <Facebook className="h-3.5 w-3.5" />
                </a>
                <a
                  href="#youtube"
                  aria-label="YouTube"
                  className="flex h-7 w-7 items-center justify-center rounded-full bg-neutral-800 text-gray-300 hover:bg-blue-600 hover:text-white transition-colors"
                >
                  <Youtube className="h-3.5 w-3.5" />
                </a>
                <a
                  href="#instagram"
                  aria-label="Instagram"
                  className="flex h-7 w-7 items-center justify-center rounded-full bg-neutral-800 text-gray-300 hover:bg-pink-600 hover:text-white transition-colors"
                >
                  <Instagram className="h-3.5 w-3.5" />
                </a>
              </div>
            </div>

            {/* Hotlines */}
            <div className="space-y-1.5 pt-2">
              <p className="text-xs font-bold text-white uppercase">
                TỔNG ĐÀI HỖ TRỢ (8:00 - 21:00)
              </p>
              <p>
                <span className="text-gray-400">Tư vấn mua hàng:</span>{' '}
                <strong className="text-white hover:text-red-400 cursor-pointer">
                  1900.5301
                </strong>
              </p>
              <p>
                <span className="text-gray-400">Hỗ trợ kỹ thuật, bảo hành:</span>{' '}
                <strong className="text-white hover:text-red-400 cursor-pointer">
                  1900.5325
                </strong>
              </p>
              <p>
                <span className="text-gray-400">Góp ý, khiếu nại:</span>{' '}
                <strong className="text-white hover:text-red-400 cursor-pointer">
                  1800.6173
                </strong>
              </p>
              <p>
                <span className="text-gray-400">Email:</span>{' '}
                <strong className="text-white hover:text-red-400 cursor-pointer">
                  cskh@PC STORE.com
                </strong>
              </p>
            </div>
          </div>

          {/* Column 2: VỀ PC STORE & CHÍNH SÁCH */}
          <div className="space-y-4">
            <div>
              <p className="text-xs font-bold text-white uppercase mb-2">
                VỀ PC STORE
              </p>
              <ul className="space-y-1.5 text-gray-400">
                <li>
                  <a href="#gioi-thieu" className="hover:text-white transition-colors">
                    Giới thiệu
                  </a>
                </li>
                <li>
                  <a href="#tuyen-dung" className="hover:text-white transition-colors">
                    Tuyển dụng
                  </a>
                </li>
                <li>
                  <a href="#lien-he" className="hover:text-white transition-colors">
                    Liên hệ
                  </a>
                </li>
              </ul>
            </div>

            <div>
              <p className="text-xs font-bold text-white uppercase mb-2">
                CHÍNH SÁCH
              </p>
              <ul className="space-y-1.5 text-gray-400">
                <li>
                  <a href="#chinh-sach-bao-hanh" className="hover:text-white transition-colors">
                    Chính sách bảo hành
                  </a>
                </li>
                <li>
                  <a href="#chinh-sach-doi-tra" className="hover:text-white transition-colors">
                    Chính sách đổi trả
                  </a>
                </li>
                <li>
                  <a href="#chinh-sach-van-chuyen" className="hover:text-white transition-colors">
                    Chính sách vận chuyển
                  </a>
                </li>
                <li>
                  <a href="#chinh-sach-bao-mat" className="hover:text-white transition-colors">
                    Chính sách bảo mật
                  </a>
                </li>
                <li>
                  <a href="#cai-dat-cookie" className="hover:text-white transition-colors">
                    Cài đặt cookie
                  </a>
                </li>
              </ul>
            </div>
          </div>

          {/* Column 3: THÔNG TIN */}
          <div className="space-y-2">
            <p className="text-xs font-bold text-white uppercase mb-2">
              THÔNG TIN
            </p>
            <ul className="space-y-2 text-gray-400">
              <li>
                <a href="#he-thong-cua-hang" className="hover:text-white transition-colors">
                  Hệ thống cửa hàng
                </a>
              </li>
              <li>
                <a href="#huong-dan-mua-hang" className="hover:text-white transition-colors">
                  Hướng dẫn mua hàng
                </a>
              </li>
              <li>
                <a href="#huong-dan-thanh-toan" className="hover:text-white transition-colors">
                  Hướng dẫn thanh toán
                </a>
              </li>
              <li>
                <a href="#huong-dan-tra-gop" className="hover:text-white transition-colors">
                  Hướng dẫn trả góp
                </a>
              </li>
              <li>
                <a href="#tra-cuu-bao-hanh" className="hover:text-white transition-colors">
                  Tra cứu địa chỉ bảo hành
                </a>
              </li>
              <li>
                <a href="#build-pc" className="hover:text-white transition-colors">
                  Build PC
                </a>
              </li>
            </ul>
          </div>

          {/* Column 4: HỖ TRỢ THANH TOÁN & VẬN CHUYỂN */}
          <div className="space-y-4">
            <div>
              <p className="text-xs font-bold text-white uppercase mb-2">
                HỖ TRỢ THANH TOÁN
              </p>
              <div className="flex flex-wrap gap-1.5">
                {['VISA', 'Mastercard', 'JCB', 'VNPAY', 'MoMo', 'ZaloPay', 'Tiền mặt'].map(
                  (method) => (
                    <span
                      key={method}
                      className="rounded bg-neutral-800 px-2 py-1 text-[10px] font-bold text-gray-200 border border-neutral-700"
                    >
                      {method}
                    </span>
                  )
                )}
              </div>
            </div>

            <div>
              <p className="text-xs font-bold text-white uppercase mb-2">
                ĐƠN VỊ VẬN CHUYỂN
              </p>
              <div className="flex flex-wrap gap-1.5">
                {['GHN', 'GHTK', 'Viettel Post', 'GrabExpress', 'Hỏa Tốc 2H'].map((carrier) => (
                  <span
                    key={carrier}
                    className="rounded bg-neutral-800 px-2 py-1 text-[10px] font-bold text-gray-200 border border-neutral-700"
                  >
                    {carrier}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Bottom Legal & Certification Bar */}
        <div className="mt-8 border-t border-neutral-800 pt-5 flex flex-col md:flex-row items-center justify-between gap-4 text-center md:text-left text-[11px] text-gray-500">
          <div className="max-w-3xl space-y-1">
            <p>
              Công Ty TNHH Thương Mại PC STORE, địa chỉ: 82 Hoàng Hoa Thám, Phường Bảy Hiền, TP. Hồ Chí Minh. MST/GCN ĐKKD số: 0316517194, đăng ký lần đầu ngày 01/10/2020. Đăng ký thay đổi lần thứ 9 ngày 23/02/2025. Đại diện pháp luật: Nguyễn Thế Anh.
            </p>
            <p>© 2026 PC STORE. Tất cả các quyền được bảo lưu.</p>
          </div>

          {/* Blue Ministry seal */}
          <div className="shrink-0 flex items-center gap-2 rounded border border-blue-900 bg-blue-950/40 px-3 py-1 text-blue-400">
            <ShieldCheck className="h-6 w-6 text-blue-400" />
            <div className="text-[10px] text-left leading-tight font-bold">
              <span>ĐÃ THÔNG BÁO</span>
              <br />
              <span className="text-white">BỘ CÔNG THƯƠNG</span>
            </div>
          </div>
        </div>
      </div>

      {/* Floating Action Buttons */}
      <div className="fixed bottom-5 right-5 z-40 flex flex-col items-end gap-2.5">
        {/* Support speech pill */}
        {showSupportPill && (
          <div className="flex items-center gap-1.5 rounded-full bg-[#2563eb] px-3.5 py-1.5 text-xs font-bold text-white shadow-xl animate-bounce">
            <span>Bạn cần hỗ trợ gì?</span>
            <button
              type="button"
              onClick={() => setShowSupportPill(false)}
              className="rounded-full hover:bg-black/20 p-0.5"
              aria-label="Đóng hỗ trợ"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          </div>
        )}

        <div className="flex items-center gap-2">
          {/* Chat bubble button */}
          <button
            type="button"
            className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-600 text-white shadow-lg hover:bg-blue-700 transition-all hover:scale-105"
            title="Chat tư vấn"
            aria-label="Chat tư vấn"
          >
            <MessageCircle className="h-5 w-5" />
          </button>

          {/* Scroll to Top */}
          <button
            type="button"
            onClick={scrollToTop}
            className="flex h-10 w-10 items-center justify-center rounded-full bg-neutral-900 text-white shadow-lg hover:bg-black transition-all hover:scale-105 border border-neutral-700"
            title="Lên đầu trang"
            aria-label="Lên đầu trang"
          >
            <ChevronUp className="h-5 w-5" />
          </button>
        </div>
      </div>
    </footer>
  );
};
