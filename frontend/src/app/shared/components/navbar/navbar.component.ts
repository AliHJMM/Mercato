import { Component } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { Router } from '@angular/router';
import { User } from '../../../core/models/user.model';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html'
})
export class NavbarComponent {
  currentUser$: Observable<User | null>;

  constructor(
    private authService: AuthService,
    public cartService: CartService,
    public wishlistService: WishlistService,
    private router: Router
  ) {
    this.currentUser$ = this.authService.currentUser$;
  }

  logout(): void {
    this.authService.logout();
  }

  isSeller(): boolean {
    return this.authService.isSeller();
  }

  isClient(): boolean {
    return this.authService.isClient();
  }
}
