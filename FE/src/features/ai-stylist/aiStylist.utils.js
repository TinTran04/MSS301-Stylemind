export function formatTimestamp(dateStr) {
  return new Date(dateStr).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}

export function getTypingDelay() {
  return 1000 + Math.random() * 2000
}
