import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProductManagementComponent } from './product-management/product-management.component';
import { ProductFormComponent } from './product-form/product-form.component';
import { MediaManagerComponent } from './media-manager/media-manager.component';
import { SellerOrdersComponent } from './seller-orders/seller-orders.component';

const routes: Routes = [
  { path: 'dashboard', component: DashboardComponent },
  { path: 'products', component: ProductManagementComponent },
  { path: 'products/new', component: ProductFormComponent },
  { path: 'products/:id/edit', component: ProductFormComponent },
  { path: 'media', component: MediaManagerComponent },
  { path: 'orders', component: SellerOrdersComponent }
];

@NgModule({
  declarations: [
    DashboardComponent,
    ProductManagementComponent,
    ProductFormComponent,
    MediaManagerComponent,
    SellerOrdersComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes)
  ]
})
export class SellerModule {}
