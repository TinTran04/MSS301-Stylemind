import assert from 'node:assert/strict'
import test from 'node:test'

import {
  buildSearchPath,
  buildShopCategoryPath,
  groupCategories,
} from './headerNavigation.js'

test('buildSearchPath trims a query and preserves URL-safe characters', () => {
  assert.equal(buildSearchPath('  ao khoac & blazer  '), '/shop?search=ao%20khoac%20%26%20blazer')
  assert.equal(buildSearchPath('   '), null)
})

test('buildShopCategoryPath uses the existing numeric category query contract', () => {
  assert.equal(buildShopCategoryPath({ id: 12, name: 'Váy' }), '/shop?category=12')
  assert.equal(buildShopCategoryPath({ id: null }), '/shop')
})

test('groupCategories keeps child categories with their real parent and preserves standalone categories', () => {
  const groups = groupCategories([
    { id: 1, name: 'Áo', parentId: null, slug: 'ao' },
    { id: 2, name: 'Áo thun', parentId: 1, slug: 'ao-thun' },
    { id: 3, name: 'Nữ', parentId: null, slug: 'nu' },
  ])

  assert.deepEqual(groups, [
    {
      id: 'parent-1',
      label: 'Áo',
      parent: { id: 1, name: 'Áo', parentId: null, slug: 'ao' },
      categories: [{ id: 2, name: 'Áo thun', parentId: 1, slug: 'ao-thun' }],
    },
    {
      id: 'explore',
      label: 'Khám phá',
      parent: null,
      categories: [{ id: 3, name: 'Nữ', parentId: null, slug: 'nu' }],
    },
  ])
})
