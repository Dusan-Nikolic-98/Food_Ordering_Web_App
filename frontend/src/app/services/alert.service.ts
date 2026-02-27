import { Injectable } from '@angular/core';
import {MatSnackBar} from "@angular/material/snack-bar";

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  constructor(private snackBar: MatSnackBar){}

  show(message: string, isError = false, duration = 3000){
    this.snackBar.open(message, isError ? 'X' : '✓',{
      duration,
      horizontalPosition: 'right',
      verticalPosition: 'bottom',
      panelClass: isError ? ['alert-error'] : ['alert-success']
    });
  }
}
