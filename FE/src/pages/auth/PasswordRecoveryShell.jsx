import { Link } from 'react-router-dom'

export default function PasswordRecoveryShell({ eyebrow, title, description, children }) {
  return (
    <div className="flex min-h-screen bg-background">
      <aside className="hidden lg:flex lg:w-2/5 bg-primary text-on-primary px-16 py-14 flex-col justify-between">
        <Link to="/" className="font-display-lg text-on-primary no-underline">
          StyleMind
        </Link>
        <div>
          <p className="font-label-sm uppercase text-on-primary/60 mb-4">{eyebrow}</p>
          <p className="font-headline-lg max-w-md">Return to your wardrobe, securely.</p>
        </div>
        <p className="text-sm text-on-primary/50">Identity protected by StyleMind</p>
      </aside>

      <main className="w-full lg:w-3/5 flex items-center justify-center px-6 py-12 md:px-16">
        <div className="w-full max-w-md">
          <Link to="/" className="font-display-lg text-primary no-underline lg:hidden">
            StyleMind
          </Link>
          <p className="font-label-sm uppercase text-on-surface-variant mt-10 lg:mt-0 mb-3">
            {eyebrow}
          </p>
          <h1 className="font-headline-md text-primary">{title}</h1>
          <p className="text-on-surface-variant mt-2 mb-8">{description}</p>
          {children}
        </div>
      </main>
    </div>
  )
}
