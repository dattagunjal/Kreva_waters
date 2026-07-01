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
  upiTimer: any = null;
  showPayNowModal = false;
  payNowOrder: any = null;
  upiDeepLinkUrl = '';
  enteredUtr = '';
  utrError = '';
  showPaymentSuccess = false;
  isCheckoutPay = false;
  enteredTxnId = '';
  isVerifyingUpi = false;
  upiFocusListener: any = null;

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

  /** Validate pincode when user tabs out of the field */
  onPincodeBlur(): void {
    this.pincodeCtrl.markAsTouched();
    const val = (this.pincodeCtrl.value || '').toString().trim();
    
    // If less than 6 digits, the sync validator already marks it invalid
    if (val.length < 6) return;

    // If exactly 6 digits, call India Post API to verify
    if (/^[1-9][0-9]{5}$/.test(val)) {
      this.http.get<any[]>(`https://api.postalpincode.in/pincode/${val}`).subscribe({
        next: (res) => {
          if (res && res[0] && res[0].Status === 'Success') {
            this.pincodeCtrl.setErrors(null);
          } else {
            this.pincodeCtrl.setErrors({ invalidPincode: true });
          }
        },
        error: () => {
          this.pincodeCtrl.setErrors(null);
        }
      });
    }
  }

  /** Assembles the three address fields into a single delivery address string */
  buildAddress(): string {
    const { houseNo, area, pincode } = this.checkoutForm.value;
    return `${houseNo.trim()}, ${area.trim()} - ${pincode.trim()}`;
  }

  loadOrders(): void {
    this.orderService.getMyOrders().subscribe({
      next: (orders) => this.myOrders = orders,
      error: (err) => console.error('Failed to load orders', err)
    });
  }

  placeOrder(): void {
    if (this.checkoutForm.invalid) {
      this.checkoutForm.markAllAsTouched();
      return;
    }

    const paymentMethod = this.checkoutForm.value.paymentMethod;
    if (paymentMethod === 'COD') {
      const dto = this.orderService.buildOrderDto(
        this.cartService.items,
        this.buildAddress(),
        'COD'
      );
      this.submitOrder(dto);
    } else if (paymentMethod === 'UPI') {
      if (this.cartService.items.length === 0) return;
      this.loading = true;
      this.orderError = '';
      this.initiateRazorpayUpi();
    } else {
      this.initiateOnlinePayment();
    }
  }

  cancelUpiModal(): void {
    this.showUpiModal = false;
    this.loading = false;
  }

  submitUpiPayment(): void {
    // Keep placeholder to avoid any potential compilation issues
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

  isUpiOrder(order: any): boolean {
    return order.paymentId && (order.paymentId.startsWith('UPI-') || /^\d{12}$/.test(order.paymentId));
  }

  isMobileDevice(): boolean {
    return typeof window !== 'undefined' && /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
  }

  closePayNowModal(): void {
    if (this.upiTimer) {
      clearTimeout(this.upiTimer);
      this.upiTimer = null;
    }
    if (this.upiFocusListener) {
      window.removeEventListener('focus', this.upiFocusListener);
      this.upiFocusListener = null;
    }
    this.showPayNowModal = false;
    this.payNowOrder = null;
    this.enteredUtr = '';
    this.enteredTxnId = '';
    this.showPaymentSuccess = false;
    this.isVerifyingUpi = false;
    this.loading = false;
  }

  /** Loads Razorpay checkout.js dynamically if not already present */
  private loadRazorpayScript(): Promise<void> {
    return new Promise((resolve, reject) => {
      if ((window as any).Razorpay) {
        resolve();
        return;
      }
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Failed to load Razorpay SDK'));
      document.head.appendChild(script);
    });
  }

  /** Initiates UPI payment through Razorpay Checkout with real verification */
  initiateRazorpayUpi(): void {
    const totalAmount = this.cartService.getTotal();

    // Step 1: Create Razorpay order on the backend
    this.paymentService.createRazorpayOrder(totalAmount).subscribe({
      next: async (razorpayOrder) => {
        try {
          // Step 2: Load Razorpay SDK dynamically
          await this.loadRazorpayScript();
        } catch (e) {
          this.orderError = 'Failed to load payment gateway. Please try again.';
          this.loading = false;
          return;
        }

        // Step 3: Get Razorpay key from backend
        this.paymentService.getRazorpayKey().subscribe({
          next: (keyData) => {
            const options = {
              key: keyData.keyId,
              amount: razorpayOrder.amount,
              currency: razorpayOrder.currency || 'INR',
              name: 'Ugam Waters',
              description: 'Water Order Payment',
              order_id: razorpayOrder.id,
              prefill: {
                contact: '',
                email: ''
              },
              handler: (response: any) => {
                // Real payment success callback from Razorpay
                const paymentId = response.razorpay_payment_id;
                this.enteredTxnId = response.razorpay_order_id || razorpayOrder.id;
                this.enteredUtr = paymentId;

                // Show success modal
                this.showPayNowModal = true;
                this.isCheckoutPay = true;
                this.showPaymentSuccess = true;
                this.totalForUpi = totalAmount;

                // Place the order on the backend with verified payment ID
                const dto = this.orderService.buildOrderDto(
                  this.cartService.items,
                  this.buildAddress(),
                  'UPI'
                );
                dto.paymentId = paymentId;

                this.orderService.placeOrder(dto).subscribe({
                  next: () => {
                    // Confirm payment on backend
                    this.paymentService.confirmRazorpayPayment(paymentId).subscribe({
                      next: () => {},
                      error: () => {} // Order already placed, notification failure is non-critical
                    });

                    this.cartService.clearCart();
                    this.loadOrders();
                    this.loading = false;

                    setTimeout(() => {
                      this.showPaymentSuccess = false;
                      this.showPayNowModal = false;
                      this.view = 'history';
                    }, 3000);
                  },
                  error: (err) => {
                    this.showPaymentSuccess = false;
                    this.showPayNowModal = false;
                    this.loading = false;
                    this.orderError = 'Payment received but failed to place order: ' + (err.error?.message || 'Server error.');
                  }
                });
              },
              modal: {
                ondismiss: () => {
                  this.loading = false;
                  this.orderError = 'Payment was cancelled.';
                }
              },
              theme: {
                color: '#1a6bbf'
              }
            };

            const rzp = new (window as any).Razorpay(options);
            rzp.on('payment.failed', (response: any) => {
              this.loading = false;
              this.orderError = 'Payment failed: ' + (response.error?.description || 'Please try again.');
            });
            rzp.open();
          },
          error: () => {
            this.orderError = 'Failed to load payment configuration.';
            this.loading = false;
          }
        });
      },
      error: (err) => {
        this.orderError = 'Failed to create payment order: ' + (err.error?.message || 'Server error.');
        this.loading = false;
      }
    });
  }
}
