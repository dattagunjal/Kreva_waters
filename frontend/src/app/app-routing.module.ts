import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard, AdminGuard } from './core/guards/auth.guard';
import { HomeComponent } from './modules/home/home.component';
import { ProductListComponent } from './modules/products/product-list/product-list.component';
import { CartComponent } from './modules/cart/cart.component';
import { ProfileComponent } from './modules/profile/profile.component';
import { AboutComponent } from './modules/about/about.component';
import { ContactComponent } from './modules/contact/contact.component';

const routes: Routes = [
  // '' now goes to the Home landing page
  { path: '',        redirectTo: '/home', pathMatch: 'full' },
  { path: 'home',    component: HomeComponent },
  { path: 'about',   component: AboutComponent },
  { path: 'contact', component: ContactComponent },

  { path: 'auth',    loadChildren: () => import('./modules/auth/auth.module').then(m => m.AuthModule) },

  { path: 'products', component: ProductListComponent },
  { path: 'cart',     component: CartComponent,    canActivate: [AuthGuard] },

  { path: 'orders',   loadChildren: () => import('./modules/orders/orders.module').then(m => m.OrdersModule), canActivate: [AuthGuard] },
  { path: 'profile',  component: ProfileComponent, canActivate: [AuthGuard] },
  
  { path: 'admin',    loadChildren: () => import('./modules/admin/admin.module').then(m => m.AdminModule), canActivate: [AuthGuard, AdminGuard] },

  { path: '**', redirectTo: '/home' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
