import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { OrdersComponent } from './orders.component';

@NgModule({
  declarations: [OrdersComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule.forChild([
      { path: '', component: OrdersComponent },
      { path: 'checkout', component: OrdersComponent }
    ])
  ]
})
export class OrdersModule {}
