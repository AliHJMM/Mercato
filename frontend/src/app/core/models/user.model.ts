export type Role = 'CLIENT' | 'SELLER';

export interface User {
  id: string;
  username: string;
  email: string;
  role: Role;
  avatarUrl?: string;
  createdAt?: string;
}

export interface AuthResponse {
  token: string;
  id: string;
  username: string;
  email: string;
  role: Role;
  avatarUrl?: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  role: Role;
}

export interface LoginRequest {
  email: string;
  password: string;
}
