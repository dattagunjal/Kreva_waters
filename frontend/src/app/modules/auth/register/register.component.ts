import { Component } from '@angular/core';
import {
  AbstractControl, FormBuilder, FormGroup,
  ValidatorFn, Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

// ── Validators ────────────────────────────────────────────────────────────────

function strongPasswordValidator(): ValidatorFn {
  return (ctrl: AbstractControl) => {
    const val: string = ctrl.value || '';
    const errors: Record<string, boolean> = {};
    if (val.length < 8)               errors['minLength']  = true;
    if (!/[A-Z]/.test(val))           errors['uppercase']  = true;
    if (!/[a-z]/.test(val))           errors['lowercase']  = true;
    if (!/[0-9]/.test(val))           errors['number']     = true;
    if (!/[^A-Za-z0-9]/.test(val))   errors['special']    = true;
    return Object.keys(errors).length ? errors : null;
  };
}

// ── Component ─────────────────────────────────────────────────────────────────

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  registerForm: FormGroup;

  // OTP flow state
  otpSent        = false;
  otpVerified    = false;
  otpLoading     = false;
  otpCountdown   = 0;
  devOtp         = '';    // shown in dev mode only
  otpError       = '';
  otpSuccess     = '';

  // Form state
  loading        = false;
  error          = '';
  showPassword   = false;

  private countdownTimer: any;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      name:         ['', [Validators.required, Validators.minLength(2)]],
      email:        [''],
      mobileNumber: [''],
      otpChannel:   ['email', [Validators.required]],
      otp:          ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
      password:     ['', [Validators.required, strongPasswordValidator()]]
    }, { validators: this.emailOrMobileRequiredValidator });
  }

  // Custom form group validator
  emailOrMobileRequiredValidator(group: AbstractControl): Record<string, any> | null {
    const email = (group.get('email')?.value || '').trim();
    const mobile = (group.get('mobileNumber')?.value || '').trim();
    const otpChannel = group.get('otpChannel')?.value;

    const errors: Record<string, any> = {};

    if (!email && !mobile) {
      errors['emailOrMobileRequired'] = true;
    }

    const emailRe  = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const mobileRe = /^[6-9]\d{9}$/;

    if (email && !emailRe.test(email)) {
      errors['invalidEmail'] = true;
    }
    if (mobile && !mobileRe.test(mobile)) {
      errors['invalidMobile'] = true;
    }

    if (otpChannel === 'email' && !email) {
      errors['emailVerificationRequired'] = true;
    }
    if (otpChannel === 'mobile' && !mobile) {
      errors['mobileVerificationRequired'] = true;
    }

    return Object.keys(errors).length ? errors : null;
  }

  // ── Field getters ──────────────────────────────────────────────────────────
  get nameCtrl()         { return this.registerForm.get('name')!; }
  get emailCtrl()        { return this.registerForm.get('email')!; }
  get mobileNumberCtrl() { return this.registerForm.get('mobileNumber')!; }
  get otpChannelCtrl()   { return this.registerForm.get('otpChannel')!; }
  get otpCtrl()          { return this.registerForm.get('otp')!; }
  get passwordCtrl()     { return this.registerForm.get('password')!; }

  // ── Password strength helpers ──────────────────────────────────────────────
  get pwVal()       { return this.passwordCtrl.value || ''; }
  get pwMinLen()    { return this.pwVal.length >= 8; }
  get pwUppercase() { return /[A-Z]/.test(this.pwVal); }
  get pwLowercase() { return /[a-z]/.test(this.pwVal); }
  get pwNumber()    { return /[0-9]/.test(this.pwVal); }
  get pwSpecial()   { return /[^A-Za-z0-9]/.test(this.pwVal); }
  get pwStrength(): 0|1|2|3 {
    const passed = [this.pwMinLen, this.pwUppercase, this.pwLowercase, this.pwNumber, this.pwSpecial]
      .filter(Boolean).length;
    if (passed <= 2) return 0;
    if (passed === 3) return 1;
    if (passed === 4) return 2;
    return 3;
  }
  get pwStrengthLabel() { return ['Weak','Fair','Good','Strong'][this.pwStrength]; }

  // ── OTP actions ────────────────────────────────────────────────────────────

  canSendOtp(): boolean {
    const otpChannel = this.registerForm.get('otpChannel')?.value;
    const emailValid = !this.registerForm.errors?.['invalidEmail'] && !!(this.registerForm.get('email')?.value || '').trim();
    const mobileValid = !this.registerForm.errors?.['invalidMobile'] && !!(this.registerForm.get('mobileNumber')?.value || '').trim();
    
    const targetValid = otpChannel === 'email' ? emailValid : mobileValid;
    return targetValid && !this.otpLoading && this.otpCountdown === 0;
  }

  sendOtp(): void {
    const email = (this.registerForm.get('email')?.value || '').trim();
    const mobile = (this.registerForm.get('mobileNumber')?.value || '').trim();
    const otpChannel = this.registerForm.get('otpChannel')?.value;

    let targetId = '';
    if (otpChannel === 'email') {
      if (!email) {
        this.otpError = 'Please enter an email address to verify.';
        return;
      }
      targetId = email;
    } else {
      if (!mobile) {
        this.otpError = 'Please enter a mobile number to verify.';
        return;
      }
      targetId = mobile;
    }

    this.otpLoading = true;
    this.otpError   = '';
    this.otpSuccess = '';
    this.devOtp     = '';

    this.authService.sendOtp({
      loginId: targetId,
      purpose: 'REGISTER'
    }).subscribe({
      next: (res) => {
        this.otpSent    = true;
        this.otpLoading = false;
        this.otpSuccess = `OTP sent to ${targetId}!`;
        this.startCountdown(60);

        if (res.devOtp) {
          this.devOtp = res.devOtp;
        }
      },
      error: (err) => {
        this.otpError   = err.error?.message || 'Failed to send OTP. Please try again.';
        this.otpLoading = false;
      }
    });
  }

  private startCountdown(seconds: number): void {
    clearInterval(this.countdownTimer);
    this.otpCountdown = seconds;
    this.countdownTimer = setInterval(() => {
      this.otpCountdown--;
      if (this.otpCountdown <= 0) clearInterval(this.countdownTimer);
    }, 1000);
  }

  // ── Form submit ────────────────────────────────────────────────────────────

  onSubmit(): void {
    this.registerForm.markAllAsTouched();
    if (!this.otpSent) {
      this.error = 'Please send and verify an OTP before registering.';
      return;
    }
    if (this.registerForm.invalid) return;

    this.loading = true;
    this.error   = '';

    const otpChannel = this.registerForm.get('otpChannel')?.value;
    const verifiedLoginId = otpChannel === 'email' 
      ? this.registerForm.get('email')?.value.trim() 
      : this.registerForm.get('mobileNumber')?.value.trim();

    this.authService.register({
      name:         this.nameCtrl.value.trim(),
      email:        (this.registerForm.get('email')?.value || '').trim() || null,
      mobileNumber: (this.registerForm.get('mobileNumber')?.value || '').trim() || null,
      loginId:      verifiedLoginId,
      password:     this.passwordCtrl.value,
      otp:          this.otpCtrl.value.trim()
    } as any).subscribe({
      next: () => this.router.navigate(['/products']),
      error: (err) => {
        this.error   = err.error?.message || 'Registration failed. Please try again.';
        this.loading = false;
      }
    });
  }
}
