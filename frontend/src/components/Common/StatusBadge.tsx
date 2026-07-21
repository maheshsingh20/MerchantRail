import React from 'react';
import { TransactionStatus } from '../../types/transaction';

interface StatusBadgeProps {
  status: TransactionStatus;
  size?: 'small' | 'medium' | 'large';
}

const statusConfig = {
  PENDING: {
    color: 'bg-yellow-100 text-yellow-800 border-yellow-200',
    icon: '⏳',
    label: 'Pending',
  },
  FRAUD_CHECK: {
    color: 'bg-blue-100 text-blue-800 border-blue-200',
    icon: '🔍',
    label: 'Fraud Check',
  },
  APPROVED: {
    color: 'bg-green-100 text-green-800 border-green-200',
    icon: '✅',
    label: 'Approved',
  },
  REJECTED: {
    color: 'bg-red-100 text-red-800 border-red-200',
    icon: '❌',
    label: 'Rejected',
  },
  SETTLED: {
    color: 'bg-purple-100 text-purple-800 border-purple-200',
    icon: '💰',
    label: 'Settled',
  },
  REVERSED: {
    color: 'bg-gray-100 text-gray-800 border-gray-200',
    icon: '↩️',
    label: 'Reversed',
  },
};

const sizeClasses = {
  small: 'px-2 py-1 text-xs',
  medium: 'px-3 py-1 text-sm',
  large: 'px-4 py-2 text-base',
};

const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'medium' }) => {
  const config = statusConfig[status];

  return (
    <span
      className={`inline-flex items-center space-x-1 rounded-full border font-medium ${config.color} ${sizeClasses[size]}`}
    >
      <span>{config.icon}</span>
      <span>{config.label}</span>
    </span>
  );
};

export default StatusBadge;
