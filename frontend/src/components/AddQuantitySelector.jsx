import React, { useEffect, useId, useState } from 'react'
import { Minus, Plus } from 'lucide-react'
import { parseStrictIntegerQuantity } from '../utils/cartAddIdempotency'
import './CartQuantity.css'

export default function AddQuantitySelector({
  productName,
  value,
  onChange,
  max = 99,
  disabled = false,
  compact = false,
  error = '',
  onErrorChange,
}) {
  const effectiveMax = Math.max(1, Math.min(99, max ?? 99))
  const [draft, setDraft] = useState(String(value))
  const errorId = useId()

  useEffect(() => {
    setDraft(String(value))
  }, [value])

  const applyDraft = raw => {
    setDraft(raw)
    const check = parseStrictIntegerQuantity(raw, effectiveMax)
    if (check.valid) {
      onErrorChange?.('')
      onChange(check.value)
    } else {
      onErrorChange?.(check.message)
    }
  }

  const step = delta => {
    if (disabled) return
    const currentCheck = parseStrictIntegerQuantity(draft, effectiveMax)
    const base = currentCheck.valid ? currentCheck.value : value
    const next = Math.min(effectiveMax, Math.max(1, base + delta))
    setDraft(String(next))
    onErrorChange?.('')
    onChange(next)
  }

  const currentCheck = parseStrictIntegerQuantity(draft, effectiveMax)

  return (
    <div className={`add-quantity-selector${compact ? ' add-quantity-selector--compact' : ''}`}>
      <div className="cart-quantity-controls">
        <button
          type="button"
          disabled={disabled || (currentCheck.valid && currentCheck.value <= 1)}
          onClick={() => step(-1)}
          aria-label={`Giảm số lượng chọn mua ${productName}`}
        >
          <Minus size={compact ? 12 : 14} />
        </button>
        <input
          type="text"
          inputMode="numeric"
          value={draft}
          disabled={disabled}
          aria-label={`Số lượng thêm ${productName}`}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined}
          onChange={event => applyDraft(event.target.value)}
          onBlur={() => {
            const check = parseStrictIntegerQuantity(draft, effectiveMax)
            if (!check.valid) {
              onErrorChange?.(check.message)
            }
          }}
        />
        <button
          type="button"
          disabled={disabled || (currentCheck.valid && currentCheck.value >= effectiveMax)}
          onClick={() => step(1)}
          aria-label={`Tăng số lượng chọn mua ${productName}`}
        >
          <Plus size={compact ? 12 : 14} />
        </button>
      </div>
      {error && !compact && (
        <small id={errorId} className="cart-quantity-error" role="alert">
          {error}
        </small>
      )}
    </div>
  )
}
