import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 개발 중 /api 요청을 Spring Boot(8080)로 넘긴다.
// 브라우저 입장에선 같은 출처라 CORS 문제가 없고,
// 배포 시에도 리버스 프록시(Nginx 등)로 같은 구조를 유지할 수 있다.
export default defineConfig({
  plugins: [react()],
  // 빌드 결과물을 백엔드의 정적 리소스 폴더로 내보낸다.
  // 그러면 Spring Boot 하나가 화면(/)과 API(/api)를 같은 8080에서 서빙한다.
  // → 서버 하나만 띄우면 끝. 더블클릭 런처(SportsInfo.bat)가 이 구조를 쓴다.
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
