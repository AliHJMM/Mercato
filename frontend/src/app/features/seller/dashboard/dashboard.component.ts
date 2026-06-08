import { Component, OnInit } from '@angular/core';
import { ProductService } from '../../../core/services/product.service';
import { MediaService } from '../../../core/services/media.service';
import { AuthService } from '../../../core/services/auth.service';
import { OrderService } from '../../../core/services/order.service';
import { Product } from '../../../core/models/product.model';
import { Media } from '../../../core/models/media.model';
import { SellerAnalytics } from '../../../core/models/order.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  products: Product[] = [];
  media: Media[] = [];
  analytics: SellerAnalytics | null = null;
  loading = true;
  analyticsLoading = false;

  constructor(
    private productService: ProductService,
    private mediaService: MediaService,
    public authService: AuthService,
    private orderService: OrderService
  ) {}

  ngOnInit(): void {
    forkJoin({
      products: this.productService.getMyProducts(),
      media: this.mediaService.getMyMedia()
    }).subscribe({
      next: ({ products, media }) => {
        this.products = products;
        this.media = media;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });

    this.analyticsLoading = true;
    this.orderService.getSellerAnalytics().subscribe({
      next: (a) => { this.analytics = a; this.analyticsLoading = false; },
      error: () => { this.analyticsLoading = false; }
    });
  }

  get totalValue(): number {
    return this.products.reduce((sum, p) => sum + (p.price * p.quantity), 0);
  }

  get recentProducts(): Product[] {
    return this.products.slice(0, 5);
  }

  maxRevenue(): number {
    if (!this.analytics?.bestSellingProducts?.length) return 1;
    return Math.max(...this.analytics.bestSellingProducts.map(p => p.revenue));
  }
}
