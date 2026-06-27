import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SalesService {
  private apiUrl = environment.apiUrl + '/api/admin/sales';

  constructor(private http: HttpClient) {}

  getDailySales(date: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/daily?date=${date}`);
  }

  getMonthlySales(year: number, month: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/monthly?year=${year}&month=${month}`);
  }

  getYearlyChart(year: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/monthly-chart?year=${year}`);
  }

  exportDailyPdf(date: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export/daily?date=${date}`, {
      responseType: 'blob'
    });
  }

  exportMonthlyPdf(year: number, month: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export/monthly?year=${year}&month=${month}`, {
      responseType: 'blob'
    });
  }
}
