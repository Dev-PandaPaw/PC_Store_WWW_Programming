import React, { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, ArrowUp, ChevronRight, LoaderCircle, Paperclip, Plus, TriangleAlert } from 'lucide-react'
import { analyzeHardwareAttachment, getAdvisorStatus, sendAdvisorMessage, type AdvisorResponse, type AdvisorStatus, type ChatHistoryItem, type GameReport, type HardwareContext } from '../services/aiAdvisor'
import { resolveProductImage } from '../assets/productImages'
import { formatVND } from '../utils/cart'
import AdvisorRobot from '../components/AdvisorRobot'
import Breadcrumbs from '../components/Breadcrumbs'
import ComponentBuildCard from '../components/ComponentBuildCard'
import ChatAttachmentInput, { ChatFilePreview, type ChatAttachment, type ChatAttachmentHandle } from '../components/ChatAttachmentInput'
import advisorMascot from '../assets/branding/pixelcore-assistant.png'
import '../styles/ai-chat.css'

interface Turn { id: number; question: string; answer?: AdvisorResponse; error?: string; code?: string; hardware?: HardwareContext; attachment?: ChatAttachment }
interface Conversation { id: number; turns: Turn[]; hardware?: HardwareContext }
function AdvisorIcon() {
  return <img className="ai-brand-icon" src={advisorMascot} alt="" aria-hidden="true"/>
}
function GameRequirementsCard({game}: {game: GameReport}) {
  const levels = [{label:'Tối thiểu',value:game.minimum},{label:'Khuyến nghị',value:game.recommended}]
  return <section className="ai-game-report" aria-label={`Yêu cầu cấu hình ${game.title}`}>
    <h3>{game.title} · Yêu cầu cấu hình</h3>
    <div className="ai-game-levels">{levels.map(({label,value})=><div key={label}>
      <h4>{label}</h4><dl>
        <dt>CPU</dt><dd>{value.cpu}</dd><dt>GPU</dt><dd>{value.gpu}</dd>
        <dt>RAM</dt><dd>{value.ramGb} GB</dd><dt>Lưu trữ</dt><dd>{value.storageGb} GB trống{value.ssdRequired?' · Bắt buộc SSD':''}</dd>
      </dl>
    </div>)}</div>
    <p className="ai-game-note">{game.minimum.notes}</p>
    <div className="ai-game-sources"><span>{game.lookupStatus==='live_search'?'Tra cứu có nguồn':'Dữ liệu tham chiếu'} · {game.checkedAt}</span>
      {game.sources.map(source=><a key={source.url} href={source.url} target="_blank" rel="noopener noreferrer">{source.title}</a>)}
    </div>
  </section>
}
const prompts = [
  {title:'Tư vấn PC chơi game', description:'Dưới 25 triệu', text:'Tư vấn PC chơi game dưới 25 triệu'},
  {title:'Build PC từ linh kiện rời', description:'Đủ linh kiện · Trong ngân sách', text:'Build PC từ linh kiện rời để chơi game dưới 35 triệu'},
  {title:'Nâng cấp PC hiện tại', description:'Tối ưu hiệu năng', text:'Mình muốn nâng cấp PC hiện tại, cần cung cấp thông tin gì?'},
]
const quickPrompts = ['Build PC linh kiện rời dưới 35 triệu', 'PC đồ họa, chỉnh sửa video', 'Build PC màu trắng dưới 35 triệu', 'Chuột không dây dưới 1 triệu']

