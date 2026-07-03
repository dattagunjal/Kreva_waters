import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import {
  AuthRequest, AuthResponse, RegisterRequest,
  SendOtpRequest, SendOtpResponse, User
} from '../../shared/models/models';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = `${environment.apiUrl}/api/auth`;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();

  get currentUser(): User | null {
    return this.currentUserSubject.value;
  }

  constructor(private http: HttpClient, private router: Router) {
    const token = localStorage.getItem('token');
    const stored = localStorage.getItem('user');
    
    if (token && this.isTokenExpired(token)) {
      this.logout();
    } else if (stored) {
      this.currentUserSubject.next(JSON.parse(stored));
    }
  }

  private isTokenExpired(token: string): boolean {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return true;
      const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
      if (payload.exp) {
        return (Date.now() / 1000) >= payload.exp;
      }
      return false;
    } catch (e) {
      return true;
    }
  }

  /** Send OTP for either registration or passwordless login */
  sendOtp(req: SendOtpRequest): Observable<SendOtpResponse> {
    return this.http.post<SendOtpResponse>(`${this.apiUrl}/send-otp`, req);
  }

  register(req: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, req).pipe(
      tap(res => this.storeSession(res))
    );
  }

  login(req: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, req).pipe(
      tap(res => this.storeSession(res))
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('kreva_cart');
    localStorage.removeItem('last_active');
    this.currentUserSubject.next(null);
    this.router.navigate(['/auth/login']);
  }

  getToken(): string | null        { return localStorage.getItem('token'); }
  isLoggedIn(): boolean            { return !!this.getToken(); }
  isAdmin(): boolean               { return this.currentUserSubject.value?.role === 'ADMIN'; }

  private storeSession(res: AuthResponse): void {
    localStorage.setItem('token', res.token);
    localStorage.setItem('user', JSON.stringify(res.user));
    this.currentUserSubject.next(res.user);
  }
}
