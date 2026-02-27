import { Component, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import {User} from '../../model'
import { Router } from '@angular/router';

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.css']
})
export class UserListComponent implements OnInit {
  users: User[] = [];
  errorMessage: string = '';
  page: number = 0;
  size: number = 10;
  permissionsUsers: string[] = [];

  constructor(private userService: UserService, private router: Router) {}

  ngOnInit(): void {
    this.getUsers();
    this.loadPermissions();
  }

  getUsers(): void {
    this.userService.getUsers(this.page, this.size).subscribe(
      (response) => {
        this.users = response.content;
      },
      (error) => {
        this.errorMessage = 'Failed to load users. :(';
      }
    );
  }
  editUser(userId: number): void {
    this.router.navigate([`/user/edit/${userId}`]);
  }

  deleteUser(userId: number): void {
    if (confirm('Are you sure you want to delete this user?')) {
      this.userService.deleteUser(userId).subscribe(
        () => {
          this.getUsers();
        },
        (error) => {
          this.errorMessage = 'Failed to delete the user. :(';
        }
      );
      // alert("prebaci posle?");
    }
  }

  loadPermissions(): void {
    const token = localStorage.getItem('jwt');
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]));
      this.permissionsUsers = payload.permissions || [];
    }
  }

  hasPermission(permission: string): boolean {
    return this.permissionsUsers.includes(permission);
  }

}
