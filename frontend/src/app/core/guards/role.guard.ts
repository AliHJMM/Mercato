import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ToastrService } from 'ngx-toastr';
import { Role } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const requiredRole: Role = route.data['role'];
    const user = this.authService.currentUser;

    if (!user) {
      this.router.navigate(['/login']);
      return false;
    }

    if (user.role === requiredRole) {
      return true;
    }

    this.toastr.error(`This page requires ${requiredRole} role.`, 'Access Denied');
    this.router.navigate(['/']);
    return false;
  }
}
