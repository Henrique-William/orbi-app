export interface Delivery {
  id: number;
  order: number;
  status: string;
  dropoffAddress: string;
  pickUpDateTime?: string;
  deliveredAt?: string;
}

export interface RouteData {
  id: number;
  driverName: string;
  deliveries: Delivery[];
  createdAt?: string;
}