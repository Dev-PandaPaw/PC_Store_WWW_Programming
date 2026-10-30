import React from 'react'
import { Link } from 'react-router-dom'
import { ArrowUpRight, Clock3 } from 'lucide-react'
import { useCatalog } from '../contexts/CatalogContext'

export default function ArticleCard({ article, compact = false }) {
  const { productsInCategory } = useCatalog()
  const image = productsInCategory(article.imageCategory)[0]?.image
  return (
    <Link
      className={`article-card${compact ? ' article-card--compact' : ''}`}
      to={`/blog/${article.slug}`}
    >
      <div className="article-card__image">
        <span>{article.category}</span>
        <img src={image} alt="" loading="lazy" width="400" height="300" />
      </div>
      <div className="article-card__content">
        <p className="article-meta">
          <Clock3 size={13} /> {article.readMinutes} phút đọc <span>·</span>{' '}
          25/10/2026
        </p>
        <h3>{article.title}</h3>
        {!compact && <p>{article.excerpt}</p>}
        <span className="article-card__read">
          Đọc bài viết <ArrowUpRight size={16} />
        </span>
      </div>
    </Link>
  )
}
