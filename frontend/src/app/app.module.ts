import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AddUserComponent } from './components/add-user/add-user.component';
import { ErrorListComponent } from './components/error-list/error-list.component';
import { HomeComponent } from './components/home/home.component';
import { ListOfOrdersComponent } from './components/list-of-orders/list-of-orders.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { OrderFormComponent } from './components/order-form/order-form.component';
import { LoginComponent } from './components/login/login.component';
import { UserEditComponent } from './components/user-edit/user-edit.component';
import { UserListComponent } from './components/user-list/user-list.component';
import { PercentagePipe } from './pipes/percentage.pipe';

@NgModule({
  declarations: [
    AppComponent,
    AddUserComponent,
    ErrorListComponent,
    HomeComponent,
    ListOfOrdersComponent,
    NavbarComponent,
    OrderFormComponent,
    LoginComponent,
    UserEditComponent,
    UserListComponent,
    PercentagePipe
  ],
  imports: [
    BrowserModule,
    AppRoutingModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
