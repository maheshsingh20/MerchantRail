import React, { useState, useEffect } from 'react';
import { transactionApi } from '../../services/api';
import { Transaction, TransactionListResponse } from '../../types/transaction';
import TransactionTable from './TransactionTable';
import TransactionFilters from './TransactionFilters';
import TransactionDetails from './TransactionDetails';

const TransactionList: React.FC = () => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null);

  // Filters
  const [merchantId, setMerchantId] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');

  useEffect(() => {
    fetchTransactions();
  }, [page, merchantId]);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const response: TransactionListResponse = await transactionApi.listTransactions({
        merchantId: merchantId || undefined,
        page,
        size: 20,
        sort: 'createdAt,desc',
      });

      setTransactions(response.content);
      setTotalPages(response.totalPages);
      setError(null);
    } catch (err) {
      setError('Failed to load transactions');
      console.error('Error fetching transactions:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    if (!searchQuery.trim()) {
      fetchTransactions();
      return;
    }

    try {
      setLoading(true);
      const results = await transactionApi.searchTransactions(searchQuery);
      setTransactions(results);
      setError(null);
    } catch (err) {
      setError('Search failed');
      console.error('Error searching transactions:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleTransactionClick = (transaction: Transaction) => {
    setSelectedTransaction(transaction);
  };

  const handleCloseDetails = () => {
    setSelectedTransaction(null);
  };

  const handleRefresh = () => {
    fetchTransactions();
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Transactions</h1>
        <button
          onClick={handleRefresh}
          disabled={loading}
          className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition disabled:opacity-50"
        >
          {loading ? 'Loading...' : 'Refresh'}
        </button>
      </div>

      {/* Filters */}
      <TransactionFilters
        merchantId={merchantId}
        searchQuery={searchQuery}
        onMerchantIdChange={setMerchantId}
        onSearchQueryChange={setSearchQuery}
        onSearch={handleSearch}
      />

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-800">{error}</p>
        </div>
      )}

      {/* Transaction Table */}
      <TransactionTable
        transactions={transactions}
        loading={loading}
        onTransactionClick={handleTransactionClick}
      />

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center items-center space-x-2">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0 || loading}
            className="px-4 py-2 bg-gray-200 rounded hover:bg-gray-300 disabled:opacity-50"
          >
            Previous
          </button>
          <span className="text-gray-700">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
            disabled={page >= totalPages - 1 || loading}
            className="px-4 py-2 bg-gray-200 rounded hover:bg-gray-300 disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}

      {/* Transaction Details Modal */}
      {selectedTransaction && (
        <TransactionDetails
          transaction={selectedTransaction}
          onClose={handleCloseDetails}
        />
      )}
    </div>
  );
};

export default TransactionList;
