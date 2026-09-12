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

  // New Transaction Modal State
  const [showModal, setShowModal] = useState(false);
  const [newCardNumber, setNewCardNumber] = useState('5123451234567890');
  const [newAmount, setNewAmount] = useState('150.00');
  const [newMerchantId, setNewMerchantId] = useState('MCH00001');
  const [submitting, setSubmitting] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState<string | null>(null);

  useEffect(() => {
    fetchTransactions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
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

      setTransactions(response?.content || []);
      setTotalPages(response?.totalPages || 1);
      setError(null);
    } catch (err) {
      console.warn('Could not load transactions:', err);
      setError(null);
    } finally {
      setLoading(false);
    }
  };

  const handleSimulateTransaction = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      const idemp = `TXN_SIM_${Date.now()}`;
      const res = await transactionApi.submitTransaction({
        merchantId: newMerchantId,
        amount: parseFloat(newAmount),
        currency: 'USD',
        idempotencyKey: idemp,
        cardNumber: newCardNumber,
      });

      setSubmitSuccess(`Authorized: ${res.transactionId} (${res.cardBrand || 'Card'} Routed)`);
      setShowModal(false);
      await fetchTransactions();
      setTimeout(() => setSubmitSuccess(null), 5000);
    } catch (err: any) {
      alert(`Submission error: ${err?.response?.data?.message || err?.message || 'Check card format'}`);
    } finally {
      setSubmitting(false);
    }
  };

  const setPreset = (card: string, amt: string, mch: string) => {
    setNewCardNumber(card);
    setNewAmount(amt);
    setNewMerchantId(mch);
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
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Transactions</h1>
          <p className="text-sm text-gray-500 mt-1">Live card network switching ledger & routing trace</p>
        </div>
        <div className="flex space-x-3">
          <button
            onClick={() => setShowModal(true)}
            className="px-4 py-2 bg-emerald-600 text-white font-medium rounded-lg hover:bg-emerald-700 shadow transition flex items-center space-x-2"
          >
            <span>💳</span>
            <span>Simulate Card Transaction</span>
          </button>
          <button
            onClick={handleRefresh}
            disabled={loading}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition disabled:opacity-50"
          >
            {loading ? 'Loading...' : 'Refresh'}
          </button>
        </div>
      </div>

      {submitSuccess && (
        <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 p-4 rounded-lg flex items-center space-x-2">
          <span>✅</span>
          <span className="font-semibold">{submitSuccess}</span>
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl max-w-lg w-full p-6 shadow-2xl space-y-5 animate-fade-in">
            <div className="flex justify-between items-center border-b pb-3">
              <h2 className="text-xl font-bold text-gray-900">Simulate Card Switch Transaction</h2>
              <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-gray-600 text-2xl font-bold">×</button>
            </div>

            {/* Quick Pick Presets */}
            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Quick Test BINs</p>
              <div className="grid grid-cols-2 gap-2 text-xs">
                <button
                  type="button"
                  onClick={() => setPreset('5123451234567890', '180.00', 'MCH00001')}
                  className="p-2 border rounded-lg hover:border-blue-500 hover:bg-blue-50 text-left"
                >
                  <p className="font-bold text-blue-900">Mastercard (Citi)</p>
                  <p className="text-gray-500">BIN 512345 | $180.00</p>
                </button>
                <button
                  type="button"
                  onClick={() => setPreset('5234561234567890', '320.50', 'MCH00001')}
                  className="p-2 border rounded-lg hover:border-blue-500 hover:bg-blue-50 text-left"
                >
                  <p className="font-bold text-blue-900">Mastercard (Chase)</p>
                  <p className="text-gray-500">BIN 523456 | $320.50</p>
                </button>
                <button
                  type="button"
                  onClick={() => setPreset('5432101234567890', '45.00', 'MCH00001')}
                  className="p-2 border rounded-lg hover:border-blue-500 hover:bg-blue-50 text-left"
                >
                  <p className="font-bold text-blue-900">Mastercard (HSBC)</p>
                  <p className="text-gray-500">BIN 543210 | $45.00</p>
                </button>
                <button
                  type="button"
                  onClick={() => setPreset('4111111111111111', '95.00', 'MCH00001')}
                  className="p-2 border rounded-lg hover:border-blue-500 hover:bg-blue-50 text-left"
                >
                  <p className="font-bold text-blue-900">Visa Net</p>
                  <p className="text-gray-500">BIN 411111 | $95.00</p>
                </button>
              </div>
            </div>

            <form onSubmit={handleSimulateTransaction} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700">Card Number (PAN)</label>
                <input
                  type="text"
                  required
                  value={newCardNumber}
                  onChange={(e) => setNewCardNumber(e.target.value)}
                  className="mt-1 block w-full rounded-md border border-gray-300 p-2 font-mono text-sm focus:border-blue-500 focus:ring-blue-500"
                  placeholder="e.g. 5123451234567890"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700">Amount (USD)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="1"
                    required
                    value={newAmount}
                    onChange={(e) => setNewAmount(e.target.value)}
                    className="mt-1 block w-full rounded-md border border-gray-300 p-2 text-sm focus:border-blue-500 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">Merchant ID (8 Chars)</label>
                  <input
                    type="text"
                    required
                    maxLength={8}
                    value={newMerchantId}
                    onChange={(e) => setNewMerchantId(e.target.value)}
                    className="mt-1 block w-full rounded-md border border-gray-300 p-2 font-mono text-sm uppercase focus:border-blue-500 focus:ring-blue-500"
                  />
                </div>
              </div>

              <div className="flex justify-end space-x-3 pt-3 border-t">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 border rounded-lg text-gray-700 hover:bg-gray-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-5 py-2 bg-blue-600 text-white rounded-lg font-semibold hover:bg-blue-700 disabled:opacity-50"
                >
                  {submitting ? 'Routing...' : 'Authorize & Switch'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

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
