import { Component, OnInit } from '@angular/core';
import { MediaService } from '../../../core/services/media.service';
import { Media } from '../../../core/models/media.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-media-manager',
  templateUrl: './media-manager.component.html'
})
export class MediaManagerComponent implements OnInit {
  media: Media[] = [];
  loading = true;
  uploading = false;
  deletingId: string | null = null;
  dragOver = false;

  constructor(
    private mediaService: MediaService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadMedia();
  }

  loadMedia(): void {
    this.loading = true;
    this.mediaService.getMyMedia().subscribe({
      next: (media) => {
        this.media = media;
        this.loading = false;
      },
      error: () => {
        this.toastr.error('Failed to load media', 'Error');
        this.loading = false;
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.uploadFile(input.files[0]);
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file) this.uploadFile(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = true;
  }

  onDragLeave(): void {
    this.dragOver = false;
  }

  private uploadFile(file: File): void {
    const error = this.mediaService.validateFile(file);
    if (error) {
      this.toastr.error(error, 'Invalid File');
      return;
    }

    this.uploading = true;
    this.mediaService.uploadImage(file).subscribe({
      next: (uploaded) => {
        this.media.unshift(uploaded);
        this.uploading = false;
        this.toastr.success(`"${file.name}" uploaded successfully`, 'Upload Success');
      },
      error: (err) => {
        this.uploading = false;
        const msg = err.error?.message || 'Upload failed';
        this.toastr.error(msg, 'Upload Failed');
      }
    });
  }

  deleteMedia(mediaItem: Media): void {
    if (!confirm(`Delete image "${mediaItem.originalFilename}"?`)) return;
    this.deletingId = mediaItem.id;
    this.mediaService.deleteMedia(mediaItem.id).subscribe({
      next: () => {
        this.media = this.media.filter(m => m.id !== mediaItem.id);
        this.deletingId = null;
        this.toastr.success('Image deleted', 'Deleted');
      },
      error: (err) => {
        this.deletingId = null;
        const msg = err.error?.message || 'Failed to delete image';
        this.toastr.error(msg, 'Error');
      }
    });
  }

  formatSize(bytes: number): string {
    return bytes < 1024 * 1024
      ? `${(bytes / 1024).toFixed(1)} KB`
      : `${(bytes / 1024 / 1024).toFixed(2)} MB`;
  }

  copyUrl(url: string): void {
    navigator.clipboard.writeText(url).then(() => {
      this.toastr.info('URL copied to clipboard', 'Copied');
    });
  }
}
