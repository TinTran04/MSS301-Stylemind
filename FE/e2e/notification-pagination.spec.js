import { test, expect } from '@playwright/test'

test('customer notifications use bounded numbered pagination and read filters', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('auth_token', 'playwright-token')
    localStorage.setItem('auth_user', JSON.stringify({ id: 'customer-1', email: 'customer@example.com', role: 'CUSTOMER' }))
  })

  const requests = []
  await page.route('**/api/v1/**', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname === '/api/v1/notifications/unread-count') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { unreadCount: 1 } }) })
      return
    }
    if (url.pathname === '/api/v1/notifications') {
      requests.push(url.search)
      const pageNumber = Number(url.searchParams.get('page') || 0)
      const content = [{ id: pageNumber + 1, title: 'Thông báo kiểm thử', type: 'SYSTEM', status: 'SENT', read: pageNumber > 0, createdAt: '2026-07-22T10:00:00Z' }]
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { content, page: pageNumber, size: 10, totalElements: 21, totalPages: 3, first: pageNumber === 0, last: pageNumber === 2, numberOfElements: 1 } }) })
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

  await page.goto('/notifications')
  await expect(page.getByRole('heading', { name: 'Thông báo' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Trang 1' })).toHaveAttribute('aria-current', 'page')
  expect(requests[0]).toContain('page=0')
  expect(requests[0]).toContain('size=10')

  await page.getByRole('button', { name: 'Trang 2' }).click()
  await expect(page.getByRole('button', { name: 'Trang 2' })).toHaveAttribute('aria-current', 'page')
  expect(requests.at(-1)).toContain('page=1')

  await page.getByRole('button', { name: 'Chưa đọc' }).click()
  await expect(page.getByRole('button', { name: 'Trang 1' })).toHaveAttribute('aria-current', 'page')
  expect(requests.at(-1)).toContain('page=0')
  expect(requests.at(-1)).toContain('read=false')
})
