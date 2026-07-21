import { Sparkles, Info } from 'lucide-react'

export default function RecommendationAnalyticsPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-headline-md text-primary">Phân tích gợi ý</h1>
          <p className="text-sm text-on-surface-variant mt-1">Báo cáo hiệu quả &amp; chuyển đổi từ AI Stylist</p>
        </div>
      </div>

      <div className="bg-surface-container-lowest rounded-2xl p-8 border border-outline-variant/30 text-center space-y-4 ambient-shadow my-8">
        <div className="w-16 h-16 rounded-2xl bg-surface-container-high flex items-center justify-center mx-auto text-primary">
          <Sparkles size={32} />
        </div>
        <div className="max-w-md mx-auto space-y-2">
          <h2 className="font-title-lg text-primary font-semibold">
            Tính năng này sẽ được hoàn thiện trong tương lai
          </h2>
          <p className="text-sm text-on-surface-variant leading-relaxed">
            Hệ thống phân tích phễu chuyển đổi, CTR và doanh thu phát sinh từ AI Stylist đang trong giai đoạn tích hợp thu thập dữ liệu tương tác thực tế.
          </p>
        </div>
        <div className="pt-2 inline-flex items-center gap-2 px-4 py-2 rounded-full bg-surface-container text-xs text-on-surface-variant font-medium">
          <Info size={14} /> Dữ liệu sẽ tự động tổng hợp khi kết nối API Analytics chính thức
        </div>
      </div>
    </div>
  )
}
