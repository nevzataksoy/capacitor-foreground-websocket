import { registerPlugin } from '@capacitor/core';
import type { ForegroundWebsocketOptions } from './definitions';

export interface CapacitorForegroundWebsocketPlugin {
  start(options: ForegroundWebsocketOptions): Promise<void>;
  stop(): Promise<void>;
  send(data: string): Promise<void>;
  addListener(
    eventName: 'onopen' | 'onmessage' | 'onclose' | 'onerror' | 'onpush' | 'onpushToken',
    listenerFunc: (data: any) => void
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
}

const CapacitorForegroundWebsocket = registerPlugin<CapacitorForegroundWebsocketPlugin>(
  'CapacitorForegroundWebsocket',
  {
    web: () => import('./web').then(m => new m.CapacitorForegroundWebsocketWeb()),
  }
);

export { CapacitorForegroundWebsocket };