export function getAdminNetRevenue(data) {
  const value = data?.netRevenue ?? data?.totalRevenue
  return Number.isFinite(Number(value)) ? Number(value) : 0
}

export function getAdminRevenueMetrics(data) {
  const numeric = (value) => Number.isFinite(Number(value)) ? Number(value) : 0

  return {
    vatCollected: numeric(data?.vatCollected),
    shippingFeesCollected: numeric(data?.shippingFeesCollected),
    grossCustomerPayments: numeric(data?.grossCustomerPayments),
    refundAmount: numeric(data?.refundAmount),
    recognizedOrderCount: numeric(data?.recognizedOrderCount),
  }
}
