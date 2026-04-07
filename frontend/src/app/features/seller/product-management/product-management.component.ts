import { Component, OnInit } from '@angular/core';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../core/models/product.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-product-management',
  templateUrl: './product-management.component.html'
})
export class ProductManagementComponent implements OnInit {
  products: Product[] = [];
  loading = true;
  deletingId: string | null = null;

  constructor(
    private productService: ProductService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.productService.getMyProducts().subscribe({
      next: (products) => {
        this.products = products;
        this.loading = false;
      },
      error: () => {
        this.toastr.error('Failed to load products', 'Error');
        this.loading = false;
      }
    });
  }

  deleteProduct(product: Product): void {
    if (!confirm(`Are you sure you want to delete "${product.name}"?`)) return;

    this.deletingId = product.id;
    this.productService.delete(product.id).subscribe({
      next: () => {
        this.products = this.products.filter(p => p.id !== product.id);
        this.deletingId = null;
        this.toastr.success(`"${product.name}" deleted successfully`, 'Deleted');
      },
      error: (err) => {
        this.deletingId = null;
        const msg = err.error?.message || 'Failed to delete product';
        this.toastr.error(msg, 'Error');
      }
    });
  }
}
