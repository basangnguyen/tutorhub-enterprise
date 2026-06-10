import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { viteSingleFile } from 'vite-plugin-singlefile'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), viteSingleFile()],
  build: {
    outDir: '../html',
    emptyOutDir: false,
    rollupOptions: {
      output: {
        entryFileNames: `tldraw_board.js`,
        assetFileNames: `tldraw_board.[ext]`,
        chunkFileNames: `tldraw_board.chunk.js`,
      }
    }
  }
})
