import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Check, ChevronDown, Image, List, ArrowUpRight, TriangleAlert } from 'lucide-react'
import type { AdvisorProduct, ComponentBuild } from '../services/aiAdvisor'
import { resolveProductImage } from '../assets/productImages'
import { formatVND } from '../utils/cart'

/** A composition of real catalog photos; never presented as a render of the assembled machine. */
export default function ComponentBuildCard({build, products}: {build: ComponentBuild; products: AdvisorProduct[]}) {
  const [preview, setPreview] = useState(false)
  const items = build.items.flatMap(item => {
    const product = products.find(product => product.id === item.productId)
    return product ? [{...item, product}] : []
  })
  if (build.status === 'no_match' || !items.length) return null
  const pcCase = items.find(item => item.category === 'case')
  const pending = build.checks.filter(check => check.status !== 'passed').length
  return <section className="ai-build" aria-label="Cấu hình PC từ linh kiện rời">
    <header className="ai-build__header">
      <div><span className="ai-build__eyebrow">CẤU HÌNH CỦA BẠN</span><h3>8 nhóm linh kiện · Một bộ PC</h3></div>
      <div className="ai-build__total"><strong>{formatVND(build.total)}</strong><span>Còn {formatVND(Math.max(0, build.budget-build.total))} trong ngân sách</span></div>
    </header>
    <div className="ai-build__toolbar" role="group" aria-label="Cách xem cấu hình">
      <button type="button" className={!preview?'is-active':''} aria-pressed={!preview} onClick={()=>setPreview(false)}><List size={16}/>Danh sách linh kiện</button>
      <button type="button" className={preview?'is-active':''} aria-pressed={preview} onClick={()=>setPreview(true)}><Image size={16}/>Xem ảnh cấu hình</button>
    </div>
    {preview && <figure className="ai-build__preview">
      <div className="ai-build__composition">
        {pcCase && <Link className="ai-build__case" to={pcCase.product.url}><img src={resolveProductImage(pcCase.product.image)} alt={pcCase.product.name}/><span>Vỏ máy đã chọn</span></Link>}
        <div className="ai-build__parts">{items.filter(item=>item.category!=='case').map(item=><Link key={item.productId} to={item.product.url} title={item.product.name}><img src={resolveProductImage(item.product.image)} alt={item.product.name} loading="lazy"/><span>{item.label}</span></Link>)}</div>
      </div>
      <figcaption>Ảnh sản phẩm trong cấu hình. Đây là minh họa các linh kiện đã chọn, chưa phải ảnh máy sau khi lắp.</figcaption>
    </figure>}
    {!preview && <ol className="ai-build__list">{items.map((item,index)=><li key={item.productId}>
      <span className="ai-build__number">{String(index+1).padStart(2,'0')}</span>
      <Link className="ai-build__photo" to={item.product.url}><img src={resolveProductImage(item.product.image)} alt="" loading="lazy" width="64" height="64"/></Link>
      <div className="ai-build__product"><span>{item.label} · SL {item.quantity}</span><Link to={item.product.url}>{item.product.name}</Link><small>{item.product.specs.slice(0,2).map(spec=>spec.label).join(' · ')}</small></div>
      <div className="ai-build__price"><strong>{formatVND(item.product.price*item.quantity)}</strong><Link to={item.product.url}>Chi tiết <ArrowUpRight size={13}/></Link></div>
    </li>)}</ol>}
    <details className="ai-build__checks" open>
      <summary><span>{pending ? <TriangleAlert size={17}/> : <Check size={17}/>} {pending ? `${pending} mục cần xác minh trước khi lắp` : 'Đã đối chiếu thông số linh kiện'}</span><ChevronDown size={16}/></summary>
      <ul>{build.checks.map(check=><li key={check.label}><span className={check.status==='passed'?'is-passed':''}>{check.status==='passed'?<Check size={14}/>:<TriangleAlert size={14}/>}</span><div><strong>{check.label}</strong><p>{check.detail}</p></div></li>)}</ul>
    </details>
    <footer className="ai-build__footer"><span>{items.some(item=>item.product.demoCatalog) ? 'Cấu hình có giá và tồn kho mẫu để thử chọn linh kiện; cần xác nhận giá bán và hàng thực tế. ' : 'Giá và tồn kho tại thời điểm tư vấn. '}Chưa bao gồm màn hình, thiết bị ngoại vi và hệ điều hành.</span><button type="button" onClick={()=>{
      const text=items.map(item=>`${item.label}: ${item.product.name} × ${item.quantity} — ${formatVND(item.product.price*item.quantity)}`).join('\n')+`\nTổng: ${formatVND(build.total)}`
      const blob=new Blob([text],{type:'text/plain;charset=utf-8'});const url=URL.createObjectURL(blob)
      const link=document.createElement('a');link.href=url;link.download='cau-hinh-pixelcore.txt';link.click();URL.revokeObjectURL(url)
    }}>Lưu danh sách</button></footer>
  </section>
}
