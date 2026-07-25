export function getPageItems(currentPage, totalPages) {
  if (totalPages <= 7) return Array.from({ length: totalPages }, (_, index) => index)

  const pages = new Set([0, totalPages - 1, currentPage - 1, currentPage, currentPage + 1])
  const sorted = [...pages].filter((page) => page >= 0 && page < totalPages).sort((a, b) => a - b)
  const items = []
  sorted.forEach((page, index) => {
    if (index > 0 && page - sorted[index - 1] > 1) items.push(`ellipsis-${page}`)
    items.push(page)
  })
  return items
}
