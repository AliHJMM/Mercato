export interface OrderItem {
  productId: string;
  productName: string;
  sellerName: string;
  imageUrl: string;
  price: number;
  quantity: number;
  subtotal: number;
}

export interface Order {
  id: string;
  buyerId: string;
  items: OrderItem[];
  total: number;
  status: string;
  createdAt: string;
}

export interface PlaceOrderRequest {
  items: { productId: string; quantity: number }[];
}
