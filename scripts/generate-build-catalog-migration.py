"""Render a reviewed catalog manifest into a NEW migration. Never replace an applied migration."""
import json
import argparse
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--manifest',default='src/main/resources/catalog/build-catalog-2026-11.json')
parser.add_argument('--output',required=True)
parser.add_argument('--append-sql')
parser.add_argument('--replace',action='store_true',help='Only for an unapplied migration still being reviewed')
args=parser.parse_args()
if (ROOT/args.output).exists() and not args.replace:
    raise SystemExit('Output already exists. Choose a new migration version; --replace is only for unapplied files.')
rows=json.loads((ROOT/args.manifest).read_text(encoding='utf-8'))
LABELS={
 'socket':'Socket CPU','cores':'Số nhân','threads':'Số luồng','base_clock_ghz':'Xung cơ bản (GHz)',
 'boost_clock_ghz':'Xung boost tối đa (GHz)','l3_cache_mb':'Bộ nhớ đệm L3 (MB)','tdp_w':'TDP công bố (W)',
 'max_power_w':'Công suất turbo tối đa (W)','supported_memory_types':'Chuẩn RAM CPU hỗ trợ','integrated_graphics':'Đồ họa tích hợp',
 'family':'Dòng CPU','part_number':'Mã sản phẩm','capacity_gb':'Dung lượng (GB)','module_count':'Số thanh RAM',
 'memory_type':'Loại bộ nhớ','form_factor':'Kích thước / chuẩn','speed_mts':'Tốc độ RAM (MT/s)',
 'cas_latency':'Độ trễ CL','voltage_v':'Điện áp (V)','height_mm':'Chiều cao (mm)',
 'max_memory_gb':'RAM tối đa (GB)','dimm_slots':'Số khe RAM','memory_form_factor':'Loại khe RAM',
 'm2_slots':'Số khe M.2','m2_form_factors':'Kích thước SSD M.2 hỗ trợ','storage_interfaces':'Chuẩn ổ lưu trữ hỗ trợ',
 'sata_ports':'Số cổng SATA','eps_8pin_count':'Số đầu nguồn CPU EPS 8-pin','atx_24pin_count':'Số đầu ATX 24-pin',
 'chipset':'Chipset','supported_cpu_families':'Dòng CPU hỗ trợ (BIOS tùy phiên bản)','gpu_slot':'Khe card đồ họa',
 'interface':'Chuẩn kết nối ổ','m2_form_factor':'Kích thước M.2','pcie_generation':'Thế hệ PCIe','pcie_lanes':'Số làn PCIe',
 'read_mbs':'Tốc độ đọc tối đa (MB/s)','write_mbs':'Tốc độ ghi tối đa (MB/s)','gpu_model':'Bộ xử lý đồ họa',
 'vram_gb':'VRAM (GB)','memory_bus_bits':'Bus bộ nhớ (bit)','length_mm':'Chiều dài (mm)','width_mm':'Chiều rộng (mm)',
 'thickness_mm':'Độ dày (mm)','recommended_psu_w':'Nguồn hệ thống hãng khuyến nghị (W)','pci_slot':'Chuẩn khe GPU',
 'pcie_8pin_count':'Số đầu nguồn GPU PCIe 8-pin','pcie_16pin_count':'Số đầu nguồn GPU 16-pin',
 'power_connectors':'Đầu cấp điện GPU','board_power_w':'Công suất card công bố (W)','wattage_w':'Công suất nguồn (W)',
 'pcie_connectors':'Đầu PCIe của nguồn','efficiency_rating':'Chứng nhận hiệu suất','modularity':'Kiểu dây nguồn',
 'max_gpu_length_mm':'Chiều dài GPU tối đa (mm)','max_cooler_height_mm':'Chiều cao tản CPU tối đa (mm)',
 'max_psu_length_mm':'Chiều dài PSU tối đa (mm)','supported_motherboard_form_factors':'Khổ mainboard hỗ trợ',
 'supported_psu_form_factors':'Chuẩn PSU hỗ trợ','expansion_slots':'Số khe mở rộng vỏ','supported_radiator_sizes_mm':'Radiator hỗ trợ (mm)',
 'color':'Màu sắc','cooler_type':'Loại tản nhiệt','supported_sockets':'Socket tản hỗ trợ',
 'max_tdp_w':'Mức tản nhiệt hãng công bố (W)','ram_clearance_unrestricted':'Thiết kế không che khe RAM',
 'ram_clearance_mm':'Khoảng trống RAM với quạt mặc định (mm)','fan_size_mm':'Cỡ quạt (mm)','fan_count':'Số quạt',
 'heatpipes':'Số ống dẫn nhiệt','fan_connector':'Đầu quạt','radiator_size_mm':'Cỡ radiator (mm)',
 'screen_size_in':'Kích thước hiển thị (inch)','panel_type':'Tấm nền','resolution':'Độ phân giải','refresh_rate_hz':'Tần số quét (Hz)',
 'video_ports':'Cổng hình ảnh','brightness_nits':'Độ sáng điển hình (nit)','vesa':'Chuẩn VESA','response_time':'Thời gian đáp ứng',
 'speakers':'Loa tích hợp','height_adjustment_mm':'Nâng hạ chân đế (mm)','overclock_hz':'Tần số ép xung tối đa (Hz)',
 'layout':'Bố cục phím','key_count':'Số phím','connection':'Kết nối','switch_type':'Switch','hot_swap':'Thay switch nóng',
 'keycaps':'Keycap','backlight':'Đèn nền','battery_mah':'Dung lượng pin (mAh)','dimensions':'Kích thước',
 'sensor':'Cảm biến','dpi_min':'DPI tối thiểu','dpi_max':'DPI tối đa','weight_g':'Khối lượng (g)',
 'battery':'Pin','polling_rate_hz':'Tần số phản hồi (Hz)','imported_description':'Mô tả từ dữ liệu cũ',
 'ram_type':'Chuẩn RAM','ram_gb':'RAM (GB)','storage_gb':'Lưu trữ (GB)','cpu':'CPU','gpu':'GPU',
 'frequency_mhz':'Tốc độ (MHz)','speed_mhz':'Tốc độ (MHz)','storage_type':'Loại ổ','bus_width':'Bus bộ nhớ',
 'jedec_speed_mts':'Tốc độ JEDEC mặc định (MT/s)','jedec_voltage_v':'Điện áp JEDEC (V)','memory_profile':'Profile bộ nhớ',
}

