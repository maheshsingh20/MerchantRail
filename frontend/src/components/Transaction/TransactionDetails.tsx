import React, { useEffect, useState } from 'react';
import { Transaction } from '../../types/transaction';
import StatusBadge from '../Common/StatusBadge';
import websocketService from '../../services/websocket';

interface TransactionDetailsProps {
  transaction: Transaction;
  onClose: () => void;
}

const TransactionDetails: React.FC<TransactionDetailsProps> = ({
  transaction: initialTransaction,
  onClose,
}) => {
  const [transaction, setTransaction] = useState(initialTransaction);

  useEffect(() => {
    // Subscribe to real-time updates for this transaction
    websocketService.subscribeToTransaction(transaction.transactionId, (update) => {
      setTransaction((prev) => ({
        ...prev,
        status: update.status,
        updatedAt: update.timestamp,
      }));
    });

    return () => {
      websocketService.unsubscribeFromTransaction(transaction.transactionId);
    };
  }, [transaction.transactionId]);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex justify-between items-center p-6 border-b">
          <h2 className="text-2xl font-bold text-gray-900">Transaction Details</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-2xl"
          >
            ×
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6">
          {/* Transaction ID */}
          <div>
            <label className="block text-sm font-medium text-gray-500 mb-1">
              Transaction ID
            </label>
            <p className="text-lg font-mono text-gray-900">{transaction.transactionId}</p>
          </div>

          {/* Status */}
          <div>
            <label className="block text-sm font-medium text-gray-500 mb-2">
              Current Status
            </label>
            <StatusBadge status={transaction.status} size="large" />
          </div>

          {/* Amount & Currency */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                Amount
              </label>
              <p className="text-2xl font-bold text-gray-900">
                ${transaction.amount.toFixed(2)}
              </p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                Currency
              </label>
              <p className="text-2xl font-bold text-gray-900">{transaction.currency}</p>
            </div>
          </div>

          {/* Merchant */}
          <div>
            <label className="block text-sm font-medium text-gray-500 mb-1">
              Merchant ID
            </label>
            <p className="text-lg font-mono text-gray-900">{transaction.merchantId}</p>
          </div>

          {/* Timestamps */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                Created At
              </label>
              <p className="text-sm text-gray-900">
                {new Date(transaction.createdAt).toLocaleString()}
              </p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                Last Updated
              </label>
              <p className="text-sm text-gray-900">
                {new Date(transaction.updatedAt).toLocaleString()}
              </p>
            </div>
          </div>

          {/* Idempotency Key */}
          {transaction.idempotencyKey && (
            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                Idempotency Key
              </label>
              <p className="text-sm font-mono text-gray-600 break-all">
                {transaction.idempotencyKey}
              </p>
            </div>
          )}

          {/* Status Timeline */}
          <div>
            <label className="block text-sm font-medium text-gray-500 mb-3">
              Status Timeline
            </label>
            <div className="space-y-2">
              <div className="flex items-center space-x-3">
                <div className="w-3 h-3 rounded-full bg-blue-500"></div>
                <span className="text-sm text-gray-700">
                  {new Date(transaction.createdAt).toLocaleString()} - PENDING
                </span>
              </div>
              {transaction.status !== 'PENDING' && (
                <div className="flex items-center space-x-3">
                  <div className="w-3 h-3 rounded-full bg-green-500"></div>
                  <span className="text-sm text-gray-700">
                    {new Date(transaction.updatedAt).toLocaleString()} - {transaction.status}
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-6 border-t bg-gray-50">
          <button
            onClick={onClose}
            className="w-full px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default TransactionDetails;
