import axios from 'axios';
import {
  Transaction,
  TransactionRequest,
  TransactionListResponse,
  TransactionStats,
  SettlementSummary,
  SwitchingAnalytics,
} from '../types/transaction';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request interceptor for adding auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('auth_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const transactionApi = {
  submitTransaction: async (request: TransactionRequest): Promise<Transaction> => {
    const response = await api.post<Transaction>('/api/v1/transactions', request);
    return response.data;
  },

  getTransaction: async (id: string): Promise<Transaction> => {
    const response = await api.get<Transaction>(`/api/v1/transactions/${id}`);
    return response.data;
  },

  listTransactions: async (params: {
    merchantId?: string;
    page?: number;
    size?: number;
    sort?: string;
  }): Promise<TransactionListResponse> => {
    const response = await api.get<TransactionListResponse>(
      '/api/v1/transactions',
      { params }
    );
    return response.data;
  },

  getStats: async (merchantId?: string): Promise<TransactionStats> => {
    const response = await api.get<TransactionStats>('/api/v1/transactions/stats', {
      params: { merchantId },
    });
    return response.data;
  },

  searchTransactions: async (query: string): Promise<Transaction[]> => {
    const response = await api.get<Transaction[]>('/api/v1/transactions/search', {
      params: { q: query },
    });
    return response.data;
  },
};

export const reportsApi = {
  getSettlementSummary: async (): Promise<SettlementSummary> => {
    const response = await api.get<SettlementSummary>('/api/v1/reports/settlement-summary');
    return response.data;
  },

  getSwitchingAnalytics: async (): Promise<SwitchingAnalytics> => {
    const response = await api.get<SwitchingAnalytics>('/api/v1/reports/switching-analytics');
    return response.data;
  },

  getReconciliationEntries: async (): Promise<any[]> => {
    const response = await api.get<any[]>('/api/v1/reports/reconciliation/entries');
    return response.data;
  },

  exportReconciliationCsvUrl: (): string => {
    return `${API_BASE_URL}/api/v1/reports/reconciliation/export.csv`;
  },
};

export const batchApi = {
  runClearingBatch: async (): Promise<{ jobId: number; status: string }> => {
    const response = await api.post<{ jobId: number; status: string }>('/api/v1/ledger/batch/clearing');
    return response.data;
  },

  getUnsettledCount: async (): Promise<{ unsettledEntriesCount: number }> => {
    const response = await api.get<{ unsettledEntriesCount: number }>('/api/v1/ledger/batch/unsettled-count');
    return response.data;
  },
};

export const healthApi = {
  checkHealth: async (): Promise<{ status: string }> => {
    const response = await api.get('/actuator/health');
    return response.data;
  },

  getMetrics: async (): Promise<string> => {
    const response = await api.get('/actuator/prometheus', {
      headers: { Accept: 'text/plain' },
    });
    return response.data;
  },
};

export default api;
