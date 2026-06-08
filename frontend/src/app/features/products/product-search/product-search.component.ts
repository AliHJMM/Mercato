import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { Product } from '../../../core/models/product.model';
import { ToastrService } from 'ngx-toastr';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';

@Component({
  selector: 'app-product-search',
  templateUrl: './product-search.component.html'
})
export class ProductSearchComponent implements OnInit {
  products: Product[] = [];
  categories: string[] = [];
  loading = false;
  error = '';

  q = '';
  selectedCategory = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  sort = 'newest';
  page = 0;
  size = 12;
  total = 0;
  totalPages = 0;

  private searchSubject = new Subject<string>();

  readonly sortOptions = [
    { value: 'newest', label: 'Newest First' },
    { value: 'price_asc', label: 'Price: Low to High' },
    { value: 'price_desc', label: 'Price: High to Low' },
    { value: 'name_asc', label: 'Name: A–Z' }
  ];

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    public authService: AuthService,
    public wishlistService: WishlistService,
    private toastr: ToastrService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  toggleWishlist(product: Product, event: Event): void {
    event.stopPropagation();
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']); return;
    }
    this.wishlistService.toggle(product);
    const msg = this.wishlistService.isWishlisted(product.id)
      ? `${product.name} added to wishlist!`
      : `${product.name} removed from wishlist.`;
    this.toastr.info(msg, 'Wishlist');
  }

  ngOnInit(): void {
    this.productService.getCategories().subscribe({
      next: (cats) => this.categories = cats
    });

    this.route.queryParams.subscribe(params => {
      this.q = params['q'] || '';
      this.selectedCategory = params['category'] || '';
      this.sort = params['sort'] || 'newest';
      this.page = +(params['page'] || 0);
      this.doSearch();
    });

    this.searchSubject.pipe(debounceTime(350), distinctUntilChanged())
      .subscribe(() => this.doSearch());
  }

  onSearchInput(): void {
    this.page = 0;
    this.searchSubject.next(this.q);
  }

  onFilterChange(): void {
    this.page = 0;
    this.doSearch();
  }

  doSearch(): void {
    this.loading = true;
    this.error = '';
    this.productService.search({
      q: this.q || undefined,
      category: this.selectedCategory || undefined,
      minPrice: this.minPrice ?? undefined,
      maxPrice: this.maxPrice ?? undefined,
      sort: this.sort,
      page: this.page,
      size: this.size
    }).subscribe({
      next: (res) => {
        this.products = res.products;
        this.total = res.total;
        this.totalPages = res.totalPages;
        this.loading = false;
      },
      error: () => { this.error = 'Search failed. Please try again.'; this.loading = false; }
    });
  }

  clearFilters(): void {
    this.q = '';
    this.selectedCategory = '';
    this.minPrice = null;
    this.maxPrice = null;
    this.sort = 'newest';
    this.page = 0;
    this.doSearch();
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.page = p;
    this.doSearch();
    window.scrollTo(0, 0);
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  addToCart(product: Product): void {
    if (!this.authService.isLoggedIn()) {
      this.toastr.info('Please log in to add items to your cart.', 'Login Required');
      this.router.navigate(['/login']);
      return;
    }
    this.cartService.addToCart(product);
    this.toastr.success(`${product.name} added to cart!`, 'Cart Updated');
  }

  getFirstImage(product: Product): string | null {
    return product.imageUrls?.length > 0 ? product.imageUrls[0] : null;
  }
}
