export const paths = {
  home: '/',
  login: '/login',
  register: '/register',
  categories: '/categories',
  productDetail: '/products/:productId',
  account: '/account',
  addresses: '/addresses',
  cart: '/cart',
  checkout: '/checkout',
  orders: '/orders',
  orderDetail: '/orders/:orderNo',
  adminLogin: '/admin/login',
  adminHome: '/admin',
  forbidden: '/403',
} as const

export function productDetailPath(productId: number): string {
  return `/products/${productId}`
}

export function orderDetailPath(orderNo: string): string {
  return `/orders/${encodeURIComponent(orderNo)}`
}
