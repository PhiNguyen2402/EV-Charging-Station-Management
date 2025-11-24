export type ConnectorType = 'CCS' | 'CHADEMO' | 'AC_TYPE_2' | 'AC_TYPE_1';
export type SessionStatus = 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'FAILED' | 'CHARGING';
export type ChargingPointStatus = 'AVAILABLE' | 'CHARGING' | 'RESERVED' | 'OFFLINE' | 'FAULTED';
export type PaymentType = 'CREDIT_CARD' | 'E_WALLET' | 'BANK_TRANSFER' | 'CASH';
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export interface AuthResponse {
    token: string;
    email: string;
    role: 'ROLE_ADMIN' | 'ROLE_CSSTAFF' | 'ROLE_EVDRIVER';
}

export interface UserSummaryDto {
    id: number;
    profileId?: number | null;
    stationId?: number | null;
    assignedStation?: StationSummaryDto | null;
    name: string;
    email: string;
    role: string;
}

export interface VehicleDto {
    id: number;
    vehicleId: string;
    brand: string;
    model: string;
    batteryCapacity: number;
    connectorType: ConnectorType;
}

export interface CreateVehicleRequest {
    vehicleId: string;
    brand: string;
    model: string;
    batteryCapacity: number;
    connectorType: ConnectorType;
}

export interface EVDriverProfileDto {
    id: number;
    userAccount: UserSummaryDto;
    phoneNumber: string | null;
    walletBalance: number;
    vehicles: VehicleDto[];
}

export interface ChargeSessionDto {
    sessionId: number;
    driverId: number;
    vehicleId: number;
    chargingPointId: number;
    stationId: number;
    startTime: string;
    endTime: string | null;
    energyUsed: number;
    cost: number;
    status: SessionStatus;
}

export interface CreateSessionData {
    driverId: number;
    vehicleId: number;
    chargingPointId: number;
}

export interface StopSessionData {
    energyUsed: number;
}

export interface ChargingStationDto {
  stationId: number;
  name: string;
  location: string;
  status: 'AVAILABLE' | 'IN_USE' | 'OFFLINE' | 'FAULTED';
  totalChargingPoint: number;
  availableChargers: number;
}
export interface ChargingPointDto {
    chargingPointId: number;
    stationId: number;
    type: ConnectorType;
    power: number;
    status: ChargingPointStatus;
}

export interface Notification {
    id: string;
    message: string;
    timeAgo: string;
}

export interface NotificationApiResponse {
    unreadCount: number;
    notifications: Notification[];
}

export interface WalletBalanceApiResponse {
    driverProfileId: number;
    email: string;
    oldBalance: number;
    amountChanged: number;
    newBalance: number;
}

export interface WalletTopUpRequest {
    amount: number;
    paymentMethodId: number;
}

export interface PaymentMethodDto {
    id: number;
    type: 'CREDIT_CARD' | 'E_WALLET' | 'BANK_TRANSFER' | 'CASH';
    provider: string;
    isDefault: boolean;
}

export interface Station {
  id: number;
  name: string;
  location: string;
  ports: number;
  status: 'active' | 'maintenance' | 'offline';
  description?: string;
  operatingHours?: string;
}

export interface ChargingPoint {
  id: number;
  name: string;
  status: 'active' | 'maintenance' | 'offline';
  stationId: number;
}

export interface PaymentMethodDto {
    id: number;
    type: PaymentType;
    provider: string;
    isDefault: boolean;
}

export interface CreatePaymentMethodRequest {
    driverId: number;
    type: PaymentType;
    provider: string;
    isDefault: boolean;
}

export interface BookingDto {
    id: number;
    driverId: number;
    chargingPointId: number;
    stationName: string;
    startTime: string;
    endTime: string;
    status: BookingStatus;
}

export interface CreateBookingRequest {
    chargingPointId: number;
    startTime: string;
    endTime: string;
}

export interface CreateCSStaffRequest {
    email: string;
    name: string;
    password: string;
    phoneNumber: string;
    stationId: number;
}

export interface CSStaffResponseDto {
    id: number;
    userAccount: UserSummaryDto;
    phoneNumber: string;
    assignedStation: StationSummaryDto | null;
}

export interface UpdateCSStaffProfileRequest {
    name?: string;
    phoneNumber?: string;
    stationId?: number;
}

export interface StationSummaryDto {
    stationId: number;
    name: string;
    location: string;
    status: string;
}

export interface ChatMessage {
    id: string;
    role: 'user' | 'assistant';
    content: string;
    timestamp: number;
}

export interface ChatHistoryDto {
    id: number;
    userMessage: string;
    botResponse: string;
    timestamp: string;
}

export interface InvoiceDto {
    invoiceId: number;
    chargeSessionId: number;
    driverId: number;
    issueDate: string;
    totalEnergy: number;
    amount: number;
    paymentMethodId: number | null;
    status: 'PENDING' | 'PAID' | 'CANCELLED';
}

export interface PayInvoiceRequest {
    paymentMethodId: number;
}