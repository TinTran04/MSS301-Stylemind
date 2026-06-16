import { mockInventory } from '../../data/mockInventory'

export async function getInventory() {
  return mockInventory
}

export async function updateStock(productId, quantity) {
  const item = mockInventory.find(i => i.productId === productId)
  if (item) {
    item.currentStock = quantity
    item.lastUpdated = new Date().toISOString()
  }
  return item
}

export async function getInventoryByProduct(productId) {
  return mockInventory.find(i => i.productId === productId) || null
}
