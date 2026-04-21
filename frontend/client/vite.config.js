import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 18085,
    strictPort: true,
    proxy: {
      "/user": {
        target: "http://127.0.0.1:8082",
        changeOrigin: true,
      },
      "/auth": {
        target: "http://127.0.0.1:8083",
        changeOrigin: true,
      },
    },
  },
});
