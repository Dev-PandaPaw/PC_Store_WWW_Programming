import React, { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ChevronDown, Clock3, List } from 'lucide-react'
import { useCatalog } from '../contexts/catalogContextStore'
import { ProductCard } from '../components/gear/ProductCard'
import ArticleCard from '../components/ArticleCard'
import Breadcrumbs from '../components/Breadcrumbs'
import { articleCover, articleDate } from '../utils/journal'

export default function ArticlePage() {
  const { ARTICLES, PRODUCTS } = useCatalog()
  const { slug } = useParams()
  const [tocOpen, setTocOpen] = useState(() => window.matchMedia('(min-width: 901px)').matches)
  const article = ARTICLES.find((article) => article.slug === slug)
  useEffect(() => {
    const desktop = window.matchMedia('(min-width: 901px)')
    const update = () => setTocOpen(desktop.matches)
    update()
    desktop.addEventListener('change', update)
    return () => desktop.removeEventListener('change', update)
  }, [slug])
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
          Về góc kiến thức
        </Link>
      </div>
    )
  const cover = articleCover(article)
  const relatedArticles = ARTICLES.filter(item => item.slug !== article.slug)
    .sort((a, b) => Number(b.category === article.category) - Number(a.category === article.category))
    .slice(0, 3)
  const related = PRODUCTS.filter((product) =>
    article.relatedCategories.includes(product.category),
  ).slice(0, 4)
  return (
    <div className="article-page article-page--reading">
      <Breadcrumbs items={[{ label: 'Góc kiến thức', to: '/blog' }, { label: article.category, to: `/blog?category=${encodeURIComponent(article.category)}` }, { label: article.title }]} />
      <article>
        <header className="article-heading">
          <Link className="article-tag" to={`/blog?category=${encodeURIComponent(article.category)}`}>{article.category}</Link>
          <h1>{article.title}</h1>
          <p className="article-heading__excerpt">{article.excerpt}</p>
          <p className="article-meta">
            Biên tập PixelCore <span>·</span> <time dateTime={article.date}>{articleDate(article)}</time> <span>·</span>
            <Clock3 size={14} /> {article.readMinutes} phút đọc
          </p>
        </header>
        <figure className={`reading-cover${article.promotion ? ' reading-cover--promotion' : ''}`}>
          <img src={cover} alt="" width="1280" height="500" fetchPriority="high" />
          <figcaption>{article.promotion ? 'Thông tin từ banner trang chủ PixelCore.' : 'Ảnh minh họa: Unsplash'}</figcaption>
        </figure>
        <div className="article-layout">
          <details className="article-toc" open={tocOpen} onToggle={event => setTocOpen(event.currentTarget.open)}>
            <summary><List size={17} /><span>Mục lục</span><ChevronDown size={15} /></summary>
            <nav aria-label="Mục lục bài viết">
            {article.sections.map((section, index) => (
              <a key={section.heading} href={`#phan-${index + 1}`}>
                {section.heading}
              </a>
            ))}
            </nav>
          </details>
          <div className="article-body">
            <p className="article-intro">{article.intro}</p>
            <blockquote className="reading-takeaway">{article.takeaway}</blockquote>
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
            {article.action && <p className="reading-action"><Link className="text-link" to={article.action.to}>{article.action.label}</Link></p>}
            {article.sources?.length > 0 && <section className="reading-sources" aria-labelledby="reading-sources-heading">
              <h2 id="reading-sources-heading">Tài liệu tham khảo</h2>
              <ul>{article.sources.map(source => <li key={source.url}><a href={source.url} target="_blank" rel="noopener noreferrer">{source.name}</a></li>)}</ul>
            </section>}
            <Link className="text-link" to="/blog">Xem tất cả bài viết</Link>
          </div>
        </div>
      </article>
      <section className="reading-related" aria-labelledby="reading-related-heading">
        <h2 id="reading-related-heading">Có thể bạn muốn đọc</h2>
        <div className="journal-grid">{relatedArticles.map(item => <ArticleCard key={item.slug} article={item} />)}</div>
      </section>
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
