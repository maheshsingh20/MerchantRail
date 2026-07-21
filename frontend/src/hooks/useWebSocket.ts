import { useEffect, useState, useCallback } from 'react';
import websocketService from '../services/websocket';
import { StatusUpdate } from '../types/transaction';

export const useWebSocket = () => {
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    websocketService.connect();
    setIsConnected(websocketService.isConnected());

    const interval = setInterval(() => {
      setIsConnected(websocketService.isConnected());
    }, 1000);

    return () => {
      clearInterval(interval);
      websocketService.disconnect();
    };
  }, []);

  return { isConnected };
};

export const useTransactionUpdates = (
  transactionId: string | null,
  callback: (update: StatusUpdate) => void
) => {
  useEffect(() => {
    if (!transactionId) return;

    websocketService.subscribeToTransaction(transactionId, callback);

    return () => {
      if (transactionId) {
        websocketService.unsubscribeFromTransaction(transactionId);
      }
    };
  }, [transactionId, callback]);
};

export const useAllTransactionUpdates = (callback: (update: StatusUpdate) => void) => {
  const stableCallback = useCallback(callback, []);

  useEffect(() => {
    websocketService.subscribeToAll(stableCallback);

    return () => {
      websocketService.unsubscribeFromAll();
    };
  }, [stableCallback]);
};
