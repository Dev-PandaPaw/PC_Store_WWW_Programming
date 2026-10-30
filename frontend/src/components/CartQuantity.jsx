import React, { useEffect, useId, useRef, useState } from 'react'
import { Minus, Plus } from 'lucide-react'
import './CartQuantity.css'

export default function CartQuantity({ id, name, quantity, availableQuantity, onUpdate, disabled = false }) {
  const [draft, setDraft] = useState(String(quantity))
  const [error, setError] = useState('')
  const draftRef = useRef(String(quantity))
  const requested = useRef(quantity)
  const attempt = useRef(0)
  const errorId = useId()
  const max = Math.max(1, Math.min(99, availableQuantity ?? 99))

  useEffect(() => { requested.current = quantity; draftRef.current = String(quantity); setDraft(String(quantity)) }, [quantity])

  const commit = async (value = draftRef.current) => {
    if (disabled) return
    const next = Number(value)
    if (!/^\d+$/.test(value) || !Number.isInteger(next) || next < 1 || next > max) {
      setError(`Nhập số nguyên từ 1 đến ${max}.`)
      return
    }
    setError('')
    if (next === requested.current) return
    requested.current = next
    const current = ++attempt.current
    try {
      const result = await onUpdate(id, next)
      if (current === attempt.current && !result?.success) {
        setError(result?.message || 'Không thể cập nhật số lượng. Vui lòng thử lại.')
      }
    } catch {
      if (current === attempt.current) setError('Không thể cập nhật số lượng. Vui lòng thử lại.')
    }
  }

  const step = delta => {
    const next = String(Math.min(max, Math.max(1, (Number(draftRef.current) || quantity) + delta)))
    draftRef.current = next
    setDraft(next)
    setError('')
    commit(next)
  }

  return (
    <form className="cart-quantity-editor"
      onSubmit={event => { event.preventDefault(); commit() }}
      onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget)) commit() }}>
      <div className="cart-quantity-controls">
        <button type="button" disabled={disabled || Number(draft) <= 1}
          onClick={() => step(-1)} aria-label={`Giảm số lượng ${name}`}><Minus size={14} /></button>
        <input type="text" inputMode="numeric" value={draft}
          aria-label={`Số lượng ${name}`} aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined} disabled={disabled}
          onChange={event => {
            const value = event.target.value
            draftRef.current = value
            setDraft(value)
            setError('')
            if (/^\d+$/.test(value)) commit(value)
          }}
          onKeyDown={event => {
            if (event.key === 'Escape') { draftRef.current = String(quantity); setDraft(String(quantity)); setError('') }
          }} />
        <button type="button" disabled={disabled || Number(draft) >= max}
          onClick={() => step(1)} aria-label={`Tăng số lượng ${name}`}><Plus size={14} /></button>
      </div>
      {error && <small id={errorId} className="cart-quantity-error" role="alert">{error}</small>}
    </form>
  )
}
