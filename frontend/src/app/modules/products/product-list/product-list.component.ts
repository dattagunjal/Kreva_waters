import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Product } from '../../../shared/models/models';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';

@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html',
  styleUrls: []
})
export class ProductListComponent implements OnInit {
  products: Product[] = [];
  loading = true;
  addedToCart: { [key: number]: boolean } = {};

  showCustomizer = false;
  customizerForm!: FormGroup;
  selectedProduct: Product | null = null;
  logoBase64 = '';

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private fb: FormBuilder
  ) {
    this.initCustomizerForm();
  }

  private initCustomizerForm(): void {
    this.customizerForm = this.fb.group({
      businessName: ['', [Validators.required]],
      tagline: [''],
      contactNumber: ['', [Validators.pattern('^[0-9]{10}$')]],
      website: [''],
      notes: ['']
    });
  }

  ngOnInit(): void {
    this.productService.getAll().subscribe({
      next: (data) => { this.products = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  addToCart(product: Product): void {
    this.cartService.addToCart(product);
    this.addedToCart[product.id!] = true;
    setTimeout(() => this.addedToCart[product.id!] = false, 1500);
  }

  openCustomizer(product: Product): void {
    this.selectedProduct = product;
    this.showCustomizer = true;
    this.logoBase64 = '';
    this.initCustomizerForm();
  }

  closeCustomizer(): void {
    this.showCustomizer = false;
    this.selectedProduct = null;
    this.logoBase64 = '';
  }

  onLogoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const reader = new FileReader();
      reader.onload = () => {
        this.logoBase64 = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  updatePreview(): void {
    // preview updates dynamically through binding, this is a hook if needed
  }

  addCustomizedToCart(): void {
    if (this.customizerForm.invalid || !this.selectedProduct) return;

    const customizationData = {
      ...this.customizerForm.value,
      logoUrl: this.logoBase64 || null
    };

    this.cartService.addToCart(this.selectedProduct, customizationData);
    
    // show success toast or toggle state
    const prodId = this.selectedProduct.id!;
    this.closeCustomizer();

    this.addedToCart[prodId] = true;
    setTimeout(() => this.addedToCart[prodId] = false, 1500);
  }

  onlyNumbers(event: KeyboardEvent): boolean {
    const charCode = event.key;
    return /^[0-9]$/.test(charCode);
  }
}