export default function AiChatPage() {
  const [status,setStatus]=useState<AdvisorStatus|null>(null)
  const [statusError,setStatusError]=useState(false)
  const [draft,setDraft]=useState('')
  const [conversations,setConversations]=useState<Conversation[]>([{id:0,turns:[]}])
  const [conversationId,setConversationId]=useState(0)
  const [loading,setLoading]=useState(false)
  const [attachmentBusy,setAttachmentBusy]=useState(false)
  const [pendingAttachment,setPendingAttachment]=useState<ChatAttachment>()
  const [retryAt,setRetryAt]=useState(0)
  const [now,setNow]=useState(Date.now())
  const [refresh,setRefresh]=useState(0)
  const abort=useRef<AbortController|null>(null)
  const requestNumber=useRef(0)
  const nextId=useRef(0)
  const log=useRef<HTMLDivElement>(null)
  const input=useRef<HTMLTextAreaElement>(null)
  useEffect(()=>{const el=input.current;if(el){el.style.height='auto';el.style.height=`${Math.min(132,Math.max(44,el.scrollHeight))}px`}},[draft])
  const attachment=useRef<ChatAttachmentHandle>(null)
  const hardware=conversations.find(conversation=>conversation.id===conversationId)?.hardware
  function setHardware(value?:HardwareContext) {setConversations(previous=>previous.map(conversation=>conversation.id===conversationId?{...conversation,hardware:value}:conversation))}
  const turns=conversations.find(conversation=>conversation.id===conversationId)?.turns||[]
  const successful=turns.filter(turn=>turn.answer)
  const showGreeting=turns.length===0
  const maxLength=status?.maxMessageLength||1200
  const maxTurns=status?.maxTurns||8
  const remaining=Math.max(0,Math.ceil((retryAt-now)/1000))
  const ended=successful.length>=maxTurns
  const ready=status?.configured===true

  useEffect(()=>{
    const controller=new AbortController()
    setStatus(null);setStatusError(false)
    getAdvisorStatus(controller.signal).then(setStatus).catch(()=>{if(!controller.signal.aborted)setStatusError(true)})
    return ()=>controller.abort()
  },[refresh])
  useEffect(()=>()=>{requestNumber.current++;abort.current?.abort()},[])
  useEffect(()=>{
    if(retryAt<=Date.now())return
    setNow(Date.now())
    const timer=window.setInterval(()=>{
      const time=Date.now()
      setNow(time)
      if(time>=retryAt)window.clearInterval(timer)
    },1000)
    return()=>window.clearInterval(timer)
  },[retryAt])
  useEffect(()=>{
    const container=log.current
    if(!container)return
    const latest=container.querySelectorAll('.ai-turn').item(turns.length-1)
    const top=latest?latest.getBoundingClientRect().top-container.getBoundingClientRect().top+container.scrollTop-parseFloat(getComputedStyle(container).paddingTop):0
    container.scrollTo({top:Math.max(0,top),behavior:window.matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'})
  },[turns,loading,conversationId])
  useEffect(()=>{const before=document.title;document.title='Tư vấn cấu hình | PixelCore';return()=>{document.title=before}},[])

  function setTurns(update:(previous:Turn[])=>Turn[]) {
    setConversations(previous=>previous.map(conversation=>conversation.id===conversationId?{...conversation,turns:update(conversation.turns)}:conversation))
  }
  function stopRequest() {
    requestNumber.current++;abort.current?.abort();abort.current=null
    if(loading)setTurns(previous=>previous.map(turn=>!turn.answer&&!turn.error?{...turn,error:'Đã dừng câu trả lời. Bạn có thể thử lại tin nhắn này.'}:turn))
    setLoading(false)
  }
  function openConversation(id:number) {
    if(id===conversationId)return
    stopRequest();setDraft('');setPendingAttachment(undefined);setAttachmentBusy(false);setConversationId(id)
    input.current?.focus()
  }
  function reset() {
    attachment.current?.clear()
    stopRequest();setDraft('');setPendingAttachment(undefined);setAttachmentBusy(false);setHardware(undefined)
    if(turns.length){
      const empty=conversations.find(conversation=>!conversation.turns.length)
      if(empty)setConversationId(empty.id)
      else {
        const id=++nextId.current
        setConversations(previous=>[{id,turns:[]},...previous]);setConversationId(id)
      }
    }
    input.current?.focus()
  }
  async function send(text=draft, retryId?:number) {
    const previousTurn=retryId?turns.find(turn=>turn.id===retryId):undefined
    const attached=previousTurn?.attachment||(!retryId?pendingAttachment:undefined)
    const message=text.trim()||(attached||hardware?'Đề xuất linh kiện phù hợp với thông số trong file.':'')
    if(!message||message.length>maxLength||loading||attachmentBusy||(abort.current&&!abort.current.signal.aborted)||remaining>0||ended||!ready) return
    let context=retryId?previousTurn?.hardware:attached?undefined:hardware
    const history:ChatHistoryItem[]=successful.flatMap(turn=>[{role:'user' as const,content:turn.question},{role:'assistant' as const,content:turn.answer!.message}])
    const id=retryId||Date.now()
    setTurns(previous=>retryId?previous.map(turn=>turn.id===id?{id,question:message,hardware:context,attachment:attached}:turn):[...previous,{id,question:message,hardware:context,attachment:attached}])
    setDraft('');setPendingAttachment(undefined);setLoading(true)
    const controller=new AbortController();abort.current=controller
    const sequence=++requestNumber.current
    try {
      if(attached&&!context) {
        setHardware(undefined)
        setAttachmentBusy(true)
        const analysis=await analyzeHardwareAttachment(attached.file,controller.signal)
        if(controller.signal.aborted||sequence!==requestNumber.current)return
        if(!analysis.readable||!analysis.components.length)throw new Error('UNREADABLE_ATTACHMENT')
        context={use:attached.use,filename:attached.file.name.slice(0,160).replace(/[\x00-\x1F\x7F]/g,''),components:analysis.components}
        setHardware(context)
        setTurns(previous=>previous.map(turn=>turn.id===id?{...turn,hardware:context}:turn))
        setAttachmentBusy(false)
      }
      const answer=await sendAdvisorMessage(message,history,controller.signal,context)
      // Resolve only local bundled photos; provider output never determines image URLs.
      answer.products.forEach(product=>resolveProductImage(product.image))
      if(sequence===requestNumber.current)setTurns(previous=>previous.map(turn=>turn.id===id?{...turn,answer}:turn))
    } catch(error:unknown) {
      if(controller.signal.aborted||sequence!==requestNumber.current)return
      const response=(error as {response?:{data?:{error?:{message?:string;code?:string;retryAfterSeconds?:number}},headers?:Record<string,string>}}).response
      const details=response?.data?.error
      const delay=Number(details?.retryAfterSeconds||response?.headers?.['retry-after'])
      if(Number.isFinite(delay)&&delay>0)setRetryAt(Date.now()+Math.min(delay,86400)*1000)
      const fallback=error instanceof Error&&error.message==='UNREADABLE_ATTACHMENT'?'Ảnh hoặc file chưa có thông số PC đọc được. Bạn gửi ảnh rõ hơn hoặc nhập tên linh kiện cần tìm nhé.':'Chưa nhận được câu trả lời. Bạn kiểm tra kết nối và thử lại nhé.'
      setTurns(previous=>previous.map(turn=>turn.id===id?{...turn,error:details?.message||fallback,code:details?.code}:turn))
    } finally {if(sequence===requestNumber.current){abort.current=null;setLoading(false);setAttachmentBusy(false);input.current?.focus()}}
  }

  return <div className="ai-page">
    <Breadcrumbs className="ai-breadcrumbs" items={[{ label: 'Tư vấn cấu hình' }]} />
    <div className="ai-layout">
      <aside className="ai-history" aria-label="Lịch sử hội thoại">
        <button type="button" className="ai-reset" onClick={reset}><Plus size={17}/><span>Hội thoại mới</span></button>
        <h2>Hội thoại gần đây</h2>
        <div className="ai-history__list">
          {conversations.filter(conversation=>conversation.turns.length).map(conversation=><button type="button" key={conversation.id} className={conversation.id===conversationId?'is-active':''} aria-current={conversation.id===conversationId?'true':undefined} onClick={()=>openConversation(conversation.id)} title={conversation.turns[0].question}>{conversation.turns[0].question}</button>)}
          {!conversations.some(conversation=>conversation.turns.length)&&<p className="ai-history__empty">Những cuộc trò chuyện của bạn sẽ xuất hiện ở đây.</p>}
        </div>
      </aside>
      <section className="ai-chat" aria-label="Trò chuyện tư vấn sản phẩm">
        <button type="button" className="ai-reset ai-reset--compact" onClick={reset}><Plus size={17}/><span>Hội thoại mới</span></button>

        <div className={`ai-chat__log${showGreeting?' ai-chat__log--welcome':''}`} ref={log} role="log" aria-live="polite" aria-relevant="additions text">
          {showGreeting&&<div className="ai-welcome">
            <div className="ai-greeting">
              <AdvisorRobot />
              <div className="ai-greeting__copy"><p>Xin chào</p><h1>Mình là <span>PixelCore AI</span></h1><p>Mình có thể giúp bạn tư vấn cấu hình PC, chọn linh kiện phù hợp với nhu cầu và ngân sách.</p></div>
            </div>
            <div className="ai-prompts">{prompts.map(prompt=><button type="button" key={prompt.text} disabled={!ready||loading||remaining>0} onClick={()=>send(prompt.text)}><strong>{prompt.title}</strong><small>{prompt.description}</small></button>)}</div>
          </div>}
          {(statusError||status?.configured===false)&&<div className="ai-status-note" role="status"><TriangleAlert size={18}/><div><strong>Trợ lý AI tạm thời chưa sẵn sàng</strong><p>Bạn có thể kiểm tra lại kết nối hoặc quay lại sau.</p><button type="button" onClick={()=>setRefresh(value=>value+1)}>Kiểm tra lại kết nối</button></div></div>}
          {turns.map(turn=><div className="ai-turn" key={turn.id}>
            <div className="ai-user-message"><span>Bạn</span>{turn.attachment&&<ChatFilePreview attachment={turn.attachment} sent/>}<p>{turn.question}</p></div>
            {turn.answer&&<div className="ai-answer"><div className="ai-answer__label"><AdvisorIcon/> PixelCore</div><p className="ai-answer__text">{turn.answer.message}</p>
              {turn.answer.game&&<GameRequirementsCard game={turn.answer.game}/>}
              {turn.answer.build&&<ComponentBuildCard build={turn.answer.build} products={turn.answer.products}/>}
              {!turn.answer.build&&!!turn.answer.products.length&&<div className="ai-products">{turn.answer.products.map(product=><article className="ai-product" key={product.id}>
                <Link className="ai-product__image" to={`/products/${product.slug}`}><img src={resolveProductImage(product.image)} alt={product.name} loading="lazy" width="180" height="180"/></Link>
                <div className="ai-product__body">
                  <h3><Link to={`/products/${product.slug}`} title={product.name}>{product.name}</Link></h3>
                  <strong className="ai-product__price">{formatVND(product.price)}</strong>
                  <p className="ai-product__specs" title={product.specs.map(spec=>spec.label).join(' · ')}>{product.specs.slice(0,2).map(spec=>spec.label).join(' · ')}</p>
                  {product.gameFit&&<div className="ai-game-fit"><strong>{product.gameFit.level==='recommended'?'Mức khuyến nghị · Đối chiếu sơ bộ':'Mức tối thiểu · Đối chiếu sơ bộ'}</strong>
                    <p>{product.gameFit.ramComparison}</p><p>{product.gameFit.storageComparison}</p>
                    <p title={product.gameFit.cpuComparison}>CPU: {product.gameFit.cpu}</p><p title={product.gameFit.gpuComparison}>GPU: {product.gameFit.gpu}</p>
                  </div>}
                  {product.hardwareFit&&<div className="ai-hardware-fit"><strong>Đối chiếu theo thông số đã gửi</strong>{product.hardwareFit.checks.map((check,index)=><p key={index}>{check}</p>)}</div>}
                  <div className="ai-product__meta"><span className={`ai-stock${product.stockOnHand>0?' is-available':''}`}>{product.stockOnHand>0?'Còn hàng':'Đang kinh doanh'}</span><Link className="ai-product__link" to={`/products/${product.slug}`}>Chi tiết <ArrowRight size={12}/></Link></div>
                  {product.reasons.some(reason=>reason.includes('tương thích'))&&<p className="ai-product__compatibility">Cần kiểm tra tương thích với cấu hình hiện tại.</p>}
                </div>
              </article>)}</div>}
              {turn.answer.outcome==='no_match'&&<Link className="ai-empty-link" to="/catalog">Xem danh mục hiện tại <ArrowRight size={15}/></Link>}
            </div>}
            {turn.error&&<div className="ai-error" role="alert"><TriangleAlert size={19}/><div><p>{turn.error}</p><button type="button" disabled={loading||remaining>0||!ready} onClick={()=>send(turn.question,turn.id)}>{remaining>0?`Thử lại sau ${remaining}s`:'Thử lại tin nhắn này'}</button></div></div>}
          </div>)}
          {loading&&<div className="ai-thinking" role="status"><AdvisorIcon/><span>{attachmentBusy?'Đang đọc ảnh và thông số bạn gửi':'Đang tìm linh kiện phù hợp'}</span><i/><i/><i/></div>}
        </div>

        {successful.length>0&&!loading&&!ended&&<div className="ai-followups" aria-label="Câu hỏi gợi ý">{successful.at(-1)?.answer?.suggestions.map(suggestion=><button type="button" key={suggestion} disabled={remaining>0||!ready} onClick={()=>send(suggestion)}>{suggestion}</button>)}</div>}
        <form className="ai-composer" onSubmit={event=>{event.preventDefault();send()}} onDragOver={event=>{if(event.dataTransfer.types.includes('Files'))event.preventDefault()}} onDrop={event=>{if(event.dataTransfer.files.length){event.preventDefault();attachment.current?.accept(event.dataTransfer.files[0])}}}>
          <ChatAttachmentInput key={conversationId} ref={attachment} value={pendingAttachment} onChange={setPendingAttachment} disabled={ended||!ready||remaining>0} sending={loading}/>
          <div className="ai-composer__input"><button type="button" className="ai-attach-button" aria-label="Đính kèm ảnh hoặc file thông số" title="Đính kèm ảnh, PDF hoặc file thông số" disabled={ended||!ready||loading||remaining>0} onClick={()=>attachment.current?.choose()}><Paperclip size={20}/></button><label className="ai-sr-only" htmlFor="ai-message">Nhu cầu tư vấn của bạn</label><textarea ref={input} id="ai-message" placeholder={ended?'Tạo hội thoại mới để tiếp tục nhé…':pendingAttachment?'Thêm yêu cầu cho ảnh này…':hardware?'Hỏi thêm về cấu hình này…':'Nhập câu hỏi hoặc dán ảnh…'} value={draft} maxLength={maxLength} rows={1} disabled={ended||!ready} onPaste={event=>{
            const file=event.clipboardData.files[0]
            if(file){event.preventDefault();attachment.current?.accept(file);return}
            const text=event.clipboardData.getData('text/plain')
            if(text.length>maxLength||(text.match(/(?:^|\n)\s*(?:cpu|gpu|vga|ram|mainboard|motherboard|ssd|hdd|psu|storage|socket|nguồn)\s*:/gi)||[]).length>=2){event.preventDefault();attachment.current?.accept(new File([text],'thong-so-da-dan.txt',{type:'text/plain'}))}
          }} onChange={event=>setDraft(event.target.value)} onKeyDown={event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.nativeEvent.isComposing){event.preventDefault();send()}}}/><button className="ai-send" type="submit" disabled={(!draft.trim()&&!pendingAttachment&&!hardware)||loading||attachmentBusy||remaining>0||ended||!ready} aria-label="Gửi tin nhắn">{loading?<LoaderCircle size={20} className="ai-spin"/>:<ArrowUp size={21}/>}</button></div>
          <div className="ai-composer__meta"><span>{ended?<button type="button" onClick={reset}>Đã đủ {maxTurns} lượt · Tạo hội thoại mới</button>:remaining>0?`Có thể gửi lại sau ${remaining}s`:'PNG, JPG, WebP, PDF ≤ 5 MB · TXT, CSV, JSON ≤ 32 KB'}</span><span>{draft.length}/{maxLength}</span></div>
        </form>
      </section>
      <aside className="ai-resources" aria-label="Gợi ý tư vấn">
        <section className="ai-side-card"><h2>Gợi ý nhanh</h2><div className="ai-quick-prompts">{quickPrompts.map(prompt=><button type="button" key={prompt} disabled={!ready||loading||remaining>0||ended} onClick={()=>send(prompt)}><span>{prompt}</span><ChevronRight size={16}/></button>)}</div></section>
        <section className="ai-side-card"><h2>Thông tin hữu ích</h2><Link to="/blog/chon-cpu-va-gpu-cho-pc-gaming"><strong>Hướng dẫn chọn cấu hình PC</strong><span>Bắt đầu từ nhu cầu của bạn</span></Link><Link to="/catalog"><strong>Khám phá linh kiện PC</strong><span>Tìm lựa chọn vừa ngân sách</span></Link><Link to="/blog"><strong>Kiến thức về PC</strong><span>Kinh nghiệm và mẹo hữu ích</span></Link></section>
      </aside>
    </div>
  </div>
}
