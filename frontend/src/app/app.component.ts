import { Component } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from './core/services/auth.service';
import { CartService } from './core/services/cart.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: []
})
export class AppComponent {
  /** True when the current route is /home — hides the default banner */
  isHomePage = false;

  constructor(
    public authService: AuthService,
    public cartService: CartService,
    private router: Router
  ) {
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => {
        this.isHomePage = e.urlAfterRedirects === '/home' || e.urlAfterRedirects === '/';
      });
  }

  logout(): void {
    this.authService.logout();
  }
}
