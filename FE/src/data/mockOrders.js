export const mockOrders = [
  {
    id: 'ORD-2026-001',
    date: '2026-05-28',
    status: 'shipped',
    items: [
      { productId: '1', name: 'Silk Midi Dress', size: 'M', color: 'Ivory', quantity: 1, price: 285, image: 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=200&h=200&fit=crop' },
      { productId: '5', name: 'Leather Crossbody Bag', size: 'One Size', color: 'Cognac', quantity: 1, price: 340, image: 'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=200&h=200&fit=crop' },
    ],
    total: 625,
    carrier: 'FedEx Express',
    tracking: 'FX-9283746510',
    timeline: [
      { status: 'pending', date: '2026-05-28T10:00:00', completed: true },
      { status: 'confirmed', date: '2026-05-28T10:15:00', completed: true },
      { status: 'processing', date: '2026-05-29T08:30:00', completed: true },
      { status: 'shipped', date: '2026-05-30T14:00:00', completed: true },
      { status: 'delivered', date: null, completed: false },
    ],
  },
  {
    id: 'ORD-2026-002',
    date: '2026-05-20',
    status: 'delivered',
    items: [
      { productId: '2', name: 'Structured Wool Blazer', size: 'L', color: 'Charcoal', quantity: 1, price: 420, image: 'https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=200&h=200&fit=crop' },
    ],
    total: 420,
    carrier: 'UPS',
    tracking: 'UP-182736450',
    timeline: [
      { status: 'pending', date: '2026-05-20T09:00:00', completed: true },
      { status: 'confirmed', date: '2026-05-20T09:20:00', completed: true },
      { status: 'processing', date: '2026-05-21T07:00:00', completed: true },
      { status: 'shipped', date: '2026-05-22T11:00:00', completed: true },
      { status: 'delivered', date: '2026-05-25T14:30:00', completed: true },
    ],
  },
  {
    id: 'ORD-2026-003',
    date: '2026-06-01',
    status: 'processing',
    items: [
      { productId: '7', name: 'Linen Wrap Dress', size: 'S', color: 'Sky Blue', quantity: 1, price: 225, image: 'https://images.unsplash.com/photo-1496747611176-843222e1e57c?w=200&h=200&fit=crop' },
      { productId: '6', name: 'Suede Ankle Boots', size: '38', color: 'Taupe', quantity: 1, price: 275, image: 'https://images.unsplash.com/photo-1543163521-1bf539c55dd2?w=200&h=200&fit=crop' },
      { productId: '8', name: 'Oversized Sunglasses', size: 'One Size', color: 'Tortoise', quantity: 1, price: 185, image: 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=200&h=200&fit=crop' },
    ],
    total: 685,
    carrier: null,
    tracking: null,
    timeline: [
      { status: 'pending', date: '2026-06-01T12:00:00', completed: true },
      { status: 'confirmed', date: '2026-06-01T12:30:00', completed: true },
      { status: 'processing', date: '2026-06-02T08:00:00', completed: true },
      { status: 'shipped', date: null, completed: false },
      { status: 'delivered', date: null, completed: false },
    ],
  },
]
