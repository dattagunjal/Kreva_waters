import { Component, OnInit } from '@angular/core';
import { SalesService } from '../../../core/services/sales.service';

@Component({
  selector: 'app-sales',
  templateUrl: './sales.component.html',
  styleUrls: ['./sales.component.scss']
})
export class SalesComponent implements OnInit {

  activeTab: 'daily' | 'monthly' = 'daily';

  selectedDate: string = new Date().toISOString().split('T')[0];
  dailyData: any = null;
  dailyLoading = false;
  dailyExporting = false;

  selectedYear: number = new Date().getFullYear();
  selectedMonth: number = new Date().getMonth() + 1;
  monthlyData: any = null;
  monthlyLoading = false;
  monthlyExporting = false;

  yearlyData: any = null;

  months = [
    { value: 1, label: 'January' }, { value: 2, label: 'February' },
    { value: 3, label: 'March' }, { value: 4, label: 'April' },
    { value: 5, label: 'May' }, { value: 6, label: 'June' },
    { value: 7, label: 'July' }, { value: 8, label: 'August' },
    { value: 9, label: 'September' }, { value: 10, label: 'October' },
    { value: 11, label: 'November' }, { value: 12, label: 'December' }
  ];

  years: number[] = [];

  constructor(private salesService: SalesService) {
    const currentYear = new Date().getFullYear();
    for (let y = currentYear; y >= currentYear - 3; y--) this.years.push(y);
  }

  ngOnInit(): void {
    this.loadDailySales();
    this.loadMonthlySales();
  }

  loadDailySales(): void {
    this.dailyLoading = true;
    this.salesService.getDailySales(this.selectedDate).subscribe({
      next: (data) => { this.dailyData = data; this.dailyLoading = false; },
      error: () => { this.dailyLoading = false; }
    });
  }

  loadMonthlySales(): void {
    this.monthlyLoading = true;
    this.salesService.getMonthlySales(this.selectedYear, this.selectedMonth).subscribe({
      next: (data) => { this.monthlyData = data; this.monthlyLoading = false; this.loadYearlyChart(); },
      error: () => { this.monthlyLoading = false; }
    });
  }

  loadYearlyChart(): void {
    this.salesService.getYearlyChart(this.selectedYear).subscribe({
      next: (data) => { this.yearlyData = data; }
    });
  }

  onDateChange(): void { this.loadDailySales(); }
  onMonthChange(): void { this.loadMonthlySales(); }
  onYearChange(): void { this.loadMonthlySales(); }

  exportDailyPdf(): void {
    this.dailyExporting = true;
    this.salesService.exportDailyPdf(this.selectedDate).subscribe({
      next: (blob) => { this.downloadFile(blob, `ugamwaters-daily-${this.selectedDate}.pdf`); this.dailyExporting = false; },
      error: () => { this.dailyExporting = false; }
    });
  }

  exportMonthlyPdf(): void {
    this.monthlyExporting = true;
    this.salesService.exportMonthlyPdf(this.selectedYear, this.selectedMonth).subscribe({
      next: (blob) => {
        const m = this.months.find(x => x.value === this.selectedMonth)?.label.toLowerCase();
        this.downloadFile(blob, `ugamwaters-monthly-${m}-${this.selectedYear}.pdf`);
        this.monthlyExporting = false;
      },
      error: () => { this.monthlyExporting = false; }
    });
  }

  private downloadFile(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    window.URL.revokeObjectURL(url);
  }

  getDailyRevenueArray(): { day: number; revenue: number; orders: number }[] {
    if (!this.monthlyData?.dailyRevenue) return [];
    return Object.keys(this.monthlyData.dailyRevenue).map(day => ({
      day: +day,
      revenue: this.monthlyData.dailyRevenue[day],
      orders: this.monthlyData.dailyOrders[day]
    }));
  }

  getBarWidth(value: number, max: number): string {
    if (!max) return '0%';
    return Math.round((value / max) * 100) + '%';
  }

  getMaxRevenue(): number {
    return Math.max(...this.getDailyRevenueArray().map(d => d.revenue), 1);
  }

  getMaxMonthlyRevenue(): number {
    if (!this.yearlyData?.revenue) return 1;
    return Math.max(...this.yearlyData.revenue, 1);
  }
}
