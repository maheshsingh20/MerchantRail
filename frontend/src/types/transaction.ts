// Transaction & Switching type definitions

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
  statusReason?: string;
  
  // Switching & Network metadata
  cardPan?: string;
  cardBin?: string;
  cardBrand?: string;
  routedIssuerId?: string;
  isStip?: boolean;
  authCode?: string;
  interchangeFee?: number;
  switchFee?: number;
}

export interface TransactionRequest {
  merchantId: string;
  amount: number;
  currency: string;
  idempotencyKey: string;
  cardNumber?: string;
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

export interface SettlementSummary {
  totalEntriesCount: number;
  settledCount: number;
  pendingCount: number;
  totalVolumeUsd: number;
  totalInterchangeFeesUsd: number;
  isBalanced: boolean;
  netImbalanceUsd: number;
  auditTimestamp: string;
}

export interface SwitchingAnalytics {
  totalTransactions: number;
  mastercardSharePct: number;
  visaSharePct: number;
  stipAuthorizationRatePct: number;
  totalStipTransactions: number;
  issuerVolumeMap: Record<string, number>;
}

export interface StatusUpdate {
  transactionId: string;
  status: TransactionStatus;
  timestamp: string;
}
