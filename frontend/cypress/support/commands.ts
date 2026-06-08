declare global {
  namespace Cypress {
    interface Chainable {
      loginAsClient(email?: string, password?: string): Chainable<void>;
    }
  }
}

Cypress.Commands.add('loginAsClient', (email = 'client@test.com', password = 'Password1') => {
  cy.visit('/login');
  cy.get('input[type="email"]').type(email);
  cy.get('input[type="password"]').type(password);
  cy.get('button[type="submit"]').click();
  cy.url().should('not.include', '/login');
});
