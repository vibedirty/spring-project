import { createBrowserRouter } from 'react-router-dom'
import { AdminLayout } from '@/layouts/AdminLayout'
import { UserLayout } from '@/layouts/UserLayout'
import { AdminHomePage } from '@/pages/admin/AdminHomePage'
import { AdminLoginPlaceholderPage } from '@/pages/admin/AdminLoginPlaceholderPage'
import { ForbiddenPage } from '@/pages/shared/ForbiddenPage'
import { NotFoundPage } from '@/pages/shared/NotFoundPage'
import { AccountPage } from '@/pages/user/AccountPage'
import { AddressPage } from '@/pages/user/AddressPage'
import { CartPage } from '@/pages/user/CartPage'
import { CategoriesPage } from '@/pages/user/CategoriesPage'
import { CheckoutPage } from '@/pages/user/CheckoutPage'
import { HomePage } from '@/pages/user/HomePage'
import { OrderDetailPage } from '@/pages/user/OrderDetailPage'
import { OrderListPage } from '@/pages/user/OrderListPage'
import { ProductDetailPage } from '@/pages/user/ProductDetailPage'
import { RegisterPage } from '@/pages/user/RegisterPage'
import { UserLoginPage } from '@/pages/user/UserLoginPage'
import { RoleGuard } from './RoleGuard'
import { paths } from './paths'

export const router = createBrowserRouter([
  {
    element: <UserLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: paths.categories, element: <CategoriesPage /> },
      { path: paths.productDetail, element: <ProductDetailPage /> },
      { path: paths.login, element: <UserLoginPage /> },
      { path: paths.register, element: <RegisterPage /> },
      {
        element: <RoleGuard allowedRoles={['USER']} loginPath={paths.login} />,
        children: [
          { path: paths.account, element: <AccountPage /> },
          { path: paths.addresses, element: <AddressPage /> },
          { path: paths.cart, element: <CartPage /> },
          { path: paths.checkout, element: <CheckoutPage /> },
          { path: paths.orders, element: <OrderListPage /> },
          { path: paths.orderDetail, element: <OrderDetailPage /> },
        ],
      },
    ],
  },
  { path: paths.adminLogin, element: <AdminLoginPlaceholderPage /> },
  {
    element: (
      <RoleGuard allowedRoles={['ADMIN']} loginPath={paths.adminLogin} />
    ),
    children: [
      {
        path: paths.adminHome,
        element: <AdminLayout />,
        children: [{ index: true, element: <AdminHomePage /> }],
      },
    ],
  },
  { path: paths.forbidden, element: <ForbiddenPage /> },
  { path: '*', element: <NotFoundPage /> },
])
