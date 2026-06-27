import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
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

  constructor(
    private fb: FormBuilder,
    public cartService: CartService,
    private orderService: OrderService,
    private paymentService: PaymentService,
    private authService: AuthService,
    private route: ActivatedRoute
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

  /** Strips any non-digit characters as the user types in the pincode field */
  onPincodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digitsOnly = input.value.replace(/\D/g, '');
    this.pincodeCtrl.setValue(digitsOnly);
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
    } else {
      this.initiateOnlinePayment();
    }
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
    this.paymentService.createOrder(totalAmount).subscribe({
      next: (orderData) => {
        if (orderData.keyId === 'rzp_test_MOCK' || !(window as any).Razorpay) {
          if (confirm(`[Mock Payment Gateway]\nSimulating payment of ₹${totalAmount}.\n\nClick OK to simulate payment SUCCESS.\nClick Cancel to simulate payment CANCEL/FAILURE.`)) {
            const mockResponse = {
              razorpay_payment_id: 'mock_pay_' + Date.now(),
              razorpay_signature: 'mock_sig_' + Date.now()
            };
            this.verifyPaymentAndPlaceOrder(mockResponse, orderData.orderId);
          } else {
            this.loading = false;
            this.orderError = 'Payment cancelled by user (Simulated).';
          }
          return;
        }

        const options = {
          key: orderData.keyId,
          amount: orderData.amount,
          currency: orderData.currency,
          name: 'Ugam Waters',
          description: 'Premium Mineral Water Delivery',
          order_id: orderData.orderId,
          handler: (response: any) => {
            this.verifyPaymentAndPlaceOrder(response, orderData.orderId);
          },
          prefill: {
            name: this.authService.currentUser?.name || '',
            email: this.authService.currentUser?.email || '',
            contact: this.authService.currentUser?.mobileNumber || ''
          },
          theme: {
            color: '#3b82f6'
          },
          modal: {
            ondismiss: () => {
              this.loading = false;
              this.orderError = 'Payment cancelled by user.';
            }
          }
        };

        const rzp = new (window as any).Razorpay(options);
        rzp.open();
      },
      error: (err) => {
        this.orderError = err.error?.message || 'Failed to initiate online payment. Please try again.';
        this.loading = false;
      }
    });
  }

  private verifyPaymentAndPlaceOrder(response: any, razorpayOrderId: string): void {
    const payload = {
      razorpay_order_id: razorpayOrderId,
      razorpay_payment_id: response.razorpay_payment_id,
      razorpay_signature: response.razorpay_signature
    };

    this.paymentService.verifyPayment(payload).subscribe({
      next: () => {
        const dto = this.orderService.buildOrderDto(
          this.cartService.items,
          this.buildAddress(),
          this.checkoutForm.value.paymentMethod
        );
        dto.paymentId = response.razorpay_payment_id;
        this.submitOrder(dto);
      },
      error: (err) => {
        this.orderError = err.error?.message || 'Payment verification failed. Please contact support.';
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
}
