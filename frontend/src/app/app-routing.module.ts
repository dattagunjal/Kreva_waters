import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard, AdminGuard } from './core/guards/auth.guard';
import { LoginComponent } from './modules/auth/login/login.component';
import { RegisterComponent } from './modules/auth/register/register.component';
import { HomeComponent } from './modules/home/home.component';
import { ProductListComponent } from './modules/products/product-list/product-list.component';
import { CartComponent } from './modules/cart/cart.component';
import { OrdersComponent } from './modules/orders/orders.component';
import { AdminDashboardComponent } from './modules/admin/dashboard/dashboard.component';
import { SalesComponent } from './modules/admin/sales/sales.component';
import { ProfileComponent } from './modules/profile/profile.component';

import { AboutComponent } from './modules/about/about.component';
import { ContactComponent } from './modules/contact/contact.component';

const routes: Routes = [
  // '' now goes to the Home landing page
  { path: '',        redirectTo: '/home', pathMatch: 'full' },
  { path: 'home',    component: HomeComponent },
  { path: 'about',   component: AboutComponent },
  { path: 'contact', component: ContactComponent },

  { path: 'auth/login',    component: LoginComponent },
  { path: 'auth/register', component: RegisterComponent },

  { path: 'products', component: ProductListComponent },
  { path: 'cart',     component: CartComponent,    canActivate: [AuthGuard] },

  { path: 'orders',          component: OrdersComponent, canActivate: [AuthGuard] },
  { path: 'orders/checkout', component: OrdersComponent, canActivate: [AuthGuard] },
  { path: 'profile',         component: ProfileComponent, canActivate: [AuthGuard] },
  
  { path: 'admin',       component: AdminDashboardComponent, canActivate: [AuthGuard, AdminGuard] },
  { path: 'admin/sales', component: SalesComponent,          canActivate: [AuthGuard, AdminGuard] },

  { path: '**', redirectTo: '/home' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
