import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, ArrowRight, Sparkles } from 'lucide-react'
import clsx from 'clsx'
import { mockStyleOptions } from '../../features/profile/profile.mock'
import { getProfile, updateProfile } from '../../features/profile/profile.api'

const steps = [
  { num: '01', title: 'Sở thích phong cách', description: 'Chọn dấu ấn thời trang của bạn' },
  { num: '02', title: 'Hồ sơ vóc dáng', description: 'Giúp chúng tôi tìm phom dáng phù hợp' },
  { num: '03', title: 'Sở thích phom dáng', description: 'Bạn thích trang phục ôm vừa hay thoải mái?' },
  { num: '04', title: 'Màu sắc & kích cỡ', description: 'Hoàn thiện hồ sơ phong cách của bạn' },
]

const styleOptions = mockStyleOptions.stylePreferences

function getOptionLabel(options, value) {
  return options.find((option) => option.value === value || option.id === value)?.label || value
}

export default function StyleProfilePage() {
  const [currentStep, setCurrentStep] = useState(1)
  const [displayName, setDisplayName] = useState('')
  const [selectedStyles, setSelectedStyles] = useState([])
  const [selectedBodyType, setSelectedBodyType] = useState(null)
  const [selectedFit, setSelectedFit] = useState(null)
  const [selectedColors, setSelectedColors] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const bodyTypes = mockStyleOptions.bodyTypes
  const fits = mockStyleOptions.fitPreferences
  const colors = mockStyleOptions.colorPalettes

  useEffect(() => {
    let active = true

    getProfile()
      .then((profile) => {
        if (!active) return
        setDisplayName(profile?.displayName || '')
        setSelectedBodyType(profile?.bodyMorphology || null)
        setSelectedFit(profile?.preferredFit || null)

        if (profile?.stylePersonas) {
          try {
            const preferences = JSON.parse(profile.stylePersonas)
            setSelectedStyles(preferences.styles || [])
            setSelectedColors(preferences.colors || [])
          } catch {
            setSelectedStyles([])
            setSelectedColors([])
          }
        }
      })
      .catch(() => {
        if (active) setError('Không thể tải hồ sơ phong cách của bạn.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  const toggleStyle = (style) => {
    setSelectedStyles((previous) =>
      previous.includes(style)
        ? previous.filter((value) => value !== style)
        : [...previous, style]
    )
  }

  const toggleColor = (color) => {
    setSelectedColors((prev) =>
      prev.includes(color) ? prev.filter((c) => c !== color) : [...prev, color]
    )
  }

  const getInsightText = () => {
    const bodyTypeLabel = getOptionLabel(bodyTypes, selectedBodyType)
    const fitLabel = getOptionLabel(fits, selectedFit)

    if (selectedBodyType && selectedFit) {
      return `Dựa trên vóc dáng ${bodyTypeLabel} và sở thích phom dáng ${fitLabel}, chúng tôi gợi ý các thiết kế có đường cắt rõ ràng, gọn gàng. Tủ đồ của bạn nên ưu tiên những phom dáng tinh chỉnh để tôn lên tỷ lệ tự nhiên.`
    }
    if (selectedBodyType) {
      return `Hãy cho chúng tôi biết bạn thích phom dáng nào, Stylist AI sẽ tạo bản gợi ý tủ đồ cá nhân dựa trên vóc dáng ${bodyTypeLabel} của bạn.`
    }
    return 'Chọn vóc dáng và phom dáng yêu thích để nhận những gợi ý thời trang được AI cá nhân hóa.'
  }

  const completeProfile = async () => {
    setSaving(true)
    setError('')

    try {
      await updateProfile({
        displayName: displayName.trim() || null,
        bodyMorphology: selectedBodyType,
        preferredFit: selectedFit,
        stylePersonas: JSON.stringify({
          styles: selectedStyles,
          colors: selectedColors,
        }),
      })
      navigate('/')
    } catch {
      setError('Không thể lưu hồ sơ phong cách của bạn.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center text-on-surface-variant">
        Đang tải hồ sơ của bạn...
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Minimal Header */}
      <header className="flex items-center justify-between px-8 py-5">
        <a href="/" className="font-display-lg tracking-tighter text-primary no-underline">StyleMind</a>
        <span className="font-label-sm uppercase text-on-surface-variant">
          BƯỚC {String(currentStep).padStart(2, '0')}/04
        </span>
      </header>

      {/* Progress Bar */}
      <div className="px-8">
        <div className="max-w-[800px] mx-auto h-1 bg-surface-container-high rounded-full overflow-hidden">
          <div
            className="h-full bg-primary rounded-full transition-all duration-500"
            style={{ width: `${(currentStep / 4) * 100}%` }}
          />
        </div>
      </div>

      {/* Main Content */}
      <main className="max-w-[800px] mx-auto px-8 py-12">
        <h1 className="font-headline-md text-primary mb-2">
          {steps[currentStep - 1].title}
        </h1>
        <p className="text-on-surface-variant mb-8">{steps[currentStep - 1].description}</p>

        {/* Step 1: Style DNA */}
        {currentStep === 1 && (
          <div className="space-y-6">
            <div>
              <label className="block font-label-sm uppercase text-on-surface-variant mb-2">
                Tên hiển thị
              </label>
              <input
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                maxLength={150}
                className="w-full bg-transparent border-0 border-b border-outline-variant py-2 text-sm text-primary focus:border-tertiary-container focus:outline-none"
                placeholder="StyleMind nên gọi bạn như thế nào?"
              />
            </div>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              {styleOptions.map((style) => (
                <button
                  key={style.value}
                  onClick={() => toggleStyle(style.value)}
                  className={clsx(
                    'p-6 rounded-2xl border-2 text-center transition-all',
                    selectedStyles.includes(style.value)
                      ? 'border-tertiary-container bg-surface-container-low'
                      : 'border-outline-variant/20 hover:border-primary'
                  )}
                >
                  <span className="font-title-lg text-primary">{style.label}</span>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Step 2: Body Profile */}
        {currentStep === 2 && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {bodyTypes.map((type) => (
              <button
                key={type.id}
                onClick={() => setSelectedBodyType(type.id)}
                className={clsx(
                  'aspect-[3/4] rounded-2xl border-2 flex flex-col items-center justify-center gap-3 transition-all',
                  selectedBodyType === type.id
                    ? 'border-tertiary-container bg-surface-container-low'
                    : 'border-outline-variant/20 hover:border-outline-variant'
                )}
              >
                <span className="material-symbols-outlined text-4xl text-on-surface-variant">{type.icon}</span>
                <span className="font-title-lg text-primary">{type.label}</span>
                <span className="text-xs text-on-surface-variant text-center px-2">{type.description}</span>
              </button>
            ))}
          </div>
        )}

        {/* Step 3: Fit Preferences */}
        {currentStep === 3 && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {fits.map((fit) => (
              <button
                key={fit.id}
                onClick={() => setSelectedFit(fit.id)}
                className={clsx(
                  'p-6 rounded-2xl border-2 text-center transition-all',
                  selectedFit === fit.id
                    ? 'border-tertiary-container bg-surface-container-low'
                    : 'border-outline-variant/20 hover:border-outline-variant'
                )}
              >
                <span className="font-title-lg text-primary">{fit.label}</span>
              </button>
            ))}
          </div>
        )}

        {/* Step 4: Colors & Size */}
        {currentStep === 4 && (
          <div className="space-y-8">
            <div>
              <h3 className="font-label-md uppercase text-on-surface-variant mb-3">Màu sắc yêu thích</h3>
              <div className="flex flex-wrap gap-3">
                {colors.map((color) => (
                  <button
                    key={color.value}
                    onClick={() => toggleColor(color.value)}
                    className={clsx(
                      'px-4 py-2 rounded-full border text-sm transition-all',
                      selectedColors.includes(color.value)
                        ? 'border-tertiary-container bg-surface-container-low text-primary'
                        : 'border-outline-variant/20 text-on-surface-variant hover:border-outline-variant'
                    )}
                  >
                    {color.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* AI Insight Card */}
        <div className="mt-10 bg-ai-lavender/20 rounded-2xl p-6 border border-ai-lavender/30">
          <div className="flex items-center gap-2 mb-3">
            <Sparkles size={16} className="text-tertiary" />
            <span className="font-label-sm uppercase text-on-surface-variant">Gợi ý từ AI</span>
          </div>
          <p className="text-sm text-on-surface-variant leading-relaxed">{getInsightText()}</p>
        </div>
        {error && (
          <p role="alert" className="mt-4 text-sm text-error">
            {error}
          </p>
        )}
      </main>

      {/* Fixed Bottom Navigation */}
      <footer className="fixed bottom-0 left-0 right-0 bg-surface/80 backdrop-blur-xl border-t border-outline-variant/20 px-8 py-4">
        <div className="max-w-[800px] mx-auto flex justify-between">
          <button
            onClick={() => currentStep > 1 ? setCurrentStep(currentStep - 1) : navigate(-1)}
            className="flex items-center gap-2 text-on-surface-variant hover:text-primary transition-colors text-sm"
          >
            <ArrowLeft size={16} />
            Quay lại
          </button>
          <button
            onClick={() => {
              if (currentStep < 4) setCurrentStep(currentStep + 1)
              else completeProfile()
            }}
            disabled={saving}
            className="flex items-center gap-2 bg-primary text-on-primary px-6 py-2.5 rounded-lg text-sm font-medium hover:opacity-90 transition-opacity"
          >
            {currentStep === 4 ? (saving ? 'Đang lưu...' : 'Hoàn tất') : 'Tiếp tục'}
            <ArrowRight size={16} />
          </button>
        </div>
      </footer>
    </div>
  )
}
