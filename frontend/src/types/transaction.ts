// Transaction type definitions

export enum TransactionStatus {
  PENDING = 'PENDING',
  FRAUD_CHECK = 'FRAUD_CHECK',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  SETTLED = 'SETTLED',
  REVERSED = 'REVERSED',
}

export interface Transaction {
  transactionId: string;
  merchantId: string;
  amount: number;
  currency: string;
  status: TransactionStatus;
  createdAt: string;
  updatedAt: string;
  idempotencyKey?: string;
}

export interface TransactionRequest {
  merchantId: string;
  amount: number;
  currency: string;
  idempotencyKey: string;
}

export interface TransactionListResponse {
  content: Transaction[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TransactionStats {
  totalTransactions: number;
  successRate: number;
  averageAmount: number;
  pendingCount: number;
  approvedCount: number;
  rejectedCount: number;
}

export interface StatusUpdate {
  transactionId: string;
  status: TransactionStatus;
  timestamp: string;
}
