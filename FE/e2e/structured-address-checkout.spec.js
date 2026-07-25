import { test, expect } from '@playwright/test'

const customerEmail = process.env.PLAYWRIGHT_CUSTOMER_EMAIL
const customerPassword = process.env.PLAYWRIGHT_CUSTOMER_PASSWORD
const testRecipientName = process.env.PLAYWRIGHT_TEST_RECIPIENT_NAME || 'Playwright Test Recipient'
const testPhone = process.env.PLAYWRIGHT_TEST_PHONE || '0912345678'
const testAddressLine = process.env.PLAYWRIGHT_TEST_ADDRESS_LINE || 'Số 1 Đường Kiểm thử'

test.describe('structured Vietnamese address checkout', () => {
  test.skip(
    !customerEmail || !customerPassword,
    'Set PLAYWRIGHT_CUSTOMER_EMAIL and PLAYWRIGHT_CUSTOMER_PASSWORD for the real checkout journey.',
  )

  test('saves a validated address and sends addressId during checkout', async ({ page }, testInfo) => {
    const forbiddenRequests = []
    page.on('request', (request) => {
      const url = request.url()
      if (
        /\/internal\/v1\//.test(url)
        || /(?:auth|product|order|payment|user|cart|notification)-service:/.test(url)
        || /localhost:808[1-9]/.test(url)
      ) {
        forbiddenRequests.push(url)
      }
    })

    await page.goto('/login')
    await page.getByRole('textbox').nth(0).fill(customerEmail)
    await page.getByRole('textbox').nth(1).fill(customerPassword)
    await page.getByRole('button', { name: 'Đăng nhập' }).click()
    await expect(page).toHaveURL(/\/$/)

    await page.goto('/profile')
    await expect(page.getByRole('heading', { name: 'Địa chỉ giao hàng' })).toBeVisible()
    await expect(page.getByText('Hồ sơ phong cách')).toHaveCount(0)
    await page.getByRole('button', { name: 'Thêm địa chỉ' }).click()
    await page.getByPlaceholder('Ví dụ: Nguyễn Minh Khôi').fill(testRecipientName)
    await page.getByPlaceholder('Ví dụ: 09xxxxxxxx').fill(testPhone)
    await page.getByPlaceholder('Số nhà, tên đường, phường/xã...').fill(testAddressLine)

    const province = page.locator('#address-province')
    await expect(province).toBeEnabled()
    await province.selectOption({ index: 1 })
    const ward = page.locator('#address-ward')
    await expect(ward).toBeEnabled()
    await ward.selectOption({ index: 1 })

    const createAddressResponse = page.waitForResponse((response) => (
      response.request().method() === 'POST'
      && response.url().includes('/api/v1/users/addresses')
    ))
    await page.getByRole('button', { name: 'Lưu địa chỉ' }).click()
    expect((await createAddressResponse).status()).toBeLessThan(300)
    await expect(page.getByText(testRecipientName)).toBeVisible()

    await page.goto('/shop')
    const firstProduct = page.locator('a[href^="/products/"]').first()
    await expect(firstProduct).toBeVisible()
    await firstProduct.click()
    await expect(page.locator('h1')).toBeVisible()

    const fieldsets = page.locator('fieldset')
    if (await fieldsets.count()) {
      await fieldsets.nth(0).getByRole('button').first().click()
      if (await fieldsets.count() > 1) {
        await fieldsets.nth(1).getByRole('button').first().click()
      }
    }
    await page.getByRole('button', { name: 'Thêm vào giỏ hàng' }).click()

    await page.goto('/cart')
    await page.getByRole('button', { name: 'Thanh toán' }).click()
    await expect(page).toHaveURL(/\/checkout$/)
    await expect(page.getByRole('heading', { name: 'Địa chỉ giao hàng' })).toBeVisible()

    const addressRequest = page.waitForRequest((request) => (
      request.method() === 'POST'
      && request.url().includes('/api/v1/orders')
    ))
    await page.getByRole('button', { name: 'Đặt hàng' }).click()
    const request = await addressRequest
    const body = request.postDataJSON()
    expect(typeof body.addressId).toBe('string')
    expect(body.addressId.length).toBeGreaterThan(0)
    expect(body.shippingAddress).toBeUndefined()
    await expect(page.getByText('Đơn hàng đã được xác nhận!')).toBeVisible()

    expect(forbiddenRequests).toEqual([])
    await page.screenshot({ path: testInfo.outputPath('structured-address-checkout.png'), fullPage: true })
  })
})
