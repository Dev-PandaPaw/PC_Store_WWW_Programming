import { forwardRef, useEffect, useImperativeHandle, useRef, useState } from 'react'
import { FileText, TriangleAlert, X } from 'lucide-react'
import type { HardwareContext } from '../services/aiAdvisor'

export interface ChatAttachment {file: File; use: HardwareContext['use']}
export interface ChatAttachmentHandle {choose(): void; accept(file: File): void; clear(): void}
interface Props {value?: ChatAttachment; onChange(value?: ChatAttachment): void; disabled: boolean; sending: boolean}
const accept='.png,.jpg,.jpeg,.webp,.pdf,.txt,.csv,.json'

/** Each preview owns its URL, so clearing a draft cannot break a sent photo. */
export function ChatFilePreview({attachment,sent=false}: {attachment: ChatAttachment; sent?: boolean}) {
  const [url,setUrl]=useState('')
  useEffect(()=>{
    const objectUrl=URL.createObjectURL(attachment.file);setUrl(objectUrl)
    return()=>URL.revokeObjectURL(objectUrl)
  },[attachment.file])
  const image=/\.(png|jpe?g|webp)$/i.test(attachment.file.name)
  if(!url)return null
  return <a className={sent?'ai-sent-attachment':'ai-attachment__preview'} href={url} target="_blank" rel="noopener noreferrer" aria-label={`Xem ${attachment.file.name}`}>
    {image?<img src={url} alt={attachment.file.name}/>:<FileText size={sent?28:24}/>}
    {sent&&<span>{attachment.file.name}</span>}
  </a>
}

const ChatAttachmentInput=forwardRef<ChatAttachmentHandle,Props>(function ChatAttachmentInput({value,onChange,disabled,sending},ref) {
  const fileInput=useRef<HTMLInputElement>(null)
  const [error,setError]=useState('')
  function remove() {setError('');onChange(undefined)}
  function add(file: File) {
    if(disabled||sending)return
    const ext=file.name.split('.').at(-1)?.toLowerCase()
    if(!ext||!accept.split(',').includes('.'+ext)){setError('Chọn ảnh PNG/JPG/WebP, PDF hoặc file TXT/CSV/JSON.');return}
    if(!file.size){setError('File đang trống. Hãy chọn file có thông số máy.');return}
    if(file.size>5*1024*1024){setError('Mỗi file tối đa 5 MB.');return}
    if(['txt','csv','json'].includes(ext)&&file.size>32768){setError('File văn bản tối đa 32 KB. Hãy chỉ giữ phần thông số.');return}
    setError('');onChange({file,use:value?.use||'current'})
  }
  useImperativeHandle(ref,()=>({choose:()=>{if(!disabled&&!sending)fileInput.current?.click()},accept:add,clear:remove}))
  return <div className="ai-attachment">
    <input ref={fileInput} type="file" accept={accept} className="ai-sr-only" tabIndex={-1} aria-label="Chọn file thông số máy" disabled={disabled||sending} onChange={event=>{const file=event.target.files?.[0];if(file)add(file);event.target.value=''}}/>
    {value&&<section className="ai-attachment__draft" aria-label="File chờ gửi">
      <ChatFilePreview attachment={value}/>
      <div className="ai-attachment__caption"><strong title={value.file.name}>{value.file.name}</strong>
        <label><span className="ai-sr-only">Nội dung ảnh hoặc file</span><select value={value.use} disabled={sending} onChange={event=>onChange({...value,use:event.target.value as HardwareContext['use']})}><option value="current">Máy hiện tại</option><option value="desired">Cấu hình tham khảo</option></select></label>
      </div>
      <button type="button" className="ai-attachment__remove" aria-label="Bỏ file đính kèm" onClick={remove} disabled={sending}><X size={17}/></button>
    </section>}
    {error&&<div className="ai-attachment__error" role="alert"><TriangleAlert size={14}/><span>{error}</span><button type="button" aria-label="Đóng thông báo file" onClick={()=>setError('')}><X size={13}/></button></div>}
  </div>
})
export default ChatAttachmentInput
