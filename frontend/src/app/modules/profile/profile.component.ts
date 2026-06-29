import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ValidatorFn, AbstractControl } from '@angular/forms';
import { UserService } from '../../core/services/user.service';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';
import { User, Order } from '../../shared/models/models';

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

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  userProfile: User | null = null;
  orders: Order[] = [];
  
  profileForm: FormGroup;
  passwordForm: FormGroup;
  
  profileSuccess = '';
  profileError = '';
  passwordSuccess = '';
  passwordError = '';
  
  loading = false;
  passwordLoading = false;
  imageUploading = false;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private orderService: OrderService,
    private authService: AuthService
  ) {
    this.profileForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.email]],
      mobileNumber: ['', [Validators.pattern('^[6-9]\\d{9}$')]],
      address: [''],
      profileImage: ['']
    });

    this.passwordForm = this.fb.group({
      oldPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, strongPasswordValidator()]],
      confirmNewPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  get newPasswordCtrl() { return this.passwordForm.get('newPassword')!; }
  get pwVal()       { return this.newPasswordCtrl?.value || ''; }
  get pwMinLen()    { return this.pwVal.length >= 8; }
  get pwUppercase() { return /[A-Z]/.test(this.pwVal); }
  get pwLowercase() { return /[a-z]/.test(this.pwVal); }
  get pwNumber()    { return /[0-9]/.test(this.pwVal); }
  get pwSpecial()   { return /[^A-Za-z0-9]/.test(this.pwVal); }

  ngOnInit(): void {
    this.loadProfile();
    this.loadOrders();
  }

  loadProfile(): void {
    this.loading = true;
    this.userService.getProfile().subscribe({
      next: (profile) => {
        this.userProfile = profile;
        this.profileForm.patchValue({
          name: profile.name,
          email: profile.email || '',
          mobileNumber: profile.mobileNumber || '',
          address: profile.address || '',
          profileImage: profile.profileImage || ''
        });
        this.loading = false;
      },
      error: (err) => {
        this.profileError = 'Failed to load user profile.';
        this.loading = false;
      }
    });
  }

  loadOrders(): void {
    this.orderService.getMyOrders().subscribe({
      next: (data) => {
        this.orders = data.slice(0, 5); // display 5 most recent orders
      }
    });
  }

  passwordMatchValidator(group: FormGroup): Record<string, any> | null {
    const newPass = group.get('newPassword')?.value;
    const confirmPass = group.get('confirmNewPassword')?.value;
    return newPass === confirmPass ? null : { passwordMismatch: true };
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      if (file.size > 2 * 1024 * 1024) {
        this.profileError = 'Image must be under 2MB.';
        return;
      }
      this.imageUploading = true;
      const reader = new FileReader();
      reader.onload = () => {
        const base64 = reader.result as string;
        this.profileForm.patchValue({ profileImage: base64 });
        this.imageUploading = false;
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage(): void {
    this.profileForm.patchValue({ profileImage: '' });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) return;
    
    this.profileSuccess = '';
    this.profileError = '';
    this.loading = true;

    this.userService.updateProfile(this.profileForm.value).subscribe({
      next: (updatedProfile) => {
        this.userProfile = updatedProfile;
        
        // Sync auth service state
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
          const userObj = JSON.parse(storedUser);
          userObj.name = updatedProfile.name;
          userObj.email = updatedProfile.email;
          userObj.mobileNumber = updatedProfile.mobileNumber;
          localStorage.setItem('user', JSON.stringify(userObj));
        }

        this.profileSuccess = 'Profile updated successfully.';
        this.loading = false;
        setTimeout(() => this.profileSuccess = '', 3000);
      },
      error: (err) => {
        this.profileError = err.error?.message || 'Failed to update profile.';
        this.loading = false;
      }
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) return;

    this.passwordSuccess = '';
    this.passwordError = '';
    this.passwordLoading = true;

    const payload = {
      oldPassword: this.passwordForm.value.oldPassword,
      newPassword: this.passwordForm.value.newPassword
    };

    this.userService.changePassword(payload).subscribe({
      next: (res) => {
        this.passwordSuccess = res.message || 'Password changed successfully.';
        this.passwordLoading = false;
        this.passwordForm.reset();
        setTimeout(() => this.passwordSuccess = '', 3000);
      },
      error: (err) => {
        this.passwordError = err.error?.message || 'Incorrect old password.';
        this.passwordLoading = false;
      }
    });
  }

  onlyNumbers(event: KeyboardEvent): boolean {
    const charCode = event.key;
    return /^[0-9]$/.test(charCode);
  }
}
