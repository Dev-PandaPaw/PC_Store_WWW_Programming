import React, { useEffect, useMemo, useState } from 'react'
import { Check, LoaderCircle, Plus, RefreshCw, Sparkles, X } from 'lucide-react'
import http from '../services/http'
import { syncVectorDatabase } from '../services/vectorSync'
import '../styles/admin-products.css'

const emptyForm = {
  categoryId: '', sku: '', slug: '', name: '', brand: '', description: '',
  price: '', stockOnHand: '0', warrantyMonths: '12',
  spec: {
    cpu: '', gpu: '', ramGb: '', storageGb: '', storageType: '',
    motherboard: '', psu: '', caseName: '', operatingSystem: '',
  },
}

const money = value => Number(value || 0).toLocaleString('vi-VN', {
  style: 'currency', currency: 'VND', maximumFractionDigits: 0,
})

const slugify = value => value.toLowerCase().trim()
  .normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd')
  .replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '')

function getErrorMessage(error) {
  const data = error?.response?.data
  const message = data?.error?.message || data?.detail || data?.message
  if (message) return message
  if (error?.response?.status === 403) return 'Yêu cầu bị từ chối (403). Hãy tải lại trang quản trị và thử lại.'
  if (error?.response?.status === 401) return 'Chưa xác thực được tài khoản quản trị. Hãy tải lại trang và thử lại.'
  if (error?.response?.status === 404) return 'Không tìm thấy tài nguyên trên máy chủ.'
  if (error?.code === 'ECONNABORTED' || error?.code === 'ETIMEDOUT') return 'Đã hết thời gian chờ phản hồi. Hãy kiểm tra kết quả trước khi thử lại.'
  return error?.message || 'Đã xảy ra lỗi. Vui lòng thử lại.'
}

