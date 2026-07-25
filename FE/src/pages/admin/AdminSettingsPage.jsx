import { useState } from 'react'
import { Store, Shield, Lock, Brain, Server, BarChart3, CheckCircle } from 'lucide-react'

export default function AdminSettingsPage() {
  const [storeProfile, setStoreProfile] = useState({
    name: 'StyleMind Atelier',
    email: 'admin@stylemind.ai',
    description: 'Nền tảng thương mại điện tử thời trang cao cấp ứng dụng AI.',
  })

  const [llmProvider, setLlmProvider] = useState('GPT-4o')
  const [embeddingModel, setEmbeddingModel] = useState('text-embedding-3-large')
  const [temperature, setTemperature] = useState(0.7)
  const [saveSuccess, setSaveSuccess] = useState(false)

  const handleSave = () => {
    setSaveSuccess(true)
    setTimeout(() => setSaveSuccess(false), 2000)
  }

  return (
    <div className="space-y-6">
      <h1 className="font-headline-md text-primary">Cài đặt hệ thống</h1>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-8 space-y-6">
          <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow">
            <div className="flex items-center gap-2 mb-4">
              <Store size={18} className="text-primary" />
              <h2 className="font-title-lg text-primary">Hồ sơ cửa hàng</h2>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Tên cửa hàng</label>
                <input value={storeProfile.name} onChange={(e) => setStoreProfile({ ...storeProfile, name: e.target.value })} className="w-full bg-transparent border-0 border-b border-outline-variant py-2 text-sm focus:border-tertiary-container outline-none transition-colors" />
              </div>
              <div>
                <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Email liên hệ</label>
                <input value={storeProfile.email} onChange={(e) => setStoreProfile({ ...storeProfile, email: e.target.value })} className="w-full bg-transparent border-0 border-b border-outline-variant py-2 text-sm focus:border-tertiary-container outline-none transition-colors" />
              </div>
              <div>
                <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Mô tả</label>
                <textarea value={storeProfile.description} onChange={(e) => setStoreProfile({ ...storeProfile, description: e.target.value })} rows={3} className="w-full bg-transparent border-0 border-b border-outline-variant py-2 text-sm focus:border-tertiary-container outline-none transition-colors resize-none" />
              </div>
              <button onClick={handleSave} className="bg-primary text-on-primary px-6 py-2 rounded-lg text-sm font-medium hover:opacity-90 flex items-center gap-2">
                {saveSuccess ? <><CheckCircle size={14} /> Đã lưu!</> : 'Lưu thay đổi'}
              </button>
            </div>
          </div>

          <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow">
            <div className="flex items-center gap-2 mb-4">
              <Shield size={18} className="text-primary" />
              <h2 className="font-title-lg text-primary">Kiểm soát truy cập</h2>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {[
                { role: 'Quản trị viên', desc: 'Toàn quyền truy cập', color: 'bg-primary text-on-primary' },
                { role: 'Quản lý', desc: 'Truy cập sản phẩm và đơn hàng', color: 'bg-secondary-container text-on-secondary-container' },
                { role: 'Nhà phân tích', desc: 'Chỉ xem phân tích', color: 'bg-ai-lavender text-ai-indigo' },
              ].map((r) => (
                <div key={r.role} className="bg-surface-container-low rounded-xl p-4">
                  <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${r.color}`}>{r.role}</span>
                  <p className="text-xs text-on-surface-variant mt-2">{r.desc}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow">
            <div className="flex items-center gap-2 mb-4">
              <Lock size={18} className="text-primary" />
              <h2 className="font-title-lg text-primary">Kiểm tra bảo mật</h2>
            </div>
            <div className="flex items-center gap-3 p-4 bg-green-status/10 rounded-xl">
              <div className="w-10 h-10 rounded-full bg-green-status/20 flex items-center justify-center">
                <Lock size={18} className="text-green-status" />
              </div>
              <div>
                <p className="text-sm font-medium text-green-status">99.9% thời gian hoạt động</p>
                <p className="text-xs text-on-surface-variant">Tất cả hệ thống đang hoạt động. Lần kiểm tra gần nhất: 2 giờ trước.</p>
              </div>
            </div>
          </div>
        </div>

        <div className="lg:col-span-4 space-y-6">
          <div className="bg-primary-container rounded-xl p-6">
            <div className="flex items-center gap-2 mb-4">
              <Brain size={18} className="text-on-primary-container" />
              <h2 className="font-title-lg text-on-primary-container">Cấu hình AI Engine</h2>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block font-label-sm uppercase tracking-wider text-on-primary-container/70 mb-2">Nhà cung cấp LLM</label>
                <select value={llmProvider} onChange={(e) => setLlmProvider(e.target.value)} className="w-full bg-surface-container-lowest/10 border border-on-primary-container/20 rounded-lg px-3 py-2 text-sm text-on-primary-container focus:outline-none">
                  <option>GPT-4o</option>
                  <option>Claude 3.5</option>
                  <option>Gemini Pro</option>
                </select>
              </div>
              <div>
                <label className="block font-label-sm uppercase tracking-wider text-on-primary-container/70 mb-2">Mô hình embedding</label>
                <select value={embeddingModel} onChange={(e) => setEmbeddingModel(e.target.value)} className="w-full bg-surface-container-lowest/10 border border-on-primary-container/20 rounded-lg px-3 py-2 text-sm text-on-primary-container focus:outline-none">
                  <option>text-embedding-3-large</option>
                  <option>text-embedding-3-small</option>
                </select>
              </div>
              <div>
                <label className="block font-label-sm uppercase tracking-wider text-on-primary-container/70 mb-2">Nhiệt độ: {temperature}</label>
                <input type="range" min="0" max="1" step="0.1" value={temperature} onChange={(e) => setTemperature(Number(e.target.value))} className="w-full accent-tertiary-container" />
              </div>
            </div>
          </div>

          <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow">
            <div className="flex items-center gap-2 mb-4">
              <Server size={18} className="text-primary" />
              <h2 className="font-title-lg text-primary">Bộ xử lý sự kiện</h2>
            </div>
            <div className="flex items-center gap-2 mb-3">
              <span className="w-2 h-2 rounded-full bg-green-status animate-pulse" />
              <span className="text-sm text-green-status font-medium">Hoạt động tốt</span>
            </div>
            <div className="space-y-2">
              {['Sự kiện xác thực', 'Sự kiện sản phẩm', 'Sự kiện đơn hàng', 'Sự kiện AI'].map((queue) => (
                <div key={queue} className="flex items-center justify-between text-xs">
                  <span className="text-on-surface-variant">{queue}</span>
                  <div className="w-20 h-1.5 bg-surface-container-high rounded-full overflow-hidden">
                    <div className="h-full bg-primary rounded-full" style={{ width: `${70 + Math.random() * 25}%` }} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
