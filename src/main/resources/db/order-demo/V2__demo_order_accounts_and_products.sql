INSERT INTO users(id,email,phone,full_name,status,phone_verified_at) VALUES
('a0000000-0000-0000-0000-000000000001','admin@pcstore.vn','+84901234567','Quản trị demo','ACTIVE',now()),
('a0000000-0000-0000-0000-000000000002','customer@pcstore.vn','+84912345678','Khách hàng demo','ACTIVE',now()),
('a0000000-0000-0000-0000-000000000003','unverified@pcstore.vn','+84923456789','Khách chưa xác minh','ACTIVE',NULL),
('a0000000-0000-0000-0000-000000000004','locked@pcstore.vn','+84934567890','Khách bị khóa','LOCKED',now());
INSERT INTO user_roles VALUES
('a0000000-0000-0000-0000-000000000001','ROLE_ADMIN'),
('a0000000-0000-0000-0000-000000000001','ROLE_CUSTOMER'),
('a0000000-0000-0000-0000-000000000002','ROLE_CUSTOMER'),
('a0000000-0000-0000-0000-000000000003','ROLE_CUSTOMER'),
('a0000000-0000-0000-0000-000000000004','ROLE_CUSTOMER');
INSERT INTO categories(id,name,slug) VALUES ('c0000000-0000-0000-0000-000000000001','PC Gaming','pc-gaming');
INSERT INTO products(id,category_id,sku,slug,name,brand,price,warranty_months,stock_on_hand) VALUES
('b0000000-0000-0000-0000-000000000001','c0000000-0000-0000-0000-000000000001','PC-TITAN-01','pc-gaming-titan','PC Gaming Titan i7 / RTX 4070','PC Store',33500000,36,10),
('b0000000-0000-0000-0000-000000000002','c0000000-0000-0000-0000-000000000001','PC-DRAGON-02','pc-gaming-dragon','PC Gaming Dragon R5 / RTX 4060','PC Store',19900000,36,15);
INSERT INTO product_specs(product_id,cpu,gpu,ram_gb,storage_gb,storage_type) VALUES
('b0000000-0000-0000-0000-000000000001','Intel Core i7 14700F','RTX 4070',32,1024,'SSD'),
('b0000000-0000-0000-0000-000000000002','AMD Ryzen 5 7600','RTX 4060',16,512,'SSD');
INSERT INTO storefront_products(product_id,catalog_key,original_price,specs,sort_order) VALUES
('b0000000-0000-0000-0000-000000000001','demo-titan',33500000,'["Core i7 14700F","RTX 4070","32GB DDR5"]',1),
('b0000000-0000-0000-0000-000000000002','demo-dragon',19900000,'["Ryzen 5 7600","RTX 4060","16GB DDR5"]',2);
INSERT INTO product_images(id,product_id,storage_key,is_cover,sort_order) VALUES
('e0000000-0000-0000-0000-000000000001','b0000000-0000-0000-0000-000000000001','products/pc-gvn-i7-14700f-rtx-5060.jpg',true,0),
('e0000000-0000-0000-0000-000000000002','b0000000-0000-0000-0000-000000000002','products/pc-gvn-i5-12400f-rtx-5060.png',true,0);
