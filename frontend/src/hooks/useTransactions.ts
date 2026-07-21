import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { transactionApi } from '../services/api';
import { TransactionRequest } from '../types/transaction';

export const useTransactions = (params?: {
  merchantId?: string;
  page?: number;
  size?: number;
}) => {
  return useQuery({
    queryKey: ['transactions', params],
    queryFn: () => transactionApi.listTransactions(params || {}),
    staleTime: 10000, // Consider data fresh for 10 seconds
  });
};

export const useTransaction = (id: string) => {
  return useQuery({
    queryKey: ['transaction', id],
    queryFn: () => transactionApi.getTransaction(id),
    enabled: !!id,
  });
};

export const useTransactionStats = (merchantId?: string) => {
  return useQuery({
    queryKey: ['transaction-stats', merchantId],
    queryFn: () => transactionApi.getStats(merchantId),
    staleTime: 30000, // Consider stats fresh for 30 seconds
  });
};

export const useSubmitTransaction = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: TransactionRequest) =>
      transactionApi.submitTransaction(request),
    onSuccess: () => {
      // Invalidate and refetch transactions
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      queryClient.invalidateQueries({ queryKey: ['transaction-stats'] });
    },
  });
};

export const useSearchTransactions = (query: string) => {
  return useQuery({
    queryKey: ['search-transactions', query],
    queryFn: () => transactionApi.searchTransactions(query),
    enabled: !!query && query.length > 0,
  });
};
