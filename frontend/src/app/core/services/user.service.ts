import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly apiUrl = '/api/users';

  constructor(private http: HttpClient) {}

  getMe(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/me`);
  }

  updateMe(request: { username?: string; avatarUrl?: string }): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/me`, request);
  }

  deleteMe(): Observable<void> {
    // responseType: 'text' avoids JSON-parse errors on 204 No Content responses
    return this.http.delete(`${this.apiUrl}/me`, { responseType: 'text' }).pipe(map(() => undefined));
  }
}
