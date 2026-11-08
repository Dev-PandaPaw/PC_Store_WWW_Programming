// Storefront identity comes from its login session, never the local demo admin.
export function isDevelopmentAdminPath(url) {
  return /^\/(?:api\/)?(?:admin|staff)(?:\/|$)/.test(String(url || '').split('?')[0])
}
