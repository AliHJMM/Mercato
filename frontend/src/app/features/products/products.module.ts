import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { ProductListComponent } from './product-list/product-list.component';
import { ProductDetailComponent } from './product-detail/product-detail.component';
import { ProductSearchComponent } from './product-search/product-search.component';

const routes: Routes = [
  { path: '', component: ProductListComponent },
  { path: 'search', component: ProductSearchComponent },
  { path: 'products/:id', component: ProductDetailComponent }
];

@NgModule({
  declarations: [ProductListComponent, ProductDetailComponent, ProductSearchComponent],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule.forChild(routes)
  ]
})
export class ProductsModule {}
