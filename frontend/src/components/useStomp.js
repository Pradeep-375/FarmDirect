import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import { tokens } from '../api';

const wsUrl = () => import.meta.env.VITE_WS_URL || `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`;

/** Subscribes to a STOMP destination with the JWT on CONNECT. Reconnects automatically. */
export function useStomp(destination, onMessage, enabled = true) {
  const cb = useRef(onMessage); cb.current = onMessage;
  useEffect(() => {
    const t = tokens.get();
    if (!enabled || !destination || !t) return undefined;
    const client = new Client({
      brokerURL: wsUrl(), reconnectDelay: 5000,
      beforeConnect: () => { client.connectHeaders = { Authorization: `Bearer ${tokens.get()?.accessToken}` }; },
      onConnect: () => client.subscribe(destination, (m) => { try { cb.current(JSON.parse(m.body)); } catch { /* ignore malformed frame */ } }),
    });
    client.activate();
    return () => { client.deactivate(); };
  }, [destination, enabled]);
}
