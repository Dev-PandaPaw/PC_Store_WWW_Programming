export function proposalOption(result, proposal) {
  return result?.advice?.build_search?.[proposal.group]?.[proposal.optionIndex] || null
}
export function proposalChanges(proposal, cartItems) {
  const old = new Map(cartItems.map(item => [item.id, item]))
  return {
    removed: proposal.removedProductIds.map(id => old.get(id)?.name || id),
    added: proposal.items.filter(item => !old.has(item.productId)),
    changed: proposal.items.filter(item => old.has(item.productId) && old.get(item.productId).quantity !== item.quantity),
  }
}
export function canConfirm(proposal, acknowledged, stale, busy) {
  return Boolean(proposal && !stale && !busy &&
    Date.parse(proposal.expiresAt) > Date.now())
}
export const buildStatus = {
  found: 'Có cấu hình đã qua các bước kiểm tra',
  needs_verification: 'Có gợi ý cần bổ sung dữ liệu trước khi đặt hàng',
  no_solution: 'Chưa có cấu hình phù hợp ngân sách',
  search_limited: 'Chưa tìm xong cấu hình phù hợp',
  invalid_selection: 'Cần kiểm tra lại các món đang chọn',
  over_budget: 'Các món giữ lại đã vượt ngân sách',
}
