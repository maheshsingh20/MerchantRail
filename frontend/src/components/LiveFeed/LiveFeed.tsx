import React, { useState, useEffect } from 'react';
import websocketService from '../../services/websocket';
import { StatusUpdate } from '../../types/transaction';
import TransactionCard from './TransactionCard';

const LiveFeed: React.FC = () => {
  const [updates, setUpdates] = useState<StatusUpdate[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const maxUpdates = 20; // Keep last 20 updates

  useEffect(() => {
    // Connect to WebSocket
    websocketService.connect();
    setIsConnected(websocketService.isConnected());

    // Subscribe to all transaction updates
    websocketService.subscribeToAll((update) => {
      setUpdates((prev) => {
        const newUpdates = [update, ...prev];
        return newUpdates.slice(0, maxUpdates);
      });
    });

    // Check connection status periodically
    const statusInterval = setInterval(() => {
      setIsConnected(websocketService.isConnected());
    }, 1000);

    return () => {
      websocketService.unsubscribeFromAll();
      clearInterval(statusInterval);
    };
  }, []);

  const handleClearFeed = () => {
    setUpdates([]);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div className="flex items-center space-x-3">
          <h1 className="text-3xl font-bold text-gray-900">Live Transaction Feed</h1>
          {/* Connection Status */}
          <div className="flex items-center space-x-2">
            <div
              className={`w-3 h-3 rounded-full ${isConnected ? 'bg-green-500 animate-pulse' : 'bg-red-500'
                }`}
            ></div>
            <span className="text-sm text-gray-600">
              {isConnected ? 'Connected' : 'Disconnected'}
            </span>
          </div>
        </div>

        <button
          onClick={handleClearFeed}
          className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
        >
          Clear Feed
        </button>
      </div>

      {/* Connection Status Banner */}
      {!isConnected && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
          <p className="text-yellow-800">
            ⚠️ WebSocket disconnected. Attempting to reconnect...
          </p>
        </div>
      )}

      {/* Live Updates */}
      <div className="space-y-3">
        {updates.length === 0 ? (
          <div className="bg-white rounded-lg shadow p-12">
            <div className="text-center text-gray-500">
              <p className="text-xl mb-2">No live updates yet</p>
              <p className="text-sm">
                Transaction status updates will appear here in real-time
              </p>
            </div>
          </div>
        ) : (
          updates.map((update, index) => (
            <TransactionCard key={`${update.transactionId}-${index}`} update={update} />
          ))
        )}
      </div>

      {/* Stats Footer */}
      <div className="bg-white rounded-lg shadow p-4">
        <div className="grid grid-cols-3 gap-4 text-center">
          <div>
            <p className="text-sm text-gray-500">Total Updates</p>
            <p className="text-2xl font-bold text-gray-900">{updates.length}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Connection Status</p>
            <p className="text-2xl font-bold text-gray-900">
              {isConnected ? '🟢 Live' : '🔴 Offline'}
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Last Update</p>
            <p className="text-sm font-medium text-gray-900">
              {updates.length > 0
                ? new Date(updates[0].timestamp).toLocaleTimeString()
                : 'N/A'}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LiveFeed;
