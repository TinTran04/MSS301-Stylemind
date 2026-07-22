import { expect, test } from '@playwright/test'

function collectForbiddenBrowserCalls(page) {
  const forbiddenCalls = []
  page.on('request', (request) => {
    const url = request.url()
    if (/\/internal\/v1\/|(?:auth|product|order|payment|notification)-service:|localhost:808\d/.test(url)) {
      forbiddenCalls.push(url)
    }
  })
  return forbiddenCalls
}

test('desktop Header opens Search and the real-category Mega Menu through the Gateway', async ({ page }) => {
  const forbiddenCalls = collectForbiddenBrowserCalls(page)

  await page.goto('/')
  await expect(page.getByRole('link', { name: /StyleMind, về trang chủ/ })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Trang chủ', exact: true })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Cửa hàng', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Tìm kiếm sản phẩm' })).toBeVisible()

  const searchTrigger = page.getByRole('button', { name: 'Tìm kiếm sản phẩm' })
  await searchTrigger.click()
  const searchDialog = page.getByRole('dialog', { name: 'Tìm kiếm sản phẩm' })
  await expect(searchDialog).toBeVisible()
  await expect(searchDialog.getByRole('textbox', { name: 'Từ khóa tìm kiếm sản phẩm' })).toBeFocused()
  await page.keyboard.press('Escape')
  await expect(searchDialog).toHaveCount(0)
  await expect(searchTrigger).toBeFocused()

  await searchTrigger.click()
  await searchDialog.getByRole('textbox', { name: 'Từ khóa tìm kiếm sản phẩm' }).fill('áo khoác')
  await searchDialog.getByRole('button', { name: /Tìm/ }).click()
  await expect(page).toHaveURL(/\/shop\?search=%C3%A1o%20kho%C3%A1c/)

  await page.goto('/')

  await page.getByRole('button', { name: 'Mở danh mục Cửa hàng' }).click()
  const categoryDialog = page.getByRole('dialog', { name: 'Danh mục Cửa hàng' })
  await expect(categoryDialog).toBeVisible()
  await expect(categoryDialog.getByText('Danh mục tuyển chọn')).toBeVisible()
  await expect(categoryDialog.locator('a[href^="/shop?category="]').first()).toBeVisible()
  await page.screenshot({ path: 'test-results/customer-header-desktop.png', fullPage: true })

  await categoryDialog.getByRole('button', { name: 'Đóng danh mục' }).click()
  await expect(categoryDialog).toHaveCount(0)
  expect(forbiddenCalls).toEqual([])
})

test('tablet Header uses the compact menu without horizontal overflow', async ({ page }) => {
  await page.setViewportSize({ width: 900, height: 900 })
  await page.goto('/')

  await expect(page.getByRole('button', { name: 'Mở menu điều hướng' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Cửa hàng', exact: true })).toHaveCount(0)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
})

test('mobile Header opens a drawer, shows real category groups, and keeps the layout within the viewport', async ({ page }) => {
  const forbiddenCalls = collectForbiddenBrowserCalls(page)
  await page.setViewportSize({ width: 390, height: 844 })

  await page.goto('/')
  await expect(page.getByRole('button', { name: 'Mở menu điều hướng' })).toBeVisible()
  await page.getByRole('button', { name: 'Mở menu điều hướng' }).click()

  const mobileDialog = page.getByRole('dialog', { name: 'Khám phá StyleMind' })
  await expect(mobileDialog).toBeVisible()
  await expect(mobileDialog.getByRole('link', { name: 'Trang chủ' })).toBeVisible()
  await expect(mobileDialog.locator('details').first()).toBeVisible()
  await mobileDialog.locator('details').first().locator('summary').click()
  await expect(mobileDialog.locator('details').first().locator('a[href^="/shop?category="]').first()).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.screenshot({ path: 'test-results/customer-header-mobile.png', fullPage: true })

  await mobileDialog.getByRole('button', { name: 'Đóng' }).click()
  await expect(mobileDialog).toHaveCount(0)
  expect(forbiddenCalls).toEqual([])
})
