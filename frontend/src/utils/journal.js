import gaming from '../assets/journal/gaming.jpg'
import components from '../assets/journal/components.jpg'
import workspace from '../assets/journal/workspace.jpg'
import monitor from '../assets/journal/monitor.jpg'
import keyboard from '../assets/journal/keyboard.jpg'
import laptop from '../assets/journal/laptop.jpg'
import buildPromotion from '../assets/promotions/build-pc-wide.webp'
import collectionPromotion from '../assets/promotions/pc-collection.png'
import i5Promotion from '../assets/promotions/pc-i5-rtx5060.webp'

const covers = {
  'pc-gaming': gaming, cpu: components, mainboard: components, vga: gaming,
  'pc-office': workspace, 'pc-workstation': workspace, monitor,
  cooling: gaming, case: gaming, psu: components, storage: laptop,
  ram: components, keyboard, mouse: keyboard,
  'promo-build': buildPromotion, 'promo-collection': collectionPromotion, 'promo-i5': i5Promotion,
}
export const articleCover = article => covers[article.imageCategory] || workspace
export const articleDate = article => new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC',
}).format(new Date(`${article.date || '2026-10-25'}T00:00:00Z`))
export const normalizeJournalSearch = text => text.toLocaleLowerCase('vi')
  .normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd')
