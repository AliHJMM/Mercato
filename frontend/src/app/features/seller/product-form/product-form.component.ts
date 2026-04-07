import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { MediaService } from '../../../core/services/media.service';
import { ToastrService } from 'ngx-toastr';
import { Product } from '../../../core/models/product.model';
import { Media } from '../../../core/models/media.model';

@Component({
  selector: 'app-product-form',
  templateUrl: './product-form.component.html'
})
export class ProductFormComponent implements OnInit {
  form!: FormGroup;
  isEdit = false;
  productId: string | null = null;
  loading = false;
  saving = false;
  imageUrls: string[] = [];
  uploadingImage = false;
  showMediaPicker = false;
  mediaLibrary: Media[] = [];
  loadingMedia = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService,
    private mediaService: MediaService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      description: [''],
      price: [null, [Validators.required, Validators.min(0.01)]],
      quantity: [null, [Validators.required, Validators.min(0)]]
    });

    this.productId = this.route.snapshot.paramMap.get('id');
    if (this.productId) {
      this.isEdit = true;
      this.loading = true;
      this.productService.getById(this.productId).subscribe({
        next: (product: Product) => {
          this.form.patchValue({
            name: product.name,
            description: product.description,
            price: product.price,
            quantity: product.quantity
          });
          this.imageUrls = [...(product.imageUrls || [])];
          this.loading = false;
        },
        error: () => {
          this.toastr.error('Product not found', 'Error');
          this.router.navigate(['/seller/products']);
        }
      });
    }
  }

  get name() { return this.form.get('name')!; }
  get price() { return this.form.get('price')!; }
  get quantity() { return this.form.get('quantity')!; }

  onImageFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    input.value = '';

    const error = this.mediaService.validateFile(file);
    if (error) {
      this.toastr.error(error, 'Invalid File');
      return;
    }

    this.uploadingImage = true;
    this.mediaService.uploadImage(file, this.productId || undefined).subscribe({
      next: (media) => {
        this.imageUrls.push(media.url);
        this.uploadingImage = false;
        this.toastr.success('Image uploaded successfully', 'Uploaded');
      },
      error: (err) => {
        this.uploadingImage = false;
        const msg = err.error?.message || 'Failed to upload image';
        this.toastr.error(msg, 'Upload Failed');
      }
    });
  }

  removeImage(index: number): void {
    this.imageUrls.splice(index, 1);
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
    if (!this.imageUrls.includes(url)) {
      this.imageUrls.push(url);
    }
    this.showMediaPicker = false;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const request = { ...this.form.value, imageUrls: this.imageUrls };

    const operation = this.isEdit
      ? this.productService.update(this.productId!, request)
      : this.productService.create(request);

    operation.subscribe({
      next: () => {
        this.toastr.success(
          this.isEdit ? 'Product updated successfully' : 'Product created successfully',
          'Success'
        );
        this.router.navigate(['/seller/products']);
      },
      error: (err) => {
        this.saving = false;
        const msg = err.error?.message || 'Failed to save product';
        this.toastr.error(msg, 'Error');
      }
    });
  }
}
