import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastrService } from 'ngx-toastr';

const USERNAME_PATTERN = /^[a-zA-Z]+$/;
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&_#^()\-+=])[A-Za-z\d@$!%*?&_#^()\-+=]{6,50}$/;

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  form: FormGroup;
  loading = false;
  showPassword = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {
    this.form = this.fb.group({
      username: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        Validators.pattern(USERNAME_PATTERN)
      ]],
      email: ['', [
        Validators.required,
        Validators.email,
        Validators.maxLength(50)
      ]],
      password: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(50),
        Validators.pattern(PASSWORD_PATTERN)
      ]],
      role: ['CLIENT', Validators.required]
    });

    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/']);
    }
  }

  get username() { return this.form.get('username')!; }
  get email()    { return this.form.get('email')!; }
  get password() { return this.form.get('password')!; }
  get role()     { return this.form.get('role')!; }

  // Live password requirement checks
  get pwValue(): string { return this.password.value || ''; }
  get pwHasMin():     boolean { return this.pwValue.length >= 6; }
  get pwHasUpper():   boolean { return /[A-Z]/.test(this.pwValue); }
  get pwHasLower():   boolean { return /[a-z]/.test(this.pwValue); }
  get pwHasDigit():   boolean { return /\d/.test(this.pwValue); }
  get pwHasSpecial(): boolean { return /[@$!%*?&_#^()\-+=]/.test(this.pwValue); }

  selectRole(role: 'CLIENT' | 'SELLER'): void {
    this.role.setValue(role);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.authService.register(this.form.value).subscribe({
      next: (res) => {
        this.toastr.success(`Account created! Welcome, ${res.username}!`, 'Registration Successful');
        this.router.navigate([res.role === 'SELLER' ? '/seller/dashboard' : '/']);
      },
      error: (err) => {
        this.loading = false;
        const msg = err.error?.message || 'Registration failed. Please try again.';
        this.toastr.error(msg, 'Registration Failed');
      }
    });
  }
}