def value(v):
    if isinstance(v,bool): return 'Có' if v else 'Không'
    if isinstance(v,list): return ', '.join(map(str,v))
    return str(v)

for r in rows:
    r['technicalSpecs']=[dict(label=LABELS.get(k,'Thông số từ bản nhập cũ' if k.startswith('imported_') else k.replace('_',' ')),
        value=value(v),verified=k in r['verifiedFields']) for k,v in r['facts'].items()
        if k!='verified_fields' and not (k=='ram_type' and 'memory_type' in r['facts'])]
    r['badges']=[dict(label=t['label']+': '+t['value']) for t in r['technicalSpecs'][:5]]
    r['description']='. '.join(t['label']+': '+t['value'] for t in r['technicalSpecs'][:8])+'.'
data=json.dumps(rows,ensure_ascii=False,separators=(',',':'))
assert '$catalog$' not in data
sql='''-- Reviewed manufacturer facts, legacy enrichment and explicitly marked demo products.
-- Generated by scripts/generate-build-catalog-migration.py from the versioned manifest.
-- Existing products' names, prices, stock, reservations and retailer provenance are preserved.
ALTER TABLE product_component_specs ADD COLUMN IF NOT EXISTS socket VARCHAR(20);
ALTER TABLE product_component_specs ADD COLUMN IF NOT EXISTS component_type TEXT;
ALTER TABLE product_component_specs ADD COLUMN IF NOT EXISTS specs JSONB NOT NULL DEFAULT '{}';
ALTER TABLE product_component_specs ADD COLUMN IF NOT EXISTS verification_status TEXT NOT NULL DEFAULT 'draft';
ALTER TABLE product_component_specs ADD COLUMN IF NOT EXISTS evidence JSONB NOT NULL DEFAULT '[]';
ALTER TABLE product_component_specs ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE product_component_specs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
UPDATE product_component_specs cs SET component_type=c.slug
FROM products p JOIN categories c ON c.id=p.category_id
WHERE cs.product_id=p.id AND cs.component_type IS NULL;

CREATE TEMP TABLE reviewed_build_catalog(data JSONB) ON COMMIT DROP;
INSERT INTO reviewed_build_catalog SELECT value FROM jsonb_array_elements($catalog$'''+data+'''$catalog$::jsonb);

INSERT INTO products(id,category_id,sku,slug,name,brand,description,price,status,warranty_months,
 stock_on_hand,reserved_quantity,version,created_at,updated_at)
SELECT md5('pixelcore:catalog:'||(data->>'slug'))::uuid,c.id,'DEMO-'||(data->>'slug'),data->>'slug',
 data->>'name',data->>'brand',data->>'description',(data->>'price')::numeric,'ACTIVE',0,
 (data->>'stock')::int,0,0,NOW(),NOW()
FROM reviewed_build_catalog r JOIN categories c ON c.slug=r.data->>'category'
WHERE (data->>'new')::boolean
ON CONFLICT(slug) DO NOTHING;

INSERT INTO product_component_specs(product_id,socket,component_type,specs,verification_status,evidence,verified_at,updated_at)
SELECT p.id,data->'facts'->>'socket',data->>'category',data->'facts',data->>'status',data->'sources',
 CASE WHEN data->>'status'='verified' THEN NOW() END,NOW()
FROM reviewed_build_catalog r JOIN products p ON p.slug=r.data->>'slug'
WHERE data->>'category' IN ('cpu','mainboard','ram','vga','storage','psu','case','cooling')
ON CONFLICT(product_id) DO UPDATE SET
 socket=COALESCE(EXCLUDED.socket,product_component_specs.socket),component_type=EXCLUDED.component_type,
 specs=product_component_specs.specs || EXCLUDED.specs,verification_status=EXCLUDED.verification_status,
 evidence=EXCLUDED.evidence,verified_at=EXCLUDED.verified_at,updated_at=NOW();

INSERT INTO storefront_products(product_id,catalog_key,original_price,specs,provenance,sort_order)
SELECT p.id,'build-'||p.slug,p.price,data->'badges',jsonb_build_object(
 'technicalSpecs',data->'technicalSpecs','specSources',data->'sources','specNotes',data->'notes',
 'specCheckedAt','2026-11-26','demoCatalog',true),200+ROW_NUMBER() OVER(ORDER BY p.slug)
FROM reviewed_build_catalog r JOIN products p ON p.slug=r.data->>'slug'
WHERE (data->>'expose')::boolean
ON CONFLICT(product_id) DO UPDATE SET provenance=storefront_products.provenance ||
 jsonb_build_object('technicalSpecs',EXCLUDED.provenance->'technicalSpecs',
 'specSources',EXCLUDED.provenance->'specSources','specNotes',EXCLUDED.provenance->'specNotes','specCheckedAt','2026-11-26');

-- Detailed information is also available for legacy products outside the storefront.
-- Image keys for new products are local reviewed assets; placeholders are explicitly labeled.
INSERT INTO product_images(id,product_id,storage_key,mime_type,size_bytes,sort_order,is_cover,alt_text,created_at)
SELECT md5('pixelcore:catalog:image:'||p.slug)::uuid,p.id,
 COALESCE(data->>'image','products/catalog-'||(data->>'category')||'.svg'),
 CASE WHEN data->>'image' LIKE '%.webp' THEN 'image/webp' ELSE 'image/svg+xml' END,0,0,true,p.name,NOW()
FROM reviewed_build_catalog r JOIN products p ON p.slug=r.data->>'slug'
WHERE (data->>'expose')::boolean AND NOT EXISTS(SELECT 1 FROM product_images i WHERE i.product_id=p.id)
ON CONFLICT(id) DO NOTHING;
'''
if args.append_sql: sql+='\n'+(ROOT/args.append_sql).read_text(encoding='utf-8')
(ROOT/args.output).write_text(sql,encoding='utf-8')
print(f'{Path(args.output).name}: {len(rows)} records; {sum(r["new"] for r in rows)} new demo products')
