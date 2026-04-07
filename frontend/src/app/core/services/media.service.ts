import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Media } from '../models/media.model';

@Injectable({ providedIn: 'root' })
export class MediaService {
  private readonly apiUrl = '/api/media';
  private readonly MAX_SIZE = 2 * 1024 * 1024; // 2MB
  private readonly ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

  constructor(private http: HttpClient) {}

  uploadImage(file: File, productId?: string): Observable<Media> {
    const formData = new FormData();
    formData.append('file', file);
    if (productId) {
      formData.append('productId', productId);
    }
    return this.http.post<Media>(`${this.apiUrl}/images`, formData).pipe(
      map(m => ({ ...m, url: this.getImageUrl(m.id) }))
    );
  }

  getMyMedia(): Observable<Media[]> {
    return this.http.get<Media[]>(`${this.apiUrl}/my`).pipe(
      map(list => list.map(m => ({ ...m, url: this.getImageUrl(m.id) })))
    );
  }

  getMediaInfo(id: string): Observable<Media> {
    return this.http.get<Media>(`${this.apiUrl}/images/${id}/info`).pipe(
      map(m => ({ ...m, url: this.getImageUrl(m.id) }))
    );
  }

  deleteMedia(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/images/${id}`);
  }

  validateFile(file: File): string | null {
    if (!this.ALLOWED_TYPES.includes(file.type)) {
      return `Invalid file type "${file.type}". Only JPEG, PNG, GIF, and WebP images are allowed.`;
    }
    if (file.size > this.MAX_SIZE) {
      return `File size ${(file.size / 1024 / 1024).toFixed(2)}MB exceeds the 2MB limit.`;
    }
    return null;
  }

  getImageUrl(id: string): string {
    return `${this.apiUrl}/images/${id}`;
  }
}
