export interface Product {
  id: string;
  name: string;
  description?: string;
  price: number;
  quantity: number;
  sellerId: string;
  sellerName: string;
  category?: string;
  imageUrls: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductRequest {
  name: string;
  description?: string;
  price: number;
  quantity: number;
  category?: string;
  imageUrls: string[];
}

export interface ProductSearchResponse {
  products: Product[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}
