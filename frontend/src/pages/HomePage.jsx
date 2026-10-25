import React, { useState } from 'react'
import { HeroSection } from '../components/gear/HeroSection'
import { FlashDeal } from '../components/gear/FlashDeal'
import { PromoBanners } from '../components/gear/PromoBanners'
import { ProductSectionPC } from '../components/gear/ProductSectionPC'
import { ProductSectionLaptopGaming } from '../components/gear/ProductSectionLaptopGaming'
import { ProductSectionLaptopOffice } from '../components/gear/ProductSectionLaptopOffice'
import { ProductSectionMouse } from '../components/gear/ProductSectionMouse'
import { ProductSectionKeyboard } from '../components/gear/ProductSectionKeyboard'
import { MidPromoBanners } from '../components/gear/MidPromoBanners'
import { ProductSectionMonitor } from '../components/gear/ProductSectionMonitor'
import { NewsSection } from '../components/gear/NewsSection'
import { CartDrawer } from '../components/gear/CartDrawer'
import { useCart } from '../contexts/CartContext'

export default function HomePage() {
  const { items, addItem, removeItem, clearCart } = useCart()
  const [isCartOpen, setIsCartOpen] = useState(false)

  const handleAddToCart = (product) => {
    addItem({
      id: product.id,
      name: product.name,
      price: product.price,
      image: product.image,
    }, 1)
    setIsCartOpen(true)
  }

  const handleUpdateQuantity = (id, delta) => {
    const item = items.find(i => i.id === id)
    if (item) {
      if (item.quantity + delta <= 0) {
        removeItem(id)
      } else {
        addItem(item, delta)
      }
    }
  }

  return (
    <div className="min-h-screen bg-[#f4f4f4] text-gray-900 font-sans pb-10">
      {/* 1. Hero Section: 16 Danh mục + Banner chính diện ô vừa (Video Neon Nhut_dev) + 2 Side Banners + 4 Mini Banners */}
      <HeroSection />

      {/* 2. Flash Deal Laptop & PC có đồng hồ đếm ngược */}
      <FlashDeal onAddToCart={handleAddToCart} />

      {/* 3. Promo Banners: Build PC Tặng Màn Hình & Sắm Laptop Tặng Phụ Kiện */}
      <PromoBanners />

      {/* 4. Khối PC Bán Chạy (5 card/hàng, tabs bộ lọc) */}
      <ProductSectionPC onAddToCart={handleAddToCart} />

      {/* 5. Khối Laptop Gaming Bán Chạy (5 card/hàng) */}
      <ProductSectionLaptopGaming onAddToCart={handleAddToCart} />

      {/* 6. Khối Laptop Văn Phòng Bán Chạy (5 card/hàng) */}
      <ProductSectionLaptopOffice onAddToCart={handleAddToCart} />

      {/* 7. Khối Chuột Máy Tính Bán Chạy (5 card/hàng) */}
      <ProductSectionMouse onAddToCart={handleAddToCart} />

      {/* 8. Khối Bàn Phím Cơ Bán Chạy (5 card/hàng) */}
      <ProductSectionKeyboard onAddToCart={handleAddToCart} />

      {/* 9. Mid Promo Banners: Đêm Huyền Ảo & Combo Streaming */}
      <MidPromoBanners />

      {/* 10. Khối Màn Hình Chính Hãng (10 sản phẩm = 2 hàng x 5 card) */}
      <ProductSectionMonitor onAddToCart={handleAddToCart} />

      {/* 11. Khối Tin Tức Công Nghệ & Video Highlights */}
      <NewsSection />

      {/* 12. Slide-out Cart Drawer bên phải (Khớp popup giỏ hàng trong ảnh mẫu) */}
      <CartDrawer
        isOpen={isCartOpen}
        onClose={() => setIsCartOpen(false)}
        items={items}
        onUpdateQuantity={handleUpdateQuantity}
        onRemoveItem={removeItem}
        onClearCart={clearCart}
      />
    </div>
  )
}
