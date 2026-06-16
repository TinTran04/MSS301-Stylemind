import { mockAnalytics } from '../../data/mockAnalytics'

export async function getDashboardMetrics() {
  return mockAnalytics.dashboard
}

export async function getAIPipelineEvents() {
  return mockAnalytics.aiPipeline.recentEvents
}

export async function getRecommendationFunnel() {
  return mockAnalytics.funnel
}

export async function getTopProducts() {
  return mockAnalytics.topProducts
}

export async function getCTRData() {
  return mockAnalytics.ctrOverTime
}

export async function getKnowledgeGraph() {
  return mockAnalytics.knowledgeGraph
}

export async function getAdminOrders() {
  return mockAnalytics.adminOrders
}
