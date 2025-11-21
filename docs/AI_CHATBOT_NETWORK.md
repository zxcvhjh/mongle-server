# KNU AI 챗봇 네트워크 설정 가이드

## 🌐 환경별 네트워크 구성

### 로컬 개발 환경
```
로컬 PC (VPC 밖)
  → 인터넷
    → AWS Public IP
      → AI 서버 (172.31.37.76)
```

**사용할 IP:** AI 서버의 **Public IP** (예: `http://3.34.XXX.XXX:5000`)

---

### EC2 배포 환경
```
Spring Boot EC2 (VPC 안)
  → AWS 내부 네트워크
    → AI 서버 (172.31.37.76)
```

**사용할 IP:** AI 서버의 **Private IP** (`http://172.31.37.76:5000`) ✅ 권장

---

## ⚙️ 설정 방법

### 1️⃣ 로컬 개발 시

`.env` 파일:
```env
AI_CHATBOT_BASE_URL=http://<AI서버_Public_IP>:5000
```

### 2️⃣ EC2 배포 시

`.env` 또는 환경 변수:
```env
AI_CHATBOT_BASE_URL=http://172.31.37.76:5000
```

---

## 💡 Private IP를 사용하는 이유

| 항목 | Private IP | Public IP |
|------|-----------|-----------|
| **속도** | 빠름 (내부망) | 느림 (인터넷 경유) |
| **비용** | 무료 | 데이터 전송 비용 |
| **보안** | VPC 내부만 접근 | 외부 노출 |

---

## 🔒 보안 그룹 설정

### AI 서버 인바운드 규칙

| 타입 | 프로토콜 | 포트 | 소스 | 용도 |
|------|---------|------|------|------|
| Custom TCP | TCP | 5000 | `0.0.0.0/0` | 로컬 테스트용 (Public) |
| Custom TCP | TCP | 5000 | `172.31.0.0/16` | EC2 내부 통신용 (Private) |

> **보안 강화:** 프로덕션 배포 후 `0.0.0.0/0` 규칙은 제거하고 VPC 내부만 허용하는 것을 권장합니다.

---

## 🚀 실행 방법

### 로컬 Docker
```bash
# .env 파일에 Public IP 설정 후
docker-compose -f docker-compose.local.yml up
```

### EC2 배포
```bash
# .env 파일에 Private IP 설정 후
docker-compose up -d
```
