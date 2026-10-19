import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Vite configuration for the React frontend.
// The dev server runs on port 5173, which must be whitelisted by the backend CORS config.
export default defineConfig({
  plugins: [react()],
  server: {
    host: true, // listen on all interfaces so phones on the same Wi-Fi can connect
    port: 5173,
    open: false,
  },
  preview: {
    port: 4173,
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
    css: false,
  },
});
