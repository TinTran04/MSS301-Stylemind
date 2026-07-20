import { Fragment } from 'react'

// Lightweight markdown-ish renderer for AI replies (bold/italic/code, bullet and
// numbered lists, headings, paragraphs). Kept dependency-free on purpose ΓÇö the
// agent replies in plain prose with occasional markdown emphasis and lists.

function renderInline(text) {
  // Split on **bold**, *italic* and `code` spans, preserving the delimiters' content.
  const parts = text.split(/(\*\*[^*]+\*\*|\*[^*\n]+\*|`[^`]+`)/g)
  return parts.map((part, i) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={i} className="font-semibold">{part.slice(2, -2)}</strong>
    }
    if (part.startsWith('*') && part.endsWith('*') && part.length > 2) {
      return <em key={i}>{part.slice(1, -1)}</em>
    }
    if (part.startsWith('`') && part.endsWith('`') && part.length > 2) {
      return (
        <code key={i} className="px-1 py-0.5 rounded bg-surface-container text-[0.85em] font-mono">
          {part.slice(1, -1)}
        </code>
      )
    }
    return <Fragment key={i}>{part}</Fragment>
  })
}

function parseBlocks(content) {
  const lines = content.split('\n')
  const blocks = []
  let list = null

  const flushList = () => {
    if (list) {
      blocks.push(list)
      list = null
    }
  }

  for (const rawLine of lines) {
    const line = rawLine.trimEnd()
    const bulletMatch = line.match(/^\s*[-*ΓÇó]\s+(.*)/)
    const numberedMatch = line.match(/^\s*(\d+)[.)]\s+(.*)/)
    const headingMatch = line.match(/^#{1,4}\s+(.*)/)

    if (bulletMatch) {
      if (!list || list.type !== 'ul') {
        flushList()
        list = { type: 'ul', items: [] }
      }
      list.items.push(bulletMatch[1])
    } else if (numberedMatch) {
      if (!list || list.type !== 'ol') {
        flushList()
        list = { type: 'ol', items: [] }
      }
      list.items.push(numberedMatch[2])
    } else {
      flushList()
      if (headingMatch) {
        blocks.push({ type: 'heading', text: headingMatch[1] })
      } else if (line.trim()) {
        blocks.push({ type: 'p', text: line })
      }
    }
  }
  flushList()
  return blocks
}

export default function MessageContent({ content }) {
  if (!content) return null
  const blocks = parseBlocks(content)

  return (
    <div className="space-y-2">
      {blocks.map((block, i) => {
        if (block.type === 'ul') {
          return (
            <ul key={i} className="list-disc pl-5 space-y-1">
              {block.items.map((item, j) => <li key={j}>{renderInline(item)}</li>)}
            </ul>
          )
        }
        if (block.type === 'ol') {
          return (
            <ol key={i} className="list-decimal pl-5 space-y-1">
              {block.items.map((item, j) => <li key={j}>{renderInline(item)}</li>)}
            </ol>
          )
        }
        if (block.type === 'heading') {
          return <p key={i} className="font-semibold text-primary">{renderInline(block.text)}</p>
        }
        return <p key={i}>{renderInline(block.text)}</p>
      })}
    </div>
  )
}