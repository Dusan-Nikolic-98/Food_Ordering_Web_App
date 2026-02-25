import { Injectable } from '@angular/core';
import { CanActivate, CanDeactivate, Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate, CanDeactivate<unknown> {

  constructor(private router: Router) {}

  canActivate(): boolean {
    const jwt = localStorage.getItem('jwt');
    if (jwt) {
      return true;
    } else {
      this.router.navigate(['/']);
      return false;
    }
  }

  canDeactivate(): boolean {
    return this.canActivate(); //pp da je ovo ok
  }
}
