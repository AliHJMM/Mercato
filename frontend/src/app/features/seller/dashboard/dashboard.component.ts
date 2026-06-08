import { Component, OnInit } from '@angular/core';
import { ProductService } from '../../../core/services/product.service';
import { MediaService } from '../../../core/services/media.service';
import { AuthService } from '../../../core/services/auth.service';
import { OrderService } from '../../../core/services/order.service';
import { Product } from '../../../core/models/product.model';
import { Media } from '../../../core/models/media.model';
import { SellerAnalytics } from '../../../core/models/order.model';
import { forkJoin } from 'rxjs';
import { ChartConfiguration } from 'chart.js';

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

  revenueChartConfig: ChartConfiguration | null = null;
  unitsChartConfig: ChartConfiguration | null = null;

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
      next: (a) => {
        this.analytics = a;
        this.buildSellerCharts(a);
        this.analyticsLoading = false;
      },
      error: () => { this.analyticsLoading = false; }
    });
  }

  private buildSellerCharts(a: SellerAnalytics): void {
    if (!a.bestSellingProducts?.length) return;
    const labels = a.bestSellingProducts.map(p => p.productName);

    this.revenueChartConfig = {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Revenue ($)',
          data: a.bestSellingProducts.map(p => p.revenue),
          backgroundColor: 'rgba(124,58,237,0.8)',
          borderColor: '#7C3AED',
          borderWidth: 1,
          borderRadius: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y',
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { label: (ctx) => ` $${Number(ctx.parsed.x).toFixed(2)}` } }
        },
        scales: {
          x: { beginAtZero: true, ticks: { callback: (v) => `$${v}` }, grid: { color: 'rgba(0,0,0,0.05)' } },
          y: { grid: { display: false } }
        }
      }
    };

    this.unitsChartConfig = {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Units Sold',
          data: a.bestSellingProducts.map(p => p.unitsSold),
          backgroundColor: 'rgba(245,158,11,0.8)',
          borderColor: '#F59E0B',
          borderWidth: 1,
          borderRadius: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y',
        plugins: { legend: { display: false } },
        scales: {
          x: { beginAtZero: true, ticks: { stepSize: 1 }, grid: { color: 'rgba(0,0,0,0.05)' } },
          y: { grid: { display: false } }
        }
      }
    };
  }

  get totalValue(): number {
    return this.products.reduce((sum, p) => sum + (p.price * p.quantity), 0);
  }

  get recentProducts(): Product[] {
    return this.products.slice(0, 5);
  }
}
