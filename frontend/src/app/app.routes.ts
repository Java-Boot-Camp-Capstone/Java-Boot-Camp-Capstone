
import { Routes } from '@angular/router';

import { LoginComponent } from './auth/login/login.component';
import { authGuard } from './auth/auth.guard';

import { CustomerListComponent } from './customers/customer-list/customer-list.component';
import { CustomerDetailComponent } from './customers/customer-detail/customer-detail.component';
import { InteractionListComponent } from './interactions/interaction-list/interaction-list.component';
import { InteractionFormComponent } from './interactions/interaction-form/interaction-form.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },

  {
    path: 'customers',
    component: CustomerListComponent,
    canActivate: [authGuard]
  },
  {
    path: 'customers/:customerId',
    component: CustomerDetailComponent,
    canActivate: [authGuard]
  },
  {
    path: 'interactions',
    component: InteractionListComponent,
    canActivate: [authGuard]
  },
  {
    path: 'interactions/new',
    component: InteractionFormComponent,
    canActivate: [authGuard]
  },

  { path: '', redirectTo: 'customers', pathMatch: 'full' },
  { path: '**', redirectTo: 'customers' }
];
