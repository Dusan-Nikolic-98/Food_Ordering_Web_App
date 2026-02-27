import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'percentage'
})
export class PercentagePipe implements PipeTransform {
  transform(value: number): string {
    return value !== null && value !== undefined ? Math.round(value * 100) + '%' : 'N/A';
  }

}
