import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private apiUrl = `${environment.apiUrl}/api/payment`;

  constructor(private http: HttpClient) {}

  createPaymentIntent(amount: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/create-intent`, { amount });
  }

  simulateWebhookSuccess(paymentIntentId: string): Observable<any> {
    const headers = new HttpHeaders().set('Stripe-Signature', 'mock_signature');
    const payload = {
      type: 'payment_intent.succeeded',
      data: {
        object: {
          id: paymentIntentId,
          status: 'succeeded'
        }
      }
    };
    return this.http.post<any>(`${this.apiUrl}/webhook`, payload, { headers });
  }
}
