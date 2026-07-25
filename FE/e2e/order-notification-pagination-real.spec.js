import { test, expect } from '@playwright/test'

const customerEmail = process.env.PLAYWRIGHT_CUSTOMER_EMAIL
const customerPassword = process.env.PLAYWRIGHT_CUSTOMER_PASSWORD

test.describe('real customer pagination contracts', () => {
  test.skip(
    !customerEmail || !customerPassword,
    'Set PLAYWRIGHT_CUSTOMER_EMAIL and PLAYWRIGHT_CUSTOMER_PASSWORD for the real customer journey.',
  )

  test('loads existing orders and notifications through the Gateway', async ({ page }, testInfo) => {
    const forbiddenRequests = []
    const listResponses = new Map()
    const consoleErrors = []
    const pageErrors = []

    page.on('request', (request) => {
      const url = request.url()
      if (/\/internal\/v1\//.test(url) || /(?:auth|product|order|payment|user|cart|notification)-service:/.test(url) || /localhost:808[1-9]/.test(url)) {
        forbiddenRequests.push(url)
      }
    })
    page.on('response', (response) => {
      const url = new URL(response.url())
      if (response.request().method() === 'GET' && ['/api/v1/orders', '/api/v1/notifications', '/api/v1/notifications/unread-count'].includes(url.pathname)) {
        listResponses.set(url.pathname, { status: response.status(), query: url.searchParams })
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

    const ordersResponsePromise = page.waitForResponse((response) => {
      const url = new URL(response.url())
      return response.request().method() === 'GET'
        && url.pathname === '/api/v1/orders'
        && response.status() === 200
    })
    await page.goto('/orders')
    await expect(page.getByRole('heading', { name: 'Đơn hàng của tôi' })).toBeVisible()
    await expect(page.getByText('Mã đơn hàng:').first()).toBeVisible()

    const ordersResponseBody = await ordersResponsePromise.then((response) => response.json())
    const ordersResponse = listResponses.get('/api/v1/orders')
    expect(ordersResponse?.status).toBe(200)
    expect(ordersResponse?.query.get('page')).toBe('0')
    expect(ordersResponse?.query.get('size')).toBe('10')
    expect(Array.isArray(ordersResponseBody?.data?.content)).toBe(true)
    expect(ordersResponseBody.data.content.length).toBeGreaterThan(0)

    const notificationsResponsePromise = page.waitForResponse((response) => {
      const url = new URL(response.url())
      return response.request().method() === 'GET'
        && url.pathname === '/api/v1/notifications'
        && response.status() === 200
    })
    const unreadResponsePromise = page.waitForResponse((response) => {
      const url = new URL(response.url())
      return response.request().method() === 'GET'
        && url.pathname === '/api/v1/notifications/unread-count'
        && response.status() === 200
    })
    await page.goto('/notifications')
    await expect(page.getByRole('heading', { name: 'Thông báo' })).toBeVisible()

    const [notificationsResponseBody, unreadResponseBody] = await Promise.all([
      notificationsResponsePromise.then((response) => response.json()),
      unreadResponsePromise.then((response) => response.json()),
    ])
    const notificationsResponse = listResponses.get('/api/v1/notifications')
    const unreadResponse = listResponses.get('/api/v1/notifications/unread-count')
    expect(notificationsResponse?.status).toBe(200)
    expect(notificationsResponse?.query.get('page')).toBe('0')
    expect(notificationsResponse?.query.get('size')).toBe('10')
    expect(Array.isArray(notificationsResponseBody?.data?.content)).toBe(true)
    expect(notificationsResponseBody.data.content.length).toBeGreaterThan(0)
    expect(unreadResponse?.status).toBe(200)
    expect(typeof unreadResponseBody?.data?.unreadCount).toBe('number')

    expect(forbiddenRequests).toEqual([])
    expect(consoleErrors).toEqual([])
    expect(pageErrors).toEqual([])
    await page.screenshot({ path: testInfo.outputPath('customer-pagination-runtime.png'), fullPage: true })
  })
})
