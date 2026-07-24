import { useCallback, useEffect, useState } from 'react'
import { getCategories } from '../products/product.api'

let cachedCategories = null
let pendingRequest = null

async function fetchCategories() {
  if (cachedCategories) return cachedCategories
  if (!pendingRequest) {
    pendingRequest = getCategories()
      .then((categories) => {
        cachedCategories = categories
        return categories
      })
      .finally(() => {
        pendingRequest = null
      })
  }
  return pendingRequest
}

export function useHeaderCategories(enabled) {
  const [categories, setCategories] = useState(cachedCategories || [])
  const [status, setStatus] = useState(cachedCategories ? 'ready' : 'idle')

  const loadCategories = useCallback(async () => {
    setStatus('loading')
    try {
      const nextCategories = await fetchCategories()
      setCategories(nextCategories)
      setStatus('ready')
    } catch {
      setStatus('error')
    }
  }, [])

  useEffect(() => {
    if (enabled && status === 'idle') loadCategories()
  }, [enabled, loadCategories, status])

  return { categories, status, retry: loadCategories }
}
