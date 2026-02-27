import {Component, OnInit, OnDestroy} from '@angular/core';
import {Delivery, DeliveryStatus, OrderStatusMessage} from "../../model";
import {DeliveryService} from "../../services/delivery.service";
import * as SockJS from 'sockjs-client';
import {Client, CompatClient, IMessage, Stomp, StompSubscription} from '@stomp/stompjs';
import { Subscription } from 'rxjs';
import {AuthService} from "../../services/auth.service";
import {UserService} from "../../services/user.service";
import {AlertService} from "../../services/alert.service";

@Component({
  selector: 'app-list-of-orders',
  templateUrl: './list-of-orders.component.html',
  styleUrls: ['./list-of-orders.component.css']
})
export class ListOfOrdersComponent implements OnInit, OnDestroy{
  deliveries: Delivery[] = [];
  isAdmin: boolean = false;

  //forma
  // status: string = '';
  statuses: string[] = [];
  dateFrom: string = '';
  dateTo: string = '';
  userId: number | null = null;
  users: any[] = [];

  //websoket
  // @ts-ignore
  stompClient: CompatClient;
  isConnected: boolean = false;
  newOrd: OrderStatusMessage | undefined;
  // username: string = '';

  //jer eh filter nacin
  // currentStatusFilter: string | null = null;
  currentStatusFilter: string[] | null = null;


  constructor(
    private deliveryService: DeliveryService,
    private userService: UserService,
    private authService: AuthService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    this.isAdmin = this.authService.getIsUserAdmin();
    if(this.isAdmin){
      this.userService.getUsers(0, 100).subscribe({
        next: (response) => {
          this.users = response.content;
        },
        error: (err) => {
          console.error('Failed to load users', err);
          this.alertService.show("Failed to load users", true);
        }
      });
    }

    if(this.hasPermission('can_search_order')){
      this.search();
    }else{
      this.alertService.show('You do not have permission to search orders', true);
    }

    if(this.hasPermission('can_track_order')){
      this.connectWebSocket();
    }else{
      this.alertService.show('You do not have permission to track orders', true);
    }
  }

  ngOnDestroy() {
    if(this.stompClient != null) {
      this.stompClient.disconnect();
    }
    this.isConnected = false;
  }

  search(): void{
    if (!this.hasPermission('can_search_order')) {
      this.alertService.show('You do not have permission to search orders', true);
      return;
    }

    const statuses = this.statuses.length > 0? this.statuses : undefined;
    //da se menja!
    this.currentStatusFilter = this.statuses.length > 0 ? [...this.statuses] : null;

    this.deliveryService.searchDeliveries(
      statuses,
      this.formatDateForBack(this.dateFrom, true) || undefined,
      this.formatDateForBack(this.dateTo, false) || undefined,
      this.userId ?? undefined
    ).subscribe({
      next: (data) => this.deliveries = data.sort((a, b) => b.id - a.id),
      error: (err) => {
        console.error('Search failed :(', err);
        if(err.status === 404){
          this.alertService.show("User not found", true);
        }else if(err.status === 400){
          this.alertService.show("Invalid date format", true);
        } else {
          this.alertService.show("Search failed :(", true);
        }
      }
    });
  }

  resetForm(): void{
    // this.status = '';
    this.statuses = [];
    this.dateFrom = '';
    this.dateTo = '';
    this.userId = null;
  }

  //za farbanje klasu:
  statusClass(status: string): string{
    if(!status)return 'status-ordered';
    return 'status-'+status.toLowerCase();
  }

  connectWebSocket(): void{
    const jwt = localStorage.getItem('jwt');
    const socket = SockJS(`http://localhost:8080/ws?jwt=${jwt}`);
    this.stompClient = Stomp.over(socket);
    this.stompClient.connect({}, this.onConnect.bind(this));
  }

  onConnect(frame: any){
    this.stompClient.subscribe('/topic/messages', this.handleOrderStatusMessage.bind(this));
    this.isConnected = true;
  }


  handleOrderStatusMessage(msg: any){
    this.newOrd = JSON.parse(msg.body);
    console.log(this.newOrd);

    const myUsername = this.authService.getMyUsername();
    if(!this.isAdmin && this.newOrd?.username !== myUsername){
      return; //ako nije moje da se ne cimam s proverom
    }
    const idx = this.deliveries.findIndex(d => d.id === this.newOrd?.deliveryId);
    if(idx !== -1){
      //ima je
      this.deliveries[idx].status = this.newOrd?.status as DeliveryStatus;

      if(this.currentStatusFilter && !this.currentStatusFilter.includes(this.newOrd?.status ?? '')){
        console.log("removed for not being in line :)");
        this.deliveries.splice(idx, 1); //da je maknem ako ne treba da bude tu vise
      }
      return;
    }
    this.deliveryService.searchDeliveries(
      this.statuses ? this.statuses : undefined,
      this.formatDateForBack(this.dateFrom, true) || undefined,
      this.formatDateForBack(this.dateTo, false) || undefined,
      this.userId || undefined
    ).subscribe({
      next: (data) => {
        const found = data.find(d => d.id === this.newOrd?.deliveryId);
        if(found && this.newOrd){
          found.status = this.newOrd.status as DeliveryStatus;
          this.deliveries = [...this.deliveries, found].sort((a, b) => b.id - a.id);
        }
      },
      error: (err) => {
        console.error("Failed to refresh deliveries after WS msg", err);
        this.alertService.show("Failed to refresh deliveries after WS msg", true);
      }
    });
  }

  cancelOrder(id: number){

    if (!this.hasPermission('can_cancel_order')) {
      this.alertService.show('You do not have permission to cancel orders', true);
      return;
    }

    this.deliveryService.cancelOrder(id).subscribe({
      next: () => {
        console.log("Order canceled yay");
        this.alertService.show("Order canceled successfully yay", false);
      },
      error: (err) => {
        console.error("Failed to cancel order", err);
        if(err.status === 404){
          this.alertService.show("Delivery not found or User not found", true);
        } else if(err.status === 403){
          this.alertService.show("User not allowed to cancel this delivery", true);
        } else if(err.status === 400){
          this.alertService.show("Don't be mean and try to cancel an on-your-way delivery", true);
        } else {
          this.alertService.show("Failed to cancel order", true);
        }
      }
    })
  }

  private formatDateForBack(date: string | null, isStart: boolean): string | null{
    if(!date)return null;

    return isStart
      ? `${date}T00:00:00`
      : `${date}T23:59:59`;
  }

  hasPermission(perm: string): boolean{
    return this.authService.getPermissions().includes(perm);
  }


}
