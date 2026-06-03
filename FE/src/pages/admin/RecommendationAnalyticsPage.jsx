import { useState, useEffect } from 'react'
import { MousePointerClick, TrendingUp, DollarSign, BarChart3 } from 'lucide-react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import ChartCard from '../../components/admin/ChartCard'
import MetricCard from '../../components/admin/MetricCard'
import { getRecommendationFunnel, getTopProducts, getCTRData } from '../../features/analytics/analytics.api'
import { formatCurrency, formatNumber } from '../../utils/formatCurrency'

export default function RecommendationAnalyticsPage() {
  const [funnel, setFunnel] = useState([])
  const [products, setProducts] = useState([])
  const [ctrData, setCtrData] = useState([])

  useEffect(() => {
    getRecommendationFunnel().then(setFunnel)
    getTopProducts().then(setProducts)
    getCTRData().then(setCtrData)
  }, [])

  return (
    <div className="space-y-6">
      <h1 className="font-headline-md text-primary">Recommendation Analytics</h1>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <MetricCard title="Total Recommendations" value={formatNumber(12500)} change={18.2} icon={MousePointerClick} />
        <MetricCard title="Avg CTR" value="12.4%" change={14.8} icon={TrendingUp} />
        <MetricCard title="AI Revenue" value={formatCurrency(45320)} change={19.0} icon={DollarSign} />
        <MetricCard title="Conversion Rate" value="10%" change={12.5} icon={BarChart3} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Funnel */}
        <ChartCard title="Recommendation Funnel">
          <div className="space-y-4">
            {funnel.map((f, idx) => (
              <div key={f.stage} className="flex items-center gap-4">
                <span className="text-sm text-on-surface-variant w-28">{f.stage}</span>
                <div className="flex-1 h-10 bg-surface-container-high rounded-xl overflow-hidden relative">
                  <div className="h-full bg-primary rounded-xl flex items-center px-4 transition-all duration-500" style={{ width: `${f.rate}%` }}>
                    <span className="text-xs text-on-primary font-medium">{formatNumber(f.count)}</span>
                  </div>
                </div>
                <span className="text-sm text-on-surface-variant w-12 text-right font-medium">{f.rate}%</span>
              </div>
            ))}
          </div>
        </ChartCard>

        {/* CTR Over Time */}
        <ChartCard title="CTR Trend">
          <ResponsiveContainer width="100%" height={220}>
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
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-2">Recs</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-2">Clicks</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-2">Purchases</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-2">Conv.</th>
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
