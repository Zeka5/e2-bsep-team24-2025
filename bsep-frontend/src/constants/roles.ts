export const ROLES = {
  USER: 'USER',
  ADMIN: 'ADMIN',
  CA: 'CA',
} as const;

export type Role = typeof ROLES[keyof typeof ROLES];