import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { ToastrService } from 'ngx-toastr';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    const authSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn']);
    const toastrSpy = jasmine.createSpyObj('ToastrService', ['warning']);

    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [
        AuthGuard,
        { provide: AuthService, useValue: authSpy },
        { provide: ToastrService, useValue: toastrSpy }
      ]
    });

    guard = TestBed.inject(AuthGuard);
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router);
  });

  it('allows navigation when user is logged in', () => {
    authService.isLoggedIn.and.returnValue(true);
    const result = guard.canActivate();
    expect(result).toBe(true);
  });

  it('redirects to /login when user is not logged in', () => {
    authService.isLoggedIn.and.returnValue(false);
    const navigateSpy = spyOn(router, 'navigate');
    const result = guard.canActivate();
    expect(result).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });
});
