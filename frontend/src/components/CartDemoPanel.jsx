import React, { useState } from 'react'
import { useCart } from '../contexts/CartContext'

export default function CartDemoPanel() {
  const { demo, selectDemo, ready, mergeCart } = useCart()
  const [userId, setUserId] = useState('')
  const [busy, setBusy] = useState(false)
  const [feedback, setFeedback] = useState('')
  if (!import.meta.env.DEV || !demo) return null
  const run = async action => {
    setBusy(true)
    try {
      const result = await action()
      setFeedback(result.success ? 'Đã cập nhật giỏ hàng giả lập.' : result.message)
    } finally { setBusy(false) }
  }
  return <section className="card" aria-label="Giả lập giỏ hàng">
    <div className="order-actions">
      <button disabled={!ready || busy} onClick={() => run(() => selectDemo('a0000000-0000-0000-0000-000000000002'))}>Khách demo</button>
      <button disabled={!ready || busy} onClick={() => run(() => selectDemo('a0000000-0000-0000-0000-000000000001'))}>Admin demo</button>
      <button disabled={!ready || busy} onClick={() => run(() => selectDemo('a0000000-0000-0000-0000-000000000003'))}>Khách chưa xác minh</button>
    </div>
    <p>Chế độ kiểm thử Cart: {demo.userId ? 'User ' + demo.userId : 'Khách — lưu session'}</p>
    <form onSubmit={event => { event.preventDefault(); run(() => selectDemo(userId.trim())) }}>
      <label htmlFor="cart-demo-user">UUID tài khoản ACTIVE có sẵn</label>
      <input id="cart-demo-user" required value={userId} onChange={event => setUserId(event.target.value)} />
      <button type="submit" disabled={!ready || busy}>Giả lập user</button>
      <button type="button" disabled={!ready || busy} onClick={() => run(() => selectDemo(null))}>Về khách</button>
      <button type="button" disabled={!ready || busy} onClick={() => run(mergeCart)}>Gộp lại giỏ khách</button>
    </form>
    {feedback && <p role="status">{feedback}</p>}
  </section>
}
