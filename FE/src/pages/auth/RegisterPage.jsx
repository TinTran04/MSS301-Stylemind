import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Lock, Mail, User } from 'lucide-react'
import useAuthStore from '../../features/auth/auth.store'

export default function RegisterPage() {
  const [formData, setFormData] = useState({ name: '', email: '', password: '' })
  const [showPassword, setShowPassword] = useState(false)
  const navigate = useNavigate()
  const login = useAuthStore((s) => s.login)

  const handleSubmit = (e) => {
    e.preventDefault()
    login({ id: '1', name: formData.name, email: formData.email, role: 'customer' })
    navigate('/style-profile')
  }

  return (
    <div className="flex min-h-screen">
      {/* Left: Register Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 md:p-16">
        <div className="w-full max-w-md">
          <Link to="/" className="font-display-lg tracking-tighter text-primary no-underline lg:hidden">
            StyleMind
          </Link>
          <h2 className="font-headline-md text-primary mt-8 lg:mt-0">Join the Atelier</h2>
          <p className="text-on-surface-variant mt-2">Begin your AI-powered style journey</p>

          <form onSubmit={handleSubmit} className="space-y-6 mt-8">
            <div>
              <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Full Name</label>
              <div className="relative">
                <User size={16} className="absolute left-0 top-1/2 -translate-y-1/2 text-on-surface-variant" />
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full bg-transparent border-0 border-b border-outline-variant py-2 pl-7 text-sm text-on-surface focus:border-tertiary-container focus:outline-none transition-colors"
                  placeholder="Your name"
                />
              </div>
            </div>

            <div>
              <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Email</label>
              <div className="relative">
                <Mail size={16} className="absolute left-0 top-1/2 -translate-y-1/2 text-on-surface-variant" />
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  className="w-full bg-transparent border-0 border-b border-outline-variant py-2 pl-7 text-sm text-on-surface focus:border-tertiary-container focus:outline-none transition-colors"
                  placeholder="your@email.com"
                />
              </div>
            </div>

            <div>
              <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Password</label>
              <div className="relative">
                <Lock size={16} className="absolute left-0 top-1/2 -translate-y-1/2 text-on-surface-variant" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  className="w-full bg-transparent border-0 border-b border-outline-variant py-2 pl-7 pr-10 text-sm text-on-surface focus:border-tertiary-container focus:outline-none transition-colors"
                  placeholder="Create a password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-0 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-primary transition-colors"
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              className="w-full bg-primary text-on-primary rounded-lg py-3 text-sm font-medium hover:opacity-90 transition-opacity tracking-[0.1em] uppercase mt-8"
            >
              Create Account
            </button>

            <div className="relative my-6">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-outline-variant/20" />
              </div>
              <div className="relative flex justify-center text-xs">
                <span className="bg-surface-container-lowest px-4 text-on-surface-variant uppercase tracking-wider">OR</span>
              </div>
            </div>

            <button
              type="button"
              className="w-full border border-outline-variant rounded-lg py-3 text-sm font-medium hover:bg-surface-container-high transition-colors flex items-center justify-center gap-3"
            >
              <svg width="18" height="18" viewBox="0 0 24 24">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4"/>
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
              </svg>
              Sign up with Google
            </button>
          </form>

          <p className="text-center text-sm text-on-surface-variant mt-8">
            Already have an account?{' '}
            <Link to="/login" className="text-primary font-medium hover:underline">
              Sign in
            </Link>
          </p>
        </div>
      </div>

      {/* Right: Editorial Image */}
      <div className="hidden lg:flex lg:w-1/2 relative overflow-hidden">
        <img
          src="https://images.unsplash.com/photo-1509631179647-0177331693ae?w=1200&h=1600&fit=crop"
          alt="Fashion editorial"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-primary/80 via-primary/20 to-transparent" />
        <div className="absolute bottom-16 left-16 right-16">
          <blockquote className="font-headline-lg text-on-primary/90 italic leading-relaxed">
            "Fashion is the armour to survive the reality of everyday life."
          </blockquote>
          <p className="text-on-primary/60 mt-4 font-label-sm uppercase tracking-wider">- Bill Cunningham</p>
        </div>
      </div>
    </div>
  )
}