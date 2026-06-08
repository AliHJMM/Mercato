export type OrderStatus = 'PLACED' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
export type PaymentMethod = 'PAY_ON_DELIVERY' | 'CREDIT_CARD' | 'PAYPAL';

export interface DeliveryAddress {
  fullName: string;
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  phone: string;
}

export interface OrderItem {
  productId: string;
  productName: string;
  sellerId: string;
  sellerName: string;
  imageUrl: string;
  category: string;
  price: number;
  quantity: number;
  subtotal: number;
}

export interface Order {
  id: string;
  buyerId: string;
  items: OrderItem[];
  total: number;
  status: OrderStatus;
  paymentMethod?: PaymentMethod;
  deliveryAddress?: DeliveryAddress;
  cancelledAt?: string;
  createdAt: string;
}

export interface PlaceOrderRequest {
  items: { productId: string; quantity: number }[];
  deliveryAddress: DeliveryAddress;
  paymentMethod: PaymentMethod;
}

export interface UserAnalytics {
  totalSpent: number;
  totalOrders: number;
  mostBoughtProducts: ProductPurchase[];
  topCategories: CategorySpend[];
}

export interface ProductPurchase {
  productId: string;
  productName: string;
  totalQuantity: number;
  totalSpent: number;
}

export interface CategorySpend {
  category: string;
  totalSpent: number;
  totalItems: number;
}

export interface SellerAnalytics {
  totalRevenue: number;
  totalOrdersProcessed: number;
  totalUnitsSold: number;
  bestSellingProducts: ProductSales[];
}

export interface ProductSales {
  productId: string;
  productName: string;
  unitsSold: number;
  revenue: number;
}
