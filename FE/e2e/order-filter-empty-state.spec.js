import { test, expect } from '@playwright/test'

const customerEmail = process.env.PLAYWRIGHT_CUSTOMER_EMAIL
const customerPassword = process.env.PLAYWRIGHT_CUSTOMER_PASSWORD

const order = {
  id: 'order-filter-test',
  orderStatus: 'PROCESSING',
  paymentMethod: 'cod',
  createdAt: '2026-07-22T01:31:00Z',
  totalAmount: 219000,
  itemCount: 1,
  items: [],
}

test('keeps order status filters visible when the selected status is empty', async ({ page }) => {
  if (process.env.PLAYWRIGHT_MOBILE === 'true') await page.setViewportSize({ width: 390, height: 844 })

  await page.addInitScript(() => {
    localStorage.setItem('auth_token', 'playwright-token')
    localStorage.setItem('auth_user', JSON.stringify({ id: 'customer-1', email: 'customer@example.com', role: 'CUSTOMER' }))
  })

  const requests = []
  await page.route('**/api/v1/**', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname === '/api/v1/orders') {
      requests.push(url.search)
      const isFiltered = url.searchParams.get('status') === 'PROCESSING'
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: isFiltered
            ? { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, first: true, last: true, numberOfElements: 0 }
            : { content: [order], page: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true, numberOfElements: 1 },
        }),
      })
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
  await expect(page.getByRole('button', { name: 'Tất cả' })).toBeVisible()

  await page.getByRole('button', { name: 'Đang xử lý' }).click()
  await expect(page.getByText('Không có đơn hàng trong trạng thái này.')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Tất cả' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Đang xử lý' })).toHaveAttribute('aria-pressed', 'true')
  await expect(page.getByRole('navigation', { name: 'Phân trang đơn hàng' })).toHaveCount(0)
  expect(requests.at(-1)).toContain('page=0')
  expect(requests.at(-1)).toContain('status=PROCESSING')

  await page.getByRole('button', { name: 'Tất cả' }).click()
  await expect(page.getByText('Mã đơn hàng: order-filter-test')).toBeVisible()
  expect(requests.at(-1)).toContain('page=0')
  expect(requests.at(-1)).not.toContain('status=')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true)
})

test('keeps order status filters visible when the order request fails', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('auth_token', 'playwright-token')
    localStorage.setItem('auth_user', JSON.stringify({ id: 'customer-1', email: 'customer@example.com', role: 'CUSTOMER' }))
  })

  await page.route('**/api/v1/**', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname === '/api/v1/orders') {
      await route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ success: false, message: 'Service unavailable' }) })
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
  await expect(page.getByRole('button', { name: 'Tất cả' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Đang xử lý' })).toBeVisible()
  await expect(page.getByRole('alert')).toContainText('Không thể tải đơn hàng.')
})

test.describe('real customer empty status regression', () => {
  test.skip(
    !customerEmail || !customerPassword,
    'Set PLAYWRIGHT_CUSTOMER_EMAIL and PLAYWRIGHT_CUSTOMER_PASSWORD for the real customer journey.',
  )

  test('keeps filters visible for an empty status returned by the Gateway', async ({ page }) => {
    const forbiddenRequests = []
    const consoleErrors = []
    const pageErrors = []

    page.on('request', (request) => {
      const url = request.url()
      if (/\/internal\/v1\//.test(url) || /(?:auth|product|order|payment|user|cart|notification)-service:/.test(url) || /localhost:808[1-9]/.test(url)) {
        forbiddenRequests.push(url)
      }
    })
    page.on('console', (message) => {
      if (message.type() === 'error') consoleErrors.push(message.text())
    })
    page.on('pageerror', (error) => pageErrors.push(error.message))

    await page.goto('/login')
    await page.locator('input[type="email"]').fill(customerEmail)
    await page.locator('input[type="password"]').fill(customerPassword)
    await page.getByRole('button', { name: 'Đăng nhập' }).click()
    await expect(page).toHaveURL(/\/$/)
    await page.goto('/orders')
    await expect(page.getByRole('heading', { name: 'Đơn hàng của tôi' })).toBeVisible()

    const statusTabs = [
      ['Đang xử lý', 'PROCESSING'],
      ['Đang giao', 'SHIPPED'],
      ['Đã giao thành công', 'COMPLETED'],
    ]
    let emptyStatusLabel = null

    for (const [label, status] of statusTabs) {
      const responsePromise = page.waitForResponse((response) => {
        const url = new URL(response.url())
        return response.request().method() === 'GET'
          && url.pathname === '/api/v1/orders'
          && url.searchParams.get('page') === '0'
          && url.searchParams.get('status') === status
          && response.status() === 200
      })
      await page.getByRole('button', { name: label }).click()
      const response = await responsePromise
      const body = await response.json()
      if (body?.data?.totalElements === 0) {
        emptyStatusLabel = label
        break
      }
    }

    test.skip(!emptyStatusLabel, 'The real customer has no empty status to exercise this scenario.')
    await expect(page.getByText('Không có đơn hàng trong trạng thái này.')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Tất cả' })).toBeVisible()
    await expect(page.getByRole('button', { name: emptyStatusLabel })).toHaveAttribute('aria-pressed', 'true')
    await expect(page.getByRole('navigation', { name: 'Phân trang đơn hàng' })).toHaveCount(0)
    expect(forbiddenRequests).toEqual([])
    expect(consoleErrors).toEqual([])
    expect(pageErrors).toEqual([])
  })
})
