import { ImageOff } from 'lucide-react'

export default function ProductImage({ src, alt, className = '' }) {
  if (!src) {
    return (
      <div
        className={`flex items-center justify-center bg-surface-container text-on-surface-variant ${className}`}
        role="img"
        aria-label={`${alt} has no image`}
      >
        <ImageOff size={24} strokeWidth={1.4} aria-hidden="true" />
      </div>
    )
  }

  return <img src={src} alt={alt} className={className} loading="lazy" />
}
