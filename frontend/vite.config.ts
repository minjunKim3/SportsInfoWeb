import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 개발 중 /api 요청을 Spring Boot(8080)로 넘긴다.
// 브라우저 입장에선 같은 출처라 CORS 문제가 없고,
// 배포 시에도 리버스 프록시(Nginx 등)로 같은 구조를 유지할 수 있다.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
