import React, { useState } from 'react'
import { Copy, Check, Hash } from 'lucide-react'
import { shortOrderCode } from '../utils/checkout'

export default function OrderReference({ code }) {
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState('')
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(code)
      setCopied(true)
      setError('')
    } catch { setError('Bạn có thể chọn và sao chép mã bên trên.') }
  }
  return <details className="order-reference">
    <summary aria-label={`Mã đơn ${shortOrderCode(code)}. Xem mã đầy đủ`}>
      <Hash size={15} aria-hidden="true" /><span>{shortOrderCode(code)}</span>
      <span className="order-reference__hint">Chi tiết</span>
    </summary>
    <div className="order-reference__full">
      <span>Mã đơn đầy đủ</span><code>{code}</code>
      <button type="button" onClick={copy} aria-label="Sao chép mã đơn đầy đủ">
        {copied ? <Check size={14} /> : <Copy size={14} />} Sao chép
      </button>
      <span role="status">{error || (copied ? 'Đã sao chép mã đơn.' : '')}</span>
    </div>
  </details>
}
