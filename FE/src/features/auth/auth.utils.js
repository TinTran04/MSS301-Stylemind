export function isCustomer(user) {
  return user?.role === 'customer'
}

export function isAdmin(user) {
  return user?.role === 'admin'
}

export function getInitials(name) {
  return name?.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) || '?'
}
