"""Build the reviewed catalog manifest from a product-only export (never credentials).

Usage: python scripts/prepare-build-catalog.py <ExportBuildCatalog.json>
Prices/stock of NEW records are demo values, not scraped offers. Existing values are untouched.
Manufacturer specifications were checked on 2026-11-26; unresolved revisions stay needs_review.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
old = json.loads(Path(sys.argv[1]).read_text(encoding='utf-8'))['products']
rows = {p['slug']: dict(slug=p['slug'], category=p['category'], name=p['name'], brand=p['brand'],
    price=p['price'], stock=p['stock'], new=False, expose=bool(p['storefront']),
    facts={}, sources=[], notes=[], verifiedFields=[]) for p in old}
AMD = 'https://www.amd.com/en/products/processors/desktops/ryzen/7000-series/amd-ryzen-'
MSI = 'https://www.msi.com/'

def enrich(slug, url, **facts):
    r = rows[slug]
    r['facts'].update(facts)
    r['verifiedFields'] = sorted(set(r['verifiedFields']) | set(facts))
    r['sources'].append(dict(url=url, name=r['brand'], checkedAt='2026-11-26', fields=list(facts), kind='manufacturer'))
    return r

def add(slug, category, name, brand, price, url, **facts):
    rows[slug] = dict(slug=slug, category=category, name=name, brand=brand, price=price,
        stock=20, new=True, expose=True, facts={}, verifiedFields=[], sources=[], notes=[])
    return enrich(slug, url, **facts)

def intel(slug, model, sku, cores, threads, base, boost, cache, power, maxpower, igpu, price=None):
    url=f'https://www.intel.com/content/www/us/en/products/sku/{sku}/intel-core-{model.lower().replace("-", "")}-processor-{cache}m-cache-up-to-{boost:.2f}-ghz/specifications.html'
    # ARK resolves by SKU; use the checked, canonical URLs below (decimal separators are hyphens).
    url=url.replace(f'{boost:.2f}', f'{boost:.2f}'.replace('.', '-'))
    f=dict(socket='LGA1700', cores=cores, threads=threads, base_clock_ghz=base, boost_clock_ghz=boost,
        l3_cache_mb=cache, tdp_w=power, max_power_w=maxpower, supported_memory_types=['DDR4','DDR5'],
        integrated_graphics=igpu, family='intel-core-'+model.split('-')[1][:2])
    if price: add(slug,'cpu','CPU Intel Core '+model,'Intel',price,url,**f)
    else: enrich(slug,url,**f)

intel('intel-core-i5-12400f-tray','i5-12400F',134587,6,12,2.5,4.4,18,65,117,'Không có')
intel('cpu-intel-core-i5-14600k','i5-14600K',236799,14,20,3.5,5.3,24,125,181,'Intel UHD 770')
intel('intel-core-i3-12100f','i3-12100F',132223,4,8,3.3,4.3,12,58,89,'Không có',1890000)
intel('intel-core-i5-12400','i5-12400',134586,6,12,2.5,4.4,18,65,117,'Intel UHD 730',3190000)
intel('intel-core-i5-14400f','i5-14400F',236777,10,16,2.5,4.7,20,65,148,'Không có',4290000)
# 14th gen uses Intel's newer URL naming, not the legacy core-i5 path.
rows['intel-core-i5-14400f']['sources'][0]['url']='https://www.intel.com/content/www/us/en/products/sku/236777/intel-core-i5-processor-14400f-20m-cache-up-to-4-70-ghz/specifications.html'
rows['cpu-intel-core-i5-14600k']['sources'][0]['url']='https://www.intel.com/content/www/us/en/products/sku/236799/intel-core-i5-processor-14600k-24m-cache-up-to-5-30-ghz/specifications.html'

def amd(slug, model, cores, base, boost, cache, tdp, socket='AM5', igpu='AMD Radeon Graphics', price=None):
    url=AMD+model.lower()+'.html' if socket=='AM5' else 'https://www.amd.com/en/support/downloads/drivers.html/processors/ryzen/ryzen-5000-series/amd-ryzen-'+model.lower()+'.html'
    f=dict(socket=socket, cores=cores, threads=cores*2, base_clock_ghz=base, boost_clock_ghz=boost,
        l3_cache_mb=cache, tdp_w=tdp, supported_memory_types=['DDR5' if socket=='AM5' else 'DDR4'],
        integrated_graphics=igpu, family='ryzen-'+('7000' if socket=='AM5' else '5000'))
    if price: add(slug,'cpu','CPU AMD Ryzen '+model,'AMD',price,url,**f)
    else: enrich(slug,url,**f)

amd('amd-ryzen-5-7500f-tray','5-7500F',6,3.7,5.0,32,65,igpu='Không có')
amd('cpu-amd-ryzen-7-7800x3d','7-7800X3D',8,4.2,5.0,96,120)
amd('amd-ryzen-5-5600','5-5600',6,3.5,4.4,32,65,'AM4','Không có',2490000)
amd('amd-ryzen-5-5600g','5-5600G',6,3.9,4.4,16,65,'AM4','Radeon Graphics 7 nhân',2690000)
amd('amd-ryzen-5-7600','5-7600',6,3.8,5.1,32,65,price=4590000)
amd('amd-ryzen-7-7700','7-7700',8,3.8,5.3,32,65,price=6490000)

def board(slug, model, socket, memory, maxgb, slots, m2, form, eps, chipset, families, price=None):
    url=MSI+'Motherboard/'+model.replace(' ','-')+'/Specification'
    if slug=='asus-tuf-b760m-plus-wifi-ii':
        url='https://www.asus.com/motherboards-components/motherboards/tuf-gaming/tuf-gaming-b760m-plus-wifi-ii/techspec/'
    f=dict(socket=socket, memory_type=memory, ram_type=memory, max_memory_gb=maxgb, dimm_slots=slots,
        memory_form_factor='UDIMM', m2_slots=m2, m2_form_factors=['2280'], storage_interfaces=['NVME','SATA'],
        sata_ports=4, form_factor=form, eps_8pin_count=eps, atx_24pin_count=1, chipset=chipset,
        supported_cpu_families=families, gpu_slot='PCIEX16')
    if price: add(slug,'mainboard','Mainboard MSI '+model,'MSI',price,url,**f)
    else: enrich(slug,url,**f)

board('msi-pro-b760m-a-wifi-ddr4','PRO B760M-A WIFI DDR4','LGA1700','DDR4',128,4,2,'MATX',2,'B760',['intel-core-12','intel-core-13','intel-core-14'])
board('asus-tuf-b760m-plus-wifi-ii','TUF GAMING B760M-PLUS WIFI II','LGA1700','DDR5',192,4,3,'MATX',2,'B760',['intel-core-12','intel-core-13','intel-core-14'])
board('msi-pro-h610m-g-ddr4','PRO H610M-G DDR4','LGA1700','DDR4',64,2,1,'MATX',1,'H610',['intel-core-12','intel-core-13','intel-core-14'],1790000)
board('msi-b550m-pro-vdh-wifi','B550M PRO-VDH WIFI','AM4','DDR4',128,4,2,'MATX',1,'B550',['ryzen-3000','ryzen-4000','ryzen-5000'],2590000)
board('msi-b650-gaming-plus-wifi','B650 GAMING PLUS WIFI','AM5','DDR5',256,4,2,'ATX',2,'B650',['ryzen-7000','ryzen-8000','ryzen-9000'],4190000)

def ram(slug, part, gb, modules, memory, rate, cl, voltage, height, price=None):
    url='https://www.kingston.com/datasheets/'+part+'.pdf'
    f=dict(part_number=part.replace('_','/'),capacity_gb=gb,module_count=modules,memory_type=memory,form_factor='UDIMM',
        speed_mts=rate,cas_latency=cl,voltage_v=voltage,height_mm=height)
    if price: add(slug,'ram',f'RAM Kingston FURY Beast {gb}GB ({modules} thanh) {memory}-{rate} {part}','Kingston',price,url,**f)
    else: enrich(slug,url,**f)

ram('kingston-fury-beast-8gb-ddr4','KF432C16BB_8',8,1,'DDR4',3200,16,1.35,34,490000)
ram('kingston-fury-beast-16gb-ddr4','KF432C16BB_16',16,1,'DDR4',3200,16,1.35,34,890000)
ram('kingston-fury-beast-32gb-ddr4','KF432C16BBK2_32',32,2,'DDR4',3200,16,1.35,34,1690000)
ram('kingston-fury-beast-32gb-ddr5','KF560C36BBE2K2-32',32,2,'DDR5',6000,36,1.35,34.9,2690000)
for slug in ['kingston-fury-beast-8gb-ddr4','kingston-fury-beast-16gb-ddr4','kingston-fury-beast-32gb-ddr4']:
    enrich(slug,rows[slug]['sources'][0]['url'],jedec_speed_mts=2400,jedec_voltage_v=1.2,memory_profile='Intel XMP 2.0')
    rows[slug]['notes'].append('3200 MT/s, CL16 và 1.35 V là cấu hình XMP; mặc định JEDEC 2400 MT/s ở 1.2 V. H610 có thể giới hạn tùy chọn XMP.')
enrich('kingston-fury-beast-32gb-ddr5',rows['kingston-fury-beast-32gb-ddr5']['sources'][0]['url'],jedec_speed_mts=4800,jedec_voltage_v=1.1,memory_profile='AMD EXPO / Intel XMP 3.0')
enrich('corsair-vengeance-rgb-32gb-ddr5-white','https://www.corsair.com/ww/en/p/memory/cmh32gx5m2e6000c36w/vengeance-rgb-32gb-2x16gb-ddr5-dram-6000mts-cl36-memory-kit-white-cmh32gx5m2e6000c36w',
    part_number='CMH32GX5M2E6000C36W',capacity_gb=32,module_count=2,memory_type='DDR5',form_factor='UDIMM',speed_mts=6000,cas_latency=36)
# An exact module part number is absent from these older imports: do not fabricate height/latency.
for slug in ['kingmax-blade-x-16gb-ddr4','ram-corsair-vengeance-rgb-32gb-ddr5-6000']:
    r=rows[slug]; r['notes'].append('Cần đối chiếu mã module trên tem để xác minh timing, điện áp và chiều cao tản RAM.')

def ssd(slug, brand, model, part, gb, read, write, gen, url, price=None):
    f=dict(part_number=part,capacity_gb=gb,interface='NVME',form_factor='M.2',m2_form_factor='2280',
        pcie_generation=gen,pcie_lanes=4,read_mbs=read,write_mbs=write)
    if price: add(slug,'storage',f'SSD {brand} {model} {gb}GB M.2 NVMe PCIe {gen}.0',brand,price,url,**f)
    else: enrich(slug,url,**f)

nv3='https://www.kingston.com/datasheets/snv3s_en.pdf'
p3='https://www.crucial.com/content/dam/crucial/ssd-products/p3-plus/flyers/b2c/crucial-p3plus-productflyer-consumer.pdf'
s990='https://download.semiconductor.samsung.com/resources/data-sheet/Samsung_NVMe_SSD_990_PRO_Datasheet_Rev.1.0_10129514049640.pdf'
ssd('kingston-nv3-1tb','Kingston','NV3','SNV3S/1000G',1000,6000,4000,4,nv3)
ssd('kingston-nv3-2tb','Kingston','NV3','SNV3S/2000G',2000,6000,5000,4,nv3,2990000)
for gb,read,write,price in [(500,4700,1900,790000),(1000,5000,3600,1390000),(2000,5000,4200,2490000)]:
    ssd(f'crucial-p3-plus-{gb}gb','Crucial','P3 Plus',f'CT{gb}P3PSSD8',gb,read,write,4,p3,price)
ssd('ssd-samsung-990-pro-1tb','Samsung','990 PRO','MZ-V9P1T0',1000,7450,6900,4,s990)
ssd('samsung-990-pro-2tb','Samsung','990 PRO','MZ-V9P2T0',2000,7450,6900,4,s990)
enrich('samsung-9100-pro-1tb','https://download.semiconductor.samsung.com/resources/data-sheet/Samsung_NVMe_SSD_9100_PRO_Datasheet_Rev.2.0.pdf',
    part_number='MZ-VAP1T0',capacity_gb=1000,interface='NVME',form_factor='M.2',m2_form_factor='2280',pcie_generation=5,pcie_lanes=4)

def gpu(slug, brand, model, gb, memory, bus, length, width, thickness, watts, psuw, pins8, pins16, url, price=None):
    f=dict(gpu_model=model,vram_gb=gb,memory_type=memory,memory_bus_bits=bus,length_mm=length,
        width_mm=width,thickness_mm=thickness,recommended_psu_w=psuw,pci_slot='PCIEX16',
        pcie_8pin_count=pins8,pcie_16pin_count=pins16,power_connectors=f'{pins8} × PCIe 8-pin; {pins16} × 16-pin')
    if watts: f['board_power_w']=watts
    if price: add(slug,'vga','VGA '+brand+' '+model,brand,price,url,**f)
    else: enrich(slug,url,**f)

gpu('msi-rtx-3050-ventus-2x-6g-oc','MSI','RTX 3050 VENTUS 2X 6G OC',6,'GDDR6',96,189,109,42,70,300,0,0,MSI+'Graphics-Card/GeForce-RTX-3050-VENTUS-2X-6G-OC/Specification',3890000)
gpu('sapphire-pulse-rx-6600','Sapphire','PULSE Radeon RX 6600',8,'GDDR6',128,193,120.05,40.05,140,500,1,0,'https://www.sapphiretech.com/en/news/sapphire-pulse-rx-6600',4990000)
gpu('sapphire-pulse-rx-7600','Sapphire','PULSE Radeon RX 7600',8,'GDDR6',128,240,107.1,44.07,185,550,1,0,'https://www.sapphiretech.com/en/consumer/pulse-radeon-rx-7600-8g-gddr6',6490000)
gpu('msi-rtx-4060-ventus-2x-black','MSI','RTX 4060 VENTUS 2X BLACK 8G OC',8,'GDDR6',128,199,120,41,120,550,1,0,MSI+'Graphics-Card/GeForce-RTX-4060-VENTUS-2X-BLACK-8G-OC/Specification',7890000)
gpu('vga-gigabyte-rtx-4060-windforce-oc-8gb','Gigabyte','RTX 4060 WINDFORCE OC 8G',8,'GDDR6',128,192,120,41,None,450,1,0,'https://www.gigabyte.com/Graphics-Card/GV-N4060WF2OC-8GD/sp')
gpu('msi-rtx-5060-ti-shadow-2x','MSI','RTX 5060 Ti 8G SHADOW 2X OC PLUS',8,'GDDR7',128,226,126,41,180,600,1,0,MSI+'Graphics-Card/GeForce-RTX-5060-Ti-8G-SHADOW-2X-OC-PLUS/Specification')
gpu('zotac-rtx-5060-twin-edge-oc','Zotac','RTX 5060 Twin Edge OC',8,'GDDR7',128,220.5,120.25,41.6,145,550,1,0,'https://www.zotacstore.com/us/gaming-geforce-rtx-5060-twin-edge-oc')
gpu('zotac-rtx-5070-solid-oc','Zotac','RTX 5070 SOLID OC',12,'GDDR7',192,304.4,115.8,41.6,250,650,0,1,'https://www.zotac.com/us/product/graphics_card/zotac-gaming-geforce-rtx-5070-solid-oc')
gpu('vga-asus-rog-strix-rtx-4080-super-16gb','ASUS','ROG STRIX RTX 4080 SUPER',16,'GDDR6X',256,357.6,149.3,70.1,None,850,0,1,
    'https://rog.asus.com/graphics-cards/graphics-cards/rog-strix/rog-strix-rtx4080s-16g-gaming/spec/')

def psu(slug, model, watts, length, eps, pins8, pins16, efficiency, modular, price=None, url=None):
    url=url or MSI+'Power-Supply/'+model.replace(' ','-')+'/Specification'
    f=dict(wattage_w=watts,form_factor='ATX',length_mm=length,eps_8pin_count=eps,atx_24pin_count=1,
        pcie_8pin_count=pins8,pcie_16pin_count=pins16,pcie_connectors=f'{pins8} × PCIe 6+2-pin; {pins16} × 16-pin',
        efficiency_rating=efficiency,modularity=modular)
    if price: add(slug,'psu','Nguồn MSI '+model,'MSI',price,url,**f)
    else: enrich(slug,url,**f)

psu('msi-mag-a550bn','MAG A550BN',550,140,1,2,0,'80 Plus Bronze','Non-modular',890000)
psu('msi-mag-a650bn','MAG A650BN',650,140,1,2,0,'80 Plus Bronze','Non-modular',1090000)
psu('msi-mag-a750gl-pcie5','MAG A750GL PCIE5',750,140,2,3,1,'80 Plus Gold','Full modular',2490000)
psu('corsair-cx650-bronze','CX650',650,125,1,2,0,'80 Plus Bronze','Non-modular',url='https://www.corsair.com/us/en/p/psu/cp-9020278-na/cx-series-cx650-650-watt-80-plus-bronze-atx-power-supply-cp-9020278-na')
psu('corsair-rm750e-gold','RM750e',750,140,2,3,0,'80 Plus Gold','Full modular',url='https://www.corsair.com/ww/en/p/psu/cp-9020248-in/rme-series-rm750e-fully-modular-low-noise-atx-power-supply-cp-9020248-in')
psu('corsair-rm850e-gold-atx30','RM850e',850,140,2,3,1,'80 Plus Gold','Full modular',url='https://www.corsair.com/us/en/p/psu/CP-9020263-NA/rme-series-rm850e-fully-modular-low-noise-atx-power-supply-cp-9020263-na')
rows['corsair-rm850e-gold-atx30']['notes'].append('Trang hãng hiện ghi ATX 3.1 và cáp 12V-2x6, khác tên ATX 3.0 trong bản nhập cũ. Cần xác minh bộ cáp theo revision đang bán.')
rows['corsair-rm850e-gold-atx30']['verifiedFields'].remove('pcie_16pin_count')

def case(slug, model, gpu_len, cooler_height, psu_len, forms, rad, url, price=None, color='Black', slots=7):
    f=dict(max_gpu_length_mm=gpu_len,max_cooler_height_mm=cooler_height,max_psu_length_mm=psu_len,
        supported_motherboard_form_factors=forms,supported_psu_form_factors=['ATX'],expansion_slots=slots,
        supported_radiator_sizes_mm=rad,color=color)
    if price: add(slug,'case','Case '+model,'Montech',price,url,**f)
    else: enrich(slug,url,**f)

case('corsair-frame-4000d-black','FRAME 4000D',430,170,220,['ATX','MATX','MINIITX','EATX'],[120,140,240,280,360],
    'https://www.corsair.com/uk/en/p/pc-cases/cc-9011290-ww/frame-4000d-modular-mid-tower-pc-case-cc-9011290-ww')
# Retain the conservative historical GPU limit (410 mm), below the current manufacturer limit 425 mm.
case('corsair-3500x-white','3500X',410,170,180,['ATX','MATX','MINIITX','EATX'],[120,240,280,360],
    'https://www.corsair.com/us/en/p/pc-cases/cc-9011277-ww/3500x-mid-tower-pc-case-white-cc-9011277-ww',color='White')
rows['corsair-3500x-white']['notes'].append('Giới hạn GPU giữ mức thận trọng 410 mm của bản nhập cũ; trang hãng hiện ghi 425 mm. Kiểm tra revision thực tế khi lắp.')
case('lian-li-o11-vision-compact-black','O11 VISION COMPACT',408,167,219,['ATX','MATX','MINIITX','EATX'],[240,280,360],
    'https://lian-li.com/product/o11-vision-compact/')
rows['lian-li-o11-vision-compact-black']['notes'].append('PSU phải ngắn hơn 220 mm. E-ATX rộng dưới 280 mm; radiator nóc chỉ dùng khi chuyển sang nắp mesh. Kiểm tra khoảng trống thực tế của radiator và quạt.')
for color in ['Black','White']:
    case('montech-air-100-argb-'+color.lower(),'Montech AIR 100 ARGB '+color,330,161,160,['MATX','MINIITX'],[120,140,240,280],
        'https://www.montechpc.com/air-100-argb',1390000,color,4)

coolurl='https://global.deepcool.com/products/Cooling/cpuaircoolers/AG400-ARGB-120mm-CPU-Cooler/2022/16084.shtml'
enrich('deepcool-ag400-argb',coolurl,cooler_type='AIR',height_mm=150,supported_sockets=['LGA1700','LGA1200','LGA1151','AM4','AM5'],
    max_tdp_w=220,ram_clearance_unrestricted=True,fan_size_mm=120,heatpipes=4,fan_connector='4-pin PWM')
enrich('cooler-master-hyper-212-spectrum-v3','https://www.coolermaster.com/en-global/products/hyper-212-spectrum-v3/',
    cooler_type='AIR',height_mm=152,supported_sockets=['LGA1700','LGA1200','LGA1151','AM4','AM5'],fan_size_mm=120,heatpipes=4,fan_connector='4-pin PWM')
enrich('corsair-nautilus-360-argb-black','https://www.corsair.com/us/en/p/cpu-coolers/cw-9060093-ww/nautilus-360-rs-argb-liquid-cpu-cooler-cw-9060093-ww',
    cooler_type='AIO',radiator_size_mm=360,supported_sockets=['LGA1700','LGA1851','AM4','AM5'],fan_size_mm=120,fan_count=3)
rows['corsair-nautilus-360-argb-black']['notes'].append('Có nhiều revision NAUTILUS 360 RS ARGB; cần xác minh mã CW trên hộp và tổng độ dày radiator + quạt trước khi lắp.')
add('deepcool-ak620','cooling','Tản nhiệt CPU DeepCool AK620','DeepCool',1490000,
    'https://global.deepcool.com/products/Cooling/cpuaircoolers/AK620-High-Performance-CPU-Cooler-1851-1700-AM5/2024/13067.shtml',
    cooler_type='AIR',height_mm=160,supported_sockets=['LGA1851','LGA1700','LGA1200','AM4','AM5'],ram_clearance_mm=43,
    fan_size_mm=120,heatpipes=6,fan_count=2,fan_connector='4-pin PWM')

def monitor(slug, brand, model, inches, panel, resolution, hz, ports, url, price=None, **extra):
    f=dict(screen_size_in= inches,panel_type=panel,resolution=resolution,refresh_rate_hz=hz,video_ports=ports,**extra)
    if price: add(slug,'monitor','Màn hình '+brand+' '+model,brand,price,url,**f)
    else: enrich(slug,url,**f)

monitor('viewsonic-vx24g30-w','ViewSonic','VX24G30-W',23.8,'IPS','1920 × 1080',240,['HDMI × 2','DisplayPort × 1'],
    'https://www.viewsonic.com/global/products/sheet/VX24G30-W',brightness_nits=300,vesa='100 × 100 mm',response_time='0.8 ms MPRT')
rows['viewsonic-vx24g30-w']['notes'].append('Trang hãng có thông tin phiên bản HDMI không thống nhất giữa sơ đồ và bảng cổng; cần kiểm tra revision nếu cần HDMI tốc độ cụ thể.')
monitor('asus-tuf-vg259q5a','ASUS','TUF VG259Q5A',24.5,'Fast IPS','1920 × 1080',200,['HDMI 2.0 × 2','DisplayPort 1.4 × 1'],
    'https://www.asus.com/displays-desktops/monitors/tuf-gaming/tuf-gaming-vg259q5a/techspec/',brightness_nits=300,vesa='100 × 100 mm',response_time='1 ms GTG (tối thiểu 0.3 ms)',speakers='2 × 2 W')
monitor('msi-pro-mp251','MSI','PRO MP251 24.5 inch IPS 100Hz',24.5,'IPS','1920 × 1080',100,['HDMI 1.4b × 1','VGA × 1'],
    'https://storage-asset.msi.com/datasheet/pro-monitors/us/PRO-MP251.pdf',1790000,brightness_nits=300,vesa='100 × 100 mm',speakers='2 × 2 W',response_time='4 ms GTG / 1 ms MPRT')
monitor('msi-g274qpf-e2','MSI','G274QPF E2 27 inch QHD 180Hz',27,'Rapid IPS','2560 × 1440',180,['HDMI 2.0b × 2','DisplayPort 1.4a × 1','USB-C DP Alt Mode 15W × 1'],
    MSI+'Monitor/G274QPF-E2/Specification',5490000,brightness_nits=400,vesa='75 × 75 mm',response_time='1 ms GTG',height_adjustment_mm=130)
rows['msi-g274qpf-e2']['notes'].append('180 Hz qua DisplayPort/USB-C, tối đa 144 Hz qua HDMI ở QHD.')
monitor('aoc-24g4','AOC','24G4 23.8 inch Fast IPS 180Hz',23.8,'Fast IPS','1920 × 1080',180,['HDMI','DisplayPort'],
    'https://saas.aoc.com/product/24G4',2990000,response_time='0.5 ms MPRT')
monitor('lg-ultragear-27gp850-b','LG','UltraGear 27GP850-B 27 inch QHD',27,'Nano IPS','2560 × 1440',165,['HDMI','DisplayPort 1.4','USB hub'],
    'https://www.lg.com/us/monitors/lg-27gp850-b-gaming-monitor',6990000,response_time='1 ms GTG',overclock_hz=180)
rows['lg-ultragear-27gp850-b']['notes'].append('180 Hz là chế độ ép xung qua DisplayPort 1.4; mức mặc định 165 Hz.')
enrich('acer-kg251q-z1','https://www.acer.com/es-es/monitors/gaming/nitro-kg1/pdp/UM.KX1EE.101',
    screen_size_in=24.5,panel_type='VA',resolution='1920 × 1080',response_time='1 ms VRB',brightness_nits=250,
    video_ports=['HDMI 2.0 × 2','DisplayPort 1.4 × 1'])
enrich('dareu-ek87-pro-black','https://dareu.com/products/dareu-ek87pro-wireless-gaming-keyboard/',
    layout='TKL',key_count=87,connection=['USB-C','Bluetooth 5.1','2.4 GHz'],switch_type='DareU Dream Linear',
    hot_swap=True,keycaps='PBT',backlight='RGB',battery_mah=4000,dimensions='364 × 136 × 41 mm')
for slug,url in [('akko-5075b-plus-black-gold','https://en.akkogear.com/product/black-gold-5075b-plus-mechanical-keyboard/'),
    ('akko-5075b-plus-red-vietnam','https://en.akkogear.com/product/world-tour-vietnam-5075b-plus-mechanical-keyboard/')]:
    enrich(slug,url,layout='75%',connection=['USB-C','Bluetooth','2.4 GHz'],hot_swap=True,backlight='RGB',battery_mah=3000)
enrich('logitech-g304-wireless-black','https://support.logi.com/hc/en-au/articles/360023303474-G304-LIGHTSPEED-Wireless-Gaming-Mouse-Technical-Specifications',
    connection=['LIGHTSPEED 2.4 GHz (USB receiver)'],sensor='HERO',dpi_min=200,dpi_max=12000,weight_g=99,battery='1 × AA',dimensions='116.6 × 62.15 × 38.2 mm')
enrich('logitech-g502-hero','https://support.logi.com/hc/de-at/articles/360023303534-Specification-G502-HERO-Gaming-Mouse',
    connection=['USB có dây'],sensor='HERO 25K',dpi_min=100,dpi_max=25600,polling_rate_hz=1000,weight_g=121,backlight='LIGHTSYNC RGB')
enrich('logitech-g102-lightsync-black','https://www.logitechg.com/en-in/shop/p/g102-lightsync-rgb-gaming-mouse',
    connection=['USB có dây'],dpi_min=200,dpi_max=8000,weight_g=85,backlight='LIGHTSYNC RGB')

# Preserve every old import, including retailer snapshots and fixtures. Missing facts are explicit.
for p in old:
    r=rows[p['slug']]
    prior=(p.get('component') or {}).get('specs') or {}
    for key,value in prior.items():
        if key not in ['product_name','brand'] and value not in [None,'']:
            r['facts'].setdefault(key,value)
    if not r['facts']:
        for n,spec in enumerate(p.get('specs') or []): r['facts'][f'imported_spec_{n+1}']=spec['label']
    if not r['facts']: r['facts']['imported_description']=p['description']
    if not r['sources']:
        source=(p.get('provenance') or {}).get('source')
        if source: r['sources'].append(dict(**source,kind='retailer_snapshot',fields=[]))
        r['notes'].append('Dữ liệu nhập cũ: cần đối chiếu mã sản phẩm/phiên bản thực tế trước khi kết luận tương thích; không tự điền thông số chưa có nguồn.')
    if not p['storefront'] and p['category'] in ['cpu','ram','vga','storage']:
        r['expose']=True
        r['notes'].append('Giá và tồn kho giữ nguyên từ dữ liệu mẫu có sẵn.')
    if p['category'].startswith('pc-'):
        r['notes'].append('PC lắp sẵn: model nguồn, mainboard, RAM và vỏ có thể thay đổi theo đợt; không dùng bản mô tả rút gọn để xác nhận nâng cấp linh kiện.')
    r['status']='verified' if r['verifiedFields'] and not r['notes'] else 'needs_review'

for r in rows.values():
    if r['new']:
        r['status']='verified'
        r['notes'].append('Giá và tồn kho mẫu để thử chọn cấu hình; cần xác nhận giá bán và hàng thực tế.')
    # Explicit per-field verification: unknown legacy facts never inherit "verified" from a different field.
    r['facts']['verified_fields']=r['verifiedFields']

images=json.loads((ROOT/'src/main/resources/catalog/product-image-sources.json').read_text(encoding='utf-8'))
for slug,url in images.items():
    rows[slug]['image']='products/'+slug+'.webp'
    rows[slug]['imageSource']=url
old_images={p['slug']:p['image'] for p in old}
for r in rows.values():
    if r['expose'] and r['slug'] not in images and not old_images.get(r['slug']):
        # Storage keys are globally unique; a separate logical key can share the same SVG contents.
        r['image']='products/catalog-'+r['slug']+'.svg'
        target=ROOT/'frontend/src/assets'/r['image']
        template=ROOT/'frontend/src/assets/products'/('catalog-'+r['category']+'.svg')
        if not template.exists(): template=ROOT/'frontend/src/assets/products/catalog-product.svg'
        target.write_text(template.read_text(encoding='utf-8'),encoding='utf-8')

out=ROOT/'src/main/resources/catalog/build-catalog-2026-11.json'
out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(list(rows.values()),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(dict(total=len(rows),new=sum(r['new'] for r in rows.values()),storefront=sum(r['expose'] for r in rows.values())),ensure_ascii=False))
