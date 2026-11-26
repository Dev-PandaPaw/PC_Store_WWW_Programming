import React, { useEffect, useRef, useState } from 'react'
import { Copy, RefreshCw, Sparkles, X } from 'lucide-react'
import { getVectorDocuments, syncVectorDatabase } from '../services/vectorSync'
import '../styles/admin-vectors.css'

const PAGE_SIZE = 10
const storageLabels = { memory: 'RAM', pgvector: 'PgVector' }

function errorMessage(error, fallback) {
  return error?.response?.data?.error?.message || error?.response?.data?.detail || fallback
}

function VectorDocumentDialog({ document, onClose }) {
  const dialog = useRef(null)
  const [copyNotice, setCopyNotice] = useState('')
  const embedding = JSON.stringify(document.embedding)

  useEffect(() => {
    if (!dialog.current.open) dialog.current.showModal()
  }, [])

  async function copyEmbedding() {
    try {
      await navigator.clipboard.writeText(embedding)
      setCopyNotice('Đã sao chép embedding.')
    } catch {
      setCopyNotice('Hãy bôi đen mảng embedding để sao chép thủ công.')
    }
  }

  return (
    <dialog ref={dialog} className="admin-list__dialog admin-vectors__dialog" aria-labelledby="vector-document-title" onClose={onClose}>
      <header className="admin-list__dialog-heading">
        <div>
          <h2 id="vector-document-title">Chi tiết tài liệu vector</h2>
          <code className="admin-list__subline admin-vectors__id">{document.id}</code>
        </div>
        <button
          type="button" className="admin-list__button admin-list__button--small" aria-label="Đóng chi tiết tài liệu"
          onClick={() => dialog.current.close()}
        >
          <X size={17} />
        </button>
      </header>

      <div className="admin-vectors__details">
        <section>
          <h3>Nội dung đã lập chỉ mục</h3>
          <pre>{document.text}</pre>
        </section>
        <section>
          <h3>Metadata</h3>
          <pre>{JSON.stringify(document.metadata, null, 2)}</pre>
        </section>
        <section>
          <div className="admin-vectors__section-heading">
            <h3>Embedding <span>{document.dimensions} chiều</span></h3>
            <button type="button" className="admin-list__button admin-list__button--small" onClick={copyEmbedding}>
              <Copy size={14} /> Sao chép
            </button>
          </div>
          {copyNotice && <p className="admin-list__subline" role="status">{copyNotice}</p>}
          <pre className="admin-vectors__embedding">{embedding}</pre>
        </section>
      </div>
    </dialog>
  )
}

export default function AdminVectorsPage() {
  const [offset, setOffset] = useState(0)
  const [revision, setRevision] = useState(0)
  const [page, setPage] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [syncing, setSyncing] = useState(false)
  const [syncNotice, setSyncNotice] = useState(null)
  const [selected, setSelected] = useState(null)

  useEffect(() => {
    const controller = new AbortController()
    const { signal } = controller
    setLoading(true)
    setLoadError('')
    getVectorDocuments({ offset, limit: PAGE_SIZE, signal })
      .then(result => {
        if (signal.aborted) return
        const lastOffset = Math.floor(Math.max(0, result.total - 1) / PAGE_SIZE) * PAGE_SIZE
        if (offset > lastOffset) setOffset(lastOffset)
        else setPage(result)
      })
      .catch(error => {
        if (!signal.aborted) setLoadError(errorMessage(error, 'Không tải được chỉ mục vector. Vui lòng thử lại.'))
      })
      .finally(() => {
        if (!signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [offset, revision])

  async function sync() {
    setSyncing(true)
    setSyncNotice(null)
    try {
      const { productCount, documentCount } = await syncVectorDatabase()
      setSyncNotice({ text: `Đã đồng bộ ${productCount} sản phẩm (${documentCount} tài liệu vector).` })
      setOffset(0)
      setRevision(value => value + 1)
    } catch (error) {
      setSyncNotice({ error: true, text: errorMessage(error, 'Chưa xác nhận được kết quả đồng bộ. Hãy tải lại danh sách để kiểm tra.') })
    } finally {
      setSyncing(false)
    }
  }

  const documents = page?.documents || []
  const totalPages = Math.ceil((page?.total || 0) / PAGE_SIZE)
  const busy = loading || syncing

  return (
    <div className="admin-list">
      <header className="admin-list__heading">
        <div>
          <h1>Quản trị Vector AI</h1>
          {page && !loadError && (
            <div className="admin-vectors__summary">
              <span>{page.total.toLocaleString('vi-VN')} tài liệu đã lập chỉ mục</span>
              <span className="admin-list__pill admin-list__pill--progress">{storageLabels[page.storage] || page.storage}</span>
            </div>
          )}
        </div>
        <div className="admin-list__actions">
          <button type="button" className="admin-list__button" disabled={busy} onClick={() => setRevision(value => value + 1)}>
            <RefreshCw size={15} /> Tải lại
          </button>
          <button type="button" className="admin-list__button admin-list__button--primary" disabled={busy} onClick={sync}>
            <Sparkles size={16} /> {syncing ? 'Đang đồng bộ…' : 'Đồng bộ Vector AI'}
          </button>
        </div>
      </header>

      {syncNotice && (
        <p className={`admin-list__notice${syncNotice.error ? ' admin-list__notice--error' : ''}`} role={syncNotice.error ? 'alert' : 'status'}>
          {syncNotice.text}
        </p>
      )}
      <div className="admin-list__table-wrap">
        <table className="admin-list__table" aria-label="Tài liệu vector" aria-busy={loading}>
          <thead><tr>
            <th scope="col">Nội dung đã lập chỉ mục</th>
            <th scope="col">Mã sản phẩm</th>
            <th scope="col">Số chiều</th>
            <th scope="col" className="admin-list__end">Thao tác</th>
          </tr></thead>
          <tbody>
            {loading || loadError || documents.length === 0 ? (
              <tr><td colSpan={4} className="admin-list__empty">
                <span role={loadError ? 'alert' : 'status'}>
                  {loading ? 'Đang tải tài liệu vector…' : loadError || 'Chưa có tài liệu vector. Bấm Đồng bộ Vector AI để lập chỉ mục.'}
                </span>
              </td></tr>
            ) : documents.map(document => (
              <tr key={document.id}>
                <td><span className="admin-vectors__preview">{document.text || '—'}</span></td>
                <td><code className="admin-vectors__id">{document.metadata.productId || '—'}</code></td>
                <td>{document.dimensions}</td>
                <td className="admin-list__end">
                  <button type="button" className="admin-list__button admin-list__button--small" onClick={() => setSelected(document)}>
                    Chi tiết
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {totalPages > 1 && !loadError && (
        <nav className="admin-list__pagination" aria-label="Phân trang tài liệu vector">
          <span>Trang {offset / PAGE_SIZE + 1} / {totalPages}</span>
          <div className="admin-list__actions">
            <button type="button" className="admin-list__button admin-list__button--small" disabled={busy || offset === 0} onClick={() => setOffset(offset - PAGE_SIZE)}>Trang trước</button>
            <button type="button" className="admin-list__button admin-list__button--small" disabled={busy || offset + PAGE_SIZE >= page.total} onClick={() => setOffset(offset + PAGE_SIZE)}>Trang sau</button>
          </div>
        </nav>
      )}

      {selected && <VectorDocumentDialog document={selected} onClose={() => setSelected(null)} />}
    </div>
  )
}
