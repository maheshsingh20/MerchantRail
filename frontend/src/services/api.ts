import axios from 'axios';
import {
  Transaction,
  TransactionRequest,
  TransactionListResponse,
  TransactionStats,
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
      // Handle unauthorized - redirect to login
      localStorage.removeItem('auth_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const transactionApi = {
  /**
   * Submit a new transaction
   */
  submitTransaction: async (request: TransactionRequest): Promise<Transaction> => {
    const response = await api.post<Transaction>('/api/v1/transactions', request);
    return response.data;
  },

  /**
   * Get transaction by ID
   */
  getTransaction: async (id: string): Promise<Transaction> => {
    const response = await api.get<Transaction>(`/api/v1/transactions/${id}`);
    return response.data;
  },

  /**
   * List transactions with pagination
   */
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

  /**
   * Get transaction statistics
   */
  getStats: async (merchantId?: string): Promise<TransactionStats> => {
    const response = await api.get<TransactionStats>('/api/v1/transactions/stats', {
      params: { merchantId },
    });
    return response.data;
  },

  /**
   * Search transactions
   */
  searchTransactions: async (query: string): Promise<Transaction[]> => {
    const response = await api.get<Transaction[]>('/api/v1/transactions/search', {
      params: { q: query },
    });
    return response.data;
  },
};

export const healthApi = {
  /**
   * Check service health
   */
  checkHealth: async (): Promise<{ status: string }> => {
    const response = await api.get('/actuator/health');
    return response.data;
  },

  /**
   * Get metrics
   */
  getMetrics: async (): Promise<string> => {
    const response = await api.get('/actuator/prometheus', {
      headers: { Accept: 'text/plain' },
    });
    return response.data;
  },
};

export default api;
