import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CartItem, Order } from '../../shared/models/models';
import { environment } from '../../../environments/environment';

export interface OrderDto {
  address: string;
  paymentMethod: string;
  paymentId?: string;
  items: {
    productId: number;
    quantity: number;
    businessName?: string;
    logoUrl?: string;
    tagline?: string;
    contactNumber?: string;
    website?: string;
    notes?: string;
  }[];
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private apiUrl = `${environment.apiUrl}/api/orders`;

  constructor(private http: HttpClient) {}

  /** Maps CartItems to the DTO shape the backend expects */
  buildOrderDto(cartItems: CartItem[], address: string, paymentMethod: string): OrderDto {
    return {
      address,
      paymentMethod,
      items: cartItems.map(i => ({
        productId: i.product.id!,
        quantity: i.quantity,
        businessName: i.customization?.businessName,
        logoUrl: i.customization?.logoUrl,
        tagline: i.customization?.tagline,
        contactNumber: i.customization?.contactNumber,
        website: i.customization?.website,
        notes: i.customization?.notes
      }))
    };
  }

  placeOrder(dto: OrderDto): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, dto);
  }

  getMyOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/my`);
  }

  getAllOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/admin/all`);
  }

  updateStatus(id: number, status: string): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/admin/${id}/status`, { status });
  }

  cancelOrder(id: number): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${id}/cancel`, {});
  }

  deleteOrder(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/admin/${id}`);
  }

  downloadInvoice(orderId: number): void {
    const url = `${this.apiUrl}/${orderId}/invoice`;
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const fileUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = fileUrl;
        a.download = `Kreva-invoice-${orderId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(fileUrl);
      },
      error: (err) => {
        alert('Failed to download invoice. Please try again.');
      }
    });
  }
}
