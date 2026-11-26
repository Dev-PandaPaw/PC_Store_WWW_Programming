"""Download reviewed product photos and render a NEW image-only Flyway migration.

Usage: python scripts/sync-product-photos.py --output src/main/resources/db/migration/V23__catalog_product_photos.sql
Existing migrations are never overwritten. Downloads must have a raster signature and
match their reviewed checksum; the browser verification checks that they decode.
"""
import argparse
import hashlib
import json
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / 'src/main/resources/catalog/product-photo-catalog.json'


def quote(value):
    return "'" + str(value).replace("'", "''") + "'"


def image_type(data):
    if data.startswith(b'\x89PNG\r\n\x1a\n'):
        return 'png', 'image/png'
    if data.startswith(b'\xff\xd8\xff'):
        return 'jpg', 'image/jpeg'
    if data[:4] == b'RIFF' and data[8:12] == b'WEBP':
        return 'webp', 'image/webp'
    raise ValueError('Source returned something other than a PNG, JPEG or WebP photo')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    output = ROOT / args.output
    if output.exists():
        raise SystemExit('Choose a new migration version; existing migrations cannot be overwritten.')
    rows = json.loads(MANIFEST.read_text(encoding='utf-8'))
    assert len({r['slug'] for r in rows}) == len(rows), 'Duplicate product slugs'
    values = []
    for row in rows:
        path = ROOT / 'frontend/src/assets' / row['storageKey']
        if not path.exists():
            request = urllib.request.Request(row['imageUrl'], headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(request, timeout=45) as response:
                data = response.read(8 * 1024 * 1024 + 1)
            if len(data) > 8 * 1024 * 1024:
                raise ValueError('Photo exceeds 8 MB: ' + row['slug'])
            image_type(data)
            if hashlib.sha256(data).hexdigest() != row['sha256']:
                raise ValueError('Source photo changed; review it before downloading: ' + row['slug'])
            path.write_bytes(data)
        data = path.read_bytes()
        extension, mime = image_type(data)
        if path.suffix != '.' + extension or hashlib.sha256(data).hexdigest() != row['sha256']:
            raise ValueError('Invalid local photo: ' + row['slug'])
        values.append('(' + ', '.join(map(quote, [row['slug'], row['storageKey'], mime,
                      len(data), row['imageUrl'], row['sourcePage']])) + ')')
    sql = '''-- Replace only the catalog placeholders introduced in V21/V22.
-- Reviewed sources and checksums: catalog/product-photo-catalog.json.
-- No product names, prices, inventory, technical facts or existing real photos change.
CREATE TEMP TABLE reviewed_product_photos (
    slug TEXT PRIMARY KEY, storage_key TEXT UNIQUE, mime_type TEXT,
    size_bytes BIGINT, source_url TEXT, source_page TEXT
) ON COMMIT DROP;
INSERT INTO reviewed_product_photos VALUES
''' + ',\n'.join(values) + ''';

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
'''
    output.write_text(sql, encoding='utf-8')
    print(f'Validated {len(rows)} local photos; wrote {output.name}.')


if __name__ == '__main__':
    main()
