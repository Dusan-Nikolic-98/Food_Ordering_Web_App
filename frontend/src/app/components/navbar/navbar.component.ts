import {Component, OnDestroy, OnInit} from '@angular/core';
import {AuthService} from "../../services/auth.service";
import {Subscription} from "rxjs";
import {Router} from "@angular/router";

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy{
  isLoggedIn: boolean = false;
  isAdmin: boolean = false;
  username: string | null = null;

  private authSubscriptions: Subscription[] = [];

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {
    this.authSubscriptions.push(this.authService.isLoggedIn$.subscribe(isLoggedIn => {
        this.isLoggedIn = isLoggedIn;
      }),
      this.authService.username$.subscribe(username => {
        this.username = username;
      }),
      this.authService.isAdmin$.subscribe(isAdmin => {
        this.isAdmin = isAdmin;
      })
    );
  }

  ngOnDestroy(){
    this.authSubscriptions.forEach(sub => sub.unsubscribe());
  }

}
