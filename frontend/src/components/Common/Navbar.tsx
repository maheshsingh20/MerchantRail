import React from 'react';

export type Page = 'dashboard' | 'transactions' | 'live-feed' | 'reports';

interface NavbarProps {
  currentPage: Page;
  onNavigate: (page: Page) => void;
}

const Navbar: React.FC<NavbarProps> = ({ currentPage, onNavigate }) => {
  const navItems: { id: Page; label: string; icon: string }[] = [
    { id: 'dashboard', label: 'Dashboard', icon: '📊' },
    { id: 'transactions', label: 'Transactions', icon: '💳' },
    { id: 'live-feed', label: 'Live Feed', icon: '📡' },
    { id: 'reports', label: 'Reports & Clearing', icon: '📈' },
  ];

  return (
    <nav className="bg-white shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          {/* Logo */}
          <div className="flex items-center">
            <h1 className="text-2xl font-bold text-primary-600">
              💳 MerchantRail
            </h1>
            <span className="ml-3 px-2 py-0.5 text-xs font-semibold bg-blue-100 text-blue-800 rounded">
              Switching Platform
            </span>
          </div>

          {/* Navigation Items */}
          <div className="flex items-center space-x-3">
            {navItems.map((item) => (
              <button
                key={item.id}
                onClick={() => onNavigate(item.id)}
                className={`px-4 py-2 rounded-lg font-medium transition ${
                  currentPage === item.id
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'text-gray-700 hover:bg-gray-100'
                }`}
              >
                <span className="mr-2">{item.icon}</span>
                {item.label}
              </button>
            ))}
          </div>

          {/* User Menu */}
          <div className="flex items-center">
            <div className="flex items-center space-x-3">
              <span className="text-sm font-medium text-gray-600">Network Operator</span>
              <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center border border-blue-200">
                <span className="text-blue-700 font-bold">OP</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
