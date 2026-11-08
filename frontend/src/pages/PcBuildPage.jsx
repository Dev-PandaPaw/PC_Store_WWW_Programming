import React, { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useCart } from '../contexts/cartContextStore'
import { useAuth } from '../contexts/authContextStore'
import { formatVND } from '../utils/cart'
import { pcBuildApi } from '../services/pcBuild'
import { proposalOption, proposalChanges, canConfirm, buildStatus } from '../utils/pcBuildView'
import '../styles/pc-build.css'

const money = value => formatVND(Number(value))
export default function PcBuildPage() {
  const { items, cartVersion, ready, syncing, refresh, notice: cartNotice, applyBuildProposal } = useCart()
  const { user } = useAuth()
  const [budget, setBudget] = useState('45000000')
  const [question, setQuestion] = useState('Giúp tôi hoàn thiện PC và giải thích những điểm cần kiểm tra.')
  const [gpu, setGpu] = useState(true)
  const [rag, setRag] = useState(false)
  const [replace, setReplace] = useState([])
  const [result, setResult] = useState(null)
  const [selected, setSelected] = useState(null)
  const [acknowledged, setAcknowledged] = useState(false)
  const [loading, setLoading] = useState(false)
  const [applying, setApplying] = useState(false)
  const [message, setMessage] = useState('')
  const abort = useRef(null)
  const sequence = useRef(0)
  const dialog = useRef(null)
  useEffect(() => {
    sequence.current++
    abort.current?.abort()
    setLoading(false); setResult(null); setSelected(null); setReplace([])
  }, [cartVersion, user?.id])
  useEffect(() => () => { sequence.current++; abort.current?.abort() }, [])
  useEffect(() => {
    sequence.current++; abort.current?.abort()
    setLoading(false); setResult(null); setSelected(null)
  }, [budget, question, gpu, rag, replace])
  useEffect(() => {
    if (!result?.proposals?.length) return
    const deadline = Math.min(...result.proposals.map(item => Date.parse(item.expiresAt)))
    const timer = setTimeout(() => {
      setSelected(null); setResult(null)
      setMessage('Đề xuất đã hết hạn. Hãy tìm lại để kiểm tra giá và tồn mới nhất.')
    }, Math.max(0, deadline - Date.now()))
    return () => clearTimeout(timer)
  }, [result])
  useEffect(() => {
    if (selected && dialog.current && !dialog.current.open) dialog.current.showModal()
  }, [selected])
  const stale = Boolean(result && result.cartVersion !== cartVersion)
  const submit = async event => {
    event.preventDefault()
    if (!ready || syncing || loading || applying) return
    const current = ++sequence.current
    abort.current?.abort()
    abort.current = new AbortController()
    setLoading(true); setMessage(''); setResult(null); setSelected(null)
    try {
      const { data } = await pcBuildApi.advice({ question, budgetVnd: budget,
        requireDedicatedGpu: gpu, includeRag: rag, replaceProductIds: replace }, abort.current.signal)
      if (current === sequence.current) setResult(data)
    } catch (error) {
      if (current === sequence.current && error.code !== 'ERR_CANCELED')
        setMessage(error?.response?.data?.detail || 'Chưa lấy được đề xuất. Vui lòng thử lại.')
    } finally { if (current === sequence.current) setLoading(false) }
  }
  const confirm = async () => {
    if (!canConfirm(selected, acknowledged, stale || syncing, applying)) return
    const current = sequence.current
    setApplying(true); setMessage('')
    try {
      const outcome = await applyBuildProposal(selected.proposalId, acknowledged)
      if (current === sequence.current) {
        if (outcome.success) { setSelected(null); setResult(null); setMessage('Đã cập nhật giỏ hàng.') }
        else setMessage(outcome.message || 'Giỏ hoặc giá đã thay đổi. Hãy lấy đề xuất mới.')
      }
    } finally { setApplying(false) }
  }
  const changes = selected ? proposalChanges(selected, items) : null
  const search = result?.advice?.build_search
  return <div className="pc-build-page">
    <nav className="breadcrumbs"><Link to="/cart">Giỏ hàng</Link><span>/</span><span>Build PC</span></nav>
    <header><p className="section-eyebrow">HOÀN THIỆN GÓC MÁY CỦA BẠN</p>
      <h1>Build PC theo ngân sách</h1>
      <p>Giữ các món đang chọn, tìm phần còn thiếu và xem kỹ trước khi cập nhật giỏ.</p></header>
    {message && <p role="alert" className="pc-build-message">{message}</p>}
    <form className="pc-build-form" onSubmit={submit}>
      <label>Ngân sách toàn bộ cấu hình (VND)<input type="number" min="0" step="1" required
        value={budget} onChange={e => setBudget(e.target.value)} disabled={loading || applying}/></label>
      <label>Nhu cầu và điều cần giải thích<textarea required maxLength={2000} value={question}
        onChange={e => setQuestion(e.target.value)} disabled={loading || applying}/></label>
      <label className="pc-build-toggle"><input type="checkbox" checked={gpu} disabled={loading || applying} onChange={e => setGpu(e.target.checked)}/>Cần card đồ họa rời</label>
      <label className="pc-build-toggle"><input type="checkbox" checked={rag} disabled={loading || applying} onChange={e => setRag(e.target.checked)}/>Tham khảo tài liệu kỹ thuật (có thể mất vài phút)</label>
      {items.length > 0 && <fieldset><legend>Món đang có trong giỏ</legend>
        <p>Mặc định giữ nguyên. Chỉ đánh dấu món bạn muốn tìm phương án thay.</p>
        {items.map(item => <label className="pc-build-toggle" key={item.id}>
          <input type="checkbox" checked={replace.includes(item.id)} disabled={loading || applying}
            onChange={e => setReplace(value => e.target.checked ? [...value, item.id] : value.filter(id => id !== item.id))}/>
          Tìm thay {item.name} × {item.quantity}</label>)}
      </fieldset>}
      {!ready && <div role="status" className="pc-build-message">
        <p>{cartNotice || 'Đang tải giỏ hàng trước khi tìm cấu hình…'}</p>
        {cartNotice && <button type="button" onClick={refresh}>Thử tải lại giỏ</button>}
      </div>}
      {ready && syncing && <p role="status">Đang đồng bộ thay đổi giỏ hàng…</p>}
      <button className="store-button" disabled={!ready || syncing || loading || applying || !question.trim()}>
        {loading ? 'Đang tìm cấu hình…' : 'Tìm cấu hình'}</button>
    </form>
    {loading && <p role="status">Đang đọc giá, tồn kho và kiểm tra cấu hình. Bạn có thể tiếp tục xem sản phẩm.</p>}
    {search && <section aria-label="Kết quả tư vấn">
      <h2>{buildStatus[search.status] || 'Kết quả tư vấn'}</h2>
      {stale && <p role="alert">Giỏ hàng đã thay đổi. <button type="button" onClick={refresh}>Đồng bộ giỏ và tìm lại</button></p>}
      {!search.search.search_exhaustive && <p>Chưa duyệt hết sản phẩm; có thể còn phương án khác phù hợp hơn.</p>}
      {search.status === 'over_budget' && <p>Phần vượt ngân sách: {money(search.over_budget_by_vnd)}.</p>}
      {search.status === 'invalid_selection' && <p>Có món không khả dụng hoặc giỏ chưa phù hợp cho một PC. Hãy kiểm tra lại giỏ.</p>}
      {['rejected', 'unavailable'].includes(result.advice.rag_status) && <p>Phần dẫn chứng tài liệu chưa sẵn sàng. Giá và kiểm tra cấu hình bên dưới vẫn được giữ nguyên.</p>}
      <div className="pc-build-options">{result.proposals.map((proposal, index) => {
        const option = proposalOption(result, proposal)
        return <article className="pc-build-option" key={proposal.proposalId}>
          <h3>Phương án {index + 1}</h3>
          <p className={proposal.requiresVerification ? 'pc-build-warning' : 'pc-build-ok'}>
            {proposal.requiresVerification ? 'Có thể thêm vào giỏ; còn dữ liệu cần bổ sung trước khi đặt hàng' : 'Đã qua các bước kiểm tra tương thích hiện có'}</p>
          <div className="pc-build-table"><table><thead><tr><th>Linh kiện</th><th>Số lượng</th><th>Đơn giá</th><th>Lựa chọn</th></tr></thead>
            <tbody>{proposal.items.map(item => <tr key={item.productId}><td>{item.name}
                {result.advice.component_sources?.[item.productId]?.map((evidence, n) =>
                  <div key={n}><a href={evidence.source} target="_blank" rel="noreferrer">{evidence.document_type || 'Tài liệu'}</a>
                    {evidence.summary && <small> — {evidence.summary}</small>}</div>)}
              </td><td>{item.quantity}</td>
              <td>{money(item.unitPrice)}</td><td>{item.kept ? 'Giữ lại' : 'Đề xuất'}</td></tr>)}</tbody></table></div>
          <strong>Tổng: {money(proposal.subtotalVnd)}</strong>
          {option?.compatibility?.missing_components?.length > 0 && <p>Còn thiếu hoặc chưa xác định: {option.compatibility.missing_components.join(', ')}.</p>}
          {option?.benchmark_references?.length > 0 && <details><summary>Benchmark GPU tham khảo</summary>
            <p>Đây là số đo trên máy thử của nguồn, không phải FPS dự đoán của cả bộ này.</p>
            <table><thead><tr><th>Game / thiết lập</th><th>AVG FPS</th><th>1% low</th><th>Nguồn</th></tr></thead>
              <tbody>{option.benchmark_references.map(reference => <tr key={reference.measurement.id}>
                <td>{reference.measurement.game} / {reference.measurement.resolution} / {reference.measurement.preset}
                  <small> — RT {reference.measurement.ray_tracing ? 'bật' : 'tắt'}; upscaling: {reference.measurement.upscaling === 'not_reported' ? 'nguồn không ghi rõ' : reference.measurement.upscaling}</small></td>
                <td>{reference.measurement.average_fps}</td><td>{reference.measurement.low_1_percent_fps ?? '—'}</td>
                <td><a href={reference.measurement.chart_url} target="_blank" rel="noreferrer">{reference.source.publisher}</a><br/>{reference.source.test_cpu}</td>
              </tr>)}</tbody></table>
          </details>}
          <details><summary>Xem kết quả từng quy tắc</summary><ul>
            {option?.compatibility?.checks?.map((check, n) =>
              <li key={`${check.code}-${n}`}>{check.code}: {check.status.toUpperCase()} — {check.message}
                {check.missing_fields?.length > 0 && <small> Còn thiếu: {check.missing_fields.join(', ')}</small>}</li>)}
          </ul></details>
          <button type="button" className="store-button" disabled={stale || syncing || applying}
            onClick={() => { setAcknowledged(false); setSelected(proposal) }}>Xem thay đổi giỏ</button>
        </article>
      })}</div>
      {result.advice.explanations?.length > 0 && <details className="pc-build-evidence"><summary>Tài liệu tham khảo cho phương án đầu tiên</summary>
        {result.advice.explanations.map((item, index) => {
          const source = result.advice.knowledge_sources.find(s => s.reference === item.source_reference)
          return <div key={index}><p>{item.message}</p><blockquote>{item.evidence_quote}</blockquote>
            {source && <small>{source.source} — trang {source.page}</small>}</div>
        })}
      </details>}
      <p>Giá chưa gồm ưu đãi và phí vận chuyển. Chưa giữ hàng; hệ thống sẽ kiểm tra lại khi bạn xác nhận.</p>
    </section>}
    {selected && <dialog ref={dialog} className="pc-build-dialog" aria-labelledby="pc-build-confirm-title"
      onCancel={event => { if (applying) event.preventDefault(); else setSelected(null) }}>
      <h2 id="pc-build-confirm-title">Xác nhận thay đổi giỏ</h2>
      {changes.removed.length > 0 && <><h3>Bỏ khỏi giỏ</h3><ul>{changes.removed.map(name => <li key={name}>{name}</li>)}</ul></>}
      <h3>Thêm hoặc cập nhật</h3>
      {[...changes.added, ...changes.changed].length ? <ul>{[...changes.added, ...changes.changed].map(item =>
        <li key={item.productId}>{item.name} × {item.quantity} — {money(item.unitPrice)}</li>)}</ul> : <p>Các món đã có được giữ nguyên.</p>}
      <p>Tổng giỏ sau thay đổi: <strong>{money(selected.subtotalVnd)}</strong></p>
      {selected.requiresVerification && <p className="pc-build-warning">
        Bạn có thể thêm cấu hình gợi ý vào giỏ. Trước khi đặt hàng, hệ thống bắt buộc kiểm tra lại các điều kiện còn thiếu.</p>}
      <p>Nếu giá, tồn hoặc giỏ đã đổi, bạn cần xem và xác nhận đề xuất mới.</p>
      <div className="pc-build-actions"><button type="button" disabled={applying} onClick={() => setSelected(null)}>Hủy</button>
        <button type="button" className="store-button" disabled={!canConfirm(selected, acknowledged, stale || syncing, applying)}
          onClick={confirm}>{applying ? 'Đang kiểm tra lại…' : 'Xác nhận cập nhật giỏ'}</button></div>
    </dialog>}
  </div>
}
