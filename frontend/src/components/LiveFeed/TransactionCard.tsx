import React from 'react';
import { StatusUpdate } from '../../types/transaction';
import StatusBadge from '../Common/StatusBadge';

interface TransactionCardProps {
  update: StatusUpdate;
}

const TransactionCard: React.FC<TransactionCardProps> = ({ update }) => {
  const timeAgo = (timestamp: string) => {
    const seconds = Math.floor((Date.now() - new Date(timestamp).getTime()) / 1000);

    if (seconds < 60) return `${seconds}s ago`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    return `${hours}h ago`;
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-4 hover:shadow-lg transition-shadow border-l-4 border-primary-500 animate-fade-in">
      <div className="flex justify-between items-start">
        <div className="flex-1">
          <div className="flex items-center space-x-3 mb-2">
            <span className="text-sm font-mono text-gray-600">
              {update.transactionId}
            </span>
            <StatusBadge status={update.status} size="small" />
          </div>
          <p className="text-xs text-gray-500">{timeAgo(update.timestamp)}</p>
        </div>
        <div className="text-right">
          <span className="inline-block px-2 py-1 bg-blue-50 text-blue-700 rounded text-xs font-medium">
            NEW
          </span>
        </div>
      </div>
    </div>
  );
};

export default TransactionCard;
