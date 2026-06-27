import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CartService } from '../../core/services/cart.service';
import { CartItem } from '../../shared/models/models';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html'
})
export class CartComponent {
  constructor(public cartService: CartService, private router: Router) {}

  get items(): CartItem[] { return this.cartService.items; }

  /**
   * Called by both the +/- buttons and the manual input field.
   * Clamps to [1, product.stock] and ignores non-positive values.
   */
  updateQty(item: CartItem, qty: number, maxStock: number): void {
    if (isNaN(qty) || qty < 1) return;           // ignore invalid/empty
    const clamped = Math.min(qty, maxStock);      // can't exceed stock
    this.cartService.updateQuantity(item.product.id!, clamped, item.customization);
  }

  /**
   * Handles the manual numeric input directly.
   * Reads the raw input value, converts to int, then delegates to updateQty.
   */
  onInputChange(event: Event, item: CartItem, maxStock: number): void {
    const raw = (event.target as HTMLInputElement).value;
    const parsed = parseInt(raw, 10);
    this.updateQty(item, parsed, maxStock);
  }

  remove(item: CartItem): void {
    this.cartService.removeFromCart(item.product.id!, item.customization);
  }

  clearCart(): void {
    if (confirm('Remove all items from your cart?')) {
      this.cartService.clearCart();
    }
  }

  checkout(): void {
    this.router.navigate(['/orders/checkout']);
  }
}
