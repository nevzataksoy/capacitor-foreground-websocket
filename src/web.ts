import { WebPlugin } from '@capacitor/core';
import type { CapacitorForegroundWebsocketPlugin } from './index';
import type { ForegroundWebsocketOptions } from './definitions';

export class CapacitorForegroundWebsocketWeb extends WebPlugin implements CapacitorForegroundWebsocketPlugin {
  private socket: WebSocket | null = null;

  async start(options: ForegroundWebsocketOptions): Promise<void> {
    const protocol = options.isWss ? 'wss' : 'ws';
    const url = `${protocol}://${options.ip}:${options.port}`;
    this.socket = new WebSocket(url);

    this.socket.onopen = (event) => {
      this.notifyListeners('onopen', event);
    };

    this.socket.onmessage = (event) => {
      this.notifyListeners('onmessage', event.data);
    };

    this.socket.onclose = (event) => {
      this.notifyListeners('onclose', event);
    };

    this.socket.onerror = (event) => {
      this.notifyListeners('onerror', event);
    };
  }

  async stop(): Promise<void> {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }

  async send(data: string): Promise<void> {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(data);
    } else {
      throw new Error('WebSocket is not open');
    }
  }
}