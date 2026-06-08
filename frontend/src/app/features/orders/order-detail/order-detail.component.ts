import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/order.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-order-detail',
  templateUrl: './order-detail.component.html'
})
export class OrderDetailComponent implements OnInit {
  order: Order | null = null;
  loading = true;
  error = '';
  actionLoading = false;

  readonly timeline = ['PLACED', 'CONFIRMED', 'SHIPPED', 'DELIVERED'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.orderService.getOrderById(id).subscribe({
      next: (order) => { this.order = order; this.loading = false; },
      error: () => { this.error = 'Order not found.'; this.loading = false; }
    });
  }

  get timelineStep(): number {
    if (!this.order || this.order.status === 'CANCELLED') return -1;
    return this.timeline.indexOf(this.order.status);
  }

  canCancel(): boolean {
    return this.order?.status === 'PLACED' || this.order?.status === 'CONFIRMED';
  }

  canDelete(): boolean {
    return this.order?.status === 'PLACED';
  }

  canRedo(): boolean {
    return this.order?.status === 'CANCELLED' || this.order?.status === 'DELIVERED';
  }

  cancel(): void {
    if (!this.order || !confirm('Cancel this order?')) return;
    this.actionLoading = true;
    this.orderService.cancelOrder(this.order.id).subscribe({
      next: (o) => { this.order = o; this.actionLoading = false; this.toastr.success('Order cancelled.'); },
      error: (err) => { this.actionLoading = false; this.toastr.error(err.error?.message || 'Failed.', 'Error'); }
    });
  }

  remove(): void {
    if (!this.order || !confirm('Delete this order permanently?')) return;
    this.actionLoading = true;
    this.orderService.deleteOrder(this.order.id).subscribe({
      next: () => { this.router.navigate(['/orders']); this.toastr.success('Order deleted.'); },
      error: (err) => { this.actionLoading = false; this.toastr.error(err.error?.message || 'Failed.', 'Error'); }
    });
  }

  redo(): void {
    if (!this.order) return;
    this.actionLoading = true;
    this.orderService.redoOrder(this.order.id).subscribe({
      next: (o) => { this.router.navigate(['/orders', o.id]); this.toastr.success('Order re-placed!'); },
      error: (err) => { this.actionLoading = false; this.toastr.error(err.error?.message || 'Failed.', 'Error'); }
    });
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
}
