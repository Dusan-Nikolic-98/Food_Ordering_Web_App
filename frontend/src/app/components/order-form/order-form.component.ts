import {Component, OnInit} from '@angular/core';
import {DishWithImg} from "../../model";
import {FormBuilder, FormGroup} from "@angular/forms";
import {DeliveryService} from "../../services/delivery.service";
import {AlertService} from "../../services/alert.service";
import {AuthService} from "../../services/auth.service";

@Component({
  selector: 'app-order-form',
  templateUrl: './order-form.component.html',
  styleUrls: ['./order-form.component.css']
})
export class OrderFormComponent implements OnInit{
  dishes: DishWithImg[] = [];
  selectedDishes: {[key: string]: number} = {};
  showSchedulePopup = false;
  scheduleForm: FormGroup;

  hours: number[] = Array.from({length:24}, (_, i) => i);
  minutes: number[] = Array.from({length:60}, (_, i) => i);

  constructor(private deliveryService: DeliveryService,
              private fb: FormBuilder,
              private authService: AuthService,
              private alertService: AlertService
  ) {

    this.scheduleForm = this.fb.group({
      date: [new Date()], //da mi bude difolt danas
      hour: [new Date().getHours()],
      minute: [0]
    });
  }

  ngOnInit() {
    this.deliveryService.getDishes().subscribe({
      next: (dishes) => {
        this.dishes = dishes;
        dishes.forEach(d => this.selectedDishes[d.name] = 0);
      },
      error: () => {
        this.alertService.show("Failed to fetch dishes", true);
      }
    });
  }

  get hasSelectedDishes(): boolean{
    return this.getSelectedDishList().length > 0;
  }

  get isScheduleTimeValid(): boolean{
    const { date, hour, minute } = this.scheduleForm.value;
    if(!date) return false;

    const scheduledDate = new Date(date);
    scheduledDate.setHours(hour, minute, 0, 0);

    return scheduledDate.getTime() > new Date().getTime();
  }

  increaseQuantity(dish: DishWithImg):void{
    this.selectedDishes[dish.name]++;
  }
  decreaseQuantity(dish: DishWithImg): void {
    if (this.selectedDishes[dish.name] > 0) {
      this.selectedDishes[dish.name]--;
    }
  }

  getSelectedDishList(): string[]{
    return Object.entries(this.selectedDishes)
      .filter(([_, qty]) => qty > 0)
      .map(([name, qty]) => {
        const dish = this.dishes.find(d => d.name === name);
        return `${name},${qty},${dish?.pricePerDish ?? 0}`;
      });
  }

  placeOrder(): void{
    if(!this.hasPermission('can_place_order')){
      this.alertService.show('You do not have permission to place orders', true);
      return;
    }

    const dto = {dishes: this.getSelectedDishList()};
    this.deliveryService.placeOrder(dto).subscribe({
      next: () => {
        this.alertService.show('Order placed!', false);
      },
      error: (err) => {
        if(err.status === 404){
          this.alertService.show("User not found", true);
        }else if (err.status === 400) {
          this.alertService.show("Invalid dish format", true);
        } else if (err.status === 409) {
          this.alertService.show("Too many deliveries at this time\nDeclined when ordered :(", true);
        } else {
          this.alertService.show("Unexpected error while placing order", true);
        }
      }
    });
  }

  openSchedulePopup(): void{
    this.showSchedulePopup = true;
  }
  closeSchedulePopup(): void {
    this.showSchedulePopup = false;
    this.scheduleForm.reset();
  }

  confirmSchedule(): void{
    if(!this.hasPermission('can_schedule_order')){
      this.alertService.show('You do not have permission to schedule orders', true);
      return;
    }

    const { date, hour, minute } = this.scheduleForm.value;
    if(!date) return;
    const scheduledDate = new Date(date);
    scheduledDate.setHours(hour, minute, 0, 0);

    //da imam dobar format
    const isoString = scheduledDate.toISOString().slice(0,16);
    console.log(isoString);
    const pad = (n: number) => n.toString().padStart(2, '0');

    const localIsoString =
      scheduledDate.getFullYear() + "-" +
      pad(scheduledDate.getMonth() + 1) + "-" +
      pad(scheduledDate.getDate()) + "T" +
      pad(scheduledDate.getHours()) + ":" +
      pad(scheduledDate.getMinutes());

    console.log(localIsoString);

    const dto = {
      dishes: this.getSelectedDishList(),
      scheduledAt: localIsoString
    };

    this.deliveryService.scheduleOrder(dto).subscribe({
      next: () => {
        this.alertService.show('Order scheduled!', false);
        this.closeSchedulePopup();
      },
      error: (err) => {
        if (err.status === 400 && err.error?.includes("Invalid schedule date")) {
          this.alertService.show("Invalid schedule date", true);
        } else if (err.status === 400) {
          this.alertService.show("Invalid dish format", true);
        } else if (err.status === 404) {
          this.alertService.show("User not found", true);
        } else if (err.status === 409) {
          this.alertService.show("Too many deliveries would be active at that time — scheduling declined", true);
        } else {
          this.alertService.show("Unexpected error while scheduling order", true);
        }
      }
    });
  }


  hasPermission(perm: string): boolean{
    return this.authService.getPermissions().includes(perm);
  }

}
