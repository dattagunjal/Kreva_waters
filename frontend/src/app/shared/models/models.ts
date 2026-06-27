export interface User {
  id?: number;
  name: string;
  email?: string;
  mobileNumber?: string;
  password?: string;
  role?: 'USER' | 'ADMIN';
  createdAt?: string;
  address?: string;
  profileImage?: string;
}

// ── Auth request / response models ────────────────────────────────────────────

export interface SendOtpRequest {
  loginId: string;
  purpose: 'REGISTER' | 'LOGIN';
}

export interface SendOtpResponse {
  message: string;
  devOtp?: string;    // only present in mock mode — never use in production UI
}

export interface RegisterRequest {
  name: string;
  loginId: string;
  password: string;
  otp: string;
}

export interface AuthRequest {
  loginId: string;
  password?: string;  // null when using OTP login
  otp?: string;       // null when using password login
}

export interface AuthResponse {
  token: string;
  user: User;
}

// ── Product / Cart / Order models ─────────────────────────────────────────────

export interface Category {
  id?: number;
  name: string;
}

export interface Product {
  id?: number;
  name: string;
  description: string;
  price: number;
  stock: number;
  imageUrl: string;
  category?: Category;
}

export interface WaterBottleCustomization {
  businessName: string;
  logoUrl?: string;
  tagline?: string;
  contactNumber?: string;
  website?: string;
  notes?: string;
}

export interface CartItem {
  id?: number;
  product: Product;
  quantity: number;
  customization?: WaterBottleCustomization;
}

export interface Order {
  id?: number;
  userId?: number;
  items: CartItem[];
  totalAmount: number;
  status: 'PENDING' | 'CONFIRMED' | 'DELIVERED' | 'CANCELLED';
  createdAt?: string;
  address: string;
}

export interface Payment {
  orderId: number;
  amount: number;
  method: 'CARD' | 'UPI' | 'COD';
  status?: 'SUCCESS' | 'FAILED' | 'PENDING';
}
