import { Component } from '@angular/core';
import { UserService } from '../../services/user.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-add-user',
  templateUrl: './add-user.component.html',
  styleUrls: ['./add-user.component.css']
})
export class AddUserComponent {
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
  constructor(private userService: UserService, private router: Router) {}

  ngOnInit(): void {
    this.loadPermissions();
  }
  onPermissionChange(permission: string, event: any): void {
    if (event.target.checked) {
      this.permissions.push(permission);
    } else {
      const index = this.permissions.indexOf(permission);
      if (index !== -1) {
        this.permissions.splice(index, 1);
      }
    }
  }

  addUser(): void {
    if (!this.name || !this.email || !this.password || this.permissions.length === 0) {
      this.errorMessage = 'All fields are required!';
      return;
    }
    const permissionsString = this.permissions.join(',');
    const newUser = {
      username: this.email,
      name: this.name,
      password: this.password,
      permissionsString: permissionsString
    };

    this.userService.addUser(newUser).subscribe(
      () => {
        this.router.navigate(['/users']);
      },
      (error) => {
        this.errorMessage = 'Error creating user: ' + error.message;
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
