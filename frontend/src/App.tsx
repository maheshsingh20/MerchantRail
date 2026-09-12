import React, { useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Dashboard from './components/Dashboard/Dashboard';
import TransactionList from './components/Transaction/TransactionList';
import LiveFeed from './components/LiveFeed/LiveFeed';
import ReportsView from './components/Reports/ReportsView';
import Navbar, { Page } from './components/Common/Navbar';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5000,
    },
  },
});

function App() {
  const [currentPage, setCurrentPage] = useState<Page>('dashboard');

  const renderPage = () => {
    switch (currentPage) {
      case 'dashboard':
        return <Dashboard />;
      case 'transactions':
        return <TransactionList />;
      case 'live-feed':
        return <LiveFeed />;
      case 'reports':
        return <ReportsView />;
      default:
        return <Dashboard />;
    }
  };

  return (
    <QueryClientProvider client={queryClient}>
      <div className="min-h-screen bg-gray-50">
        <Navbar currentPage={currentPage} onNavigate={setCurrentPage} />

        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {renderPage()}
        </main>

        <footer className="bg-white border-t mt-12">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
            <div className="text-center text-sm text-gray-500">
              <p className="font-medium text-gray-700">MerchantRail Core Payment & Switching Platform © 2026</p>
              <p className="mt-1">
                Built with Java 17, Spring Boot, Spring Batch, React, and Tanzu/PCF Cloud Native Principles |{' '}
                <a
                  href="https://github.com/maheshsingh20/merchantrail"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-600 hover:text-blue-700 font-semibold"
                >
                  GitHub Repository
                </a>
              </p>
            </div>
          </div>
        </footer>
      </div>
    </QueryClientProvider>
  );
}

export default App;
