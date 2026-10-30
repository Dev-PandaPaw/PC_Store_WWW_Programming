import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Auto CSRF token header
http.interceptors.request.use((config) => {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/)
  if (match && !config.headers['X-XSRF-TOKEN']) {
    config.headers['X-XSRF-TOKEN'] = decodeURIComponent(match[1])
  }
  return config
})

export default http

export async function ensureCsrfToken() {
  if (!document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)) await http.get('/csrf')
}
