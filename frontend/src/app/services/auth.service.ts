import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private loginUrl = 'http://localhost:8080/auth/login';

  //za pracenje
  private isLoggedInSubj = new BehaviorSubject<boolean>(this.isAuthenticated());
  private usernameSubj = new BehaviorSubject<string | null>(this.getMyUsername());
  private isAdminSubj = new BehaviorSubject<boolean>(this.getIsUserAdmin());

  //ekspouz obzerabilnih
  isLoggedIn$ = this.isLoggedInSubj.asObservable();
  username$ = this.usernameSubj.asObservable();
  isAdmin$ = this.isAdminSubj.asObservable();


  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<{ jwt: string }> {
    const payload = { username, password };
    return this.http.post<{ jwt: string }>(this.loginUrl, payload);
  }

  saveToken(jwt: string): void {
    localStorage.setItem('jwt', jwt);
    this.savePermissions(jwt);
    this.updateAuthState();
  }

  getToken(): string | null {
    return localStorage.getItem('jwt');
  }

  clearToken(): void {
    localStorage.removeItem('permissions');
    localStorage.removeItem('jwt');
    // localStorage.setItem('jwt', '');
    // localStorage.setItem('permissions', '');
    this.updateAuthState();
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  private savePermissions(token: string): void {
    const decodedToken = this.decodeToken(token);
    const permissions = decodedToken?.permissions || [];
    localStorage.setItem('permissions', JSON.stringify(permissions));
  }

  private decodeToken(token: string): any {
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload));
    } catch (error) {
      console.error('Error decoding token', error);
      return null;
    }
  }

  getPermissions(): string[] {
    const permissions = localStorage.getItem('permissions');
    return permissions ? JSON.parse(permissions) : [];
  }

  getIsUserAdmin(): boolean{
    const token = this.getToken();
    if(!token)return false;
    const decoded = this.decodeToken(token);
    return !!decoded?.isAdmin;

  }
  getMyUsername(): string | null{
    const token = this.getToken();
    if(!token)return null;
    const decoded = this.decodeToken(token);
    return decoded?.sub || null;

  }

  private updateAuthState(): void{
    this.isLoggedInSubj.next(this.isAuthenticated());
    this.usernameSubj.next(this.getMyUsername());
    this.isAdminSubj.next(this.getIsUserAdmin());
    // console.log("he updated it, so why did he die :(");
  }
}
