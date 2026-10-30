import React from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { useCatalog } from '../../contexts/CatalogContext'
import ArticleCard from '../ArticleCard'

export const NewsSection: React.FC = () => {
  const { ARTICLES } = useCatalog()
  return (
  <section className="store-section" id="goc-kien-thuc">
    <div className="store-section__heading">
      <div>
        <p className="section-eyebrow">
          HIỂU THÊM MỘT CHÚT. CHỌN ĐÚNG HƠN NHIỀU.
        </p>
        <h2>Góc kiến thức PC.</h2>
        <p className="section-description">
          Từ bộ máy đầu tiên đến lần nâng cấp tiếp theo.
        </p>
      </div>
      <Link className="text-link" to="/blog">
        Tất cả bài viết <ArrowRight size={17} />
      </Link>
    </div>
    <div className="article-grid">
      {ARTICLES.map((article) => (
        <ArticleCard key={article.slug} article={article} />
      ))}
    </div>
  </section>
)
}
