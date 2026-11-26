import http from './http.js'

export async function getVectorDocuments({ offset, limit, signal }) {
  const { data } = await http.get('/admin/vector/documents', { params: { offset, limit }, signal, timeout: 15_000 })
  return data
}

export async function syncVectorDatabase() {
  const { status, data } = await http.post('/admin/vector/reindex', {}, { timeout: 600_000 })
  if (status !== 200 || data?.status !== 'SUCCESS' ||
      !Number.isInteger(data.productCount) || data.productCount < 0 ||
      !Number.isInteger(data.documentCount) || data.documentCount < 0) {
    throw new Error('Máy chủ chưa xác nhận kết quả đồng bộ vector.')
  }
  return data
}
