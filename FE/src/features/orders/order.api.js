import { mockOrders } from '../../data/mockOrders'

export async function getOrders() {
  return mockOrders
}

export async function getOrderById(id) {
  return mockOrders.find(o => o.id === id) || null
}

export async function getOrderTracking(id) {
  const order = mockOrders.find(o => o.id === id)
  return order ? order.timeline : null
}
