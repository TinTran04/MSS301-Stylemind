export function formatTimestamp(dateStr) {
  return new Date(dateStr).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

// Compact relative time for the sessions sidebar ("5 phút", "2 giờ", "3 ngày").
export function formatRelativeTime(dateStr) {
  const diffMs = Date.now() - new Date(dateStr).getTime()
  const minutes = Math.floor(diffMs / 60000)
  if (minutes < 1) return 'Vừa xong'
  if (minutes < 60) return `${minutes} phút trước`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} giờ trước`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days} ngày trước`
  return new Date(dateStr).toLocaleDateString('vi-VN')
}

// Maps a MessageResponse from the Python chat service into the shape the chat UI renders.
export function toDisplayMessage(msg) {
  return {
    id: msg.id,
    role: msg.role === 'assistant' ? 'ai' : 'user',
    content: msg.content,
    timestamp: msg.created_at,
    intent: msg.intent || null,
    outfitPlan: msg.metadata?.outfit_plan || null,
  }
}

// Maps an outfit-plan item (snake_case from the Python service) to ProductBlock props.
export function toDisplayProduct(item) {
  return {
    productId: item.product_id,
    name: item.name,
    basePrice: item.base_price,
    imageUrl: item.image_url,
    reason: item.reason,
  }
}
