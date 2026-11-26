export const ADMIN_HOME = '/admin/products'

export function hasAdminRole(user) {
  return Array.isArray(user?.roles) && user.roles.some(role => role === 'ROLE_ADMIN' || role === 'ADMIN')
}
