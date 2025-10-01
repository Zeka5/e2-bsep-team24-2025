export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  DASHBOARD: '/dashboard',
  ADMIN: '/admin',
  PROFILE: '/profile',
  CERTIFICATES: '/certificates',
  USERS: '/users',
  CA_ASSIGNMENTS: '/ca-assignments',
  MY_CSRS: '/my-csrs',
  CSR_REVIEW: '/csr-review',
} as const;

export type RouteKey = keyof typeof ROUTES;
export type RouteValue = typeof ROUTES[RouteKey];