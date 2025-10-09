export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  DASHBOARD: '/dashboard',
  ADMIN: '/admin',
  MY_PROFILE: '/my-profile',
  CERTIFICATES: '/certificates',
  MY_CERTIFICATES: '/my-certificates',
  USERS: '/users',
  CA_ASSIGNMENTS: '/ca-assignments',
  MY_CSRS: '/my-csrs',
  CSR_REVIEW: '/csr-review',
  PASSWORD_MANAGER: '/password-manager',
} as const;

export type RouteKey = keyof typeof ROUTES;
export type RouteValue = typeof ROUTES[RouteKey];