export function createCheckoutAttemptKey() {
  return globalThis.crypto?.randomUUID?.()
    || `checkout_${Date.now()}_${Math.random().toString(36).slice(2)}`
}

export function isCurrentCheckoutAttempt(state, attemptId, orderId = null) {
  if (!state || state.attemptId !== attemptId) return false
  return orderId == null || state.activeOrderId === orderId
}
