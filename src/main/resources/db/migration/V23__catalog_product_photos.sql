-- Replace only the catalog placeholders introduced in V21/V22.
-- Reviewed sources and checksums: catalog/product-photo-catalog.json.
-- No product names, prices, inventory, technical facts or existing real photos change.
CREATE TEMP TABLE reviewed_product_photos (
    slug TEXT PRIMARY KEY, storage_key TEXT UNIQUE, mime_type TEXT,
    size_bytes BIGINT, source_url TEXT, source_page TEXT
) ON COMMIT DROP;
INSERT INTO reviewed_product_photos VALUES
('amd-ryzen-5-5600', 'products/amd-ryzen-5-5600.png', 'image/png', '133478', 'https://product.hstatic.net/200000722513/product/gearvn-amd-ryzen-5-5600-1_cc489afe754e4bee9479a0f465c19ad8_2d93e97db43746a3a0985ec1fe0ed249.png', 'https://gearvn.com/products/amd-ryzen-5-5600'),
('amd-ryzen-5-5600g', 'products/amd-ryzen-5-5600g.png', 'image/png', '711904', 'https://product.hstatic.net/200000722513/product/a_amdryzen5_wgraphics_3dpib_right_row_99e7de9476c8487fa129b02a0d338ea3_c4301323354744c18da49d272dff73ab.png', 'https://gearvn.com/products/amd-ryzen-5-5600g'),
('amd-ryzen-5-7600', 'products/amd-ryzen-5-7600.png', 'image/png', '68665', 'https://product.hstatic.net/200000722513/product/gearvn-amd-ryzen-5-7600-1_fea6e6c8d31a452fb221d1d78261bc47_3169b43038fd4de98a0d0e32feca33a0.png', 'https://gearvn.com/products/amd-ryzen-5-7600'),
('intel-core-i3-12100f', 'products/intel-core-i3-12100f.png', 'image/png', '1101850', 'https://product.hstatic.net/200000722513/product/12100f_d81914f75c254dba985d80033b522662_c3458e8ae6a24c8881b4ae103f0b6a6d.png', 'https://gearvn.com/products/intel-core-i3-12100f'),
('intel-core-i5-12400', 'products/intel-core-i5-12400.png', 'image/png', '712414', 'https://product.hstatic.net/200000722513/product/12400_7150a7594d524982ba859a04ed952903_938a2a0ca05546eb940547a6d50e7bda.png', 'https://gearvn.com/products/intel-core-i5-12400'),
('intel-core-i5-14400f', 'products/intel-core-i5-14400f.png', 'image/png', '318725', 'https://product.hstatic.net/200000722513/product/n22561-001-i5f-_univ_2e1135c9919d46ce97e95d2e19cb74f3.png', 'https://gearvn.com/products/bo-vi-xu-ly-intel-core-i5-14400f'),
('cpu-intel-core-i5-14600k', 'products/cpu-intel-core-i5-14600k.png', 'image/png', '252281', 'https://product.hstatic.net/200000722513/product/n22490-001-rpl-i5k-univ_png_dd9c15cdc33d45e5963d0a5f73f47f1d.png', 'https://gearvn.com/products/intel-core-i5-14600k'),
('amd-ryzen-7-7700', 'products/amd-ryzen-7-7700.jpg', 'image/jpeg', '76814', 'https://www.amd.com/content/dam/amd/en/images/products/processors/ryzen/2505503-ryzen-7-7700.jpg', 'https://www.amd.com/en/products/processors/desktops/ryzen/7000-series/amd-ryzen-7-7700.html'),
('cpu-amd-ryzen-7-7800x3d', 'products/cpu-amd-ryzen-7-7800x3d.jpg', 'image/jpeg', '85033', 'https://www.amd.com/content/dam/amd/en/images/products/processors/ryzen/2505503-ryzen-7-7800x3d.jpg', 'https://www.amd.com/en/products/processors/desktops/ryzen/7000-series/amd-ryzen-7-7800x3d.html'),
('kingston-fury-beast-8gb-ddr4', 'products/kingston-fury-beast-8gb-ddr4.jpg', 'image/jpeg', '14883', 'https://media.kingston.com/kingston/product/FURY_Beast_Black_DDR4_1-lg.jpg', 'https://www.kingston.com/en/memory/gaming/kingston-fury-beast-ddr4-memory'),
('kingston-fury-beast-16gb-ddr4', 'products/kingston-fury-beast-16gb-ddr4.jpg', 'image/jpeg', '14883', 'https://media.kingston.com/kingston/product/FURY_Beast_Black_DDR4_1-lg.jpg', 'https://www.kingston.com/en/memory/gaming/kingston-fury-beast-ddr4-memory'),
('kingston-fury-beast-32gb-ddr4', 'products/kingston-fury-beast-32gb-ddr4.webp', 'image/webp', '42596', 'https://media.kingston.com/kingston/product/FURY_Beast_Black_DDR4_2_angle-lg.jpg', 'https://www.kingston.com/en/memory/gaming/kingston-fury-beast-ddr4-memory'),
('kingston-fury-beast-16gb-ddr5', 'products/kingston-fury-beast-16gb-ddr5.jpg', 'image/jpeg', '25647', 'https://media.kingston.com/kingston/product/FURY_Beast_Black_DDR5_1-lg.jpg', 'https://www.kingston.com/en/memory/gaming/kingston-fury-beast-ddr5-memory'),
('kingston-fury-beast-32gb-ddr5', 'products/kingston-fury-beast-32gb-ddr5.webp', 'image/webp', '37720', 'https://media.kingston.com/kingston/product/FURY_Beast_Black_EXPO_DDR5_2_angle-lg.jpg', 'https://www.kingston.com/en/memory/gaming/kingston-fury-beast-ddr5-memory'),
('kingston-fury-beast-64gb-ddr5', 'products/kingston-fury-beast-64gb-ddr5.webp', 'image/webp', '37720', 'https://media.kingston.com/kingston/product/FURY_Beast_Black_EXPO_DDR5_2_angle-lg.jpg', 'https://www.kingston.com/en/memory/gaming/kingston-fury-beast-ddr5-memory'),
('ram-corsair-vengeance-rgb-32gb-ddr5-6000', 'products/ram-corsair-vengeance-rgb-32gb-ddr5-6000.webp', 'image/webp', '65830', 'https://assets.corsair.com/image/upload/c_pad,q_auto,h_1024,w_1024,f_auto/products/Memory/vengeance-rgb-ddr5-blk-config/Gallery/Vengeance-RGB-DDR5-2UP-BLACK_01.webp', 'https://www.corsair.com/us/en/p/memory/cmh32gx5m2d6000c36/vengeance-rgb-32gb-2x16gb-ddr5-dram-6000mhz-c36-memory-kit-black-cmh32gx5m2d6000c36'),
('crucial-p3-plus-500gb', 'products/crucial-p3-plus-500gb.jpg', 'image/jpeg', '98018', 'https://www.cfd.co.jp/webpim/product/image/c/crucial-p3-plus-m2-series/ct500p3pssd8jp/ct500p3pssd8jp__0100.jpg', 'https://www.cfd.co.jp/biz/product/detail/ct500p3pssd8jp.html'),
('crucial-p3-plus-1000gb', 'products/crucial-p3-plus-1000gb.jpg', 'image/jpeg', '97170', 'https://www.cfd.co.jp/webpim/product/image/c/crucial-p3-plus-m2-series/ct1000p3pssd8jp/ct1000p3pssd8jp__0100.jpg', 'https://www.cfd.co.jp/biz/product/detail/ct1000p3pssd8jp.html'),
('crucial-p3-plus-2000gb', 'products/crucial-p3-plus-2000gb.jpg', 'image/jpeg', '97195', 'https://www.cfd.co.jp/webpim/product/image/c/crucial-p3-plus-m2-series/ct2000p3pssd8jp/ct2000p3pssd8jp__0100.jpg', 'https://www.cfd.co.jp/biz/product/detail/ct2000p3pssd8jp.html'),
('kingston-nv3-2tb', 'products/kingston-nv3-2tb.jpg', 'image/jpeg', '53293', 'https://media.kingston.com/kingston/product/SNV3S_2000GB-lg.jpg', 'https://www.kingston.com/en/ssd/nv3-nvme-pcie-ssd?partnum=snv3s%2F2000g'),
('ssd-samsung-990-pro-1tb', 'products/ssd-samsung-990-pro-1tb.png', 'image/png', '287884', 'https://images.samsung.com/is/image/samsung/p6pim/uk/mz-v9p1t0bw/gallery/uk-990pro-nvme-m2-ssd-mz-v9p1t0bw-534234835?$1164_776_PNG$', 'https://www.samsung.com/uk/memory-storage/nvme-ssd/990-pro-1tb-nvme-pcie-gen-4-mz-v9p1t0bw/'),
('sapphire-pulse-rx-7600', 'products/sapphire-pulse-rx-7600.png', 'image/png', '68529', 'https://media.cdn.sapphiretech.com.cn/-/media/sites/sapphire/pim/product-images/11324_01_rx7600_pulse_8ggddr6/rx7600_pulse_lite_box_card_amd_374x265.ashx?v=e15edf6a8831418c9bf3f16f2432f8a8', 'https://www.sapphiretech.com/en/consumer/pulse-radeon-rx-7600-8g-gddr6'),
('sapphire-pulse-rx-6600', 'products/sapphire-pulse-rx-6600.png', 'image/png', '23761', 'https://media.cdn.sapphiretech.com.cn/-/media/sites/sapphire/pim/product-images/11310_01_rx6600_pulse_8ggddr6/11310_01_rx6600_pulse_c02_374x256.ashx?v=388daa9df3494c459dd78e9c48432fba', 'https://www.sapphiretech.com/en/consumer/pulse-radeon-rx-6600-8g-gddr6'),
('vga-asus-rog-strix-rtx-4080-super-16gb', 'products/vga-asus-rog-strix-rtx-4080-super-16gb.webp', 'image/webp', '18602', 'https://dlcdnwebimgs.asus.com/gain/2B7AD534-F177-4658-B2ED-95CA9FB1677B/w717/h525/fwebp', 'https://rog.asus.com/graphics-cards/graphics-cards/rog-strix/rog-strix-rtx4080s-16g-gaming/'),
('vga-gigabyte-rtx-4060-windforce-oc-8gb', 'products/vga-gigabyte-rtx-4060-windforce-oc-8gb.png', 'image/png', '803839', 'https://www.gigabyte.com/FileUpload/Global/KeyFeature/2421/innergigabyteimages/kf-img.png', 'https://www.gigabyte.com/Graphics-Card/GV-N4060WF2OC-8GD'),
('montech-air-100-argb-black', 'products/montech-air-100-argb-black.webp', 'image/webp', '105554', 'https://www.ryans.com/storage/products/main/montech-air-100-argb-mid-tower-black-micro-atx-11645871991.webp', 'https://www.ryans.com/montech-air-100-argb-mid-tower-black-micro-atx-gaming-desktop-casing'),
('montech-air-100-argb-white', 'products/montech-air-100-argb-white.webp', 'image/webp', '97020', 'https://www.ryans.com/storage/products/main/montech-air-100-argb-mid-tower-white-micro-atx-11645871570.webp', 'https://www.ryans.com/montech-air-100-argb-mid-tower-white-micro-atx-gaming-desktop-casing'),
('deepcool-ak620', 'products/deepcool-ak620.webp', 'image/webp', '69230', 'https://cdn.deepcool.com/public/ProductFile/DEEPCOOL/Cooling/CPUAirCoolers/AK620/Gallery/800X800/01.jpg', 'https://global.deepcool.com/products/Cooling/cpuaircoolers/AK620-High-Performance-CPU-Cooler-1851-1700-AM5/2024/13067.shtml'),
('aoc-24g4', 'products/aoc-24g4.png', 'image/png', '575519', 'https://cdn.sanity.io/images/hf5b3axp/production/20a4a6e6cd8b1c6989189fe41db3bcf18d8fc511-2000x1500.png', 'https://www.aoc.com/ph/gaming/monitors/24g4'),
('msi-pro-mp251', 'products/msi-pro-mp251.webp', 'image/webp', '75522', 'https://storage-asset.msi.com/global/picture/product/product_16964691785d67cfa559cdef49a85b33fff9b1dc40.webp', 'https://www.msi.com/Business-Productivity-Monitor/PRO-MP251/Gallery'),
('lg-ultragear-27gp850-b', 'products/lg-ultragear-27gp850-b.jpg', 'image/jpeg', '40561', 'https://media.us.lg.com/transform/ecomm-PDPGallery-1100x730/0d1e62da-95ec-40e6-b2a3-0ef94b03aea3/md08000370-Z-01-jpg', 'https://www.lg.com/us/monitors/lg-27gp850-b-gaming-monitor');

WITH replaced AS (
    UPDATE product_images i
    SET storage_key = r.storage_key, mime_type = r.mime_type,
        size_bytes = r.size_bytes, alt_text = p.name
    FROM products p JOIN reviewed_product_photos r ON r.slug = p.slug
    WHERE i.product_id = p.id
      AND i.storage_key = 'products/catalog-' || p.slug || '.svg'
    RETURNING i.product_id
)
UPDATE storefront_products s
SET provenance = COALESCE(s.provenance, '{}'::jsonb)
    || jsonb_build_object('sourceImageUrl', r.source_url, 'imageSourcePage', r.source_page)
FROM products p JOIN reviewed_product_photos r ON r.slug = p.slug
WHERE s.product_id = p.id AND p.id IN (SELECT product_id FROM replaced);
