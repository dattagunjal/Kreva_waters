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

    this.setupInactivityTimeout();
  }

  logout(): void {
    this.authService.logout();
  }

  private setupInactivityTimeout(): void {
    if (typeof window === 'undefined') return;

    const events = ['mousemove', 'mousedown', 'keypress', 'touchstart', 'scroll', 'click'];
    const INACTIVITY_LIMIT = 30 * 60 * 1000; // 30 minutes in milliseconds
    let timeoutId: any;

    const checkInactivity = () => {
      const lastActive = localStorage.getItem('last_active');
      if (lastActive && this.authService.isLoggedIn()) {
        const elapsed = Date.now() - parseInt(lastActive, 10);
        if (elapsed >= INACTIVITY_LIMIT) {
          this.authService.logout();
          alert('Your session has expired due to inactivity. Please login again.');
          return true;
        }
      }
      return false;
    };

    const resetTimer = () => {
      if (timeoutId) clearTimeout(timeoutId);
      
      if (this.authService.isLoggedIn()) {
        if (checkInactivity()) return;
        
        localStorage.setItem('last_active', Date.now().toString());
        timeoutId = setTimeout(() => {
          this.authService.logout();
          alert('Your session has expired due to inactivity. Please login again.');
        }, INACTIVITY_LIMIT);
      }
    };

    // Check immediately on startup
    checkInactivity();

    events.forEach(event => {
      window.addEventListener(event, resetTimer, { passive: true });
    });
    
    this.router.events.subscribe(() => resetTimer());
    resetTimer();
  }
}
