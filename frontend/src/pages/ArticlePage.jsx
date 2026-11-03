import React, { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Clock3 } from 'lucide-react'
import { useCatalog } from '../contexts/CatalogContext'
import { ProductCard } from '../components/gear/ProductCard'

export default function ArticlePage() {
  const { ARTICLES, PRODUCTS, productsInCategory } = useCatalog()
  const { slug } = useParams()
  const article = ARTICLES.find((article) => article.slug === slug)
  useEffect(() => {
    document.title = article
      ? `${article.title} | PixelCore`
      : 'Không tìm thấy bài viết | PixelCore'
    return () => {
      document.title = 'PixelCore'
    }
  }, [article])
  if (!article)
    return (
      <div className="store-empty">
        <h1>Không tìm thấy bài viết</h1>
        <Link className="store-button" to="/blog">
          <ArrowLeft size={16} /> Về góc kiến thức
        </Link>
      </div>
    )
  const cover = productsInCategory(article.imageCategory)[0]?.image
  const related = PRODUCTS.filter((product) =>
    article.relatedCategories.includes(product.category),
  ).slice(0, 4)
  return (
    <div className="article-page">
      <nav className="breadcrumbs" aria-label="Đường dẫn">
        <Link to="/">Trang chủ</Link>
        <span>/</span>
        <Link to="/blog">Góc kiến thức</Link>
        <span>/</span>
        <span>{article.category}</span>
      </nav>
      <article>
        <header className="article-heading">
          <span className="article-tag">{article.category}</span>
          <h1>{article.title}</h1>
          <p className="article-heading__excerpt">{article.excerpt}</p>
          <p className="article-meta">
            Đội ngũ PixelCore <span>·</span> 25/10/2026 <span>·</span>
            <Clock3 size={14} /> {article.readMinutes} phút đọc
          </p>
        </header>
        <div className="article-hero">
          <div>
            <span>PIXELCORE JOURNAL</span>
            <h2>{article.takeaway}</h2>
          </div>
          <img src={cover} alt="" width="500" height="400" />
        </div>
        <div className="article-layout">
          <aside className="article-toc">
            <h2>Trong bài viết</h2>
            {article.sections.map((section, index) => (
              <a key={section.heading} href={`#phan-${index + 1}`}>
                <span>0{index + 1}</span>
                {section.heading}
              </a>
            ))}
          </aside>
          <div className="article-body">
            <p className="article-intro">{article.intro}</p>
            {article.sections.map((section, index) => (
              <section key={section.heading} id={`phan-${index + 1}`}>
                <h2>{section.heading}</h2>
                {section.paragraphs.map((paragraph) => (
                  <p key={paragraph}>{paragraph}</p>
                ))}
                {section.bullets && (
                  <div className="article-checklist">
                    <h3>Danh sách kiểm tra</h3>
                    <ul>
                      {section.bullets.map((item) => (
                        <li key={item}>{item}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </section>
            ))}
            <Link className="text-link" to="/blog">
              <ArrowLeft size={17} /> Xem các bài viết khác
            </Link>
          </div>
        </div>
      </article>
      <section className="store-section">
        <div className="store-section__heading">
          <div>
            <p className="section-eyebrow">KHÁM PHÁ THÊM</p>
            <h2>Sản phẩm liên quan.</h2>
          </div>
        </div>
        <div className="product-grid">
          {related.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </section>
    </div>
  )
}
