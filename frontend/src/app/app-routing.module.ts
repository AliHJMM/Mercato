import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { RoleGuard } from './core/guards/role.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';

const routes: Routes = [
  // Auth (eagerly loaded — needed immediately, no benefit from lazy loading)
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  // Seller dashboard (lazy, SELLER role only)
  {
    path: 'seller',
    loadChildren: () => import('./features/seller/seller.module').then(m => m.SellerModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'SELLER' }
  },

  // Profile (lazy, authenticated)
  {
    path: 'profile',
    loadChildren: () => import('./features/profile/profile.module').then(m => m.ProfileModule),
    canActivate: [AuthGuard]
  },

  // Cart (lazy, authenticated)
  {
    path: 'cart',
    loadChildren: () => import('./features/cart/cart.module').then(m => m.CartModule),
    canActivate: [AuthGuard]
  },

  // Orders history (lazy, authenticated)
  {
    path: 'orders',
    loadChildren: () => import('./features/orders/orders.module').then(m => m.OrdersModule),
    canActivate: [AuthGuard]
  },

  // Wishlist (lazy, CLIENT only)
  {
    path: 'wishlist',
    loadChildren: () => import('./features/wishlist/wishlist.module').then(m => m.WishlistModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'CLIENT' }
  },

  // Public product listing + search (lazy, last so it doesn't swallow other routes)
  {
    path: '',
    loadChildren: () => import('./features/products/products.module').then(m => m.ProductsModule)
  },

  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
