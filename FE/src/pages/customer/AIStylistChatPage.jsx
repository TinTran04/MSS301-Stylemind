import { useState, useRef, useEffect } from 'react'
import { Send, Sparkles, ShoppingBag, AlertTriangle } from 'lucide-react'
import ChatBubble from '../../components/ai/ChatBubble'
import ProductBlock from '../../components/ai/ProductBlock'
import PromptSuggestion from '../../components/ai/PromptSuggestion'
import useCartStore from '../../features/cart/cart.store'
import { getProductById } from '../../features/products/product.api'
import { sendChatMessage, getChatHistory } from '../../features/ai-stylist/aiStylist.api'

function toDisplayMessage(chatResponse) {
  return {
    id: chatResponse.messageId,
    conversationId: chatResponse.conversationId,
    role: chatResponse.senderType === 'AI' ? 'ai' : 'user',
    content: chatResponse.messageText,
    timestamp: chatResponse.createdAt || new Date().toISOString(),
    products: chatResponse.recommendedProducts || [],
    bundleId: chatResponse.bundleId || null,
  }
}

export default function AIStylistChatPage() {
  const [messages, setMessages] = useState([])
  const [conversationId, setConversationId] = useState(null)
  const [input, setInput] = useState('')
  const [isTyping, setIsTyping] = useState(false)
  const [loadingHistory, setLoadingHistory] = useState(true)
  const [historyError, setHistoryError] = useState('')
  const [sendError, setSendError] = useState('')
  const [addingBundleId, setAddingBundleId] = useState(null)
  const addItem = useCartStore((s) => s.addItem)
  const chatEndRef = useRef(null)

  useEffect(() => {
    getChatHistory()
      .then((history) => {
        const mapped = history.map(toDisplayMessage)
        setMessages(mapped)
        if (mapped.length > 0) {
          setConversationId(mapped[mapped.length - 1].conversationId)
        }
      })
      .catch(() => setHistoryError('Không thể tải lịch sử trò chuyện.'))
      .finally(() => setLoadingHistory(false))
  }, [])

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isTyping])

  const handleSend = async (text) => {
    const prompt = text || input.trim()
    if (!prompt) return

    const userMsg = {
      id: `local-${Date.now()}`,
      role: 'user',
      content: prompt,
      timestamp: new Date().toISOString(),
      products: [],
    }
    setMessages((prev) => [...prev, userMsg])
    setInput('')
    setIsTyping(true)
    setSendError('')

    try {
      const response = await sendChatMessage(prompt, conversationId)
      setConversationId(response.conversationId)
      setMessages((prev) => [...prev, toDisplayMessage(response)])
    } catch (err) {
      setSendError('Stylist AI hiện chưa sẵn sàng. Vui lòng thử lại sau.')
    } finally {
      setIsTyping(false)
    }
  }

  const handleAddFullOutfit = async (products, bundleId) => {
    setAddingBundleId(bundleId)
    try {
      for (const prod of products) {
        const fullProduct = await getProductById(prod.productId)
        if (fullProduct) {
          await addItem(fullProduct, 1, undefined, undefined, { isAiRecommended: true, sourceBundleId: bundleId })
        }
      }
    } finally {
      setAddingBundleId(null)
    }
  }

  return (
    <div className="flex h-[calc(100vh-5rem)] overflow-hidden">
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
                <span className="text-xs text-on-surface-variant">Đã kết nối</span>
              </div>
            </div>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto custom-scrollbar px-6 py-6 space-y-6">
          {loadingHistory && (
            <div className="py-12 text-center text-sm text-on-surface-variant">Đang tải lịch sử trò chuyện...</div>
          )}

          {historyError && (
            <div role="alert" className="mx-auto max-w-md rounded-xl border border-error/20 bg-error-container/30 p-4 text-sm text-error text-center">
              {historyError}
            </div>
          )}

          {!loadingHistory && !historyError && messages.map((msg) => (
            <div key={msg.id}>
              <ChatBubble message={msg} isAI={msg.role === 'ai'} />
              {msg.products && msg.products.length > 0 && (
                <div className={`mt-3 ${msg.role === 'ai' ? 'ml-11' : 'mr-11'}`}>
                  <div className="grid grid-cols-2 gap-3 max-w-[500px]">
                    {msg.products.map((prod) => (
                      <ProductBlock key={prod.productId} product={prod} bundleId={msg.bundleId} />
                    ))}
                  </div>
                  {msg.role === 'ai' && msg.bundleId && (
                    <div className="mt-3 flex gap-2">
                      <button
                        onClick={() => handleAddFullOutfit(msg.products, msg.bundleId)}
                        disabled={addingBundleId === msg.bundleId}
                        className="flex items-center gap-1.5 px-3 py-1.5 bg-primary text-on-primary rounded-lg text-xs font-medium hover:opacity-90 transition-opacity disabled:opacity-50"
                      >
                        <ShoppingBag size={12} /> {addingBundleId === msg.bundleId ? 'Đang thêm...' : 'Thêm trọn set'}
                      </button>
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

          {sendError && (
            <div role="alert" className="mx-auto max-w-md rounded-xl border border-error/20 bg-error-container/30 p-4 text-sm text-error text-center flex items-center gap-2 justify-center">
              <AlertTriangle size={14} /> {sendError}
            </div>
          )}

          {!loadingHistory && !historyError && messages.length === 0 && (
            <div className="flex flex-col items-center justify-center py-12">
              <Sparkles size={32} className="text-tertiary mb-4" />
              <p className="text-on-surface-variant text-center mb-6 max-w-sm">
                Hãy hỏi về phong cách, gợi ý trang phục hoặc lời khuyên phối đồ.
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
              placeholder="Mô tả phong cách bạn đang cần..."
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
