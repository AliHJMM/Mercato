import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface CartItem {
  productId: string;
  name: string;
  price: number;
  quantity: number;
  imageUrl?: string;
  sellerName: string;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly CART_KEY = 'mercato_cart';
  private cartSubject = new BehaviorSubject<CartItem[]>(this.loadCart());
  cart$ = this.cartSubject.asObservable();

  get items(): CartItem[] { return this.cartSubject.value; }

  get count(): number { return this.items.reduce((sum, i) => sum + i.quantity, 0); }

  get total(): number { return this.items.reduce((sum, i) => sum + i.price * i.quantity, 0); }

  addToCart(product: { id: string; name: string; price: number; imageUrls: string[]; sellerName: string }, qty = 1): void {
    const items = [...this.items];
    const existing = items.find(i => i.productId === product.id);
    if (existing) {
      existing.quantity += qty;
    } else {
      items.push({
        productId: product.id,
        name: product.name,
        price: product.price,
        quantity: qty,
        imageUrl: product.imageUrls?.[0],
        sellerName: product.sellerName
      });
    }
    this.save(items);
  }

  removeFromCart(productId: string): void {
    this.save(this.items.filter(i => i.productId !== productId));
  }

  updateQuantity(productId: string, qty: number): void {
    if (qty <= 0) { this.removeFromCart(productId); return; }
    this.save(this.items.map(i => i.productId === productId ? { ...i, quantity: qty } : i));
  }

  clearCart(): void { this.save([]); }

  private save(items: CartItem[]): void {
    localStorage.setItem(this.CART_KEY, JSON.stringify(items));
    this.cartSubject.next(items);
  }

  private loadCart(): CartItem[] {
    const stored = localStorage.getItem(this.CART_KEY);
    return stored ? JSON.parse(stored) : [];
  }
}
