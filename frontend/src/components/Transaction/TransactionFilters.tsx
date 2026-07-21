import React from 'react';

interface TransactionFiltersProps {
  merchantId: string;
  searchQuery: string;
  onMerchantIdChange: (value: string) => void;
  onSearchQueryChange: (value: string) => void;
  onSearch: () => void;
}

const TransactionFilters: React.FC<TransactionFiltersProps> = ({
  merchantId,
  searchQuery,
  onMerchantIdChange,
  onSearchQueryChange,
  onSearch,
}) => {
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      onSearch();
    }
  };

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-lg font-semibold mb-4">Filters</h2>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Merchant ID Filter */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Merchant ID
          </label>
          <input
            type="text"
            value={merchantId}
            onChange={(e) => onMerchantIdChange(e.target.value)}
            placeholder="e.g., MERCH001"
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          />
        </div>

        {/* Search */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Search by Transaction ID
          </label>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => onSearchQueryChange(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="TXN1234567890ABC"
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          />
        </div>

        {/* Search Button */}
        <div className="flex items-end">
          <button
            onClick={onSearch}
            className="w-full px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition"
          >
            Search
          </button>
        </div>
      </div>
    </div>
  );
};

export default TransactionFilters;
