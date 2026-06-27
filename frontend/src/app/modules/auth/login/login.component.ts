import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

function emailOrMobileValidator(): ValidatorFn {
  return (ctrl: AbstractControl) => {
    const val = (ctrl.value || '').trim();
    if (!val) return { required: true };
    const emailRe  = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const mobileRe = /^[6-9]\d{9}$/;
    return emailRe.test(val) || mobileRe.test(val) ? null : { invalidLoginId: true };
  };
}

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html'
})
export class LoginComponent {
  loginForm: FormGroup;

  // Auth mode toggle
  loginMode: 'password' | 'otp' = 'password';

  // OTP flow state
  otpSent      = false;
  otpLoading   = false;
  otpCountdown = 0;
  devOtp       = '';
  otpError     = '';
  otpSuccess   = '';

  // Form state
  loading      = false;
  error        = '';
  showPassword = false;

  private countdownTimer: any;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      loginId:  ['', [Validators.required, emailOrMobileValidator()]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      otp:      ['']
    });
  }

  get loginIdCtrl()  { return this.loginForm.get('loginId')!; }
  get passwordCtrl() { return this.loginForm.get('password')!; }
  get otpCtrl()      { return this.loginForm.get('otp')!; }

  // ── Mode toggle ────────────────────────────────────────────────────────────

  switchMode(mode: 'password' | 'otp'): void {
    this.loginMode = mode;
    this.error     = '';
    this.otpError  = '';
    this.otpSent   = false;
    this.devOtp    = '';
    this.otpCtrl.reset();

    // Adjust validators based on mode
    if (mode === 'otp') {
      this.passwordCtrl.clearValidators();
      this.passwordCtrl.updateValueAndValidity();
    } else {
      this.passwordCtrl.setValidators([Validators.required, Validators.minLength(6)]);
      this.passwordCtrl.updateValueAndValidity();
    }
  }

  // ── OTP actions ────────────────────────────────────────────────────────────

  canSendOtp(): boolean {
    return this.loginIdCtrl.valid && !this.otpLoading && this.otpCountdown === 0;
  }

  sendOtp(): void {
    this.loginIdCtrl.markAsTouched();
    if (this.loginIdCtrl.invalid) return;

    this.otpLoading = true;
    this.otpError   = '';
    this.otpSuccess = '';
    this.devOtp     = '';

    this.authService.sendOtp({
      loginId: this.loginIdCtrl.value.trim(),
      purpose: 'LOGIN'
    }).subscribe({
      next: (res) => {
        this.otpSent    = true;
        this.otpLoading = false;
        this.otpSuccess = 'OTP sent! Check your email / mobile.';
        this.startCountdown(60);
        if (res.devOtp) this.devOtp = res.devOtp;
      },
      error: (err) => {
        this.otpError   = err.error?.message || 'Failed to send OTP.';
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
    this.loginIdCtrl.markAsTouched();
    this.error = '';

    if (this.loginMode === 'otp') {
      if (!this.otpSent) { this.error = 'Please send an OTP first.'; return; }
      if (!this.otpCtrl.value?.trim()) { this.error = 'Please enter the OTP.'; return; }
      this.submitLogin({ loginId: this.loginIdCtrl.value.trim(), otp: this.otpCtrl.value.trim() });
    } else {
      this.passwordCtrl.markAsTouched();
      if (this.loginForm.get('loginId')?.invalid || this.passwordCtrl.invalid) return;
      this.submitLogin({ loginId: this.loginIdCtrl.value.trim(), password: this.passwordCtrl.value });
    }
  }

  private submitLogin(payload: { loginId: string; password?: string; otp?: string }): void {
    this.loading = true;
    this.authService.login(payload).subscribe({
      next: () => this.router.navigate(['/products']),
      error: (err) => {
        this.error   = err.error?.message || 'Login failed. Please check your credentials.';
        this.loading = false;
      }
    });
  }
}
