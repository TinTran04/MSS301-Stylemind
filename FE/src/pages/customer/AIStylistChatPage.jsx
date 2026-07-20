import { useState, useRef, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { Send, Sparkles, AlertTriangle, LogIn } from 'lucide-react'
import ChatBubble from '../../components/ai/ChatBubble'
import OutfitPlanBlock from '../../components/ai/OutfitPlanBlock'
import ProductListBlock from '../../components/ai/ProductListBlock'
import PromptSuggestion from '../../components/ai/PromptSuggestion'
import SessionSidebar from '../../components/ai/SessionSidebar'
import useAuthStore from '../../features/auth/auth.store'
import {
  listSessions,
  createSession,
  deleteSession,
  getSessionMessages,
  sendChatMessage,
} from '../../features/ai-stylist/aiStylist.api'
import { toDisplayMessage } from '../../features/ai-stylist/aiStylist.utils'

export default function AIStylistChatPage() {
  const user = useAuthStore((s) => s.user)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  const [sessions, setSessions] = useState([])
  const [loadingSessions, setLoadingSessions] = useState(true)
  const [activeSessionId, setActiveSessionId] = useState(null)
  const [messages, setMessages] = useState([])
  const [loadingMessages, setLoadingMessages] = useState(false)
  const [input, setInput] = useState('')
  const [isTyping, setIsTyping] = useState(false)
  const [error, setError] = useState('')
  const chatEndRef = useRef(null)

  useEffect(() => {
    if (!user?.id) {
      setLoadingSessions(false)
      return
    }
    listSessions(user.id)
      .then((list) => {
        setSessions(list)
        if (list.length > 0) {
          selectSession(list[0].id)
        }
      })
      .catch(() => setError('Kh├┤ng thß╗â tß║úi danh s├ích tr├▓ chuyß╗çn.'))
      .finally(() => setLoadingSessions(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id])

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isTyping])

  const selectSession = useCallback(async (sessionId) => {
    setActiveSessionId(sessionId)
    setMessages([])
    setError('')
    setLoadingMessages(true)
    try {
      const history = await getSessionMessages(sessionId)
      setMessages(history.map(toDisplayMessage))
    } catch {
      setError('Kh├┤ng thß╗â tß║úi lß╗ïch sß╗¡ tr├▓ chuyß╗çn.')
    } finally {
      setLoadingMessages(false)
    }
  }, [])

  const handleNewChat = () => {
    // Session is created lazily on the first message, so empty sessions never pile up.
    setActiveSessionId(null)
    setMessages([])
    setError('')
  }

  const handleDeleteSession = async (sessionId) => {
    try {
      await deleteSession(sessionId)
      setSessions((prev) => prev.filter((s) => s.id !== sessionId))
      if (sessionId === activeSessionId) {
        handleNewChat()
      }
    } catch {
      setError('Kh├┤ng thß╗â x├│a cuß╗Öc tr├▓ chuyß╗çn.')
    }
  }

  const handleSend = async (text) => {
    const prompt = (text || input).trim()
    if (!prompt || isTyping || !user?.id) return

    setInput('')
    setError('')
    setMessages((prev) => [
      ...prev,
      {
        id: `local-${Date.now()}`,
        role: 'user',
        content: prompt,
        timestamp: new Date().toISOString(),
      },
    ])
    setIsTyping(true)

    try {
      let sessionId = activeSessionId
      if (!sessionId) {
        const session = await createSession(user.id)
        sessionId = session.id
        setActiveSessionId(sessionId)
        setSessions((prev) => [session, ...prev])
      }

      const response = await sendChatMessage(sessionId, prompt)
      // response.message holds the persisted MessageResponse (with metadata),
      // but real-time products / outfit_plan arrive at the top-level response
      // fields; merge them for immediate display before the next history fetch.
      const displayMsg = toDisplayMessage(response.message)
      if (!displayMsg.outfitPlan && response.outfit_plan) {
        displayMsg.outfitPlan = response.outfit_plan
      }
      if (!displayMsg.products && response.products?.length) {
        displayMsg.products = response.products
      }
      setMessages((prev) => [...prev, displayMsg])

      // Backend fills the title from the first message and bumps updated_at;
      // reflect that in the sidebar without refetching.
      setSessions((prev) => {
        const updated = prev.map((s) =>
          s.id === sessionId
            ? { ...s, title: s.title || prompt.slice(0, 60), updated_at: new Date().toISOString() }
            : s
        )
        return updated.sort((a, b) => new Date(b.updated_at) - new Date(a.updated_at))
      })
    } catch {
      setError('Stylist AI hiß╗çn ch╞░a sß║╡n s├áng. Vui l├▓ng thß╗¡ lß║íi sau.')
    } finally {
      setIsTyping(false)
    }
  }

  if (!isAuthenticated || !user?.id) {
    return (
      <div className="flex h-[calc(100vh-5rem)] items-center justify-center ai-bg-shimmer">
        <div className="glass-panel rounded-2xl p-8 max-w-sm text-center">
          <Sparkles size={32} className="text-tertiary mx-auto mb-4" />
          <h3 className="text-base font-medium text-primary mb-2">Stylist AI</h3>
          <p className="text-sm text-on-surface-variant mb-6">
            ─É─âng nhß║¡p ─æß╗â tr├▓ chuyß╗çn vß╗¢i Stylist AI v├á l╞░u lß║íi c├íc cuß╗Öc t╞░ vß║Ñn cß╗ºa bß║ín.
          </p>
          <Link
            to="/login"
            className="inline-flex items-center gap-2 px-4 py-2.5 bg-primary text-on-primary rounded-xl text-sm font-medium hover:opacity-90 transition-opacity"
          >
            <LogIn size={14} /> ─É─âng nhß║¡p
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-[calc(100vh-5rem)] overflow-hidden">
      <SessionSidebar
        sessions={sessions}
        activeSessionId={activeSessionId}
        loading={loadingSessions}
        onSelect={selectSession}
        onNewChat={handleNewChat}
        onDelete={handleDeleteSession}
      />

      <div className="flex-1 flex flex-col ai-bg-shimmer relative">
        <div className="h-16 glass-header flex items-center justify-between px-6 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center">
              <Sparkles size={14} className="text-on-primary" />
            </div>
            <div>
              <h3 className="text-sm font-medium text-primary">Stylist AI</h3>
              <div className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-green-status animate-pulse" />
                <span className="text-xs text-on-surface-variant">─É├ú kß║┐t nß╗æi</span>
              </div>
            </div>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto custom-scrollbar px-6 py-6 pb-24 space-y-6">
          {loadingMessages && (
            <div className="py-12 text-center text-sm text-on-surface-variant">
              ─Éang tß║úi lß╗ïch sß╗¡ tr├▓ chuyß╗çn...
            </div>
          )}

          {!loadingMessages && messages.map((msg) => (
            <div key={msg.id}>
              <ChatBubble message={msg} isAI={msg.role === 'ai'} />
              {msg.outfitPlan && (
                <div className="mt-3 ml-11">
                  <OutfitPlanBlock plan={msg.outfitPlan} messageId={msg.id} />
                </div>
              )}
              {!msg.outfitPlan && msg.products && msg.products.length > 0 && (
                <div className="mt-3 ml-11">
                  <ProductListBlock products={msg.products} messageId={msg.id} />
                </div>
              )}
            </div>
          ))}

          {isTyping && (
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center">
                <Sparkles size={14} className="text-on-primary" />
              </div>
              <div className="bg-surface-container-lowest border border-outline-variant/40 soft-shadow rounded-2xl px-4 py-3 flex gap-1.5">
                <span className="w-2 h-2 bg-on-surface-variant/40 rounded-full animate-bounce" style={{ animationDelay: '0s' }} />
                <span className="w-2 h-2 bg-on-surface-variant/40 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }} />
                <span className="w-2 h-2 bg-on-surface-variant/40 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }} />
              </div>
            </div>
          )}

          {error && (
            <div role="alert" className="mx-auto max-w-md rounded-xl border border-error/20 bg-error-container/30 p-4 text-sm text-error text-center flex items-center gap-2 justify-center">
              <AlertTriangle size={14} /> {error}
            </div>
          )}

          {!loadingMessages && !isTyping && messages.length === 0 && !error && (
            <div className="flex flex-col items-center justify-center py-12">
              <Sparkles size={32} className="text-tertiary mb-4" />
              <p className="text-on-surface-variant text-center mb-6 max-w-sm">
                H├úy hß╗Åi vß╗ü phong c├ích, gß╗úi ├╜ trang phß╗Ñc hoß║╖c lß╗¥i khuy├¬n phß╗æi ─æß╗ô.
              </p>
              <PromptSuggestion onSelect={handleSend} />
            </div>
          )}
          <div ref={chatEndRef} />
        </div>

        <div className="absolute bottom-0 left-0 right-0 p-4">
          <div className="glass-panel rounded-2xl flex items-center gap-3 px-4 py-3">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSend()}
              placeholder="M├┤ tß║ú phong c├ích bß║ín ─æang cß║ºn..."
              className="flex-1 bg-transparent text-sm text-on-surface placeholder:text-on-surface-variant/50 outline-none"
            />
            <button
              onClick={() => handleSend()}
              disabled={!input.trim() || isTyping}
              className="p-2 rounded-full bg-primary text-on-primary hover:opacity-90 transition-opacity disabled:opacity-40"
            >
              <Send size={16} />
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}