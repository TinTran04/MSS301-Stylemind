import { useState } from 'react'
import { Search, Mail, Star } from 'lucide-react'
import Badge from '../../components/common/Badge'
import { mockCustomers } from '../../data/mockUsers'
import { formatCurrency } from '../../utils/formatCurrency'

const tierColors = {
  Platinum: 'bg-ai-lavender text-ai-indigo',
  Gold: 'bg-tertiary-fixed/30 text-tertiary',
  Silver: 'bg-surface-container-high text-on-surface-variant',
}

export default function CustomerManagementPage() {
  const [search, setSearch] = useState('')
  const [selectedCustomer, setSelectedCustomer] = useState(mockCustomers[0])

  const filtered = mockCustomers.filter((c) =>
    c.name.toLowerCase().includes(search.toLowerCase()) || c.email.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-headline-md text-primary">Customers</h1>
        <p className="text-sm text-on-surface-variant mt-1">{mockCustomers.length} customers</p>
      </div>

      <div className="flex gap-6">
        {/* Table */}
        <div className="flex-1 bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
          <div className="p-4 border-b border-outline-variant/20">
            <div className="relative max-w-sm">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant" />
              <input type="text" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search customers..." className="w-full pl-9 pr-4 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none" />
            </div>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-surface-container-low/50">
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Customer</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Tier</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Orders</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Total Spent</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Return Rate</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/5">
                {filtered.map((c) => (
                  <tr key={c.id} className={`hover:bg-surface-container-high/30 cursor-pointer ${selectedCustomer?.id === c.id ? 'bg-surface-container-low' : ''}`} onClick={() => setSelectedCustomer(c)}>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center text-xs font-semibold text-on-primary-container">{c.name[0]}</div>
                        <div>
                          <p className="text-sm font-medium text-primary">{c.name}</p>
                          <p className="text-xs text-on-surface-variant">{c.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3"><span className={`text-xs font-medium px-2 py-0.5 rounded-full ${tierColors[c.tier]}`}>{c.tier}</span></td>
                    <td className="px-4 py-3 text-sm text-on-surface">{c.orders}</td>
                    <td className="px-4 py-3 text-sm text-primary font-medium">{formatCurrency(c.totalSpent)}</td>
                    <td className="px-4 py-3 text-sm text-on-surface-variant">{(c.returnRate * 100).toFixed(1)}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Customer Insight Panel */}
        {selectedCustomer && (
          <div className="w-80 shrink-0">
            <div className="bg-surface-container-lowest rounded-xl p-5 ambient-shadow sticky top-24">
              <div className="flex flex-col items-center text-center mb-4">
                <div className="w-14 h-14 rounded-full bg-primary-container flex items-center justify-center text-lg font-semibold text-on-primary-container mb-2">{selectedCustomer.name[0]}</div>
                <h3 className="font-title-lg text-primary">{selectedCustomer.name}</h3>
                <span className={`text-xs font-medium px-2 py-0.5 rounded-full mt-1 ${tierColors[selectedCustomer.tier]}`}>{selectedCustomer.tier} Member</span>
              </div>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between"><span className="text-on-surface-variant">Total Spent</span><span className="font-medium text-primary">{formatCurrency(selectedCustomer.totalSpent)}</span></div>
                <div className="flex justify-between"><span className="text-on-surface-variant">Orders</span><span className="font-medium text-primary">{selectedCustomer.orders}</span></div>
                <div className="flex justify-between"><span className="text-on-surface-variant">Return Rate</span><span className="font-medium text-primary">{(selectedCustomer.returnRate * 100).toFixed(1)}%</span></div>
                <div className="flex justify-between"><span className="text-on-surface-variant">Member Since</span><span className="text-primary">{selectedCustomer.joinDate}</span></div>
              </div>
              {selectedCustomer.styleDNA && selectedCustomer.styleDNA.length > 0 && (
                <div className="mt-4">
                  <h4 className="font-label-sm uppercase text-on-surface-variant mb-2">Style DNA</h4>
                  <div className="flex flex-wrap gap-2">
                    {selectedCustomer.styleDNA.map((s) => (
                      <span key={s} className="bg-ai-lavender/30 text-ai-indigo text-xs px-2 py-0.5 rounded-full">{s}</span>
                    ))}
                  </div>
                </div>
              )}
              <button className="w-full mt-4 bg-surface-container text-on-surface rounded-lg py-2 text-xs font-medium hover:bg-surface-container-high transition-colors flex items-center justify-center gap-2">
                <Mail size={14} /> Send Message
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
