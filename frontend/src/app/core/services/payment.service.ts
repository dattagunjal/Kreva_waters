import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private apiUrl = `${environment.apiUrl}/api/payment`;

  constructor(private http: HttpClient) {}

  createOrder(amount: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/create-order`, { amount });
  }

  verifyPayment(payload: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/verify`, payload);
  }
}
