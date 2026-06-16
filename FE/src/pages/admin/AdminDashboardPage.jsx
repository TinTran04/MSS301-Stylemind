import { useState, useEffect } from 'react'
import { DollarSign, TrendingUp, Brain, MousePointerClick } from 'lucide-react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import MetricCard from '../../components/admin/MetricCard'
import ChartCard from '../../components/admin/ChartCard'
import StatusBadge from '../../components/admin/StatusBadge'
import { getDashboardMetrics, getTopProducts, getCTRData } from '../../features/analytics/analytics.api'
import { formatCurrency, formatNumber } from '../../utils/formatCurrency'

export default function AdminDashboardPage() {
  const [metrics, setMetrics] = useState(null)
  const [products, setProducts] = useState([])
  const [ctrData, setCtrData] = useState([])

  useEffect(() => {
    getDashboardMetrics().then(setMetrics)
    getTopProducts().then(setProducts)
    getCTRData().then(setCtrData)
  }, [])

  if (!metrics) return <div className="p-8 text-on-surface-variant">Loading...</div>

  const funnel = [
    { stage: 'Chats', count: 12500, rate: 100 },
    { stage: 'Viewed', count: 8750, rate: 70 },
    { stage: 'Clicked', count: 3125, rate: 25 },
    { stage: 'Purchased', count: 1250, rate: 10 },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-headline-md text-primary">Dashboard</h1>
          <p className="text-sm text-on-surface-variant mt-1">Welcome back, Admin</p>
        </div>
        <div className="text-xs text-on-surface-variant">Last updated: Just now</div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard title="Revenue" value={formatCurrency(metrics.revenue.current)} change={metrics.revenue.change} subtitle="vs last month" icon={DollarSign} />
        <MetricCard title="Conversion" value={`${metrics.conversionRate.current}%`} change={metrics.conversionRate.change} icon={TrendingUp} />
        <MetricCard title="AI Revenue" value={formatCurrency(metrics.aiRevenue.current)} change={metrics.aiRevenue.change} icon={Brain} />
        <MetricCard title="CTR" value={`${metrics.ctr.current}%`} change={metrics.ctr.change} icon={MousePointerClick} />
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <ChartCard title="AI Conversion Funnel">
          <div className="space-y-3">
            {funnel.map((f, idx) => (
              <div key={f.stage} className="flex items-center gap-3">
                <span className="text-xs text-on-surface-variant w-20">{f.stage}</span>
                <div className="flex-1 h-6 bg-surface-container-high rounded-full overflow-hidden">
                  <div className="h-full bg-primary rounded-full flex items-center px-3" style={{ width: `${f.rate}%` }}>
                    <span className="text-[10px] text-on-primary font-medium">{formatNumber(f.count)}</span>
                  </div>
                </div>
                <span className="text-xs text-on-surface-variant w-10 text-right">{f.rate}%</span>
              </div>
            ))}
          </div>
        </ChartCard>

        <ChartCard title="CTR & Revenue Trend">
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={ctrData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#efeded" />
              <XAxis dataKey="month" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip />
              <Line type="monotone" dataKey="ctr" stroke="#000" strokeWidth={2} dot={{ fill: '#000' }} />
            </LineChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>

      {/* Top Products */}
      <ChartCard title="Top AI Recommended Products">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-outline-variant/20">
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-2">Product</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-2">Recommendations</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-2">Clicks</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-2">Purchases</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-2">Conv. Rate</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/5">
              {products.map((p) => (
                <tr key={p.id} className="hover:bg-surface-container-high/30">
                  <td className="px-4 py-3 text-sm font-medium text-primary">{p.name}</td>
                  <td className="px-4 py-3 text-sm text-on-surface-variant">{formatNumber(p.recommendations)}</td>
                  <td className="px-4 py-3 text-sm text-on-surface-variant">{formatNumber(p.clicks)}</td>
                  <td className="px-4 py-3 text-sm text-on-surface-variant">{p.purchases}</td>
                  <td className="px-4 py-3 text-sm text-primary font-medium">{p.conversionRate}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </ChartCard>
    </div>
  )
}
