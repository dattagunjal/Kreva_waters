import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Product, Order, User } from '../../../shared/models/models';
import { ProductService } from '../../../core/services/product.service';
import { OrderService } from '../../../core/services/order.service';
import { CategoryService, Category } from '../../../core/services/category.service';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-admin',
  templateUrl: './dashboard.component.html',
  styleUrls: []
})
export class AdminDashboardComponent implements OnInit {
  products: Product[] = [];
  categories: Category[] = [];
  orders: Order[] = [];
  users: User[] = [];
  stats: any = null;

  activeTab: 'overview' | 'products' | 'categories' | 'orders' | 'users' = 'overview';
  
  productForm: FormGroup;
  editingProduct: Product | null = null;
  showForm = false;

  categoryForm: FormGroup;
  editingCategory: Category | null = null;
  showCategoryForm = false;

  // Search & Filter & Pagination States
  productSearch = '';
  productCategoryFilter = '';
  productPage = 1;

  categorySearch = '';
  categoryPage = 1;

  orderSearch = '';
  orderStatusFilter = '';
  orderPage = 1;

  userSearch = '';
  userPage = 1;

  pageSize = 5;

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    private orderService: OrderService,
    private categoryService: CategoryService,
    private adminService: AdminService
  ) {
    this.productForm = this.fb.group({
      name: ['', Validators.required],
      description: ['', Validators.required],
      price: [0, [Validators.required, Validators.min(1)]],
      stock: [0, [Validators.required, Validators.min(0)]],
      imageUrl: [''],
      category: [null] // Category object
    });

    this.categoryForm = this.fb.group({
      name: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadStats();
    this.loadProducts();
    this.loadCategories();
    this.loadOrders();
    this.loadUsers();
  }

  loadStats(): void {
    this.adminService.getStats().subscribe(data => this.stats = data);
  }

  loadProducts(): void {
    this.productService.getAll().subscribe(data => this.products = data);
  }

  loadCategories(): void {
    this.categoryService.getAll().subscribe(data => this.categories = data);
  }

  loadOrders(): void {
    this.orderService.getAllOrders().subscribe(data => this.orders = data);
  }

  loadUsers(): void {
    this.adminService.getUsers().subscribe(data => this.users = data);
  }

  // ── Product CRUD ───────────────────────────────────────────────────────────
  openAddForm(): void {
    this.editingProduct = null;
    this.productForm.reset();
    this.showForm = true;
  }

  editProduct(product: Product): void {
    this.editingProduct = product;
    this.productForm.patchValue({
      name: product.name,
      description: product.description,
      price: product.price,
      stock: product.stock,
      imageUrl: product.imageUrl,
      category: product.category ? this.categories.find(c => c.id === product.category?.id) : null
    });
    this.showForm = true;
  }

  saveProduct(): void {
    if (this.productForm.invalid) return;
    const data = this.productForm.value;

    const call = this.editingProduct
      ? this.productService.update(this.editingProduct.id!, data)
      : this.productService.create(data);

    call.subscribe(() => {
      this.loadProducts();
      this.loadStats();
      this.showForm = false;
    });
  }

  deleteProduct(id: number): void {
    if (confirm('Delete this product?')) {
      this.productService.delete(id).subscribe(() => {
        this.loadProducts();
        this.loadStats();
      });
    }
  }

  // ── Category CRUD ──────────────────────────────────────────────────────────
  openAddCategoryForm(): void {
    this.editingCategory = null;
    this.categoryForm.reset();
    this.showCategoryForm = true;
  }

  editCategory(category: Category): void {
    this.editingCategory = category;
    this.categoryForm.patchValue(category);
    this.showCategoryForm = true;
  }

  saveCategory(): void {
    if (this.categoryForm.invalid) return;
    const data = this.categoryForm.value;

    const call = this.editingCategory
      ? this.categoryService.update(this.editingCategory.id!, data)
      : this.categoryService.create(data);

    call.subscribe({
      next: () => {
        this.loadCategories();
        this.loadProducts(); // Category update might affect products
        this.showCategoryForm = false;
      },
      error: (err) => alert(err.error?.message || 'Failed to save category.')
    });
  }

  deleteCategory(id: number): void {
    if (confirm('Delete this category? Products in this category will become uncategorized.')) {
      this.categoryService.delete(id).subscribe(() => {
        this.loadCategories();
        this.loadProducts();
      });
    }
  }

  // ── Order status update ────────────────────────────────────────────────────
  updateOrderStatus(id: number, status: string): void {
    this.orderService.updateStatus(id, status).subscribe(() => {
      this.loadOrders();
      this.loadStats();
    });
  }

  deleteOrder(id: number): void {
    if (confirm('Are you sure you want to completely delete this order? This action cannot be undone.')) {
      this.orderService.deleteOrder(id).subscribe({
        next: () => {
          this.loadOrders();
          this.loadStats();
        },
        error: (err) => alert(err.error?.message || 'Failed to delete order.')
      });
    }
  }

  downloadInvoice(id: number): void {
    this.orderService.downloadInvoice(id);
  }

  // ── User management ────────────────────────────────────────────────────────
  updateUserRole(id: number, currentRole: string): void {
    const newRole = currentRole === 'ADMIN' ? 'USER' : 'ADMIN';
    if (confirm(`Change user role to ${newRole}?`)) {
      this.adminService.updateUserRole(id, newRole).subscribe(() => {
        this.loadUsers();
        this.loadStats();
      });
    }
  }

  deleteUser(id: number): void {
    if (confirm('Are you sure you want to delete this user? This cannot be undone.')) {
      this.adminService.deleteUser(id).subscribe(() => {
        this.loadUsers();
        this.loadStats();
      });
    }
  }

  // ── Filtered & Paginated Lists ─────────────────────────────────────────────
  getFilteredProducts() {
    let result = [...this.products];

    if (this.productSearch) {
      const q = this.productSearch.toLowerCase();
      result = result.filter(p => p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q));
    }

    if (this.productCategoryFilter) {
      result = result.filter(p => p.category?.name === this.productCategoryFilter);
    }

    return result;
  }

  getPaginatedProducts() {
    const filtered = this.getFilteredProducts();
    const startIndex = (this.productPage - 1) * this.pageSize;
    return filtered.slice(startIndex, startIndex + this.pageSize);
  }

  getProductPageCount() {
    return Math.ceil(this.getFilteredProducts().length / this.pageSize) || 1;
  }

  // Category
  getFilteredCategories() {
    let result = [...this.categories];
    if (this.categorySearch) {
      const q = this.categorySearch.toLowerCase();
      result = result.filter(c => c.name.toLowerCase().includes(q));
    }
    return result;
  }

  getPaginatedCategories() {
    const filtered = this.getFilteredCategories();
    const startIndex = (this.categoryPage - 1) * this.pageSize;
    return filtered.slice(startIndex, startIndex + this.pageSize);
  }

  getCategoryPageCount() {
    return Math.ceil(this.getFilteredCategories().length / this.pageSize) || 1;
  }

  // Orders
  getFilteredOrders() {
    let result = [...this.orders];

    if (this.orderSearch) {
      const q = this.orderSearch.toLowerCase();
      result = result.filter(o => 
        o.id?.toString().includes(q) || 
        o.address.toLowerCase().includes(q)
      );
    }

    if (this.orderStatusFilter) {
      result = result.filter(o => o.status === this.orderStatusFilter);
    }

    return result;
  }

  getPaginatedOrders() {
    const filtered = this.getFilteredOrders();
    const startIndex = (this.orderPage - 1) * this.pageSize;
    return filtered.slice(startIndex, startIndex + this.pageSize);
  }

  getOrderPageCount() {
    return Math.ceil(this.getFilteredOrders().length / this.pageSize) || 1;
  }

  // Users
  getFilteredUsers() {
    let result = [...this.users];

    if (this.userSearch) {
      const q = this.userSearch.toLowerCase();
      result = result.filter(u => 
        u.name.toLowerCase().includes(q) || 
        u.email?.toLowerCase().includes(q) || 
        u.mobileNumber?.includes(q)
      );
    }

    return result;
  }

  getPaginatedUsers() {
    const filtered = this.getFilteredUsers();
    const startIndex = (this.userPage - 1) * this.pageSize;
    return filtered.slice(startIndex, startIndex + this.pageSize);
  }

  getUserPageCount() {
    return Math.ceil(this.getFilteredUsers().length / this.pageSize) || 1;
  }
}
