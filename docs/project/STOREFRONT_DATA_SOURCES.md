# PixelCore storefront catalog

The live storefront reads Supabase PostgreSQL through `GET /api/catalog/storefront`.
The API reads `products`, `categories`, `product_images`, `storefront_products`,
and `storefront_articles` on each request. `CatalogContext` shares this response
across catalog, home, advisory, product detail and article pages. There is no
runtime JSON fallback: a failed request displays an error and a retry button.

Flyway migration `V7__import_storefront_catalog_and_articles.sql` imports the 34
real product snapshots and four original articles. It preserves existing products,
orders and inventory. New product inventory starts at zero because retailer stock
quantities are unknown; the store's staff must enter their own stock quantities.
Unverified warranty durations are imported as zero and are not advertised in the UI.
The nine legacy demo products remain in `products` for compatibility but do not
appear in the imported storefront selection. Prices come from `products.price`;
specifications, comparison prices and source metadata are in `storefront_products`.

The two JSON files in `frontend/src/data/` and `articles.ts` are source snapshots
for maintaining the migration, not the live data source. Existing catalog and
authentication endpoints are unchanged.

## Product provenance

- Product name, model, price, specifications, and product photo were checked on the
  linked GearVN product detail page on **25 October 2026 (Asia/Saigon)**.
- Every record keeps `source.name`, `source.url`, `source.checkedAt`, and
  `sourceImageUrl` for maintenance and coursework verification. These references
  are not rendered in the storefront; product details are presented within PC Store.
- Product photos are stored under `frontend/src/assets/products/`; no generic
  stock photographs are used as product photos.
- `originalPrice` equals `price` when no comparison price was available. Displayed
  discount percentages are calculated from these two recorded prices.
- Optional `sourceAvailability` records the source website's inventory statement.
  It is not used to claim that PC Store has stock.
- Product descriptions and category grouping are editorial. In particular, the
  PC GVN i7 configuration is grouped for work/graphics browsing without claiming
  professional workstation certification or a benchmark result.

There are 4 complete PCs, 2 CPUs, 3 graphics cards, 2 motherboards, 2 RAM kits,
3 SSDs, and 3 products each in mouse, keyboard, monitor, cooling, case, and PSU.

## Design references

- https://gearvn.com/collections/pc-gvn — category hierarchy, product cards, and
  shopping navigation; PC Store retains its navy/cyan branding and original hero.
- https://phongvu.vn/c/pc-gaming — filtering by configuration and budget.
- https://phongvu.vn/c/may-tinh-de-ban — requested reference, but the fetch was
  blocked with HTTP 403; no unseen product details were taken from this page.

Retailer-specific hotlines, showroom addresses, certification badges, countdown
promotions, and unverified free gifts are not copied into PC Store.

## Articles

Four original Vietnamese guide snapshots are stored in `frontend/src/data/articles.ts`
and imported into Supabase `storefront_articles` by V7.
Each has a `/blog/:slug` page, table of contents, and related products. Manufacturer
references are retained in the data for maintenance rather than displayed as
external links in the reading experience. References include Intel, Noctua,
Kingston, Logitech G, and Keychron. Articles are editorial guides, not copied
retailer blog posts.

## Updating and checking

1. Reopen the exact source product page and compare model, specifications, image,
   current price, and comparison price.
2. Update the JSON reference record and its snapshot date; retain source attribution.
   Add a new Flyway migration updating the corresponding database rows. Do not
   edit an already applied migration.
3. Download the corresponding product image if needed, preserving its file type.
4. Run `npm run test:catalog` and `npm run build` from `frontend`.
5. Start Spring Boot from the project root with the configured `.env` database
   connection so Flyway applies pending migrations. Run the saved collection with
   `postman collection run postman/collections/PixelCore` against port 8080.

All product photos, hero media, logos and promotion banners live under
`frontend/src/assets/`. Vite imports the product folder with `import.meta.glob`,
so both development and production builds include every photo. Database image
keys such as `products/logitech-g304-wireless-black.png` are portable; they never
contain a Windows path, a Downloads path, or a build-specific hashed URL. Clone
the repository, run `npm ci` in `frontend`, then `npm run dev` with the backend
running on port 8080. No manual copy from Downloads is required.

The catalog tests verify category coverage, unique IDs/slugs, provenance, valid
prices, and local image file signatures. They do not establish live inventory or
guarantee that a retailer's price will remain unchanged.
