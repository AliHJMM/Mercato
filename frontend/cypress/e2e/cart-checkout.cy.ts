describe('Cart → Checkout → Order Confirmation Flow', () => {

  beforeEach(() => {
    cy.window().then(win => win.localStorage.clear());
  });

  it('shows empty cart message when no items in cart', () => {
    cy.loginAsClient();
    cy.visit('/cart');
    cy.contains('Nothing here yet').should('be.visible');
    cy.contains('Shop Now').should('be.visible');
  });

  it('adds a product to cart and verifies cart badge', () => {
    cy.loginAsClient();
    cy.visit('/');

    cy.get('button').contains('Add to Cart').first().click();
    cy.get('.toast').should('be.visible');

    cy.get('[routerLink="/cart"]').should('contain.text', '1');
  });

  it('completes checkout wizard - step 1 to step 2', () => {
    cy.loginAsClient();
    cy.visit('/');

    cy.get('button').contains('Add to Cart').first().click();
    cy.visit('/cart');

    // Step 1: cart items visible
    cy.contains('Continue to Delivery Address').should('be.visible').click();

    // Step 2: address form visible
    cy.get('input[formControlName="fullName"]').should('be.visible');
    cy.contains('Delivery Address').should('be.visible');
  });

  it('validates address form before proceeding to payment', () => {
    cy.loginAsClient();
    cy.visit('/');
    cy.get('button').contains('Add to Cart').first().click();
    cy.visit('/cart');

    cy.contains('Continue to Delivery Address').click();

    // Try to proceed without filling form
    cy.contains('Continue to Payment').click();

    // Validation errors should appear
    cy.contains('Full name is required').should('be.visible');
  });

  it('fills address and reaches payment step', () => {
    cy.loginAsClient();
    cy.visit('/');
    cy.get('button').contains('Add to Cart').first().click();
    cy.visit('/cart');

    cy.contains('Continue to Delivery Address').click();

    cy.get('input[formControlName="fullName"]').type('John Doe');
    cy.get('input[formControlName="street"]').type('123 Main Street');
    cy.get('input[formControlName="city"]').type('Springfield');
    cy.get('input[formControlName="state"]').type('IL');
    cy.get('input[formControlName="zipCode"]').type('62701');
    cy.get('input[formControlName="country"]').type('United States');
    cy.get('input[formControlName="phone"]').type('+1555000111');

    cy.contains('Continue to Payment').click();

    // Step 3: payment options visible
    cy.contains('Payment Method').should('be.visible');
    cy.contains('Pay on Delivery').should('be.visible');
  });

  it('selects payment and reaches review step', () => {
    cy.loginAsClient();
    cy.visit('/');
    cy.get('button').contains('Add to Cart').first().click();
    cy.visit('/cart');

    // Step 1 → 2
    cy.contains('Continue to Delivery Address').click();

    cy.get('input[formControlName="fullName"]').type('John Doe');
    cy.get('input[formControlName="street"]').type('123 Main Street');
    cy.get('input[formControlName="city"]').type('Springfield');
    cy.get('input[formControlName="state"]').type('IL');
    cy.get('input[formControlName="zipCode"]').type('62701');
    cy.get('input[formControlName="country"]').type('United States');
    cy.get('input[formControlName="phone"]').type('+1555000111');

    // Step 2 → 3
    cy.contains('Continue to Payment').click();

    // Pay on Delivery is default — step 3 → 4
    cy.contains('Review Order').click();

    // Step 4: review summary
    cy.contains('Pay on Delivery').should('be.visible');
    cy.contains('Order Items').should('be.visible');
    cy.contains('Delivery to').should('be.visible');
    cy.contains('John Doe').should('be.visible');
  });

  it('product search page loads and shows results', () => {
    cy.visit('/search');
    cy.contains('Search Products').should('be.visible');
    cy.get('input[placeholder*="Search products"]').should('be.visible');
  });

  it('product search filters by keyword', () => {
    cy.visit('/search');
    cy.get('input[placeholder*="Search products"]').type('keyboard');
    cy.wait(600);
    cy.get('.card').should('have.length.greaterThan', 0);
  });

  it('orders page shows orders for authenticated user', () => {
    cy.loginAsClient();
    cy.visit('/orders');
    cy.url().should('include', '/orders');
    cy.contains('My Orders').should('be.visible');
  });
});
