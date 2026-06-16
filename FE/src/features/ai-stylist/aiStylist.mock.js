export const mockChatMessages = [
  {
    id: '1',
    role: 'ai',
    content: "Welcome back! I've been analyzing your recent style activity. Based on your preference for minimalist silhouettes and neutral tones, I have some exciting recommendations for you today.",
    timestamp: '2026-06-02T10:00:00',
    products: [],
  },
  {
    id: '2',
    role: 'user',
    content: "I need something elegant for a rooftop dinner party this weekend. Black tie optional.",
    timestamp: '2026-06-02T10:02:00',
    products: [],
  },
  {
    id: '3',
    role: 'ai',
    content: "A rooftop dinner calls for sophisticated pieces that transition from golden hour to evening. I've curated a selection that matches your minimalist aesthetic while making a statement.",
    timestamp: '2026-06-02T10:02:30',
    products: [
      { productId: '1', matchScore: 98 },
      { productId: '2', matchScore: 92 },
    ],
  },
]

export const mockStyleProfile = {
  name: 'Julianne',
  styleDNA: ['Minimalist', 'Classic'],
  topPicks: 24,
  savedOutfits: 8,
  avgMatchScore: 91,
}
