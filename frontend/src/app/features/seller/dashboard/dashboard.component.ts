import { Component, OnInit } from '@angular/core';
import { ProductService } from '../../../core/services/product.service';
import { MediaService } from '../../../core/services/media.service';
import { AuthService } from '../../../core/services/auth.service';
import { Product } from '../../../core/models/product.model';
import { Media } from '../../../core/models/media.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  products: Product[] = [];
  media: Media[] = [];
  loading = true;

  constructor(
    private productService: ProductService,
    private mediaService: MediaService,
    public authService: AuthService
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
  }

  get totalValue(): number {
    return this.products.reduce((sum, p) => sum + (p.price * p.quantity), 0);
  }

  get recentProducts(): Product[] {
    return this.products.slice(0, 5);
  }
}
