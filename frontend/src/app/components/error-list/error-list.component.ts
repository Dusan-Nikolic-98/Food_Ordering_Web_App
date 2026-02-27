import {Component, OnInit} from '@angular/core';
import {ErrorMessageResponseDto} from "../../model";
import {DeliveryService} from "../../services/delivery.service";
import {AuthService} from "../../services/auth.service";
import {AlertService} from "../../services/alert.service";

@Component({
  selector: 'app-error-list',
  templateUrl: './error-list.component.html',
  styleUrls: ['./error-list.component.css']
})
export class ErrorListComponent implements OnInit{
  errors: ErrorMessageResponseDto[] = [];
  totalElements = 0;
  pageSize = 10;
  currentPage = 0;
  isAdmin = false;
  userId?: number;

  loading = false;

  constructor(
    private deliveryService: DeliveryService,
    private authService: AuthService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    this.isAdmin = this.authService.getIsUserAdmin();
    this.fetchErrors();
  }

  fetchErrors(page: number = 0): void{

    this.loading = true;
    this.deliveryService.searchErrors(
      this.isAdmin?this.userId : undefined,
      page,
      this.pageSize
    ).subscribe({
      next: (res) => {
        this.errors = res.content;
        this.totalElements = res.totalElements;
        this.currentPage = res.number;
        this.loading = false;
      },
      error: (err) => {
        console.error("Failed to fetch errors", err);
        this.alertService.show("Failed to fetch errors", true);
        this.loading = false;
      }
    });
  }

  onPageChange(event: any): void{
    this.fetchErrors(event.pageIndex);
  }

  skipToEdge(): void{
    const targetPage = this.currentPage === this.getLastPage() ? 0: this.getLastPage();
    this.fetchErrors(targetPage);
  }
  getLastPage(): number{
    return Math.ceil(this.totalElements / this.pageSize) -1;
  }

}
