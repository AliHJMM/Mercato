import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { OrderService } from './order.service';
import { Order, PlaceOrderRequest } from '../models/order.model';

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;

  const mockOrder: Order = {
    id: 'order-1',
    buyerId: 'buyer-1',
    items: [],
    total: 25.0,
    status: 'PLACED',
    createdAt: new Date().toISOString()
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OrderService]
    });
    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('places an order', () => {
    const req: PlaceOrderRequest = {
      items: [{ productId: 'p1', quantity: 1 }],
      deliveryAddress: {
        fullName: 'John', street: '123 Main St', city: 'NY',
        state: 'NY', zipCode: '10001', country: 'US', phone: '+1555'
      },
      paymentMethod: 'PAY_ON_DELIVERY'
    };

    service.placeOrder(req).subscribe(order => {
      expect(order.id).toBe('order-1');
      expect(order.status).toBe('PLACED');
    });

    const mockReq = httpMock.expectOne('/api/orders');
    expect(mockReq.request.method).toBe('POST');
    mockReq.flush(mockOrder);
  });

  it('gets my orders without filter', () => {
    service.getMyOrders().subscribe(orders => {
      expect(orders.length).toBe(1);
      expect(orders[0].id).toBe('order-1');
    });

    const mockReq = httpMock.expectOne('/api/orders/my');
    expect(mockReq.request.method).toBe('GET');
    mockReq.flush([mockOrder]);
  });

  it('gets my orders with status filter', () => {
    service.getMyOrders('PLACED').subscribe();

    const mockReq = httpMock.expectOne('/api/orders/my?status=PLACED');
    expect(mockReq.request.method).toBe('GET');
    mockReq.flush([mockOrder]);
  });

  it('cancels an order', () => {
    service.cancelOrder('order-1').subscribe(order => {
      expect(order.status).toBe('CANCELLED');
    });

    const mockReq = httpMock.expectOne('/api/orders/order-1/cancel');
    expect(mockReq.request.method).toBe('PUT');
    mockReq.flush({ ...mockOrder, status: 'CANCELLED' });
  });

  it('deletes an order', () => {
    service.deleteOrder('order-1').subscribe();

    const mockReq = httpMock.expectOne('/api/orders/order-1');
    expect(mockReq.request.method).toBe('DELETE');
    mockReq.flush(null);
  });

  it('re-orders', () => {
    service.redoOrder('order-1').subscribe(order => {
      expect(order.status).toBe('PLACED');
    });

    const mockReq = httpMock.expectOne('/api/orders/order-1/redo');
    expect(mockReq.request.method).toBe('POST');
    mockReq.flush(mockOrder);
  });

  it('gets seller orders', () => {
    service.getSellerOrders().subscribe(orders => {
      expect(orders).toBeDefined();
    });

    const mockReq = httpMock.expectOne('/api/orders/seller');
    expect(mockReq.request.method).toBe('GET');
    mockReq.flush([mockOrder]);
  });

  it('gets user analytics', () => {
    service.getUserAnalytics().subscribe(analytics => {
      expect(analytics.totalSpent).toBe(100);
    });

    const mockReq = httpMock.expectOne('/api/orders/analytics/me');
    mockReq.flush({ totalSpent: 100, totalOrders: 5, mostBoughtProducts: [], topCategories: [] });
  });
});
