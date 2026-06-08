import { Component, OnInit } from '@angular/core';
import { OrderService } from '../../core/services/order.service';
import { Order } from '../../core/models/order.model';
import { ToastrService } from 'ngx-toastr';
import { Router } from '@angular/router';

@Component({
  selector: 'app-orders',
  templateUrl: './orders.component.html'
})
export class OrdersComponent implements OnInit {
  orders: Order[] = [];
  filteredOrders: Order[] = [];
  loading = true;
  error = '';
  selectedStatus = '';
  actionLoading: { [orderId: string]: boolean } = {};

  readonly statuses = ['', 'PLACED', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

  constructor(
    private orderService: OrderService,
    private toastr: ToastrService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.error = '';
    const status = this.selectedStatus || undefined;
    this.orderService.getMyOrders(status).subscribe({
      next: (orders) => {
        this.orders = orders;
        this.filteredOrders = [...orders];
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load orders.';
        this.loading = false;
      }
    });
  }

  onStatusChange(): void {
    this.loadOrders();
  }

  viewDetail(id: string): void {
    this.router.navigate(['/orders', id]);
  }

  cancelOrder(order: Order): void {
    if (!confirm('Cancel this order?')) return;
    this.actionLoading[order.id] = true;
    this.orderService.cancelOrder(order.id).subscribe({
      next: (updated) => {
        this.replaceOrder(updated);
        this.actionLoading[order.id] = false;
        this.toastr.success('Order cancelled.', 'Done');
      },
      error: (err) => {
        this.actionLoading[order.id] = false;
        this.toastr.error(err.error?.message || 'Could not cancel order.', 'Error');
      }
    });
  }

  deleteOrder(order: Order): void {
    if (!confirm('Delete this order permanently?')) return;
    this.actionLoading[order.id] = true;
    this.orderService.deleteOrder(order.id).subscribe({
      next: () => {
        this.orders = this.orders.filter(o => o.id !== order.id);
        this.filteredOrders = this.filteredOrders.filter(o => o.id !== order.id);
        this.toastr.success('Order deleted.', 'Done');
      },
      error: (err) => {
        this.actionLoading[order.id] = false;
        this.toastr.error(err.error?.message || 'Could not delete order.', 'Error');
      }
    });
  }

  redoOrder(order: Order): void {
    this.actionLoading[order.id] = true;
    this.orderService.redoOrder(order.id).subscribe({
      next: () => {
        this.toastr.success('Order re-placed!', 'Done');
        this.loadOrders();
      },
      error: (err) => {
        this.actionLoading[order.id] = false;
        this.toastr.error(err.error?.message || 'Could not re-order.', 'Error');
      }
    });
  }

  canCancel(order: Order): boolean {
    return order.status === 'PLACED' || order.status === 'CONFIRMED';
  }

  canDelete(order: Order): boolean {
    return order.status === 'PLACED';
  }

  canRedo(order: Order): boolean {
    return order.status === 'CANCELLED' || order.status === 'DELIVERED';
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

  private replaceOrder(updated: Order): void {
    const idx = this.orders.findIndex(o => o.id === updated.id);
    if (idx !== -1) this.orders[idx] = updated;
    const idx2 = this.filteredOrders.findIndex(o => o.id === updated.id);
    if (idx2 !== -1) this.filteredOrders[idx2] = updated;
  }
}
