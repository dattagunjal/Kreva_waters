import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { CartItem, Product, WaterBottleCustomization } from '../../shared/models/models';
import { AuthService } from './auth.service';

const CART_KEY = 'kreva_cart';

@Injectable({ providedIn: 'root' })
export class CartService {
  private cartSubject = new BehaviorSubject<CartItem[]>([]);
  cart$ = this.cartSubject.asObservable();

  constructor(private authService: AuthService) {
    this.authService.currentUser$.subscribe(user => {
      this.cartSubject.next(this.loadFromStorage(user));
    });
  }

  // â”€â”€ helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  private getCartKey(user: any): string {
    if (user && user.email) {
      return `${CART_KEY}_${user.email}`;
    } else if (user && user.mobileNumber) {
      return `${CART_KEY}_${user.mobileNumber}`;
    }
    return CART_KEY;
  }

  private loadFromStorage(user: any = null): CartItem[] {
    try {
      const activeUser = user || this.authService?.currentUser;
      const key = this.getCartKey(activeUser);
      const raw = localStorage.getItem(key);
      return raw ? (JSON.parse(raw) as CartItem[]) : [];
    } catch {
      return [];
    }
  }

  private persist(items: CartItem[]): void {
    try {
      const activeUser = this.authService?.currentUser;
      const key = this.getCartKey(activeUser);
      localStorage.setItem(key, JSON.stringify(items));
    } catch {
      // storage quota exceeded â€” carry on in memory only
    }
  }

  private emit(items: CartItem[]): void {
    this.persist(items);
    this.cartSubject.next(items);
  }

  // â”€â”€ public API â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  get items(): CartItem[] {
    return this.cartSubject.value;
  }

  addToCart(product: Product, customization?: WaterBottleCustomization): void {
    const current = [...this.items];
    const idx = current.findIndex(i =>
      i.product.id === product.id &&
      this.areCustomizationsEqual(i.customization, customization)
    );
    if (idx > -1) {
      current[idx] = { ...current[idx], quantity: current[idx].quantity + 1 };
    } else {
      current.push({ product, quantity: 1, customization });
    }
    this.emit(current);
  }

  removeFromCart(productId: number, customization?: WaterBottleCustomization): void {
    this.emit(this.items.filter(i =>
      !(i.product.id === productId && this.areCustomizationsEqual(i.customization, customization))
    ));
  }

  updateQuantity(productId: number, quantity: number, customization?: WaterBottleCustomization): void {
    if (quantity <= 0) { this.removeFromCart(productId, customization); return; }
    this.emit(this.items.map(i =>
      (i.product.id === productId && this.areCustomizationsEqual(i.customization, customization))
        ? { ...i, quantity }
        : i
    ));
  }

  areCustomizationsEqual(c1?: WaterBottleCustomization, c2?: WaterBottleCustomization): boolean {
    if (!c1 && !c2) return true;
    if (!c1 || !c2) return false;
    return c1.businessName === c2.businessName &&
           c1.logoUrl === c2.logoUrl &&
           c1.tagline === c2.tagline &&
           c1.contactNumber === c2.contactNumber &&
           c1.website === c2.website &&
           c1.notes === c2.notes;
  }

  clearCart(): void {
    this.emit([]);
  }

  getTotal(): number {
    return this.items.reduce((sum, i) => sum + i.product.price * i.quantity, 0);
  }

  getCount(): number {
    return this.items.reduce((sum, i) => sum + i.quantity, 0);
  }
}
