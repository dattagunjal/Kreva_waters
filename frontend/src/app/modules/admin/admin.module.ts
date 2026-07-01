import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { AdminDashboardComponent } from './dashboard/dashboard.component';
import { SalesComponent } from './sales/sales.component';

@NgModule({
  declarations: [AdminDashboardComponent, SalesComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule.forChild([
      { path: '', component: AdminDashboardComponent },
      { path: 'sales', component: SalesComponent }
    ])
  ]
})
export class AdminModule {}
