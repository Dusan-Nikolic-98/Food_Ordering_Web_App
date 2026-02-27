import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private usersUrl = 'http://localhost:8080/users';

  constructor(private http: HttpClient) {}

  getUsers(page: number = 0, size: number = 10): Observable<any> {
    const token = localStorage.getItem('jwt');

    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return this.http.get<any>(`${this.usersUrl}?page=${page}&size=${size}`, { headers });

  }

  addUser(user: { username: string, name: string, password: string, permissionsString: string }): Observable<any> {
    const jwt = localStorage.getItem('jwt');
    return this.http.post<any>(this.usersUrl, user, {
      headers: {
        'Authorization': `Bearer ${jwt}`
      }
    });
  }

  getUserById(id: number): Observable<any> {
    const jwt = localStorage.getItem('jwt');
    let headers = new HttpHeaders();
    if (jwt) {
      headers = headers.set('Authorization', `Bearer ${jwt}`);
    }
    return this.http.get<any>(`${this.usersUrl}/${id}`, { headers });
  }

  updateUser(id: number, user: { username: string; name: string; password: string; permissionsString: string }): Observable<any> {
    const jwt = localStorage.getItem('jwt');
    return this.http.put<any>(`${this.usersUrl}/${id}`, user, {
      headers: {
        Authorization: `Bearer ${jwt}`,
      },
    });
  }

  deleteUser(userId: number): Observable<any> {
    const jwt = localStorage.getItem('jwt');
    let headers = new HttpHeaders();
    if (jwt) {
      headers = headers.set('Authorization', `Bearer ${jwt}`);
    }

    return this.http.delete(`${this.usersUrl}/${userId}`, { headers });
  }


}
