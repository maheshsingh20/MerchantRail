import { io, Socket } from 'socket.io-client';
import { StatusUpdate } from '../types/transaction';

const WS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8081';

class WebSocketService {
  private socket: Socket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;

  /**
   * Connect to WebSocket server
   */
  connect(): void {
    if (this.socket?.connected) {
      console.log('WebSocket already connected');
      return;
    }

    this.socket = io(WS_URL, {
      transports: ['websocket'],
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionAttempts: this.maxReconnectAttempts,
    });

    this.socket.on('connect', () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
    });

    this.socket.on('disconnect', (reason) => {
      console.log('WebSocket disconnected:', reason);
    });

    this.socket.on('connect_error', (error) => {
      console.error('WebSocket connection error:', error);
      this.reconnectAttempts++;

      if (this.reconnectAttempts >= this.maxReconnectAttempts) {
        console.error('Max reconnection attempts reached');
        this.disconnect();
      }
    });
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect(): void {
    if (this.socket) {
      this.socket.disconnect();
      this.socket = null;
    }
  }

  /**
   * Subscribe to transaction updates
   */
  subscribeToTransaction(
    transactionId: string,
    callback: (update: StatusUpdate) => void
  ): void {
    if (!this.socket) {
      console.error('WebSocket not connected');
      return;
    }

    const eventName = `transaction.${transactionId}`;
    this.socket.on(eventName, callback);

    // Join transaction room
    this.socket.emit('subscribe', { transactionId });
  }

  /**
   * Unsubscribe from transaction updates
   */
  unsubscribeFromTransaction(transactionId: string): void {
    if (!this.socket) {
      return;
    }

    const eventName = `transaction.${transactionId}`;
    this.socket.off(eventName);

    // Leave transaction room
    this.socket.emit('unsubscribe', { transactionId });
  }

  /**
   * Subscribe to all transaction updates
   */
  subscribeToAll(callback: (update: StatusUpdate) => void): void {
    if (!this.socket) {
      console.error('WebSocket not connected');
      return;
    }

    this.socket.on('transaction.update', callback);
  }

  /**
   * Unsubscribe from all transaction updates
   */
  unsubscribeFromAll(): void {
    if (!this.socket) {
      return;
    }

    this.socket.off('transaction.update');
  }

  /**
   * Check if WebSocket is connected
   */
  isConnected(): boolean {
    return this.socket?.connected || false;
  }

  /**
   * Get connection status
   */
  getStatus(): 'connected' | 'disconnected' | 'connecting' {
    if (!this.socket) return 'disconnected';
    if (this.socket.connected) return 'connected';
    return 'connecting';
  }
}

// Singleton instance
const websocketService = new WebSocketService();

export default websocketService;
