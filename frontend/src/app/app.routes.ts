import { Routes } from '@angular/router';

import { LoginComponent } from './auth/login/login.component';

import {
  CustomerListComponent
} from './customers/customer-list/customer-list.component';

import {
  CustomerDetailComponent
} from './customers/customer-detail/customer-detail.component';

import {
  InteractionListComponent
} from './interactions/interaction-list/interaction-list.component';

import {
  InteractionFormComponent
} from './interactions/interaction-form/interaction-form.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'customers',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'customers',
    component: CustomerListComponent
  },
  {
    path: 'customers/:customerId',
    component: CustomerDetailComponent
  },
  {
    path: 'interactions',
    component: InteractionListComponent
  },
  {
    path: 'interactions/new',
    component: InteractionFormComponent
  },
  {
    path: '**',
    redirectTo: 'customers'
  }
];