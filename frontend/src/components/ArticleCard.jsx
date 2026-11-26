import React from 'react'
import { Link } from 'react-router-dom'
import { Clock3 } from 'lucide-react'
import { articleCover, articleDate } from '../utils/journal'

export default function ArticleCard({ article, compact = false }) {
  return (
    <Link className={`article-card${compact ? ' article-card--compact' : ''}`} to={`/blog/${article.slug}`}>
      <div className={`article-card__image${article.promotion ? ' article-card__image--promotion' : ''}`}>
        <img src={articleCover(article)} alt="" loading="lazy" width="660" height="440" />
        <span>{article.category}</span>
      </div>
      <div className="article-card__content">
        <p className="article-meta"><time dateTime={article.date}>{articleDate(article)}</time><span aria-hidden="true">·</span><Clock3 size={14} />{article.readMinutes} phút đọc</p>
        <h3>{article.title}</h3>
        {!compact && <p>{article.excerpt}</p>}
        <span className="article-card__read">Đọc bài viết</span>
      </div>
    </Link>
  )
}
