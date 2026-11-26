import React, { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Clock3, Search, X, ChevronDown } from 'lucide-react'
import { useCatalog } from '../contexts/catalogContextStore'
import ArticleCard from '../components/ArticleCard'
import Breadcrumbs from '../components/Breadcrumbs'
import { articleCover, articleDate, normalizeJournalSearch } from '../utils/journal'

export default function BlogPage() {
  const { ARTICLES } = useCatalog()
  const [params, setParams] = useSearchParams()
  const category = params.get('category') || ''
  const query = params.get('q') || ''
  const sort = params.get('sort') || 'latest'
  const [visible, setVisible] = useState(6)
  useEffect(() => { document.title = 'Góc kiến thức PC | PixelCore'; return () => { document.title = 'PixelCore' } }, [])
  useEffect(() => { setVisible(6) }, [category, query, sort])
  const categories = [...new Set(ARTICLES.map(article => article.category))]
  const featured = ARTICLES.find(article => article.slug === 'chon-cpu-va-gpu-cho-pc-gaming') || ARTICLES[0]
  const picks = ['chon-man-hinh-theo-nhu-cau', 'chon-ram-va-ssd-khi-nang-cap-pc'].map(slug => ARTICLES.find(article => article.slug === slug)).filter(Boolean)
  const articles = useMemo(() => ARTICLES.filter(article => (!category || article.category === category) &&
    normalizeJournalSearch(`${article.title} ${article.excerpt} ${article.category}`).includes(normalizeJournalSearch(query.trim())))
    .sort((a, b) => sort === 'shortest' ? a.readMinutes - b.readMinutes || b.date.localeCompare(a.date) : b.date.localeCompare(a.date)), [ARTICLES, category, query, sort])
  const update = (key, value) => setParams(previous => {
    const next = new URLSearchParams(previous)
    if (value) next.set(key, value); else next.delete(key)
    return next
  }, { replace: true })

  return (
    <div className="blog-page journal">
      <header className="journal-header">
        <Breadcrumbs className="journal-breadcrumbs" items={category ? [{ label: 'Góc kiến thức', to: '/blog' }, { label: category }] : [{ label: 'Góc kiến thức' }]} />
        <nav className="journal-topics" aria-label="Danh mục bài viết">
          {['', ...categories].map(item => <button type="button" key={item} aria-pressed={category === item} onClick={() => update('category', item)}>{item || 'Tất cả bài viết'}</button>)}
        </nav>
      </header>
      {!category && !query && featured && (
        <section className="journal-featured" aria-label="Bài viết nổi bật">
          <Link className="journal-lead" to={`/blog/${featured.slug}`}>
            <img src={articleCover(featured)} alt="" width="900" height="600" fetchPriority="high" />
            <div className="journal-lead__content"><span className="journal-label">BÀI VIẾT NỔI BẬT</span>
              <p className="journal-lead__category">{featured.category}</p><h2>{featured.title}</h2><p>{featured.excerpt}</p>
              <span className="journal-lead__meta"><Clock3 size={15} />{featured.readMinutes} phút đọc <span>·</span> Biên tập PixelCore</span>
            </div>
          </Link>
          <aside className="journal-picks"><p className="journal-eyebrow">ĐÁNG ĐỌC</p>
            {picks.map(article => <Link className="journal-pick" key={article.slug} to={`/blog/${article.slug}`}>
              <img src={articleCover(article)} alt="" width="380" height="180" />
              <div><span>{article.category}</span><h3>{article.title}</h3><p><Clock3 size={13} />{article.readMinutes} phút đọc <span>·</span>{articleDate(article)}</p></div>
            </Link>)}
          </aside>
        </section>
      )}
      <section className="journal-library" aria-labelledby="journal-library-heading">
        <div className="journal-toolbar">
          <div><h1 id="journal-library-heading">{category || (query ? 'Kết quả tìm kiếm' : 'Bài viết mới nhất')}</h1><p role="status" aria-live="polite">{articles.length} bài viết{query ? ` cho “${query}”` : ' dành cho góc máy của bạn'}</p></div>
          <div className="journal-tools"><label className="journal-search"><Search size={18} /><input type="search" aria-label="Tìm bài viết" placeholder="Tìm bài viết, kiến thức…" value={query} maxLength={150} onChange={event => update('q', event.target.value)} /></label>
            <label className="journal-sort"><select aria-label="Sắp xếp bài viết" value={sort} onChange={event => update('sort', event.target.value)}><option value="latest">Mới nhất</option><option value="shortest">Đọc nhanh nhất</option></select><ChevronDown size={15} /></label>
          </div>
        </div>
        {articles.length ? <div className="journal-grid">{articles.slice(0, visible).map(article => <ArticleCard key={article.slug} article={article} />)}</div>
          : <div className="journal-empty"><Search size={32} /><h3>Chưa tìm thấy bài viết</h3><p>Thử từ khóa ngắn hơn hoặc khám phá một chủ đề khác.</p><button type="button" onClick={() => setParams({})}><X size={16} />Xóa bộ lọc</button></div>}
        {visible < articles.length && <div className="journal-more"><p>Đang hiển thị {Math.min(visible, articles.length)} / {articles.length} bài viết</p><button type="button" onClick={() => setVisible(count => count + 6)}>Xem thêm bài viết<ChevronDown size={16} /></button></div>}
      </section>
    </div>
  )
}
