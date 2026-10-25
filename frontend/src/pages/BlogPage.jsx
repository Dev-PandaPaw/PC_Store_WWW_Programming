import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { useCatalog } from '../contexts/CatalogContext'
import ArticleCard from '../components/ArticleCard'

export default function BlogPage() {
  const { ARTICLES } = useCatalog()
  const [category, setCategory] = useState('Tất cả')
  const categories = [
    'Tất cả',
    ...new Set(ARTICLES.map((article) => article.category)),
  ]
  const articles = ARTICLES.filter(
    (article) => category === 'Tất cả' || article.category === category,
  )
  return (
    <div className="blog-page">
      <nav className="breadcrumbs" aria-label="Đường dẫn">
        <Link to="/">Trang chủ</Link>
        <span>/</span>
        <span>Góc kiến thức</span>
      </nav>
      <div className="page-heading">
        <p className="section-eyebrow">PIXELCORE JOURNAL</p>
        <h1>Hiểu chiếc PC của bạn.</h1>
        <p>
          Những hướng dẫn dễ áp dụng cho việc chọn linh kiện, nâng cấp và hoàn
          thiện góc máy.
        </p>
      </div>
      <div className="category-tabs">
        {categories.map((item) => (
          <button
            key={item}
            aria-pressed={category === item}
            onClick={() => setCategory(item)}
          >
            {item}
          </button>
        ))}
      </div>
      <div className="article-grid article-grid--blog">
        {articles.map((article) => (
          <ArticleCard key={article.slug} article={article} />
        ))}
      </div>
    </div>
  )
}
