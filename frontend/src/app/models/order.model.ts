export interface OrderItem {
  id?: number;
  productId: number;
  productName?: string;
  quantity: number;
  price?: number;
  totalPrice?: number;
}

export interface Order {
  id: number;
  userId?: number;
  userName?: string;
  userEmail?: string;
  userMobile?: string;
  address: string;
  paymentMethod: string;
  paymentId?: string;
  status: string;
  totalAmount: number;
  items: OrderItem[];
  createdAt?: string;
  updatedAt?: string;
}

export interface OrderRequest {
  address: string;
  paymentMethod: string;
  paymentId?: string;
  items: { productId: number; quantity: number }[];
}
