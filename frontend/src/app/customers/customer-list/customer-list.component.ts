
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CustomerService } from '../customer.service';
import { Customer } from '../customer.model';

@Component({
  selector: 'app-customer-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './customer-list.component.html',
  styleUrl: './customer-list.component.css'
})
export class CustomerListComponent implements OnInit {
  private readonly customerService = inject(CustomerService);

  customers: Customer[] = [];
  loading = true;
  error = '';

  ngOnInit(): void {
    this.customerService.getCustomers().subscribe({
      next: (customers) => {
        this.customers = customers;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load customers:', err);
        this.error = 'Could not load customers. Check that the backend is running.';
        this.loading = false;
      }
    });
  }
}
