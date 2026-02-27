import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import {LoginResponse} from "../../model";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent {
  username: string = '';
  password: string = '';
  errorMessage: string = '';

  constructor(private authService: AuthService, private router: Router) {}

  login(): void {
    if (!this.username || !this.password) {
      this.errorMessage = 'Both fields are required!';
      return;
    }

    this.authService.login(this.username, this.password).subscribe(
      (response:LoginResponse) => {

        this.authService.saveToken(response.jwt);
        // za ako bas nema nista
        const permissions = this.authService.getPermissions();
        if (permissions.length === 0) {
          alert('You have no permissions!');
        }

        this.router.navigate(['/']);
      },
      () => {
        this.errorMessage = 'Invalid credentials. Please try again.';
      }
    );
  }
}
