import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Order, PlaceOrderRequest, UserAnalytics, SellerAnalytics } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly apiUrl = '/api/orders';

  constructor(private http: HttpClient) {}

  placeOrder(request: PlaceOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, request);
  }

  getMyOrders(status?: string): Observable<Order[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<Order[]>(`${this.apiUrl}/my`, { params });
  }

  getOrderById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${id}`);
  }

  cancelOrder(id: string): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${id}/cancel`, {});
  }

  deleteOrder(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  redoOrder(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/redo`, {});
  }

  advanceOrderStatus(id: string): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${id}/advance`, {});
  }

  getSellerOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/seller`);
  }

  getUserAnalytics(): Observable<UserAnalytics> {
    return this.http.get<UserAnalytics>(`${this.apiUrl}/analytics/me`);
  }

  getSellerAnalytics(): Observable<SellerAnalytics> {
    return this.http.get<SellerAnalytics>(`${this.apiUrl}/analytics/seller`);
  }
}
