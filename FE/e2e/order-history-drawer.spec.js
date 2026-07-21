import { test, expect } from '@playwright/test'

const listOrder = {
  id: 'order-a',
  orderStatus: 'PROCESSING',
  paymentMethod: 'cod',
  createdAt: '2026-07-22T01:31:00Z',
  updatedAt: '2026-07-22T01:31:00Z',
  totalAmount: 1595000,
  items: [
    { id: 'item-a', productName: 'Áo sơ mi Oxford', primaryImageUrl: '/shirt.jpg', priceAtPurchase: 598000, quantity: 2 },
    { id: 'item-b', productName: 'Quần kaki', primaryImageUrl: '/pants.jpg', priceAtPurchase: 997000, quantity: 1 },
  ],
}

const detailOrder = {
  ...listOrder,
  subtotalAmount: 1450000,
  shippingFee: 0,
  taxAmount: 145000,
  shippingAddress: 'Số 1 Đường Kiểm thử, Hà Nội',
  shippingRecipientName: 'Người nhận kiểm thử',
  shippingPhone: '0900000000',
  statusHistory: [],
}

test('customer order history opens details only from the dedicated action', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('auth_token', 'playwright-token')
    localStorage.setItem('auth_user', JSON.stringify({ id: 'customer-1', email: 'customer@example.com', role: 'CUSTOMER' }))
  })

  let detailRequests = 0
  const listRequests = []
  await page.route('**/api/v1/**', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname === '/api/v1/orders/order-a') {
      detailRequests += 1
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: detailOrder }) })
      return
    }
    if (url.pathname === '/api/v1/orders') {
      listRequests.push(url.search)
      const pageNumber = Number(url.searchParams.get('page') || 0)
      const content = pageNumber === 0 ? [listOrder] : [{ ...listOrder, id: 'order-b' }]
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { content, page: pageNumber, size: 10, totalElements: 21, totalPages: 3, first: pageNumber === 0, last: pageNumber === 2, numberOfElements: content.length } }) })
      return
    }
    if (url.pathname === '/api/v1/auth/me') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { id: 'customer-1', email: 'customer@example.com', role: 'CUSTOMER' } }) })
      return
    }
    if (url.pathname === '/api/v1/cart' || url.pathname === '/api/v1/cart/merge') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { items: [], totalAmount: 0 } }) })
      return
    }
    await route.continue()
  })

  await page.goto('/orders')
  await expect(page.getByRole('heading', { name: 'Đơn hàng của tôi' })).toBeVisible()
  await expect(page.getByText('Mã đơn hàng: order-a')).toBeVisible()
  await expect(page.getByText('Nội dung kiện hàng')).toHaveCount(0)
  expect(detailRequests).toBe(0)
  expect(listRequests[0]).toContain('page=0')
  expect(listRequests[0]).toContain('size=10')

  await page.getByRole('button', { name: 'Trang 2' }).click()
  await expect(page.getByText('Mã đơn hàng: order-b')).toBeVisible()
  expect(listRequests.at(-1)).toContain('page=1')
  await page.getByRole('button', { name: 'Trang 1' }).click()
  await expect(page.getByText('Mã đơn hàng: order-a')).toBeVisible()

  await page.getByRole('button', { name: 'Xem chi tiết đơn hàng order-a' }).click()

  await expect(page.getByRole('dialog')).toBeVisible()
  await expect(page.getByRole('dialog')).toContainText('Nội dung kiện hàng')
  await expect(page.getByRole('dialog')).toContainText('Số 1 Đường Kiểm thử, Hà Nội')
  await expect(page.getByRole('dialog')).toBeFocused()
  expect(detailRequests).toBe(1)

  const closeButton = page.getByRole('button', { name: 'Đóng' })
  await closeButton.focus()
  await page.keyboard.press('Tab')
  await expect(closeButton).toBeFocused()

  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Xem chi tiết đơn hàng order-a' })).toBeFocused()
})
