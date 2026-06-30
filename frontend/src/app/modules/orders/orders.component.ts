import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { PaymentService } from '../../core/services/payment.service';
import { AuthService } from '../../core/services/auth.service';
import { Order } from '../../shared/models/models';

/** Validates a 6-digit Indian pincode */
function pincodeValidator(): ValidatorFn {
  return (ctrl: AbstractControl) => {
    const val = (ctrl.value || '').toString().trim();
    return /^[1-9][0-9]{5}$/.test(val) ? null : { invalidPincode: true };
  };
}

@Component({
  selector: 'app-orders',
  templateUrl: './orders.component.html'
})
export class OrdersComponent implements OnInit {
  checkoutForm: FormGroup;
  myOrders: Order[] = [];
  loading = false;
  view: 'checkout' | 'history' = 'checkout';
  orderSuccess = false;
  orderError = '';
  cancelingOrderId: number | null = null;
  historySuccess = '';
  historyError = '';

  // UPI payment modal properties
  showUpiModal = false;
  totalForUpi = 0;
  upiQrCodeUrl = '';
  upiTransactionId = '';
  upiError = '';
  bankDetails: any = null;

  constructor(
    private fb: FormBuilder,
    public cartService: CartService,
    private orderService: OrderService,
    private paymentService: PaymentService,
    private authService: AuthService,
    private route: ActivatedRoute,
    private http: HttpClient
  ) {
    this.checkoutForm = this.fb.group({
      // Structured address fields
      houseNo:       ['', [Validators.required, Validators.minLength(1), Validators.maxLength(100)]],
      area:          ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
      pincode:       ['', [Validators.required, pincodeValidator()]],
      // Payment
      paymentMethod: ['COD', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadOrders();
    if (!this.route.snapshot.url.some(s => s.path === 'checkout')) {
      this.view = 'history';
    }
  }

  // ── form field getters ─────────────────────────────────────────────────────
  get houseNoCtrl()  { return this.checkoutForm.get('houseNo')!; }
  get areaCtrl()     { return this.checkoutForm.get('area')!; }
  get pincodeCtrl()  { return this.checkoutForm.get('pincode')!; }

  /** Strips any non-digit characters as the user types and validates if the pincode exists in India */
  onPincodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digitsOnly = input.value.replace(/\D/g, '');
    this.pincodeCtrl.setValue(digitsOnly);

    if (digitsOnly.length === 6) {
      this.http.get<any[]>(`https://api.postalpincode.in/pincode/${digitsOnly}`).subscribe({
        next: (res) => {
          if (res && res[0] && res[0].Status === 'Success') {
            this.pincodeCtrl.setErrors(null);
          } else {
            this.pincodeCtrl.setErrors({ invalidPincode: true });
          }
        },
        error: () => {
          // Fallback if API is down
          this.pincodeCtrl.setErrors(null);
        }
      });
    }
  }

  /** Assembles the three address fields into a single delivery address string */
  private buildAddress(): string {
    const { houseNo, area, pincode } = this.checkoutForm.value;
    return `${houseNo.trim()}, ${area.trim()} - ${pincode.trim()}`;
  }

  loadOrders(): void {
    this.orderService.getMyOrders().subscribe({
      next: (data: Order[]) => this.myOrders = data,
      error: () => {}
    });
  }

  placeOrder(): void {
    this.checkoutForm.markAllAsTouched();
    if (this.checkoutForm.invalid || this.cartService.items.length === 0) return;

    this.loading = true;
    this.orderError = '';

    const paymentMethod = this.checkoutForm.value.paymentMethod;

    if (paymentMethod === 'COD') {
      const dto = this.orderService.buildOrderDto(
        this.cartService.items,
        this.buildAddress(),
        'COD'
      );
      this.submitOrder(dto);
    } else if (paymentMethod === 'UPI') {
      this.totalForUpi = this.cartService.getTotal();
      this.paymentService.getBankDetails().subscribe({
        next: (bank) => {
          this.bankDetails = bank;
          const upiUri = `upi://pay?pa=${bank.upiId}&pn=${encodeURIComponent(bank.accountName)}&am=${this.totalForUpi}&cu=INR`;
          this.upiQrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(upiUri)}`;
          this.upiTransactionId = '';
          this.upiError = '';
          this.showUpiModal = true;
        },
        error: (err) => {
          this.loading = false;
          this.orderError = 'Failed to fetch payment bank details. Please try again.';
        }
      });
    } else {
      this.initiateOnlinePayment();
    }
  }

  onUpiTxInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.upiTransactionId = input.value.replace(/\D/g, '');
    this.upiError = '';
  }

  cancelUpiModal(): void {
    this.showUpiModal = false;
    this.loading = false;
  }

  submitUpiPayment(): void {
    if (!/^\d{12}$/.test(this.upiTransactionId)) {
      this.upiError = 'Please enter a valid 12-digit UPI transaction reference number.';
      return;
    }

    const dto = this.orderService.buildOrderDto(
      this.cartService.items,
      this.buildAddress(),
      'UPI'
    );
    dto.paymentId = this.upiTransactionId;

    this.orderService.placeOrder(dto).subscribe({
      next: () => {
        this.cartService.clearCart();
        this.orderSuccess = true;
        this.loading = false;
        this.showUpiModal = false;
        this.loadOrders();
        setTimeout(() => { this.orderSuccess = false; this.view = 'history'; }, 2500);
      },
      error: (err) => {
        this.upiError = err.error?.message || 'Failed to place order. Please try again.';
        this.loading = false;
      }
    });
  }

  private submitOrder(dto: any): void {
    this.orderService.placeOrder(dto).subscribe({
      next: () => {
        this.cartService.clearCart();
        this.orderSuccess = true;
        this.loading = false;
        this.loadOrders();
        setTimeout(() => { this.orderSuccess = false; this.view = 'history'; }, 2500);
      },
      error: (err) => {
        this.orderError = err.error?.message || 'Failed to place order. Please try again.';
        this.loading = false;
      }
    });
  }

  private initiateOnlinePayment(): void {
    const totalAmount = this.cartService.getTotal();
    const paymentMethod = this.checkoutForm.value.paymentMethod;

    this.paymentService.createPaymentIntent(totalAmount).subscribe({
      next: (orderData) => {
        // Fallback / Mock Stripe Payment Gateway simulation
        if (orderData.publishableKey === 'pk_test_MOCK' || !(window as any).Stripe) {
          let paymentInput: string | null = null;
          if (paymentMethod === 'UPI') {
            paymentInput = prompt(`[Stripe Mock Payment Gateway - UPI]\nTotal Amount: ₹${totalAmount}\n\nPlease enter a test UPI ID (e.g. user@okaxis):`, 'user@okaxis');
          } else {
            paymentInput = prompt(`[Stripe Mock Payment Gateway - CARD]\nTotal Amount: ₹${totalAmount}\n\nPlease enter a test credit card number (e.g. 4242 4242 4242 4242):`, '4242 4242 4242 4242');
          }

          if (paymentInput) {
            const dto = this.orderService.buildOrderDto(
              this.cartService.items,
              this.buildAddress(),
              paymentMethod
            );
            dto.paymentId = orderData.id;

            // Place order first (which will initially have paymentStatus = PENDING)
            this.submitOrderForStripe(dto, orderData.id);
          } else {
            this.loading = false;
            this.orderError = 'Payment cancelled by user.';
          }
          return;
        }

        // Live Stripe checkout
        try {
          const stripe = (window as any).Stripe(orderData.publishableKey);
          alert('Redirecting to Stripe verification (Dev checkout)...');
          const dto = this.orderService.buildOrderDto(
            this.cartService.items,
            this.buildAddress(),
            paymentMethod
          );
          dto.paymentId = orderData.id;
          
          this.submitOrderForStripe(dto, orderData.id);
        } catch (e) {
          this.orderError = 'Stripe integration error: ' + (e as any).message;
          this.loading = false;
        }
      },
      error: (err) => {
        this.orderError = err.error?.message || 'Failed to initiate Stripe payment.';
        this.loading = false;
      }
    });
  }

  private submitOrderForStripe(dto: any, paymentIntentId: string): void {
    this.orderService.placeOrder(dto).subscribe({
      next: () => {
        // Wait 500ms to ensure the Spring Boot database transaction commits before webhook triggers
        setTimeout(() => {
          this.paymentService.simulateWebhookSuccess(paymentIntentId).subscribe({
            next: () => {
              this.cartService.clearCart();
              this.orderSuccess = true;
              this.loading = false;
              this.loadOrders();
              setTimeout(() => { this.orderSuccess = false; this.view = 'history'; }, 2500);
            },
            error: (err) => {
              this.orderError = 'Failed to verify payment via simulated Stripe Webhook.';
              this.loading = false;
            }
          });
        }, 500);
      },
      error: (err) => {
        this.orderError = err.error?.message || 'Failed to place order.';
        this.loading = false;
      }
    });
  }

  cancelOrder(id: number): void {
    if (!confirm('Cancel this order?')) return;
    this.cancelingOrderId = id;
    this.historySuccess = '';
    this.historyError = '';
    this.orderService.cancelOrder(id).subscribe({
      next: () => {
        this.historySuccess = 'Order cancelled successfully.';
        this.cancelingOrderId = null;
        this.loadOrders();
        setTimeout(() => this.historySuccess = '', 3000);
      },
      error: (err) => {
        this.historyError = err.error?.message || 'Could not cancel order.';
        this.cancelingOrderId = null;
        this.loadOrders();
        setTimeout(() => this.historyError = '', 5000);
      }
    });
  }

  downloadInvoice(id: number): void {
    this.orderService.downloadInvoice(id);
  }
}
