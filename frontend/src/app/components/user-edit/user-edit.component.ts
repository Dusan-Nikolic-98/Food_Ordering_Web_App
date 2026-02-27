import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../services/user.service';
import {AuthService} from "../../services/auth.service";

@Component({
  selector: 'app-user-edit',
  templateUrl: './user-edit.component.html',
  styleUrls: ['./user-edit.component.css']
})
export class UserEditComponent implements OnInit {
  userId!: number;
  name: string = '';
  email: string = '';
  password: string = '';
  permissions: string[] = [];
  allPermissions: string[] = [
    'can_read_users',
    'can_create_users',
    'can_update_users',
    'can_delete_users',
    'can_search_order',
    'can_track_order',
    'can_cancel_order',
    'can_place_order',
    'can_schedule_order'
  ];
  errorMessage: string = '';
  permissionsUsers: string[] = [];
  isEditingSelf: boolean = false;

  constructor(
    private userService: UserService,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.userId = Number(this.route.snapshot.paramMap.get('id'));
    // alert(this.userId);
    this.loadUserDetails();
    this.loadPermissions();
  }

  loadUserDetails(): void {
    this.userService.getUserById(this.userId).subscribe(
      (user) => {
        this.name = user.name;
        this.email = user.username;
        this.permissions = user.permissionsString.split(',');

        const myUsername = this.authService.getMyUsername();
        this.isEditingSelf = myUsername === user.username;
      },
      (error) => {
        this.errorMessage = 'Failed to load user details. :(';
      }
    );
  }

  onPermissionChange(permission: string, event: any): void {
    if (event.target.checked) {
      if (!this.permissions.includes(permission)) {
        this.permissions.push(permission);
      }
    } else {
      this.permissions = this.permissions.filter((p) => p !== permission);
    }
  }

  updateUser(): void {
    if (!this.name || !this.email || !this.password) {
      this.errorMessage = 'All fields are required!';
      return;
    }
    const permissionsString = this.permissions.join(',');
    const updatedUser = {
      username: this.email,
      name: this.name,
      password: this.password,
      permissionsString: permissionsString
    };

    this.userService.updateUser(this.userId, updatedUser).subscribe(
      () => {
        this.router.navigate(['/users']);
      },
      (error) => {
        this.errorMessage = 'Error updating user: ' + error.message;
      }
    );
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
