import { useState, useRef, useEffect } from 'react'
import { Send, Paperclip, Mic, Sparkles, Bookmark, ShoppingBag } from 'lucide-react'
import ChatBubble from '../../components/ai/ChatBubble'
import ProductBlock from '../../components/ai/ProductBlock'
import AIReasoningPanel from '../../components/ai/AIReasoningPanel'
import PromptSuggestion from '../../components/ai/PromptSuggestion'
import useCartStore from '../../features/cart/cart.store'
import { sendStylingPrompt, getConsultationHistory } from '../../features/ai-stylist/aiStylist.api'
import { mockChatMessages, mockStyleProfile } from '../../features/ai-stylist/aiStylist.mock'
import { mockProducts } from '../../data/mockProducts'

export default function AIStylistChatPage() {
  const [messages, setMessages] = useState(mockChatMessages)
  const [input, setInput] = useState('')
  const [isTyping, setIsTyping] = useState(false)
  const [reasoning, setReasoning] = useState(null)
  const [history, setHistory] = useState([])
  const [savedOutfits, setSavedOutfits] = useState([])
  const addItem = useCartStore((s) => s.addItem)
  const chatEndRef = useRef(null)

  useEffect(() => {
    getConsultationHistory().then(setHistory)
  }, [])

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isTyping])

  const handleSend = async (text) => {
    const prompt = text || input.trim()
    if (!prompt) return

    const userMsg = {
      id: String(Date.now()),
      role: 'user',
      content: prompt,
      timestamp: new Date().toISOString(),
      products: [],
    }
    setMessages((prev) => [...prev, userMsg])
    setInput('')
    setIsTyping(true)

    setTimeout(async () => {
      const response = await sendStylingPrompt({ prompt })
      const aiMsg = {
        id: String(Date.now() + 1),
        role: 'ai',
        content: response.message,
        timestamp: new Date().toISOString(),
        products: response.products.map((p) => ({ productId: p.id, matchScore: p.aiMatchScore })),
        reasoning: response.reasoning,
      }
      setMessages((prev) => [...prev, aiMsg])
      setReasoning(response.reasoning)
      setIsTyping(false)
    }, 1200 + Math.random() * 800)
  }

  const handleAddFullOutfit = (products) => {
    products.forEach((prod) => {
      const product = mockProducts.find((p) => p.id === String(prod.productId))
      if (product) addItem(product)
    })
  }

  const handleSaveOutfit = (products) => {
    const outfit = {
      id: `outfit-${Date.now()}`,
      products: products.map((p) => p.productId),
      savedAt: new Date().toISOString(),
    }
    setSavedOutfits((prev) => [...prev, outfit])
  }

  return (
    <div className="flex h-[calc(100vh-5rem)] overflow-hidden">
      {/* Left Sidebar */}
      <aside className="hidden lg:flex w-[380px] border-r border-outline-variant/20 bg-surface-container-low flex-col">
        {/* Style Profile */}
        <div className="p-5 border-b border-outline-variant/20">
          <div className="bg-surface-container-lowest rounded-xl p-4">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 rounded-full bg-primary flex items-center justify-center">
                <Sparkles size={16} className="text-on-primary" />
              </div>
              <div>
                <p className="text-sm font-medium text-primary">{mockStyleProfile.name}'s Profile</p>
                <p className="text-xs text-on-surface-variant">{mockStyleProfile.styleDNA.join(' / ')}</p>
              </div>
            </div>
            <div className="grid grid-cols-3 gap-2 text-center">
              <div className="bg-surface-container-low rounded-lg p-2">
                <p className="text-lg font-semibold text-primary">{mockStyleProfile.topPicks}</p>
                <p className="text-[10px] text-on-surface-variant uppercase">Top Picks</p>
              </div>
              <div className="bg-surface-container-low rounded-lg p-2">
                <p className="text-lg font-semibold text-primary">{savedOutfits.length || mockStyleProfile.savedOutfits}</p>
                <p className="text-[10px] text-on-surface-variant uppercase">Saved</p>
              </div>
              <div className="bg-surface-container-low rounded-lg p-2">
                <p className="text-lg font-semibold text-primary">{mockStyleProfile.avgMatchScore}%</p>
                <p className="text-[10px] text-on-surface-variant uppercase">Avg Match</p>
              </div>
            </div>
          </div>
        </div>

        {/* Recent Consultations */}
        <div className="flex-1 overflow-y-auto custom-scrollbar p-5">
          <h3 className="font-label-sm uppercase tracking-wider text-on-surface-variant mb-3">Recent Chats</h3>
          <div className="space-y-2">
            {history.map((h) => (
              <button key={h.id} className="w-full text-left p-3 rounded-lg hover:bg-surface-container-high transition-colors border-l-4 border-primary">
                <p className="text-sm text-primary truncate">{h.prompt}</p>
                <p className="text-xs text-on-surface-variant mt-0.5">{h.date}</p>
              </button>
            ))}
          </div>

          {savedOutfits.length > 0 && (
            <>
              <h3 className="font-label-sm uppercase tracking-wider text-on-surface-variant mb-3 mt-6">Saved Outfits</h3>
              <div className="space-y-2">
                {savedOutfits.map((outfit) => (
                  <div key={outfit.id} className="p-3 bg-surface-container-high/50 rounded-lg">
                    <div className="flex items-center gap-2">
                      <Bookmark size={12} className="text-tertiary" />
                      <span className="text-xs text-on-surface-variant">{outfit.products.length} items</span>
                    </div>
                    <div className="flex gap-1 mt-2">
                      {outfit.products.map((pid) => {
                        const p = mockProducts.find((mp) => mp.id === String(pid))
                        return p ? <img key={pid} src={p.images[0]} alt="" className="w-8 h-8 rounded object-cover" /> : null
                      })}
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </aside>

      {/* Chat Area */}
      <div className="flex-1 flex flex-col ai-bg-shimmer relative">
        {/* Chat Header */}
        <div className="h-16 glass-header flex items-center justify-between px-6 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center">
              <Sparkles size={14} className="text-on-primary" />
            </div>
            <div>
              <h3 className="text-sm font-medium text-primary">AI Stylist</h3>
              <div className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-green-status animate-pulse" />
                <span className="text-xs text-on-surface-variant">Connected</span>
              </div>
            </div>
          </div>
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto custom-scrollbar px-6 py-6 space-y-6">
          {messages.map((msg) => (
            <div key={msg.id}>
              <ChatBubble message={msg} isAI={msg.role === 'ai'} />
              {msg.products && msg.products.length > 0 && (
                <div className={`mt-3 ${msg.role === 'ai' ? 'ml-11' : 'mr-11'}`}>
                  <div className="grid grid-cols-2 gap-3 max-w-[500px]">
                    {msg.products.map((prod) => (
                      <ProductBlock
                        key={prod.productId}
                        productId={prod.productId}
                        matchScore={prod.matchScore}
                      />
                    ))}
                  </div>
                  {msg.role === 'ai' && (
                    <div className="mt-3 flex gap-2">
                      <button
                        onClick={() => handleAddFullOutfit(msg.products)}
                        className="flex items-center gap-1.5 px-3 py-1.5 bg-primary text-on-primary rounded-lg text-xs font-medium hover:opacity-90 transition-opacity"
                      >
                        <ShoppingBag size={12} /> Add Full Outfit
                      </button>
                      <button
                        onClick={() => handleSaveOutfit(msg.products)}
                        className="flex items-center gap-1.5 px-3 py-1.5 border border-outline-variant text-primary rounded-lg text-xs font-medium hover:bg-surface-container-high transition-colors"
                      >
                        <Bookmark size={12} /> Save Outfit
                      </button>
                    </div>
                  )}
                  {msg.reasoning && (
                    <div className="mt-3">
                      <AIReasoningPanel reasoning={msg.reasoning} />
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}

          {isTyping && (
            <div className="flex items-center gap-3 ml-11">
              <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center">
                <Sparkles size={14} className="text-on-primary" />
              </div>
              <div className="bg-surface-container-low rounded-2xl px-4 py-3 flex gap-1.5">
                <span className="w-2 h-2 bg-on-surface-variant/40 rounded-full animate-bounce" style={{ animationDelay: '0s' }} />
                <span className="w-2 h-2 bg-on-surface-variant/40 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }} />
                <span className="w-2 h-2 bg-on-surface-variant/40 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }} />
              </div>
            </div>
          )}

          {messages.length <= 1 && (
            <div className="flex flex-col items-center justify-center py-12">
              <Sparkles size={32} className="text-tertiary mb-4" />
              <p className="text-on-surface-variant text-center mb-6 max-w-sm">
                Ask me anything about style, outfit recommendations, or wardrobe advice.
              </p>
              <PromptSuggestion onSelect={handleSend} />
            </div>
          )}
          <div ref={chatEndRef} />
        </div>

        {/* Input Bar */}
        <div className="absolute bottom-0 left-0 right-0 p-4">
          <div className="glass-panel rounded-2xl flex items-center gap-3 px-4 py-3">
            <button className="p-2 rounded-full hover:bg-surface-container-high transition-colors">
              <Paperclip size={18} className="text-on-surface-variant" />
            </button>
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSend()}
              placeholder="Describe your style needs..."
              className="flex-1 bg-transparent text-sm text-on-surface placeholder:text-on-surface-variant/50 outline-none"
            />
            <button className="p-2 rounded-full hover:bg-surface-container-high transition-colors">
              <Mic size={18} className="text-on-surface-variant" />
            </button>
            <button
              onClick={() => handleSend()}
              disabled={!input.trim()}
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
