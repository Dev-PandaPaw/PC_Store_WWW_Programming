import React, { useEffect, useId, useRef, useState } from 'react'
import { ChevronDown, Check } from 'lucide-react'
import { filterAddresses, selectedAddress } from '../utils/addresses'

export default function AddressSelect({ label, placeholder, options, value, onChange, disabled = false }) {
  const id = useId()
  const input = useRef(null)
  const [query, setQuery] = useState(value)
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const matches = filterAddresses(options, query === value ? '' : query)
  useEffect(() => { if (value) setQuery(value) }, [value])
  useEffect(() => {
    input.current?.setCustomValidity(value ? '' : `Vui lòng chọn ${label.toLowerCase()} từ danh sách.`)
  }, [value, label])
  useEffect(() => {
    if (active >= 0) document.getElementById(`${id}-option-${active}`)?.scrollIntoView({ block: 'nearest' })
  }, [active, id])
  const choose = (option, focus = true) => {
    setQuery(option.name)
    if (value !== option.name) onChange(option.name)
    if (focus) input.current?.focus()
    setOpen(false); setActive(-1)
    input.current?.setCustomValidity('')
  }
  return <div className="address-select" onBlur={event => {
    if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false)
  }}>
    <label htmlFor={id}>{label}<span aria-hidden="true"> *</span></label>
    <div className="address-select__input">
      <input id={id} ref={input} role="combobox" aria-autocomplete="list" aria-expanded={open && !disabled}
        aria-controls={`${id}-list`} aria-activedescendant={open && active >= 0 ? `${id}-option-${active}` : undefined}
        autoComplete="off" placeholder={placeholder} value={query} required disabled={disabled}
        onFocus={() => { setOpen(true); setActive(-1) }}
        onChange={event => {
          setQuery(event.target.value); onChange(''); setOpen(true); setActive(-1)
          event.target.setCustomValidity(`Vui lòng chọn ${label.toLowerCase()} từ danh sách.`)
        }}
        onBlur={event => {
          if (event.relatedTarget?.getAttribute('role') === 'option') return
          const exact = selectedAddress(options, query)
          if (exact) choose(exact, false)
        }}
        onKeyDown={event => {
          if (event.key === 'Escape') { event.preventDefault(); setOpen(false); setActive(-1) }
          if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault(); setOpen(true)
            setActive(index => !matches.length ? -1 : event.key === 'ArrowDown' ? Math.min(index + 1, matches.length - 1) : Math.max(index - 1, 0))
          }
          if (event.key === 'Enter' && open) {
            event.preventDefault()
            const option = matches[active] || selectedAddress(options, query) || (matches.length === 1 ? matches[0] : null)
            if (option) choose(option)
          }
        }} />
      <ChevronDown size={16} aria-hidden="true" />
    </div>
    {open && !disabled && <ul id={`${id}-list`} role="listbox" aria-label={label} className="address-select__list">
      {matches.length ? matches.map((option, index) => <li key={option.code} id={`${id}-option-${index}`} role="option"
        aria-selected={value === option.name} tabIndex={-1} className={active === index ? 'is-active' : ''}
        onMouseDown={event => event.preventDefault()} onClick={() => choose(option)}>
        <span>{option.name}</span>{value === option.name && <Check size={15} aria-hidden="true" />}
      </li>) : <li className="address-select__empty" role="presentation">Không tìm thấy kết quả phù hợp.</li>}
    </ul>}
  </div>
}