export default function AdminProductsPage() {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [categoryLoadError, setCategoryLoadError] = useState('')
  const [notice, setNotice] = useState(null)
  const [search, setSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('ALL')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [sortOrder, setSortOrder] = useState('updated')
  const [editorOpen, setEditorOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)
  const [changingStatus, setChangingStatus] = useState('')
  const [syncingVector, setSyncingVector] = useState(false)

  const loadData = async () => {
    setLoading(true)
    setLoadError('')
    try {
      const productRequest = http.get('/admin/catalog/products')
        .then(({ data }) => setProducts(data))
        .catch(error => setLoadError(getErrorMessage(error)))
      const categoryRequest = http.get('/catalog/categories')
        .then(({ data }) => {
          if (!Array.isArray(data)) throw new Error('Dữ liệu danh mục không hợp lệ.')
          setCategories(data)
          setCategoryLoadError(data.length ? '' : 'Chưa có danh mục trong hệ thống.')
        })
        .catch(error => setCategoryLoadError(getErrorMessage(error)))
      await Promise.all([productRequest, categoryRequest])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [])

  const stats = useMemo(() => ({
    total: products.length,
    active: products.filter(product => product.status === 'ACTIVE').length,
    inactive: products.filter(product => product.status !== 'ACTIVE').length,
    lowStock: products.filter(product => Number(product.stockOnHand || 0) - Number(product.reservedQuantity || 0) <= 5).length,
  }), [products])

  const visibleProducts = useMemo(() => {
    const query = search.trim().toLocaleLowerCase('vi')
    const filtered = products.filter(product => {
      const matchesSearch = !query || [product.name, product.sku, product.brand, product.slug]
        .some(value => value?.toLocaleLowerCase('vi').includes(query))
      const matchesCategory = categoryFilter === 'ALL' || product.category?.id === categoryFilter
      const matchesStatus = statusFilter === 'ALL' || product.status === statusFilter
      return matchesSearch && matchesCategory && matchesStatus
    })

    return filtered.sort((a, b) => {
      if (sortOrder === 'name') return a.name.localeCompare(b.name, 'vi')
      if (sortOrder === 'priceAsc') return Number(a.price) - Number(b.price)
      if (sortOrder === 'priceDesc') return Number(b.price) - Number(a.price)
      return new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt)
    })
  }, [products, search, categoryFilter, statusFilter, sortOrder])

  const openCreate = () => {
    setEditingProduct(null)
    setForm({ ...emptyForm, categoryId: categories[0]?.id || '' })
    setEditorOpen(true)
  }

  const openEdit = product => {
    setEditingProduct(product)
    setForm({
      categoryId: product.category?.id || '',
      sku: product.sku || '',
      slug: product.slug || '',
      name: product.name || '',
      brand: product.brand || '',
      description: product.description || '',
      price: String(product.price ?? ''),
      stockOnHand: String(product.stockOnHand ?? 0),
      warrantyMonths: String(product.warrantyMonths ?? 12),
      spec: {
        cpu: product.spec?.cpu || '', gpu: product.spec?.gpu || '',
        ramGb: product.spec?.ramGb ?? '', storageGb: product.spec?.storageGb ?? '',
        storageType: product.spec?.storageType || '', motherboard: product.spec?.motherboard || '',
        psu: product.spec?.psu || '', caseName: product.spec?.caseName || '',
        operatingSystem: product.spec?.operatingSystem || '',
      },
    })
    setEditorOpen(true)
  }

  const updateField = (field, value) => setForm(current => ({ ...current, [field]: value }))
  const updateSpec = (field, value) => setForm(current => ({
    ...current, spec: { ...current.spec, [field]: value },
  }))

  const saveProduct = async event => {
    event.preventDefault()
    setSaving(true)
    setNotice(null)
    const request = {
      ...form,
      price: Number(form.price),
      stockOnHand: Number(form.stockOnHand),
      warrantyMonths: form.warrantyMonths === '' ? null : Number(form.warrantyMonths),
      spec: Object.values(form.spec).some(value => value !== '' && value != null)
        ? Object.fromEntries(Object.entries(form.spec).map(([key, value]) => [
          key, ['ramGb', 'storageGb'].includes(key) ? (value === '' ? null : Number(value)) : value,
        ]))
        : null,
    }
    try {
      if (editingProduct) {
        await http.put(`/admin/catalog/products/${editingProduct.id}`, request)
        setNotice({ type: 'success', text: 'Đã cập nhật thông tin sản phẩm.' })
      } else {
        await http.post('/admin/catalog/products', request)
        setNotice({ type: 'success', text: 'Đã thêm sản phẩm mới.' })
      }
      setEditorOpen(false)
      await loadData()
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    } finally {
      setSaving(false)
    }
  }

  const toggleStatus = async product => {
    const nextStatus = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    setChangingStatus(product.id)
    setNotice(null)
    try {
      await http.patch(`/admin/catalog/products/${product.id}/status`, { status: nextStatus })
      setProducts(current => current.map(item => item.id === product.id ? { ...item, status: nextStatus } : item))
      setNotice({ type: 'success', text: nextStatus === 'ACTIVE' ? 'Sản phẩm đã được kích hoạt.' : 'Sản phẩm đã được ẩn khỏi cửa hàng.' })
    } catch (error) {
      setNotice({ type: 'error', text: getErrorMessage(error) })
    } finally {
      setChangingStatus('')
    }
  }

  const handleSyncVector = async () => {
    setSyncingVector(true)
    setNotice(null)
    try {
      const { productCount, documentCount } = await syncVectorDatabase()
      setNotice({
        type: 'success',
        text: `Đã đồng bộ ${productCount} sản phẩm (${documentCount} tài liệu vector).`,
      })
    } catch (error) {
      setNotice({ type: 'error', text: `Lỗi đồng bộ Vector AI: ${getErrorMessage(error)}` })
    } finally {
      setSyncingVector(false)
    }
  }

  return (
    <div className="admin-list admin-products">
      <header className="admin-list__heading">
        <div>
          <h1>Quản lý sản phẩm</h1>
          {!loading && !loadError && (
            <p>Tổng cộng {stats.total} sản phẩm · {stats.active} đang bán · {stats.inactive} đã ẩn hoặc ngừng bán · {stats.lowStock} tồn kho thấp</p>
          )}
        </div>
        <div className="admin-list__actions">
          <button
            type="button"
            className="admin-list__button"
            onClick={handleSyncVector}
            disabled={syncingVector || loading}
            title="Băm thông số linh kiện và nạp vào Vector Store (Spring AI)"
          >
            {syncingVector ? (
              <>
                <LoaderCircle className="admin-products__spin" size={16} />
                <span>Đang đồng bộ...</span>
              </>
            ) : (
              <>
                <Sparkles size={16} />
                <span>Đồng bộ Vector AI</span>
              </>
            )}
          </button>
          <button type="button" className="admin-list__button admin-list__button--primary" onClick={openCreate}>
            <Plus size={16} /> Thêm sản phẩm
          </button>
        </div>
      </header>

      {notice && (
        <div className={`admin-list__notice${notice.type === 'error' ? ' admin-list__notice--error' : ''}`} role={notice.type === 'error' ? 'alert' : 'status'}>
          <span>{notice.text}</span>
          <button type="button" aria-label="Đóng thông báo" onClick={() => setNotice(null)}><X size={16} /></button>
        </div>
      )}

      <div className="admin-list__toolbar">
        <div className="admin-list__actions" role="group" aria-label="Lọc và sắp xếp sản phẩm">
          <input
            className="admin-list__control" type="search" aria-label="Tìm sản phẩm"
            placeholder="Tìm tên, SKU, thương hiệu..." value={search}
            onChange={event => setSearch(event.target.value)}
          />
          <select className="admin-list__control" aria-label="Danh mục" value={categoryFilter} onChange={event => setCategoryFilter(event.target.value)}>
            <option value="ALL">Tất cả danh mục</option>
            {categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}
          </select>
          <select className="admin-list__control" aria-label="Trạng thái sản phẩm" value={statusFilter} onChange={event => setStatusFilter(event.target.value)}>
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang kinh doanh</option>
            <option value="INACTIVE">Đang ẩn</option>
            <option value="DISCONTINUED">Ngừng kinh doanh</option>
          </select>
          <select className="admin-list__control" aria-label="Sắp xếp sản phẩm" value={sortOrder} onChange={event => setSortOrder(event.target.value)}>
            <option value="updated">Mới cập nhật</option>
            <option value="name">Tên A–Z</option>
            <option value="priceAsc">Giá thấp đến cao</option>
            <option value="priceDesc">Giá cao đến thấp</option>
          </select>
        </div>
        <button type="button" className="admin-list__button" onClick={loadData} disabled={loading}>
          <RefreshCw size={15} /> Tải lại
        </button>
      </div>

      {loadError && (
        <p className="admin-list__notice admin-list__notice--error" role="alert">{loadError}</p>
      )}
      <div className="admin-list__table-wrap">
        <table className="admin-list__table" aria-label="Danh sách sản phẩm" aria-busy={loading}>
          <thead><tr>
            <th scope="col">Sản phẩm</th><th scope="col">Danh mục</th><th scope="col">Giá bán</th>
            <th scope="col">Tồn kho</th><th scope="col">Trạng thái</th><th scope="col">Cập nhật</th>
            <th scope="col" className="admin-list__end">Thao tác</th>
          </tr></thead>
          <tbody>
            {loading || loadError || visibleProducts.length === 0 ? (
              <tr><td colSpan={7} className="admin-list__empty">
                <span role="status">{loading ? 'Đang tải sản phẩm…' : loadError ? 'Không tải được danh sách sản phẩm.' : 'Không tìm thấy sản phẩm phù hợp.'}</span>
              </td></tr>
            ) : visibleProducts.map(product => {
              const available = Number(product.stockOnHand || 0) - Number(product.reservedQuantity || 0)
              const isActive = product.status === 'ACTIVE'
              return (
                <tr key={product.id}>
                  <td>
                    <span className="admin-list__name">{product.name}</span>
                    <span className="admin-list__subline">{[product.sku, product.brand].filter(Boolean).join(' · ')}</span>
                  </td>
                  <td>{product.category?.name || 'Chưa phân loại'}</td>
                  <td className="admin-list__nowrap">{money(product.price)}</td>
                  <td><span className={available <= 5 ? 'admin-products__low-stock' : undefined}>{available}</span></td>
                  <td><StatusBadge status={product.status} /></td>
                  <td className="admin-list__nowrap">{formatDate(product.updatedAt)}</td>
                  <td className="admin-list__end">
                    <div className="admin-list__actions">
                      <button type="button" className="admin-list__button admin-list__button--small" aria-label={`Sửa ${product.name}`} onClick={() => openEdit(product)}>Sửa</button>
                      <button
                        type="button"
                        className={`admin-list__button admin-list__button--small admin-list__button--${isActive ? 'negative' : 'positive'}`}
                        disabled={changingStatus === product.id}
                        onClick={() => toggleStatus(product)}
                      >
                        {changingStatus === product.id ? 'Đang cập nhật…' : isActive ? 'Ẩn' : 'Kích hoạt'}
                      </button>
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      {!loading && !loadError && visibleProducts.length !== stats.total && (
        <p className="admin-list__subline" role="status">{visibleProducts.length} sản phẩm phù hợp</p>
        )}

      {editorOpen && (
        <div className="admin-editor-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) setEditorOpen(false) }}>
          <section className="admin-editor" role="dialog" aria-modal="true" aria-labelledby="admin-editor-title">
            <header className="admin-editor__head">
              <h2 id="admin-editor-title">{editingProduct ? 'Sửa sản phẩm' : 'Thêm sản phẩm'}</h2>
              <button type="button" className="admin-list__button admin-list__button--small" aria-label="Đóng" onClick={() => setEditorOpen(false)}><X size={17} /></button>
            </header>
            <form onSubmit={saveProduct} className="admin-editor__form">
              <h3 className="admin-editor__section-title">Thông tin cơ bản</h3>
              <label className="admin-editor__field admin-editor__field--full">Tên sản phẩm<input required maxLength="255" value={form.name} onChange={event => {
                const name = event.target.value
                setForm(current => ({ ...current, name, slug: !editingProduct && (!current.slug || current.slug === slugify(current.name)) ? slugify(name) : current.slug }))
              }} /></label>
              <div className="admin-editor__grid">
                <label className="admin-editor__field">Mã SKU<input required maxLength="100" value={form.sku} onChange={event => updateField('sku', event.target.value)} /></label>
                <label className="admin-editor__field">Slug<input required maxLength="255" value={form.slug} onChange={event => updateField('slug', event.target.value)} /></label>
                <label className="admin-editor__field">Danh mục<select required value={form.categoryId} onChange={event => updateField('categoryId', event.target.value)}><option value="">{categories.length ? 'Chọn danh mục' : 'Chưa tải được danh mục'}</option>{categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</select>{categoryLoadError && <small className="admin-editor__hint">{categoryLoadError}</small>}</label>
                <label className="admin-editor__field">Thương hiệu<input maxLength="255" value={form.brand} onChange={event => updateField('brand', event.target.value)} /></label>
                <label className="admin-editor__field">Giá bán (₫)<input required type="number" min="1" step="1" value={form.price} onChange={event => updateField('price', event.target.value)} /></label>
                <label className="admin-editor__field">Bảo hành (tháng)<input type="number" min="0" value={form.warrantyMonths} onChange={event => updateField('warrantyMonths', event.target.value)} /></label>
              </div>
              <label className="admin-editor__field admin-editor__field--full">Mô tả<textarea rows="3" value={form.description} onChange={event => updateField('description', event.target.value)} /></label>

              <label className="admin-editor__field admin-editor__field--full">{'S\u1ed1 l\u01b0\u1ee3ng s\u1ea3n ph\u1ea9m'}<input required type="number" min="0" step="1" value={form.stockOnHand} onChange={event => updateField('stockOnHand', event.target.value)} /></label>
              <h3 className="admin-editor__section-title">Cấu hình kỹ thuật <small>Không bắt buộc</small></h3>
              <div className="admin-editor__grid">
                <label className="admin-editor__field">CPU<input value={form.spec.cpu} onChange={event => updateSpec('cpu', event.target.value)} /></label>
                <label className="admin-editor__field">GPU<input value={form.spec.gpu} onChange={event => updateSpec('gpu', event.target.value)} /></label>
                <label className="admin-editor__field">RAM (GB)<input type="number" min="0" value={form.spec.ramGb} onChange={event => updateSpec('ramGb', event.target.value)} /></label>
                <label className="admin-editor__field">Lưu trữ (GB)<input type="number" min="0" value={form.spec.storageGb} onChange={event => updateSpec('storageGb', event.target.value)} /></label>
                <label className="admin-editor__field">Loại lưu trữ<input placeholder="NVMe, SATA..." value={form.spec.storageType} onChange={event => updateSpec('storageType', event.target.value)} /></label>
                <label className="admin-editor__field">Bo mạch chủ<input value={form.spec.motherboard} onChange={event => updateSpec('motherboard', event.target.value)} /></label>
                <label className="admin-editor__field">Nguồn<input value={form.spec.psu} onChange={event => updateSpec('psu', event.target.value)} /></label>
                <label className="admin-editor__field">Vỏ case<input value={form.spec.caseName} onChange={event => updateSpec('caseName', event.target.value)} /></label>
                <label className="admin-editor__field admin-editor__field--full">Hệ điều hành<input value={form.spec.operatingSystem} onChange={event => updateSpec('operatingSystem', event.target.value)} /></label>
              </div>
              <footer className="admin-editor__footer">
                <button type="button" className="admin-list__button" onClick={() => setEditorOpen(false)}>Hủy</button>
                <button type="submit" className="admin-list__button admin-list__button--primary" disabled={saving}>
                  {saving ? <LoaderCircle className="admin-products__spin" size={16} /> : <Check size={16} />}
                  {saving ? 'Đang lưu...' : editingProduct ? 'Lưu thay đổi' : 'Tạo sản phẩm'}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}
    </div>
  )
}

function StatusBadge({ status }) {
  const labels = { ACTIVE: 'Đang bán', INACTIVE: 'Đang ẩn', DISCONTINUED: 'Ngừng bán' }
  const tone = { ACTIVE: 'positive', DISCONTINUED: 'negative' }[status]
  return <span className={`admin-list__pill${tone ? ` admin-list__pill--${tone}` : ''}`}>{labels[status] || status}</span>
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString('vi-VN')
}
