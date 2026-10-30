import React, { useCallback, useState } from 'react'
import { HeroSection } from '../components/gear/HeroSection'
import { NewsSection } from '../components/gear/NewsSection'
import { CartDrawer } from '../components/gear/CartDrawer'
import { FlashDeal } from '../components/gear/FlashDeal'
import ProductSection from '../components/ProductSection'
import PromoBanner from '../components/PromoBanner'
import collectionBanner from '../assets/promotions/pc-collection.png'
import buildWideBanner from '../assets/promotions/build-pc-wide.webp'
import { useCart } from '../contexts/CartContext'

export default function HomePage() {
  const { items, addItem, setQuantity, removeItem, clearCart } = useCart()
  const [isCartOpen, setIsCartOpen] = useState(false)
  const closeCart = useCallback(() => setIsCartOpen(false), [])
  const handleAddToCart = product => addItem(product, 1, () => setIsCartOpen(true))

  return (
    <div className="storefront">
      <HeroSection />
      <PromoBanner
        to="/catalog?category=pc"
        src={collectionBanner}
        alt="Gian hàng Pixel Core — PC văn phòng, gaming và cấu hình cao cấp"
        width={2172}
        height={724}
        className="promo-banner--collection"
      />
      <FlashDeal onAddToCart={handleAddToCart} />

      <ProductSection
        id="pc-bo"
        eyebrow="SẴN SÀNG CHO ĐAM MÊ"
        title="Một bộ PC. Cả thế giới của bạn."
        description="Chọn cấu hình phù hợp cho những trận game và công việc mỗi ngày."
        category="pc"
        tabs={[
          { id: 'pc-gaming', name: 'Gaming' },
          { id: 'pc-office', name: 'Văn phòng' },
          { id: 'pc-workstation', name: 'Đồ họa & làm việc' },
        ]}
        onAddToCart={handleAddToCart}
      />

      <PromoBanner
        to="/advisory"
        src={buildWideBanner}
        alt="Build PC tặng màn hình — giá chỉ từ 11.990.000 đồng, quà tặng màn hình 240Hz"
        width={1000}
        height={320}
        className="promo-banner--build"
      />

      <ProductSection
        id="linh-kien"
        eyebrow="NÂNG CẤP TỪ BÊN TRONG"
        title="Trái tim cho dàn máy."
        category="components"
        limit={8}
        tabs={[
          { id: 'cpu', name: 'CPU' },
          { id: 'vga', name: 'Card đồ họa' },
          { id: 'mainboard', name: 'Mainboard' },
          { id: 'ram', name: 'RAM' },
          { id: 'storage', name: 'SSD' },
        ]}
        onAddToCart={handleAddToCart}
      />

      <ProductSection
        id="tan-nhiet"
        eyebrow="HOÀN THIỆN HỆ THỐNG"
        title="Mát hơn. Gọn hơn. Ổn định hơn."
        description="Tản nhiệt, vỏ case và nguồn điện cho một bộ máy cân bằng."
        category="build"
        limit={8}
        tabs={[
          { id: 'cooling', name: 'Tản nhiệt' },
          { id: 'case', name: 'Vỏ case' },
          { id: 'psu', name: 'Nguồn máy tính' },
        ]}
        onAddToCart={handleAddToCart}
      />

      <ProductSection
        id="gaming-gear"
        eyebrow="CẢM HỨNG TRÊN TỪNG ĐẦU NGÓN TAY"
        title="Hoàn thiện góc chơi."
        category="gear"
        limit={8}
        tabs={[
          { id: 'keyboard', name: 'Bàn phím' },
          { id: 'mouse', name: 'Chuột máy tính' },
        ]}
        onAddToCart={handleAddToCart}
      />

      <ProductSection
        id="man-hinh"
        eyebrow="MỞ RỘNG TẦM NHÌN"
        title="Mọi chi tiết, trong tầm mắt."
        category="monitor"
        onAddToCart={handleAddToCart}
      />
      <NewsSection />
      <CartDrawer
        isOpen={isCartOpen}
        onClose={closeCart}
        items={items}
        onUpdateQuantity={setQuantity}
        onRemoveItem={removeItem}
        onClearCart={clearCart}
      />
    </div>
  )
}
