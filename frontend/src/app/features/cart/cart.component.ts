import { Component } from '@angular/core';
import { CartService, CartItem } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html'
})
export class CartComponent {
  orderPlaced = false;
  placingOrder = false;

  constructor(
    public cartService: CartService,
    private orderService: OrderService,
    private router: Router,
    private toastr: ToastrService
  ) {}

  get items(): CartItem[] { return this.cartService.items; }

  increment(item: CartItem): void {
    this.cartService.updateQuantity(item.productId, item.quantity + 1);
  }

  decrement(item: CartItem): void {
    this.cartService.updateQuantity(item.productId, item.quantity - 1);
  }

  remove(item: CartItem): void {
    this.cartService.removeFromCart(item.productId);
  }

  placeOrder(): void {
    const request = {
      items: this.items.map(item => ({ productId: item.productId, quantity: item.quantity }))
    };
    this.placingOrder = true;
    this.orderService.placeOrder(request).subscribe({
      next: () => {
        this.cartService.clearCart();
        this.orderPlaced = true;
        this.placingOrder = false;
        this.toastr.success('Order placed successfully!', 'Order Placed');
      },
      error: (err) => {
        this.placingOrder = false;
        const msg = err.error?.message || 'Failed to place order. Please try again.';
        this.toastr.error(msg, 'Order Failed');
      }
    });
  }

  viewOrders(): void {
    this.router.navigate(['/orders']);
  }

  continueShopping(): void {
    this.router.navigate(['/']);
  }
}
