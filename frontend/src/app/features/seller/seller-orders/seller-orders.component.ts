import { Component, OnInit } from '@angular/core';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/order.model';

@Component({
  selector: 'app-seller-orders',
  templateUrl: './seller-orders.component.html'
})
export class SellerOrdersComponent implements OnInit {
  orders: Order[] = [];
  filteredOrders: Order[] = [];
  loading = true;
  error = '';
  searchTerm = '';
  selectedStatus = '';

  readonly statuses = ['', 'PLACED', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

  constructor(private orderService: OrderService) {}

  ngOnInit(): void {
    this.orderService.getSellerOrders().subscribe({
      next: (orders) => { this.orders = orders; this.applyFilters(); this.loading = false; },
      error: () => { this.error = 'Failed to load orders.'; this.loading = false; }
    });
  }

  applyFilters(): void {
    let result = this.orders;
    if (this.selectedStatus) {
      result = result.filter(o => o.status === this.selectedStatus);
    }
    if (this.searchTerm.trim()) {
      const q = this.searchTerm.toLowerCase();
      result = result.filter(o =>
        o.id.toLowerCase().includes(q) ||
        o.items.some(i => i.productName.toLowerCase().includes(q))
      );
    }
    this.filteredOrders = result;
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PLACED':    return '#3B82F6';
      case 'CONFIRMED': return '#8B5CF6';
      case 'SHIPPED':   return '#F59E0B';
      case 'DELIVERED': return '#10B981';
      case 'CANCELLED': return '#EF4444';
      default:          return '#6B7280';
    }
  }

  advancingId: string | null = null;

  nextStatusLabel(status: string): string | null {
    switch (status) {
      case 'PLACED':    return 'Confirm Order';
      case 'CONFIRMED': return 'Mark as Shipped';
      case 'SHIPPED':   return 'Mark as Delivered';
      default:          return null;
    }
  }

  nextStatusIcon(status: string): string {
    switch (status) {
      case 'PLACED':    return 'bi-check-circle';
      case 'CONFIRMED': return 'bi-truck';
      case 'SHIPPED':   return 'bi-house-check';
      default:          return '';
    }
  }

  advanceStatus(order: Order): void {
    this.advancingId = order.id;
    this.orderService.advanceOrderStatus(order.id).subscribe({
      next: (updated) => {
        const idx = this.orders.findIndex(o => o.id === order.id);
        if (idx > -1) this.orders[idx] = updated;
        this.applyFilters();
        this.advancingId = null;
      },
      error: () => this.advancingId = null
    });
  }

  myItems(order: Order): { name: string; qty: number; subtotal: number }[] {
    return order.items.map(i => ({ name: i.productName, qty: i.quantity, subtotal: i.subtotal }));
  }
}
