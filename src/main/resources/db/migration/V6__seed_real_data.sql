-- V6__seed_real_data.sql: Seed dữ liệu thực tế chuẩn 100% theo Schema Supabase

-- 1. Users
INSERT INTO users (id, email, password_hash, phone, full_name, status, session_version, version, created_at, updated_at)
VALUES
('a0000000-0000-0000-0000-000000000001', 'admin@pcstore.vn', '$2a$10$w6AFFWFWkdPXUtAe5nzw5.UlGXTLUv5tHRySRp3vxo8kStpd.YV9y', '0901234567', 'Quản Trị Viên Hệ Thống', 'ACTIVE', 1, 0, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000002', 'canh@pcstore.vn', '$2a$10$cwY61nivjTVDcOyTxhSNIuhkN2GLeT1CEAwsbuW0fB7KmfPMbhUCq', '0912345678', 'Võ Văn Cảnh', 'ACTIVE', 1, 0, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name;

-- 2. User Roles
INSERT INTO user_roles (user_id, role)
VALUES
('a0000000-0000-0000-0000-000000000001', 'ROLE_ADMIN'),
('a0000000-0000-0000-0000-000000000001', 'ROLE_STAFF'),
('a0000000-0000-0000-0000-000000000001', 'ROLE_CUSTOMER'),
('a0000000-0000-0000-0000-000000000002', 'ROLE_CUSTOMER')
ON CONFLICT DO NOTHING;

-- 3. Categories
INSERT INTO categories (id, name, slug, description, version, created_at, updated_at)
VALUES
('c0000000-0000-0000-0000-000000000001', 'PC Gaming', 'pc-gaming', 'Hệ thống máy tính tối ưu chơi game mượt mà với FPS cao', 0, NOW(), NOW()),
('c0000000-0000-0000-0000-000000000002', 'PC Đồ Họa & Workstation', 'pc-workstation', 'Cấu hình chuyên dụng cho thiết kế 3D, kiến trúc, render và dựng phim 4K', 0, NOW(), NOW()),
('c0000000-0000-0000-0000-000000000003', 'Bộ Vi Xử Lý (CPU)', 'cpu', 'CPU Intel Gen 13/14 và AMD Ryzen 7000 Series chính hãng', 0, NOW(), NOW()),
('c0000000-0000-0000-0000-000000000004', 'Card Màn Hình (VGA)', 'vga', 'Card đồ họa NVIDIA GeForce RTX 40 Series và AMD Radeon', 0, NOW(), NOW()),
('c0000000-0000-0000-0000-000000000005', 'Bộ Nhớ RAM', 'ram', 'RAM DDR4 và DDR5 tốc độ cao có tản nhiệt RGB', 0, NOW(), NOW()),
('c0000000-0000-0000-0000-000000000006', 'Ổ Cứng SSD', 'storage', 'SSD NVMe PCIe Gen 4 và Gen 5 tốc độ đọc ghi siêu tốc', 0, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

-- 4. Products (Dùng b0000000... là hex hợp lệ)
INSERT INTO products (id, category_id, sku, slug, name, brand, description, price, status, warranty_months, stock_on_hand, reserved_quantity, version, created_at, updated_at)
VALUES
('b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'PC-TITAN-01', 'pc-gaming-titan-i7-14700f-rtx-4070', 'PC Gaming Titan Core i7 14700F / RTX 4070 12GB', 'PC Store Custom', 'Chiến mượt mọi tựa game AAA ở độ phân giải 2K. CPU Intel i7 14700F, 32GB RAM DDR5, RTX 4070 12GB, SSD 1TB NVMe, Nguồn 750W Gold.', 33500000, 'ACTIVE', 36, 10, 0, 0, NOW(), NOW()),

('b0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'PC-DRAGON-02', 'pc-gaming-dragon-r5-7600-rtx-4060', 'PC Gaming Dragon Ryzen 5 7600 / RTX 4060 8GB', 'PC Store Custom', 'Bộ máy Esports quốc dân, tối ưu game bắn súng CS2, Valorant 300+ FPS. CPU Ryzen 5 7600, 16GB RAM DDR5, RTX 4060 8GB, SSD 500GB.', 19900000, 'ACTIVE', 36, 15, 0, 0, NOW(), NOW()),

('b0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002', 'PC-CREATOR-03', 'pc-workstation-creator-r9-7900x-rtx4070ti', 'PC Workstation Creator Ryzen 9 7900X / 64GB RAM / RTX 4070 Ti Super', 'PC Store Custom', 'Trạm làm việc chuyên nghiệp cho kiến trúc sư, dựng hình 3D Blender, Render V-Ray và Premiere Pro 4K.', 54500000, 'ACTIVE', 36, 5, 0, 0, NOW(), NOW()),

('b0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000004', 'VGA-ASUS-4080S', 'vga-asus-rog-strix-rtx-4080-super-16gb', 'Card màn hình ASUS ROG Strix GeForce RTX 4080 SUPER 16GB', 'ASUS', 'Flagship cao cấp tản nhiệt 3 quạt Axial-tech, khung kim loại nguyên khối, xung nhịp Boost 2580 MHz.', 31900000, 'ACTIVE', 36, 8, 0, 0, NOW(), NOW()),

('b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000004', 'VGA-GIGA-4060', 'vga-gigabyte-rtx-4060-windforce-oc-8gb', 'Card màn hình GIGABYTE GeForce RTX 4060 WINDFORCE OC 8GB', 'GIGABYTE', 'Card đồ họa tầm trung tiết kiệm điện, chơi game Full HD 1080p xuất sắc với DLSS 3 Frame Generation.', 7990000, 'ACTIVE', 36, 20, 0, 0, NOW(), NOW()),

('b0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000003', 'CPU-INTEL-14600K', 'cpu-intel-core-i5-14600k', 'Bộ vi xử lý Intel Core i5 14600K (Up to 5.3GHz, 14 Cores 20 Threads)', 'Intel', 'Cân bằng hoàn hảo giữa chơi game và làm việc đa nhiệm. Socket LGA 1700, hỗ trợ DDR4/DDR5.', 7490000, 'ACTIVE', 36, 25, 0, 0, NOW(), NOW()),

('b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000003', 'CPU-AMD-7800X3D', 'cpu-amd-ryzen-7-7800x3d', 'Bộ vi xử lý AMD Ryzen 7 7800X3D (Up to 5.0GHz, 8 Cores 16 Threads, 3D V-Cache)', 'AMD', 'Vua chơi game với công nghệ bộ nhớ đệm xếp chồng 3D V-Cache 96MB, socket AM5 kiến trúc Zen 4.', 10890000, 'ACTIVE', 36, 12, 0, 0, NOW(), NOW()),

('b0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000005', 'RAM-CORSAIR-32G', 'ram-corsair-vengeance-rgb-32gb-ddr5-6000', 'RAM Corsair Vengeance RGB 32GB (2x16GB) DDR5 6000MHz Black', 'Corsair', 'Bộ nhớ DDR5 hiệu năng cao tích hợp LED RGB 10 vùng năng động, hỗ trợ Intel XMP 3.0 và AMD EXPO.', 3290000, 'ACTIVE', 36, 30, 0, 0, NOW(), NOW()),

('b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000006', 'SSD-SAM-990P-1T', 'ssd-samsung-990-pro-1tb', 'Ổ cứng SSD Samsung 990 PRO 1TB M.2 NVMe PCIe 4.0', 'Samsung', 'Tốc độ đọc tuần tự lên đến 7450 MB/s, ghi 6900 MB/s, bộ điều khiển Pascal mạ niken.', 2790000, 'ACTIVE', 60, 40, 0, 0, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

-- 5. Product Specs
INSERT INTO product_specs (product_id, cpu, gpu, ram_gb, storage_gb, storage_type, motherboard, psu, case_name, operating_system)
VALUES
('b0000000-0000-0000-0000-000000000001', 'Intel Core i7 14700F', 'NVIDIA GeForce RTX 4070 12GB', 32, 1024, 'NVMe PCIe 4.0', 'B760M Gaming Plus WiFi', '750W 80 Plus Bronze', 'NZXT H5 Flow Black', 'Windows 11 Pro 64-bit'),
('b0000000-0000-0000-0000-000000000002', 'AMD Ryzen 5 7600', 'NVIDIA GeForce RTX 4060 8GB', 16, 512, 'NVMe PCIe 4.0', 'B650M Gaming WiFi', '650W 80 Plus Bronze', 'DeepCool CC560', 'Windows 11 Home 64-bit'),
('b0000000-0000-0000-0000-000000000003', 'AMD Ryzen 9 7900X', 'NVIDIA GeForce RTX 4070 Ti Super 16GB', 64, 2048, 'NVMe PCIe 4.0', 'X670E AORUS Master', '850W 80 Plus Gold Modular', 'Lian Li O11 Dynamic EVO', 'Windows 11 Pro Workstation')
ON CONFLICT (product_id) DO NOTHING;

-- 6. Product Suitability (Dùng d0000000... là hex hợp lệ)
INSERT INTO product_suitability (id, product_id, need_profile, score, suitability_reason, limitation)
VALUES
('d0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'GAMING', 95, 'Chiến mượt mà mọi tựa game 2K/4K max settings, tản nhiệt mát mẻ.', 'Giá tầm trung cao.'),
('d0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 'GAMING', 90, 'Lựa chọn quốc dân cho game thủ chơi Esports, CS2, Valorant, GTA V 300+ FPS.', 'Không tối ưu dựng phim 8K nặng.'),
('d0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 'DESIGN', 98, 'Dành riêng cho kiến trúc sư dựng 3Ds Max, Blender, Corona và Premiere 4K.', 'Chi phí đầu tư cao.')
ON CONFLICT (id) DO NOTHING;
