-- Correct display names of the newly seeded demo SKUs only; existing retailer names are preserved.
UPDATE products SET name=replace(name,'Ryzen 5-','Ryzen 5 '),version=version+1,updated_at=NOW()
WHERE sku='DEMO-'||slug AND name LIKE 'CPU AMD Ryzen 5-%';
UPDATE products SET name=replace(name,'Ryzen 7-','Ryzen 7 '),version=version+1,updated_at=NOW()
WHERE sku='DEMO-'||slug AND name LIKE 'CPU AMD Ryzen 7-%';
UPDATE products SET name=replace(name,'_','/'),version=version+1,updated_at=NOW()
WHERE sku='DEMO-'||slug AND slug IN ('kingston-fury-beast-8gb-ddr4','kingston-fury-beast-16gb-ddr4','kingston-fury-beast-32gb-ddr4');

-- Exact model photos from the manufacturer's gallery replace only our own placeholders.
UPDATE product_images i SET storage_key='products/'||p.slug||'.webp',mime_type='image/webp',alt_text=p.name,
size_bytes=CASE p.slug WHEN 'msi-rtx-3050-ventus-2x-6g-oc' THEN 53896 WHEN 'msi-rtx-4060-ventus-2x-black' THEN 40998 ELSE 100994 END
FROM products p WHERE i.product_id=p.id
AND p.slug IN ('msi-rtx-3050-ventus-2x-6g-oc','msi-rtx-4060-ventus-2x-black','msi-b550m-pro-vdh-wifi')
AND i.storage_key='products/catalog-'||p.slug||'.svg';
UPDATE storefront_products s SET provenance=s.provenance||jsonb_build_object('sourceImageUrl',v.url)
FROM products p JOIN (VALUES
 ('msi-rtx-3050-ventus-2x-6g-oc','https://storage-asset.msi.com/global/picture/product/product_1706844025ff33dd54b9ebc705a5b841ced5d64768.webp'),
 ('msi-rtx-4060-ventus-2x-black','https://storage-asset.msi.com/global/picture/product/product_1689728820bee4166c12b53c656c39ab1b81888843.webp'),
 ('msi-b550m-pro-vdh-wifi','https://storage-asset.msi.com/global/picture/product/product_10_20200618152228_5eeb1634e9295.webp')
) AS v(slug,url) ON v.slug=p.slug WHERE s.product_id=p.id;

-- Full PC imports expose the structured BOM already in product_specs, including explicit unknowns.
-- This does not invent specific PSU, RAM, motherboard or case models for historical assemblies.
UPDATE storefront_products s SET provenance=s.provenance || jsonb_build_object('technicalSpecs',(
 SELECT jsonb_agg(jsonb_build_object('label',v.label,'value',
   COALESCE(NULLIF(v.value,''),'Chưa xác định model / thông số'),'verified',false) ORDER BY v.position)
 FROM product_specs ps CROSS JOIN LATERAL (VALUES
  (1,'CPU',ps.cpu),(2,'GPU',ps.gpu),(3,'RAM (GB)',ps.ram_gb::text),
  (4,'Dung lượng lưu trữ (GB)',ps.storage_gb::text),(5,'Loại ổ',ps.storage_type),
  (6,'Mainboard',ps.motherboard),(7,'Nguồn PSU',ps.psu),(8,'Vỏ máy',ps.case_name),(9,'Hệ điều hành',ps.operating_system)
 ) AS v(position,label,value) WHERE ps.product_id=s.product_id
)) FROM products p JOIN categories c ON c.id=p.category_id
WHERE s.product_id=p.id AND c.slug IN ('pc-gaming','pc-office','pc-workstation')
AND EXISTS(SELECT 1 FROM product_specs ps WHERE ps.product_id=p.id);

UPDATE storefront_products SET provenance=jsonb_set(provenance,'{specNotes}',
 COALESCE(provenance->'specNotes','[]')||jsonb_build_array(
 'Đường dẫn GearVN chứa i5-13400F, trong khi bản nhập cũ ghi i5-12400F. Cần đối chiếu đúng BOM của bộ máy trước khi nâng cấp.'))
WHERE product_id=(SELECT id FROM products WHERE slug='pc-gvn-i5-12400f-rtx-3050');
