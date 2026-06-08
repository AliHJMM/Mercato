import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { WishlistService, WishlistItem } from '../../core/services/wishlist.service';
import { CartService } from '../../core/services/cart.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-wishlist',
  templateUrl: './wishlist.component.html'
})
export class WishlistComponent {
  constructor(
    public wishlistService: WishlistService,
    private cartService: CartService,
    private toastr: ToastrService,
    private router: Router
  ) {}

  get items(): WishlistItem[] { return this.wishlistService.items; }

  moveToCart(item: WishlistItem): void {
    this.cartService.addToCart({
      id: item.productId,
      name: item.name,
      price: item.price,
      imageUrls: item.imageUrl ? [item.imageUrl] : [],
      sellerName: item.sellerName
    });
    this.wishlistService.remove(item.productId);
    this.toastr.success(`${item.name} moved to cart!`, 'Cart Updated');
  }

  remove(item: WishlistItem): void {
    this.wishlistService.remove(item.productId);
    this.toastr.info(`${item.name} removed from wishlist.`, 'Wishlist');
  }

  clearAll(): void {
    this.wishlistService.clear();
  }

  shopNow(): void {
    this.router.navigate(['/']);
  }
}
