import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { CartService, CartItem } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html'
})
export class CartComponent implements OnInit {
  step = 1;
  addressForm!: FormGroup;
  paymentForm!: FormGroup;
  placingOrder = false;
  placedOrderId: string | null = null;

  readonly paymentOptions = [
    { value: 'PAY_ON_DELIVERY', label: 'Pay on Delivery',  icon: 'bi-cash-coin',      desc: 'Pay when your order arrives. No upfront cost.' },
    { value: 'CREDIT_CARD',     label: 'Credit Card',       icon: 'bi-credit-card',    desc: 'Visa, Mastercard, or any major card.' },
    { value: 'PAYPAL',          label: 'PayPal',            icon: 'bi-paypal',         desc: 'Pay using your PayPal account.' }
  ];

  constructor(
    public cartService: CartService,
    private orderService: OrderService,
    private router: Router,
    private toastr: ToastrService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.addressForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      street:   ['', Validators.required],
      city:     ['', Validators.required],
      state:    ['', Validators.required],
      zipCode:  ['', Validators.required],
      country:  ['', Validators.required],
      phone:    ['', [Validators.required, Validators.pattern(/^\+?[\d\s\-]{7,20}$/)]]
    });

    this.paymentForm = this.fb.group({
      method:         ['PAY_ON_DELIVERY', Validators.required],
      cardNumber:     [''],
      cardHolder:     [''],
      cardExpiry:     [''],
      cardCvv:        [''],
      paypalEmail:    ['']
    });

    this.paymentForm.get('method')!.valueChanges.subscribe(m => this.updatePaymentValidators(m));
  }

  private updatePaymentValidators(method: string): void {
    const controls = ['cardNumber', 'cardHolder', 'cardExpiry', 'cardCvv', 'paypalEmail'];
    controls.forEach(c => {
      this.paymentForm.get(c)!.clearValidators();
      this.paymentForm.get(c)!.updateValueAndValidity();
    });

    if (method === 'CREDIT_CARD') {
      this.paymentForm.get('cardNumber')!.setValidators([Validators.required, Validators.pattern(/^\d{4} ?\d{4} ?\d{4} ?\d{4}$/)]);
      this.paymentForm.get('cardHolder')!.setValidators(Validators.required);
      this.paymentForm.get('cardExpiry')!.setValidators([Validators.required, Validators.pattern(/^(0[1-9]|1[0-2])\/\d{2}$/)]);
      this.paymentForm.get('cardCvv')!.setValidators([Validators.required, Validators.pattern(/^\d{3,4}$/)]);
    } else if (method === 'PAYPAL') {
      this.paymentForm.get('paypalEmail')!.setValidators([Validators.required, Validators.email]);
    }

    controls.forEach(c => this.paymentForm.get(c)!.updateValueAndValidity());
  }

  get items(): CartItem[] { return this.cartService.items; }
  get selectedMethod(): string { return this.paymentForm.get('method')!.value; }

  increment(item: CartItem): void { this.cartService.updateQuantity(item.productId, item.quantity + 1); }
  decrement(item: CartItem): void { this.cartService.updateQuantity(item.productId, item.quantity - 1); }
  remove(item: CartItem): void    { this.cartService.removeFromCart(item.productId); }

  goToAddress(): void {
    if (this.items.length === 0) return;
    this.step = 2; window.scrollTo(0, 0);
  }

  goToPayment(): void {
    if (this.addressForm.invalid) { this.addressForm.markAllAsTouched(); return; }
    this.step = 3; window.scrollTo(0, 0);
  }

  goToReview(): void {
    if (this.paymentForm.invalid) { this.paymentForm.markAllAsTouched(); return; }
    this.step = 4; window.scrollTo(0, 0);
  }

  backToCart():    void { this.step = 1; window.scrollTo(0, 0); }
  backToAddress(): void { this.step = 2; window.scrollTo(0, 0); }
  backToPayment(): void { this.step = 3; window.scrollTo(0, 0); }

  getPaymentLabel(value: string): string {
    return this.paymentOptions.find(o => o.value === value)?.label ?? value;
  }

  getPaymentIcon(value: string): string {
    return this.paymentOptions.find(o => o.value === value)?.icon ?? 'bi-credit-card';
  }

  confirmOrder(): void {
    if (this.addressForm.invalid || this.paymentForm.invalid) return;

    const request = {
      items: this.items.map(i => ({ productId: i.productId, quantity: i.quantity })),
      deliveryAddress: this.addressForm.value,
      paymentMethod: this.selectedMethod
    };

    this.placingOrder = true;
    this.orderService.placeOrder(request as any).subscribe({
      next: (order) => {
        this.cartService.clearCart();
        this.placedOrderId = order.id;
        this.step = 5;
        this.placingOrder = false;
        window.scrollTo(0, 0);
        this.toastr.success('Order placed successfully!', 'Order Placed');
      },
      error: (err) => {
        this.placingOrder = false;
        const msg = err.error?.message || 'Failed to place order. Please try again.';
        this.toastr.error(msg, 'Order Failed');
      }
    });
  }

  viewOrders():       void { this.router.navigate(['/orders']); }
  continueShopping(): void { this.router.navigate(['/']); }

  f(name: string) { return this.addressForm.get(name)!; }
  p(name: string) { return this.paymentForm.get(name)!; }
}
