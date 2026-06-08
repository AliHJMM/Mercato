import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { AuthService } from '../../../core/services/auth.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ToastrService } from 'ngx-toastr';
import { Product } from '../../../core/models/product.model';

@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html'
})
export class ProductListComponent implements OnInit {
  products: Product[] = [];
  loading = true;
  error = '';

  constructor(
    private productService: ProductService,
    public authService: AuthService,
    public wishlistService: WishlistService,
    private toastr: ToastrService,
    private router: Router
  ) {}

  toggleWishlist(product: Product, event: Event): void {
    event.stopPropagation();
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']); return;
    }
    this.wishlistService.toggle(product);
    const msg = this.wishlistService.isWishlisted(product.id)
      ? `${product.name} added to wishlist!`
      : `${product.name} removed from wishlist.`;
    this.toastr.info(msg, 'Wishlist');
  }

  ngOnInit(): void {
    this.productService.getAll().subscribe({
      next: (products) => {
        this.products = products;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load products. Please try again.';
        this.loading = false;
      }
    });
  }

  getFirstImage(product: Product): string | null {
    return product.imageUrls?.length > 0 ? product.imageUrls[0] : null;
  }

  isNew(product: Product): boolean {
    if (!product.createdAt) return false;
    const diffDays = (Date.now() - new Date(product.createdAt).getTime()) / (1000 * 60 * 60 * 24);
    return diffDays < 7;
  }

  onStartSelling(): void {
    if (this.authService.isSeller()) {
      this.router.navigate(['/seller/dashboard']);
    } else {
      this.router.navigate(['/register']);
    }
  }
}
