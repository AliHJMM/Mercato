import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

const USERNAME_PATTERN = /^[a-zA-Z]+$/;
import { UserService } from '../../core/services/user.service';
import { MediaService } from '../../core/services/media.service';
import { AuthService } from '../../core/services/auth.service';
import { OrderService } from '../../core/services/order.service';
import { ToastrService } from 'ngx-toastr';
import { Media } from '../../core/models/media.model';
import { UserAnalytics } from '../../core/models/order.model';
import { ChartConfiguration } from 'chart.js';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
  form!: FormGroup;
  loading = true;
  saving = false;
  uploadingAvatar = false;
  showMediaPicker = false;
  mediaLibrary: Media[] = [];
  loadingMedia = false;

  analytics: UserAnalytics | null = null;
  analyticsLoading = false;

  mostBoughtChartConfig: ChartConfiguration | null = null;
  categoriesChartConfig: ChartConfiguration | null = null;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private mediaService: MediaService,
    public authService: AuthService,
    private orderService: OrderService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50), Validators.pattern(USERNAME_PATTERN)]],
      avatarUrl: ['']
    });

    this.userService.getMe().subscribe({
      next: (user) => {
        this.form.patchValue({ username: user.username, avatarUrl: user.avatarUrl || '' });
        this.loading = false;
      },
      error: () => {
        this.toastr.error('Failed to load profile', 'Error');
        this.loading = false;
      }
    });

    if (this.authService.isClient()) {
      this.loadAnalytics();
    }
  }

  loadAnalytics(): void {
    this.analyticsLoading = true;
    this.orderService.getUserAnalytics().subscribe({
      next: (a) => {
        this.analytics = a;
        this.buildUserCharts(a);
        this.analyticsLoading = false;
      },
      error: () => { this.analyticsLoading = false; }
    });
  }

  private buildUserCharts(a: UserAnalytics): void {
    if (a.mostBoughtProducts?.length) {
      this.mostBoughtChartConfig = {
        type: 'bar',
        data: {
          labels: a.mostBoughtProducts.map(p => p.productName),
          datasets: [{
            label: 'Units Bought',
            data: a.mostBoughtProducts.map(p => p.totalQuantity),
            backgroundColor: [
              'rgba(59,130,246,0.8)',
              'rgba(139,92,246,0.8)',
              'rgba(16,185,129,0.8)',
              'rgba(245,158,11,0.8)',
              'rgba(239,68,68,0.8)'
            ],
            borderWidth: 0,
            borderRadius: 4
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: {
            y: { beginAtZero: true, ticks: { stepSize: 1 }, grid: { color: 'rgba(0,0,0,0.05)' } },
            x: { grid: { display: false } }
          }
        }
      };
    }

    if (a.topCategories?.length) {
      this.categoriesChartConfig = {
        type: 'doughnut',
        data: {
          labels: a.topCategories.map(c => c.category),
          datasets: [{
            data: a.topCategories.map(c => c.totalSpent),
            backgroundColor: ['#7C3AED', '#3B82F6', '#10B981', '#F59E0B', '#EF4444'],
            borderWidth: 2,
            borderColor: '#fff',
            hoverOffset: 6
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } },
            tooltip: { callbacks: { label: (ctx) => ` $${Number(ctx.parsed).toFixed(2)}` } }
          }
        }
      };
    }
  }

  get username() { return this.form.get('username')!; }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    input.value = '';

    const error = this.mediaService.validateFile(file);
    if (error) { this.toastr.error(error, 'Invalid File'); return; }

    this.uploadingAvatar = true;
    this.mediaService.uploadImage(file).subscribe({
      next: (media) => {
        this.form.patchValue({ avatarUrl: media.url });
        this.uploadingAvatar = false;
        this.saveAvatar(media.url);
      },
      error: (err) => {
        this.uploadingAvatar = false;
        this.toastr.error(err.error?.message || 'Failed to upload avatar', 'Upload Failed');
      }
    });
  }

  private saveAvatar(avatarUrl: string): void {
    const payload = { username: this.form.value.username, avatarUrl };
    this.userService.updateMe(payload).subscribe({
      next: (user) => { this.authService.updateProfile(user); this.toastr.success('Profile photo updated!', 'Saved'); },
      error: () => { this.toastr.warning('Photo uploaded but profile save failed. Click Save Changes to apply.', 'Notice'); }
    });
  }

  openMediaPicker(): void {
    this.showMediaPicker = true;
    if (this.mediaLibrary.length === 0) {
      this.loadingMedia = true;
      this.mediaService.getMyMedia().subscribe({
        next: (media) => { this.mediaLibrary = media; this.loadingMedia = false; },
        error: () => { this.loadingMedia = false; }
      });
    }
  }

  selectFromMedia(url: string): void {
    this.form.patchValue({ avatarUrl: url });
    this.showMediaPicker = false;
    this.saveAvatar(url);
  }

  deleteAccount(): void {
    if (!confirm('Are you sure you want to delete your account? This cannot be undone.')) return;
    this.userService.deleteMe().subscribe({
      next: () => { this.toastr.info('Your account has been deleted.', 'Account Deleted'); this.authService.logout(); },
      error: (err) => { this.toastr.error(err?.error?.message || 'Failed to delete account.', 'Error'); }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.userService.updateMe(this.form.value).subscribe({
      next: (user) => { this.authService.updateProfile(user); this.saving = false; this.toastr.success('Profile updated successfully', 'Saved'); },
      error: (err) => { this.saving = false; this.toastr.error(err.error?.message || 'Failed to update profile', 'Error'); }
    });
  }
}
