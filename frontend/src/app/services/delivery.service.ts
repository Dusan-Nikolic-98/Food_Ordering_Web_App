import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {Delivery, Dish, DishWithImg} from "../model";

@Injectable({
  providedIn: 'root',
})
export class DeliveryService {
  private baseUrl = 'http://localhost:8080/deliveries';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwt');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return headers;
  }

  searchDeliveries(
    status?: string[],
    dateFrom?: string,
    dateTo?: string,
    userId?: number
  ): Observable<Delivery[]> {
    let params = new HttpParams();
    if (status) status.forEach(s => (params = params.append('status', s)));
    if (dateFrom) params = params.set('dateFrom', dateFrom);
    if (dateTo) params = params.set('dateTo', dateTo);
    if (userId) params = params.set('userId', userId.toString());

    return this.http.get<Delivery[]>(this.baseUrl, {
      headers: this.getAuthHeaders(),
      params,
    });
  }

  placeOrder(delivery: { dishes: any[] }): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/place`, delivery, {
      headers: this.getAuthHeaders(),
    });
  }

  scheduleOrder(delivery: { dishes: any[], scheduledAt: string }): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/schedule`, delivery, {
      headers: this.getAuthHeaders(),
    });
  }

  cancelOrder(id: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/${id}/cancel`, null, {
      headers: this.getAuthHeaders(),
    });
  }

  //todo za izbrisati
  trackOrder(id: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${id}/track`, {
      headers: this.getAuthHeaders(),
    });
  }

  searchErrors(userId?: number, page: number = 0, size: number = 10): Observable<any> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (userId) params = params.set('userId', userId.toString());

    return this.http.get<any>(`${this.baseUrl}/errors`, {
      headers: this.getAuthHeaders(),
      params,
    });
  }

  getDishes(): Observable<DishWithImg[]> {
    return this.http.get<DishWithImg[]>(`${this.baseUrl}/dishes`,{
      headers: this.getAuthHeaders(),
    });
  }

}
