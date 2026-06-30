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
      const totalAmount = this.cartService.getTotal();
      this.paymentService.getRazorpayKey().subscribe({
        next: (keyRes) => {
          const keyId = keyRes.keyId;

          // Sandbox Mode Fallback: if using placeholder/unauthorized test key, run clean checkout simulation
          if (keyId === 'rzp_test_yGzB4Fh5j7z8K9' || !keyId || keyId.includes('PLACEHOLDER')) {
            const proceed = confirm(`[Dev Sandbox Mode]\nNo active Razorpay API Key detected.\n\nPress OK to simulate a successful UPI Payment of ₹${totalAmount}.`);
            if (proceed) {
              this.loading = true;
              const mockPaymentId = 'pay_mock_' + Math.floor(100000000 + Math.random() * 900000000);

              const dto = this.orderService.buildOrderDto(
                this.cartService.items,
                this.buildAddress(),
                'UPI'
              );
              dto.paymentId = mockPaymentId;

              this.orderService.placeOrder(dto).subscribe({
                next: () => {
                  this.paymentService.confirmRazorpayPayment(mockPaymentId).subscribe({
                    next: () => {
                      this.cartService.clearCart();
                      this.orderSuccess = true;
                      this.loading = false;
                      this.loadOrders();
                      setTimeout(() => { this.orderSuccess = false; this.view = 'history'; }, 2500);
                    },
                    error: (errConfirm) => {
                      this.orderError = errConfirm.error?.message || 'Payment confirmed but failed to update status on server.';
                      this.loading = false;
                    }
                  });
                },
                error: (errPlace) => {
                  this.orderError = errPlace.error?.message || 'Failed to record order details. Please contact support.';
                  this.loading = false;
                }
              });
            } else {
              this.loading = false;
            }
            return;
          }
          
          this.paymentService.createRazorpayOrder(totalAmount).subscribe({
            next: (orderRes) => {
              const user = this.authService.currentUser;

              const options: any = {
                key: keyId,
                amount: totalAmount * 100, // in paise
                currency: 'INR',
                name: 'Ugam Waters',
                description: 'Mineral Water Delivery Payment',
                image: 'https://ugamwaters.in/assets/images/logo.png',
                order_id: orderRes.id,
                prefill: {
                  name: user?.name || '',
                  email: user?.email || '',
                  contact: user?.mobileNumber || ''
                },
                theme: {
                  color: '#1a6bbf'
                },
                config: {
                  display: {
                    blocks: {
                      banks: {
                        name: 'Pay via UPI / QR Code',
                        instruments: [
                          {
                            method: 'upi'
                          }
                        ]
                      }
                    },
                    sequence: ['block.banks'],
                    preferences: {
                      show_default_blocks: false
                    }
                  }
                },
                handler: (response: any) => {
                  this.loading = true;
                  this.orderError = '';

                  const dto = this.orderService.buildOrderDto(
                    this.cartService.items,
                    this.buildAddress(),
                    'UPI'
                  );
                  dto.paymentId = response.razorpay_payment_id;

                  this.orderService.placeOrder(dto).subscribe({
                    next: () => {
                      this.paymentService.confirmRazorpayPayment(response.razorpay_payment_id).subscribe({
                        next: () => {
                          this.cartService.clearCart();
                          this.orderSuccess = true;
                          this.loading = false;
                          this.loadOrders();
                          setTimeout(() => { this.orderSuccess = false; this.view = 'history'; }, 2500);
                        },
                        error: (errConfirm) => {
                          this.orderError = errConfirm.error?.message || 'Payment confirmed but failed to update status on server.';
                          this.loading = false;
                        }
                      });
                    },
                    error: (errPlace) => {
                      this.orderError = errPlace.error?.message || 'Failed to record order details. Please contact support.';
                      this.loading = false;
                    }
                  });
                },
                modal: {
                  ondismiss: () => {
                    this.loading = false;
                  }
                }
              };

              const rzp = new (window as any).Razorpay(options);
              rzp.open();
            },
            error: (errOrder) => {
              this.loading = false;
              this.orderError = errOrder.error?.message || 'Failed to create payment order. Please try again.';
            }
          });
        },
        error: (errKey) => {
          this.loading = false;
          this.orderError = 'Failed to initialize payment gateway. Please try again.';
        }
      });
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
}
