import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CustomerService } from '../customer.service';
import { Customer } from '../customer.model';

@Component({
  selector: 'app-customer-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './customer-detail.component.html',
  styleUrl: './customer-detail.component.css'
})
export class CustomerDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly customerService = inject(CustomerService);

  customer?: Customer;
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    const customerId =
      this.route.snapshot.paramMap.get('customerId');

    if (!customerId) {
      this.errorMessage = 'Customer ID was not provided.';
      this.loading = false;
      return;
    }

    this.customerService.getCustomer(customerId).subscribe({
      next: (customer) => {
        this.customer = customer;
        this.loading = false;
      },
      error: () => {
        this.errorMessage =
          'Could not load this customer. Verify the customer ID and API.';
        this.loading = false;
      }
    });
  }
}