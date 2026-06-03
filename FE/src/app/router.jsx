import { Routes, Route, Navigate } from 'react-router-dom'
import CustomerLayout from '../layouts/CustomerLayout'
import AdminLayout from '../layouts/AdminLayout'
import AuthLayout from '../layouts/AuthLayout'

import HomePage from '../pages/customer/HomePage'
import ProductCatalogPage from '../pages/customer/ProductCatalogPage'
import ProductDetailPage from '../pages/customer/ProductDetailPage'
import AIStylistChatPage from '../pages/customer/AIStylistChatPage'
import CartPage from '../pages/customer/CartPage'
import CheckoutPage from '../pages/customer/CheckoutPage'
import OrderTrackingPage from '../pages/customer/OrderTrackingPage'
import StyleProfilePage from '../pages/auth/StyleProfilePage'
import LoginPage from '../pages/auth/LoginPage'
import RegisterPage from '../pages/auth/RegisterPage'

import AdminDashboardPage from '../pages/admin/AdminDashboardPage'
import ProductManagementPage from '../pages/admin/ProductManagementPage'
import InventoryManagementPage from '../pages/admin/InventoryManagementPage'
import OrderManagementPage from '../pages/admin/OrderManagementPage'
import CustomerManagementPage from '../pages/admin/CustomerManagementPage'
import AIPipelinePage from '../pages/admin/AIPipelinePage'
import KnowledgeGraphPage from '../pages/admin/KnowledgeGraphPage'
import RecommendationAnalyticsPage from '../pages/admin/RecommendationAnalyticsPage'
import AdminSettingsPage from '../pages/admin/AdminSettingsPage'

export default function AppRouter() {
  return (
    <Routes>
      {/* Auth Routes */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/style-profile" element={<StyleProfilePage />} />
      </Route>

      {/* Customer Routes */}
      <Route element={<CustomerLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/shop" element={<ProductCatalogPage />} />
        <Route path="/products/:id" element={<ProductDetailPage />} />
        <Route path="/ai-stylist" element={<AIStylistChatPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/orders" element={<OrderTrackingPage />} />
      </Route>

      {/* Admin Routes */}
      <Route element={<AdminLayout />}>
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/admin/products" element={<ProductManagementPage />} />
        <Route path="/admin/inventory" element={<InventoryManagementPage />} />
        <Route path="/admin/orders" element={<OrderManagementPage />} />
        <Route path="/admin/customers" element={<CustomerManagementPage />} />
        <Route path="/admin/ai-pipeline" element={<AIPipelinePage />} />
        <Route path="/admin/knowledge-graph" element={<KnowledgeGraphPage />} />
        <Route path="/admin/recommendations" element={<RecommendationAnalyticsPage />} />
        <Route path="/admin/settings" element={<AdminSettingsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
