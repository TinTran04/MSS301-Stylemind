import { useState } from 'react'
import clsx from 'clsx'
import { PRODUCT_CATEGORIES } from '../../utils/constants'

const colorPalette = [
  { name: 'Black', value: '#000' },
  { name: 'White', value: '#fff' },
  { name: 'Cream', value: '#F5F0E8' },
  { name: 'Navy', value: '#1B2A4A' },
  { name: 'Grey', value: '#808080' },
  { name: 'Brown', value: '#8B4513' },
  { name: 'Sage', value: '#9CAF88' },
  { name: 'Terracotta', value: '#E2725B' },
]

export default function ProductFilter({ filters, onFilterChange, categories = [] }) {
  const [selectedColors, setSelectedColors] = useState([])
  const displayCategories = categories.length > 0
    ? categories.map((cat) => ({ label: cat.name, value: String(cat.id) }))
    : PRODUCT_CATEGORIES.map((cat) => ({ label: cat, value: cat }))

  const toggleColor = (color) => {
    const updated = selectedColors.includes(color)
      ? selectedColors.filter((c) => c !== color)
      : [...selectedColors, color]
    setSelectedColors(updated)
    onFilterChange({ ...filters, colors: updated })
  }

  return (
    <aside className="w-64 shrink-0 space-y-6">
      <div>
        <h3 className="font-label-md uppercase tracking-wider text-primary mb-3">Categories</h3>
        <div className="space-y-2">
          {displayCategories.map((cat) => (
            <label key={cat.value} className="flex items-center gap-2 text-sm text-on-surface-variant cursor-pointer">
              <input
                type="checkbox"
                checked={filters.category === cat.value}
                onChange={() => onFilterChange({ ...filters, category: filters.category === cat.value ? null : cat.value })}
                className="w-4 h-4 rounded border-outline-variant accent-primary"
              />
              {cat.label}
            </label>
          ))}
        </div>
      </div>

      <div>
        <h3 className="font-label-md uppercase tracking-wider text-primary mb-3">Price Range</h3>
        <div className="flex gap-2">
          <input
            type="number"
            placeholder="Min"
            value={filters.minPrice || ''}
            onChange={(e) => onFilterChange({ ...filters, minPrice: e.target.value ? Number(e.target.value) : null })}
            className="w-1/2 bg-transparent border-b border-outline-variant py-1.5 text-sm focus:border-tertiary-container outline-none transition-colors"
          />
          <input
            type="number"
            placeholder="Max"
            value={filters.maxPrice || ''}
            onChange={(e) => onFilterChange({ ...filters, maxPrice: e.target.value ? Number(e.target.value) : null })}
            className="w-1/2 bg-transparent border-b border-outline-variant py-1.5 text-sm focus:border-tertiary-container outline-none transition-colors"
          />
        </div>
      </div>

      <div>
        <h3 className="font-label-md uppercase tracking-wider text-primary mb-3">Color</h3>
        <div className="flex flex-wrap gap-2">
          {colorPalette.map((color) => (
            <button
              key={color.name}
              onClick={() => toggleColor(color.name)}
              className={clsx(
                'w-7 h-7 rounded-full border-2 transition-all',
                selectedColors.includes(color.name)
                  ? 'border-tertiary-container scale-110'
                  : 'border-outline-variant/30 hover:border-outline-variant'
              )}
              style={{ backgroundColor: color.value }}
              title={color.name}
            />
          ))}
        </div>
      </div>

      <div>
        <h3 className="font-label-md uppercase tracking-wider text-primary mb-3">Material</h3>
        <div className="space-y-2">
          {['Silk', 'Cotton', 'Wool', 'Linen', 'Leather', 'Cashmere'].map((mat) => (
            <label key={mat} className="flex items-center gap-2 text-sm text-on-surface-variant cursor-pointer">
              <input
                type="checkbox"
                className="w-4 h-4 rounded border-outline-variant accent-primary"
              />
              {mat}
            </label>
          ))}
        </div>
      </div>
    </aside>
  )
}
