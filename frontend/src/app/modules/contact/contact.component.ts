import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-contact',
  templateUrl: './contact.component.html',
  styles: []
})
export class ContactComponent {
  contactForm: FormGroup;
  successMsg = '';
  errorMsg = '';
  loading = false;

  private apiUrl = `${environment.apiUrl}/api/contact`;

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.contactForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      emailOrMobile: ['', [Validators.required]],
      subject: ['', [Validators.required]],
      message: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  submitMessage(): void {
    if (this.contactForm.invalid) return;
    this.loading = true;
    this.successMsg = '';
    this.errorMsg = '';

    this.http.post(this.apiUrl, this.contactForm.value).subscribe({
      next: (res: any) => {
        this.loading = false;
        this.successMsg = '✅ Thank you! Your message has been sent successfully. We will get back to you shortly.';
        this.contactForm.reset();
      },
      error: (err: any) => {
        this.loading = false;
        this.errorMsg = '❌ Failed to send message. Please try again later.';
        console.error('Contact form submission error:', err);
      }
    });
  }
}
