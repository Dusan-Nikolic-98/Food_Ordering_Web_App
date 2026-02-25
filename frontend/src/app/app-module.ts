import { NgModule, provideBrowserGlobalErrorListeners } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { App } from './app';
import { AddUser } from './components/add-user/add-user';
import { ErrorList } from './components/error-list/error-list';
import { Home } from './components/home/home';
import { ListOfOrders } from './components/list-of-orders/list-of-orders';
import { Navbar } from './components/navbar/navbar';
import { OrderForm } from './components/order-form/order-form';
import { Login } from './components/login/login';
import { UserEdit } from './components/user-edit/user-edit';
import { UserList } from './components/user-list/user-list';
import { PercentagePipe } from './pipes/percentage-pipe';

@NgModule({
  declarations: [
    App,
    AddUser,
    ErrorList,
    Home,
    ListOfOrders,
    Navbar,
    OrderForm,
    Login,
    UserEdit,
    UserList,
    PercentagePipe
  ],
  imports: [
    BrowserModule,
    AppRoutingModule
  ],
  providers: [
    provideBrowserGlobalErrorListeners(),
  ],
  bootstrap: [App]
})
export class AppModule { }
