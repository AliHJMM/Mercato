import { CartService } from './cart.service';

describe('CartService', () => {
  let service: CartService;

  const mockProduct = {
    id: 'prod-1',
    name: 'Test Product',
    price: 10.0,
    imageUrls: ['http://example.com/img.jpg'],
    sellerName: 'Seller'
  };

  beforeEach(() => {
    localStorage.clear();
    service = new CartService();
  });

  it('starts with an empty cart', () => {
    expect(service.items.length).toBe(0);
    expect(service.count).toBe(0);
    expect(service.total).toBe(0);
  });

  it('adds a product to the cart', () => {
    service.addToCart(mockProduct);
    expect(service.items.length).toBe(1);
    expect(service.items[0].name).toBe('Test Product');
    expect(service.count).toBe(1);
  });

  it('increases quantity when same product added twice', () => {
    service.addToCart(mockProduct, 1);
    service.addToCart(mockProduct, 2);
    expect(service.items.length).toBe(1);
    expect(service.items[0].quantity).toBe(3);
    expect(service.count).toBe(3);
  });

  it('calculates total correctly', () => {
    service.addToCart(mockProduct, 3);
    expect(service.total).toBeCloseTo(30.0);
  });

  it('removes a product from the cart', () => {
    service.addToCart(mockProduct);
    service.removeFromCart('prod-1');
    expect(service.items.length).toBe(0);
  });

  it('updates quantity of an item', () => {
    service.addToCart(mockProduct);
    service.updateQuantity('prod-1', 5);
    expect(service.items[0].quantity).toBe(5);
  });

  it('removes item when quantity updated to 0', () => {
    service.addToCart(mockProduct);
    service.updateQuantity('prod-1', 0);
    expect(service.items.length).toBe(0);
  });

  it('clears all items', () => {
    service.addToCart(mockProduct, 2);
    service.clearCart();
    expect(service.items.length).toBe(0);
    expect(service.count).toBe(0);
  });

  it('emits cart$ updates on changes', (done) => {
    service.cart$.subscribe(items => {
      if (items.length > 0) {
        expect(items[0].name).toBe('Test Product');
        done();
      }
    });
    service.addToCart(mockProduct);
  });
});
