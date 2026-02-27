export interface LoginResponse {
  jwt: string;
}

export interface User {
  userId: number;
  username: string;
  name: string;
  permissions: string[];
  permissionsString: string;
  loginCount: number;
  version: number;
  balance: number;
  salary: number;
}

export interface Dish {
  noOf: number;
  name: string;
  pricePerDish: number;
}

export interface DishWithImg{
  name: string;
  pricePerDish: number;
  imageUrl: string;
}

export type DeliveryStatus = 'ORDERED' | 'PREPARING' | 'IN_DELIVERY' | 'DELIVERED' | 'CANCELED' | 'DECLINED';

export interface Delivery{
  id: number;
  status: DeliveryStatus;
  createdByUsername: string;
  active: boolean;
  items: Dish[];
  createdAt: string;
}

export interface OrderStatusMessage{
  deliveryId: number;
  status: string;
  username: string;
  timestamp: string;
}

export interface ErrorMessageResponseDto{
  id: number;
  userId: number;
  username: string;
  deliveryId: number;
  statusAtDecline: string;
  message: string;
  createdAt: string;
}
