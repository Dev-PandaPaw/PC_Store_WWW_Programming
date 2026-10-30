// The login response already contains the committed merge. Never merge it a second time.
export function splitLoginResponse(data) {
  const { cart, ...user } = data
  return { user, loginCart: cart ? { userId: user.id, cart } : null }
}

export function cartFromLogin(user, loginCart) {
  return user?.id && loginCart?.userId === user.id ? loginCart.cart : null
}
