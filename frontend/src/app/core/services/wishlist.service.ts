import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface WishlistItem {
  productId: string;
  name: string;
  price: number;
  imageUrl?: string;
  sellerName: string;
  category?: string;
}

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private readonly KEY = 'mercato_wishlist';
  private subject = new BehaviorSubject<WishlistItem[]>(this.load());
  wishlist$ = this.subject.asObservable();

  get items(): WishlistItem[] { return this.subject.value; }
  get count(): number { return this.items.length; }

  isWishlisted(productId: string): boolean {
    return this.items.some(i => i.productId === productId);
  }

  toggle(product: { id: string; name: string; price: number; imageUrls?: string[]; sellerName: string; category?: string }): void {
    if (this.isWishlisted(product.id)) {
      this.save(this.items.filter(i => i.productId !== product.id));
    } else {
      this.save([...this.items, {
        productId: product.id,
        name: product.name,
        price: product.price,
        imageUrl: product.imageUrls?.[0],
        sellerName: product.sellerName,
        category: product.category
      }]);
    }
  }

  remove(productId: string): void {
    this.save(this.items.filter(i => i.productId !== productId));
  }

  clear(): void { this.save([]); }

  private save(items: WishlistItem[]): void {
    localStorage.setItem(this.KEY, JSON.stringify(items));
    this.subject.next(items);
  }

  private load(): WishlistItem[] {
    const stored = localStorage.getItem(this.KEY);
    return stored ? JSON.parse(stored) : [];
  }
}
