import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product, ProductRequest, ProductSearchResponse } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly apiUrl = '/api/products';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Product[]> {
    return this.http.get<Product[]>(this.apiUrl);
  }

  getById(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  getMyProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/my`);
  }

  search(params: {
    q?: string;
    category?: string;
    minPrice?: number;
    maxPrice?: number;
    sort?: string;
    page?: number;
    size?: number;
  }): Observable<ProductSearchResponse> {
    let httpParams = new HttpParams();
    if (params.q) httpParams = httpParams.set('q', params.q);
    if (params.category) httpParams = httpParams.set('category', params.category);
    if (params.minPrice != null) httpParams = httpParams.set('minPrice', params.minPrice.toString());
    if (params.maxPrice != null) httpParams = httpParams.set('maxPrice', params.maxPrice.toString());
    if (params.sort) httpParams = httpParams.set('sort', params.sort);
    if (params.page != null) httpParams = httpParams.set('page', params.page.toString());
    if (params.size != null) httpParams = httpParams.set('size', params.size.toString());
    return this.http.get<ProductSearchResponse>(`${this.apiUrl}/search`, { params: httpParams });
  }

  getCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/categories`);
  }

  create(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, request);
  }

  update(id: string, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
