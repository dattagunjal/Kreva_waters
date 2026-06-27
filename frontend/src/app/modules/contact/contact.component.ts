import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-contact',
  templateUrl: './contact.component.html',
  styles: []
})
export class ContactComponent {
  contactForm: FormGroup;
  successMsg = '';
  loading = false;

  constructor(private fb: FormBuilder) {
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

    setTimeout(() => {
      this.loading = false;
      this.successMsg = '✅ Thank you! Your message has been sent successfully. We will get back to you shortly.';
      this.contactForm.reset();
    }, 1000);
  }
}
