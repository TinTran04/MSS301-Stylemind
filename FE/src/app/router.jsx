import { Routes, Route, Navigate } from 'react-router-dom'
import CustomerLayout from '../layouts/CustomerLayout'
import AdminLayout from '../layouts/AdminLayout'
import AuthLayout from '../layouts/AuthLayout'
import { RequireAuth, RequireAdmin } from './ProtectedRoute'

import HomePage from '../pages/customer/HomePage'
import ProductCatalogPage from '../pages/customer/ProductCatalogPage'
import ProductDetailPage from '../pages/customer/ProductDetailPage'
import AIStylistChatPage from '../pages/customer/AIStylistChatPage'
import CartPage from '../pages/customer/CartPage'
import CheckoutPage from '../pages/customer/CheckoutPage'
import OrderTrackingPage from '../pages/customer/OrderTrackingPage'
import NotificationsPage from '../pages/customer/NotificationsPage'
import StyleProfilePage from '../pages/auth/StyleProfilePage'
import LoginPage from '../pages/auth/LoginPage'
import RegisterPage from '../pages/auth/RegisterPage'
import ForgotPasswordPage from '../pages/auth/ForgotPasswordPage'
import VerifyResetOtpPage from '../pages/auth/VerifyResetOtpPage'
import ResetPasswordPage from '../pages/auth/ResetPasswordPage'

import AdminDashboardPage from '../pages/admin/AdminDashboardPage'
import ProductManagementPage from '../pages/admin/ProductManagementPage'
import OrderManagementPage from '../pages/admin/OrderManagementPage'
import AdminOrderDetailPage from '../pages/admin/AdminOrderDetailPage'
import UserManagementPage from '../pages/admin/UserManagementPage'
import AIPipelinePage from '../pages/admin/AIPipelinePage'
import KnowledgeGraphPage from '../pages/admin/KnowledgeGraphPage'
import RecommendationAnalyticsPage from '../pages/admin/RecommendationAnalyticsPage'
import NotificationManagementPage from '../pages/admin/NotificationManagementPage'

export default function AppRouter() {
  return (
    <Routes>
      {/* Auth Routes */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/verify-reset-otp" element={<VerifyResetOtpPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/set-password" element={<ResetPasswordPage />} />
        <Route element={<RequireAuth />}>
          <Route path="/profile" element={<StyleProfilePage />} />
          <Route path="/style-profile" element={<StyleProfilePage />} />
        </Route>
      </Route>

      {/* Customer Routes */}
      <Route element={<CustomerLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/shop" element={<ProductCatalogPage />} />
        <Route path="/products/:id" element={<ProductDetailPage />} />
        <Route path="/ai-stylist" element={<AIStylistChatPage />} />
        <Route path="/cart" element={<CartPage />} />
        {/* Checkout & order tracking require a signed-in user */}
        <Route element={<RequireAuth />}>
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/orders" element={<OrderTrackingPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
        </Route>
      </Route>

      {/* Admin Routes — admin role only */}
      <Route element={<RequireAdmin />}>
        <Route element={<AdminLayout />}>
          <Route path="/admin" element={<AdminDashboardPage />} />
          <Route path="/admin/products" element={<ProductManagementPage />} />
          <Route path="/admin/orders" element={<OrderManagementPage />} />
          <Route path="/admin/orders/:orderId" element={<AdminOrderDetailPage />} />
          <Route path="/admin/users" element={<UserManagementPage />} />
          <Route path="/admin/ai-pipeline" element={<AIPipelinePage />} />
          <Route path="/admin/knowledge-graph" element={<KnowledgeGraphPage />} />
          <Route path="/admin/recommendations" element={<RecommendationAnalyticsPage />} />
          <Route path="/admin/notifications" element={<NotificationManagementPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
