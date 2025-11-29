# MONGLE SERVER - REST API 명세서

## 응답 구조

모든 API 응답은 `ApiResponseAdvice`에 의해 자동으로 `ApiResponse`로 래핑됩니다.

### 성공 응답 구조

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공하였습니다.",
  "data": {
    // 실제 응답 데이터
  }
}
```

### 에러 응답 구조

```json
{
  "code": "ERROR_CODE",
  "message": "에러 메시지",
  "data": null
}
```

---

## 목차

1. [인증/회원가입](#1-인증회원가입-auth-signup-sociallogin)
2. [사용자](#2-사용자-user)
3. [게시글](#3-게시글-post-adminpost)
4. [댓글](#4-댓글-comment)
5. [반응](#5-반응-reaction)
6. [신고](#6-신고-report-adminreport)
7. [차단](#7-차단-block)
8. [지도](#8-지도-map)
9. [부스](#9-부스-booth)
10. [파일](#10-파일-filepresignedurl)
11. [Enum 타입 정의](#부록-enum-타입-정의)

---

## 1. 인증/회원가입 (Auth, SignUp, SocialLogin)

### 1.1 POST /api/v1/auth/login

**설명**: 로그인 <br/>
**인증**: 불필요 <br/>
**권한**: 없음

**Request Body**: `LoginRequest`

| 필드       | 타입     | 필수 | 설명   | Validation               |
|----------|--------|----|------|--------------------------|
| email    | string | O  | 이메일  | @ValidEmail              |
| password | string | O  | 비밀번호 | @NotBlank, @Size(max=72) |

**Response**: `TokenInfo`

| 필드                           | 타입     | 설명                  |
|------------------------------|--------|---------------------|
| tokenType                    | string | 토큰 타입 (Bearer)      |
| accessToken                  | string | 액세스 토큰              |
| refreshToken                 | string | 리프레시 토큰             |
| accessTokenExpirationMillis  | number | 액세스 토큰 만료 시간 (밀리초)  |
| refreshTokenExpirationMillis | number | 리프레시 토큰 만료 시간 (밀리초) |

**Example Request**

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Example Response**

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공하였습니다.",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "accessTokenExpirationMillis": 3600000,
    "refreshTokenExpirationMillis": 604800000
  }
}
```

---

### 1.2 POST /api/v1/auth/reissue

**설명**: 토큰 재발급 <br/>
**인증**: 불필요 <br/>
**권한**: 없음

**Request Body**: `ReissueTokenRequest`

| 필드           | 타입     | 필수 | 설명      | Validation |
|--------------|--------|----|---------|------------|
| refreshToken | string | O  | 리프레시 토큰 | @NotBlank  |

**Response**: `TokenInfo`

**Example Request**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### 1.3 POST /api/v1/auth/sign-up

**설명**: 회원가입 <br/>
**인증**: 불필요 <br/>
**권한**: 없음

**Request Body**: `SignUpRequest`

| 필드                | 타입     | 필수 | 설명        | Validation                      |
|-------------------|--------|----|-----------|---------------------------------|
| email             | string | O  | 이메일       | @ValidEmail                     |
| password          | string | O  | 비밀번호      | @NotBlank, @Size(min=8, max=72) |
| nickname          | string | O  | 닉네임       | @NotBlank                       |
| profileImageKey   | string | X  | 프로필 이미지 키 | -                               |
| verificationToken | string | O  | 이메일 인증 토큰 | @NotBlank                       |

**Response**: `SignUpResponse`

| 필드       | 타입     | 설명    |
|----------|--------|-------|
| memberId | string | 회원 ID |
| email    | string | 이메일   |
| nickname | string | 닉네임   |

**Example Request**

```json
{
  "email": "newuser@example.com",
  "password": "securepass123",
  "nickname": "몽글이",
  "profileImageKey": "profile/abc123.jpg",
  "verificationToken": "verification-token-abc"
}
```

**Example Response**

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공하였습니다.",
  "data": {
    "memberId": "member-uuid-123",
    "email": "newuser@example.com",
    "nickname": "몽글이"
  }
}
```

---

### 1.4 POST /api/v1/auth/verification-code

**설명**: 이메일 인증 코드 발송 <br/>
**인증**: 불필요 <br/>
**권한**: 없음

**Request Body**: `SendVerificationCodeRequest`

| 필드    | 타입     | 필수 | 설명  | Validation        |
|-------|--------|----|-----|-------------------|
| email | string | O  | 이메일 | @NotBlank, @Email |

**Response**: void

**Example Request**

```json
{
  "email": "user@example.com"
}
```

---

### 1.5 POST /api/v1/auth/verify-code

**설명**: 이메일 인증 코드 확인 <br/>
**인증**: 불필요 <br/>
**권한**: 없음

**Request Body**: `VerifyEmailRequest`

| 필드               | 타입     | 필수 | 설명    | Validation  |
|------------------|--------|----|-------|-------------|
| email            | string | O  | 이메일   | @ValidEmail |
| verificationCode | string | O  | 인증 코드 | @NotBlank   |

**Response**: `VerifyEmailResponse`

| 필드                | 타입     | 설명                |
|-------------------|--------|-------------------|
| verificationToken | string | 인증 토큰 (회원가입 시 사용) |

**Example Request**

```json
{
  "email": "user@example.com",
  "verificationCode": "123456"
}
```

---

### 1.6 GET /api/v1/auth/verify-nickname

**설명**: 닉네임 중복 확인 <br/>
**인증**: 불필요 <br/>
**권한**: 없음

**Query Parameters**

| 파라미터     | 타입     | 필수 | 설명      |
|----------|--------|----|---------|
| nickname | string | O  | 확인할 닉네임 |

**Response**: `VerifyNicknameResponse`

| 필드          | 타입      | 설명       |
|-------------|---------|----------|
| isAvailable | boolean | 사용 가능 여부 |

**Example Request**

```
GET /api/v1/auth/verify-nickname?nickname=몽글이
```

**Example Response**

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공하였습니다.",
  "data": {
    "isAvailable": true
  }
}
```

---

### 1.7 GET /api/v1/auth/social/{registrationId}/authorization-url

**설명**: 소셜 로그인 인증 URL 조회 <br/>
**인증**: 불필요 <br/>
**권한**: 없음

**Path Parameters**

| 파라미터           | 타입     | 필수 | 설명                    |
|----------------|--------|----|-----------------------|
| registrationId | string | O  | 소셜 로그인 제공자 (예: kakao) |

**Response**: `AuthorizationUrlResponse`

| 필드               | 타입     | 설명     |
|------------------|--------|--------|
| authorizationUrl | string | 인증 URL |

**Example Request**

```
GET /api/v1/auth/social/kakao/authorization-url
```

**Example Response**

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공하였습니다.",
  "data": {
    "authorizationUrl": "https://kauth.kakao.com/oauth/authorize?client_id=..."
  }
}
```

---

### 1.8 POST /api/v1/auth/social/{registrationId}/login

**설명**: 소셜 로그인 <br/>
**인증**: 불필요 <br/>
**권한**: 없음

**Path Parameters**

| 파라미터           | 타입     | 필수 | 설명                    |
|----------------|--------|----|-----------------------|
| registrationId | string | O  | 소셜 로그인 제공자 (예: kakao) |

**Query Parameters**

| 파라미터 | 타입     | 필수 | 설명    |
|------|--------|----|-------|
| code | string | O  | 인증 코드 |

**Response**: `TokenInfo`

**Example Request**

```
POST /api/v1/auth/social/kakao/login?code=authorization-code-123
```

---

## 2. 사용자 (User)

### 2.1 GET /api/v1/user/me

**설명**: 내 정보 조회 <br/>
**인증**: 필요 <br/>
**권한**: USER

**Response**: `UserDetailResponse`

| 필드              | 타입     | 설명          |
|-----------------|--------|-------------|
| nickname        | string | 닉네임         |
| email           | string | 이메일         |
| profileImageUrl | string | 프로필 이미지 URL |

**Example Response**

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공하였습니다.",
  "data": {
    "nickname": "몽글이",
    "email": "user@example.com",
    "profileImageUrl": "https://cdn.example.com/profile/abc123.jpg"
  }
}
```

---

### 2.2 PUT /api/v1/user/me/profile-image

**설명**: 프로필 이미지 수정 <br/>
**인증**: 필요 <br/>
**권한**: USER

**Request Body**: `UpdateProfileImageRequest`

| 필드      | 타입     | 필수 | 설명                   |
|---------|--------|----|----------------------|
| fileKey | string | X  | 파일 키 (null이면 이미지 삭제) |

**Response**: `UpdateProfileImageResponse`

| 필드              | 타입     | 설명                       |
|-----------------|--------|--------------------------|
| profileImageUrl | string | 프로필 이미지 URL (null이면 삭제됨) |

**Example Request**

```json
{
  "fileKey": "profile/new-image-123.jpg"
}
```

---

### 2.3 PUT /api/v1/user/me/nickname

**설명**: 닉네임 수정 <br/>
**인증**: 필요 <br/>
**권한**: USER

**Request Body**: `UpdateNicknameRequest`

| 필드       | 타입     | 필수 | 설명    | Validation                      |
|----------|--------|----|-------|---------------------------------|
| nickname | string | O  | 새 닉네임 | @NotBlank, @Size(min=1, max=20) |

**Response**: `UpdateNicknameResponse`

| 필드       | 타입     | 설명      |
|----------|--------|---------|
| nickname | string | 변경된 닉네임 |

**Example Request**

```json
{
  "nickname": "새로운몽글이"
}
```

---

### 2.4 DELETE /api/v1/user/me

**설명**: 회원 탈퇴 <br/>
**인증**: 필요 <br/>
**권한**: USER

**Response**: void

---

### 2.5 POST /api/v1/user/me/social-link/{registrationId}

**설명**: 소셜 계정 연동 <br/>
**인증**: 필요 <br/>
**권한**: USER

**Path Parameters**

| 파라미터           | 타입     | 필수 | 설명         |
|----------------|--------|----|------------|
| registrationId | string | O  | 소셜 로그인 제공자 |

**Query Parameters**

| 파라미터 | 타입     | 필수 | 설명    |
|------|--------|----|-------|
| code | string | O  | 인증 코드 |

**Response**: void

---

## 3. 게시글 (Post, AdminPost)

### 3.1 POST /api/v1/posts

**설명**: 게시글 작성 <br/>
**인증**: 필요 <br/>
**권한**: USER

**Request Body**: `PostCreateRequest`

| 필드                      | 타입       | 필수 | 설명        | Validation                      |
|-------------------------|----------|----|-----------|---------------------------------|
| latitude                | number   | O  | 위도        | @NotNull, @Min(-90), @Max(90)   |
| longitude               | number   | O  | 경도        | @NotNull, @Min(-180), @Max(180) |
| content                 | string   | O  | 게시글 내용    | @NotBlank, @Size(max=2000)      |
| fileKeyList             | string[] | X  | 파일 키 목록   | -                               |
| isRandomLocationEnabled | boolean  | O  | 위치 랜덤화 여부 | -                               |
| isAnonymous             | boolean  | X  | 익명 여부     | -                               |

**Response**: `PostCreateResponse`

| 필드             | 타입      | 설명               |
|----------------|---------|------------------|
| id             | string  | 게시글 ID           |
| content        | string  | 게시글 내용           |
| authorId       | string  | 작성자 ID           |
| s2TokenId      | string  | S2 토큰 ID         |
| staticCloudId  | number  | 정적 클라우드 ID       |
| dynamicCloudId | number  | 동적 클라우드 ID       |
| createdAt      | string  | 생성 일시 (ISO-8601) |
| isAnonymous    | boolean | 익명 여부            |

**Example Request**

```json
{
  "latitude": 37.5665,
  "longitude": 126.9780,
  "content": "홍대 축제 정말 재미있어요!",
  "fileKeyList": [
    "post/image1.jpg",
    "post/image2.jpg"
  ],
  "isRandomLocationEnabled": false,
  "isAnonymous": false
}
```

---

### 3.2 GET /api/v1/posts

**설명**: 게시글 목록 조회 (커서 기반 페이징) <br/>
**인증**: 선택 (인증 시 좋아요 등 개인화 정보 포함) <br/>
**권한**: 없음

**Query Parameters**: `PostListRequest`

| 파라미터    | 타입      | 필수 | 설명                | 기본값           |
|---------|---------|----|-------------------|---------------|
| placeId | string  | X  | 장소 ID (정적 클라우드)   | -             |
| cloudId | string  | X  | 클라우드 ID (동적 클라우드) | -             |
| cursor  | string  | X  | 페이징 커서            | -             |
| size    | integer | X  | 페이지 크기            | 10 (최대 50)    |
| sortBy  | enum    | X  | 정렬 기준             | ranking_score |

**Response**: `PostListResponse`

| 필드                             | 타입       | 설명                       |
|--------------------------------|----------|--------------------------|
| posts                          | array    | 게시글 목록                   |
| posts[].postId                 | string   | 게시글 ID                   |
| posts[].author                 | object   | 작성자 정보                   |
| posts[].author.id              | string   | 작성자 ID                   |
| posts[].author.nickname        | string   | 작성자 닉네임                  |
| posts[].author.profileImageUrl | string   | 작성자 프로필 이미지 URL          |
| posts[].content                | string   | 게시글 내용                   |
| posts[].photoUrls              | string[] | 사진 URL 목록                |
| posts[].upvotes                | number   | 좋아요 수                    |
| posts[].downvotes              | number   | 싫어요 수                    |
| posts[].myReaction             | string   | 내 반응 (LIKE/DISLIKE/null) |
| posts[].commentCount           | number   | 댓글 수                     |
| posts[].viewCount              | number   | 조회 수                     |
| posts[].createdAt              | string   | 생성 일시 (ISO-8601)         |
| posts[].updatedAt              | string   | 수정 일시 (ISO-8601)         |
| nextCursor                     | string   | 다음 커서                    |
| hasNext                        | boolean  | 다음 페이지 존재 여부             |

**Example Request**

```
GET /api/v1/posts?placeId=place-123&size=20&sortBy=ranking_score
```

---

### 3.3 GET /api/v1/posts/{postId}

**설명**: 게시글 상세 조회 <br/>
**인증**: 선택 (인증 시 좋아요 등 개인화 정보 포함) <br/>
**권한**: 없음

**Path Parameters**

| 파라미터   | 타입     | 필수 | 설명     |
|--------|--------|----|--------|
| postId | string | O  | 게시글 ID |

**Response**: `PostDetailResponse`

| 필드                     | 타입       | 설명                       |
|------------------------|----------|--------------------------|
| postId                 | string   | 게시글 ID                   |
| author                 | object   | 작성자 정보                   |
| author.id              | string   | 작성자 ID                   |
| author.nickname        | string   | 작성자 닉네임                  |
| author.profileImageUrl | string   | 작성자 프로필 이미지 URL          |
| content                | string   | 게시글 내용                   |
| latitude               | number   | 위도                       |
| longitude              | number   | 경도                       |
| photoUrls              | string[] | 사진 URL 목록                |
| videoUrls              | string[] | 비디오 URL 목록               |
| createdAt              | string   | 생성 일시 (ISO-8601)         |
| updatedAt              | string   | 수정 일시 (ISO-8601)         |
| viewCount              | number   | 조회 수                     |
| likeCount              | number   | 좋아요 수                    |
| dislikeCount           | number   | 싫어요 수                    |
| myReaction             | string   | 내 반응 (LIKE/DISLIKE/null) |
| commentCount           | number   | 댓글 수                     |

---

### 3.4 GET /api/v1/posts/{postId}/stats

**설명**: 게시글 통계 조회 (조회수 증가 없음) <br/>
**인증**: 불필요 <br/>
**권한**: 없음

**Path Parameters**

| 파라미터   | 타입     | 필수 | 설명     |
|--------|--------|----|--------|
| postId | string | O  | 게시글 ID |

**Response**: `PostStatsResponse`

| 필드           | 타입     | 설명    |
|--------------|--------|-------|
| viewCount    | number | 조회 수  |
| commentCount | number | 댓글 수  |
| likeCount    | number | 좋아요 수 |
| dislikeCount | number | 싫어요 수 |

**Example Request**

```
GET /api/v1/posts/post-uuid-123/stats
```

**Example Response**

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공하였습니다.",
  "data": {
    "viewCount": 123,
    "commentCount": 5,
    "likeCount": 10,
    "dislikeCount": 2
  }
}
```

**사용 사례**:
- Flutter 앱에서 게시글 상세 화면을 나갈 때, 조회수를 증가시키지 않고 최신 통계만 가져와서 게시판 목록을 업데이트할 때 사용

---

### 3.5 PUT /api/v1/posts/{postId}

**설명**: 게시글 수정 <br/>
**인증**: 필요 <br/>
**권한**: USER (작성자만 가능)

**Path Parameters**

| 파라미터   | 타입     | 필수 | 설명     |
|--------|--------|----|--------|
| postId | string | O  | 게시글 ID |

**Request Body**: `PostUpdateRequest`

| 필드          | 타입       | 필수 | 설명      | Validation                 |
|-------------|----------|----|---------|----------------------------|
| content     | string   | O  | 게시글 내용  | @NotBlank, @Size(max=2000) |
| fileKeyList | string[] | X  | 파일 키 목록 | -                          |
| isAnonymous | boolean  | X  | 익명 여부   | -                          |

**Response**: `PostUpdateResponse`

| 필드          | 타입      | 설명     |
|-------------|---------|--------|
| id          | string  | 게시글 ID |
| content     | string  | 게시글 내용 |
| isAnonymous | boolean | 익명 여부  |

---

### 3.6 DELETE /api/v1/posts/{postId}

**설명**: 게시글 삭제 <br/>
**인증**: 필요 <br/>
**권한**: USER (작성자만 가능)

**Path Parameters**

| 파라미터   | 타입     | 필수 | 설명     |
|--------|--------|----|--------|
| postId | string | O  | 게시글 ID |

**Response**: void

---

### 3.7 POST /api/v1/admin/posts

**설명**: 관리자 게시글 작성 <br/>
**인증**: 필요 <br/>
**권한**: ADMIN

**Request Body**: `AdminPostCreateRequest`

| 필드                      | 타입       | 필수 | 설명        | Validation                 |
|-------------------------|----------|----|-----------|----------------------------|
| latitude                | number   | O  | 위도        | @NotNull                   |
| longitude               | number   | O  | 경도        | @NotNull                   |
| content                 | string   | O  | 게시글 내용    | @NotBlank, @Size(max=2000) |
| infoText                | string   | X  | 정보 텍스트    | @Size(max=2000)            |
| fileKeyList             | string[] | X  | 파일 키 목록   | -                          |
| isRandomLocationEnabled | boolean  | O  | 위치 랜덤화 여부 | -                          |
| isAnonymous             | boolean  | X  | 익명 여부     | -                          |
| customNickname          | string   | X  | 커스텀 닉네임   | @Size(max=255)             |

**Response**: `PostCreateResponse`

---

### 3.8 PUT /api/v1/admin/posts/{postId}

**설명**: 관리자 게시글 수정 <br/>
**인증**: 필요 <br/>
**권한**: ADMIN

**Path Parameters**

| 파라미터   | 타입     | 필수 | 설명     |
|--------|--------|----|--------|
| postId | string | O  | 게시글 ID |

**Request Body**: `AdminPostUpdateRequest`

| 필드             | 타입       | 필수 | 설명      | Validation      |
|----------------|----------|----|---------|-----------------|
| content        | string   | X  | 게시글 내용  | @Size(max=2000) |
| infoText       | string   | X  | 정보 텍스트  | @Size(max=2000) |
| fileKeyList    | string[] | X  | 파일 키 목록 | -               |
| isAnonymous    | boolean  | X  | 익명 여부   | -               |
| customNickname | string   | X  | 커스텀 닉네임 | @Size(max=255)  |

**Response**: `PostUpdateResponse`

---

### 3.9 DELETE /api/v1/admin/posts/{postId}

**설명**: 관리자 게시글 삭제 <br/>
**인증**: 필요 <br/>
**권한**: ADMIN

**Response**: void

---

### 3.10 PATCH /api/v1/admin/posts/{postId}/info-text

**설명**: 게시글 정보 텍스트 수정 (관리자 전용) <br/>
**인증**: 필요 <br/>
**권한**: ADMIN

**Path Parameters**

| 파라미터   | 타입     | 필수 | 설명     |
|--------|--------|----|--------|
| postId | string | O  | 게시글 ID |

**Request Body**: `UpdateInfoTextRequest`

| 필드       | 타입     | 필수 | 설명     | Validation      |
|----------|--------|----|--------|-----------------|
| infoText | string | X  | 정보 텍스트 | @Size(max=2000) |

**Response**: void

---

## 4. 댓글 (Comment)

### 4.1 GET /api/v1/posts/{postId}/comments

**설명**: 게시글의 댓글 목록 조회 (커서 기반 페이징) <br/>
**인증**: 선택 (인증 시 좋아요 등 개인화 정보 포함) <br/>
**권한**: 없음

**Path Parameters**

| 파라미터   | 타입     | 필수 | 설명     |
|--------|--------|----|--------|
| postId | string | O  | 게시글 ID |

**Query Parameters**: `CommentQueryRequest`

| 파라미터   | 타입      | 필수 | 설명     | 기본값              |
|--------|---------|----|--------|------------------|
| cursor | string  | X  | 페이징 커서 | -                |
| size   | integer | X  | 페이지 크기 | 10 (최소 1, 최대 50) |
| sort   | enum    | X  | 정렬 기준  | LIKES            |

**Response**: `CursorInfoResponse<CommentInfoResponse>`

| 필드                                | 타입      | 설명                       |
|-----------------------------------|---------|--------------------------|
| comments                          | array   | 댓글 목록                    |
| comments[].commentId              | string  | 댓글 ID                    |
| comments[].content                | string  | 댓글 내용                    |
| comments[].author                 | object  | 작성자 정보                   |
| comments[].author.id              | string  | 작성자 ID                   |
| comments[].author.nickname        | string  | 작성자 닉네임                  |
| comments[].author.profileImageUrl | string  | 작성자 프로필 이미지 URL          |
| comments[].likeCount              | number  | 좋아요 수                    |
| comments[].dislikeCount           | number  | 싫어요 수                    |
| comments[].myReaction             | string  | 내 반응 (LIKE/DISLIKE/null) |
| comments[].createdAt              | string  | 생성 일시 (ISO-8601)         |
| comments[].isAuthor               | boolean | 내가 작성한 댓글인지              |
| comments[].isDeleted              | boolean | 삭제된 댓글인지                 |
| comments[].hasReplies             | boolean | 대댓글이 있는지                 |
| nextCursor                        | string  | 다음 커서                    |
| hasNext                           | boolean | 다음 페이지 존재 여부             |

**Example Request**

```
GET /api/v1/posts/post-uuid-123/comments?size=10&sort=LIKES
```

---

### 4.2 GET /api/v1/comments/{parentCommentId}/replies

**설명**: 대댓글 목록 조회 (커서 기반 페이징) <br/>
**인증**: 선택 <br/>
**권한**: 없음

**Path Parameters**

| 파라미터            | 타입     | 필수 | 설명       |
|-----------------|--------|----|----------|
| parentCommentId | string | O  | 부모 댓글 ID |

**Query Parameters**: `CommentQueryRequest` (4.1과 동일)

**Response**: `CursorInfoResponse<CommentInfoResponse>` (4.1과 동일)

---

### 4.3 POST /api/v1/posts/{postId}/comments

**설명**: 게시글에 댓글 작성 <br/>
**인증**: 필요 <br/>
**권한**: USER

**Path Parameters**

| 파라미터   | 타입     | 필수 | 설명     |
|--------|--------|----|--------|
| postId | string | O  | 게시글 ID |

**Request Body**: `CommentCreateRequest`

| 필드          | 타입      | 필수 | 설명    | Validation                 |
|-------------|---------|----|-------|----------------------------|
| content     | string  | O  | 댓글 내용 | @NotBlank, @Size(max=1000) |
| isAnonymous | boolean | X  | 익명 여부 | -                          |

**Response**: void

**Example Request**

```json
{
  "content": "멋진 게시글이네요!",
  "isAnonymous": false
}
```

---

### 4.4 POST /api/v1/comments/{parentCommentId}/replies

**설명**: 댓글에 대댓글 작성 <br/>
**인증**: 필요 <br/>
**권한**: USER

**Path Parameters**

| 파라미터            | 타입     | 필수 | 설명       |
|-----------------|--------|----|----------|
| parentCommentId | string | O  | 부모 댓글 ID |

**Request Body**: `CommentCreateRequest` (4.3과 동일)

**Response**: void

---

### 4.5 DELETE /api/v1/comments/{commentId}

**설명**: 댓글 삭제 <br/>
**인증**: 필요 <br/>
**권한**: USER (작성자만 가능)

**Path Parameters**

| 파라미터      | 타입     | 필수 | 설명    |
|-----------|--------|----|-------|
| commentId | string | O  | 댓글 ID |

**Response**: void

---

## 5. 반응 (Reaction)

### 5.1 POST /api/v1/{targetType}/{targetId}/reaction

**설명**: 좋아요/싫어요 추가/변경/취소 <br/>
**인증**: 필요 <br/>
**권한**: USER

**Path Parameters**

| 파라미터       | 타입     | 필수 | 설명                        |
|------------|--------|----|---------------------------|
| targetType | string | O  | 대상 타입 (posts \| comments) |
| targetId   | string | O  | 대상 ID                     |

**Request Body**: `ReactionRequest`

| 필드           | 타입   | 필수 | 설명                      |
|--------------|------|----|-------------------------|
| reactionType | enum | O  | 반응 타입 (LIKE \| DISLIKE) |

**Response**: `ReactionResponse`

| 필드           | 타입     | 설명    |
|--------------|--------|-------|
| likeCount    | number | 좋아요 수 |
| dislikeCount | number | 싫어요 수 |

**Example Request**

```
POST /api/v1/posts/post-uuid-123/reaction
```

```json
{
  "reactionType": "LIKE"
}
```

**Example Response**

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공하였습니다.",
  "data": {
    "likeCount": 16,
    "dislikeCount": 2
  }
}
```

---

## 6. 신고 (Report, AdminReport)

### 6.1 POST /api/v1/reports

**설명**: 게시글/댓글 신고 <br/>
**인증**: 선택 (비인증도 가능) <br/>
**권한**: 없음

**Request Body**: `ReportCreateRequest`

| 필드         | 타입     | 필수 | 설명       | Validation |
|------------|--------|----|----------|------------|
| targetId   | string | O  | 신고 대상 ID | @NotBlank  |
| targetType | enum   | O  | 신고 대상 타입 | @NotNull   |
| reason     | enum   | O  | 신고 사유    | @NotNull   |

**targetType**:

- `POST`: 게시글
- `COMMENT`: 댓글

**reason**:

- `SPAM`: 스팸/홍보성 콘텐츠
- `ABUSE`: 욕설/비방 등 불쾌한 표현
- `PORNOGRAPHY`: 음란물/성희롱
- `ILLEGAL`: 불법 정보
- `INAPPROPRIATE`: 기타 부적절한 콘텐츠

**Response**: void

**Example Request**

```json
{
  "targetId": "post-uuid-123",
  "targetType": "POST",
  "reason": "SPAM"
}
```

---

### 6.2 GET /api/v1/admin/reports

**설명**: 신고 목록 조회 (페이지 기반) <br/>
**인증**: 필요 <br/>
**권한**: ADMIN

**Query Parameters** (Spring Pageable)

| 파라미터 | 타입      | 필수 | 설명                        | 기본값 |
|------|---------|----|---------------------------|-----|
| page | integer | X  | 페이지 번호 (0부터 시작)           | 0   |
| size | integer | X  | 페이지 크기                    | 20  |
| sort | string  | X  | 정렬 조건 (예: createdAt,desc) | -   |

**Response**: Spring Page<ReportAdminResponse>

| 필드                              | 타입      | 설명               |
|---------------------------------|---------|------------------|
| content                         | array   | 신고 목록            |
| content[].reportId              | string  | 신고 ID            |
| content[].reporter              | object  | 신고자 정보           |
| content[].reporter.memberId     | string  | 신고자 ID           |
| content[].reporter.nickname     | string  | 신고자 닉네임          |
| content[].targetId              | string  | 신고 대상 ID         |
| content[].targetType            | enum    | 신고 대상 타입         |
| content[].targetAuthor          | object  | 신고 대상 작성자 정보     |
| content[].targetAuthor.memberId | string  | 작성자 ID           |
| content[].reason                | enum    | 신고 사유            |
| content[].reportStatus          | enum    | 신고 상태            |
| content[].createdAt             | string  | 신고 일시 (ISO-8601) |
| pageable                        | object  | 페이징 정보           |
| totalPages                      | number  | 전체 페이지 수         |
| totalElements                   | number  | 전체 요소 수          |
| size                            | number  | 페이지 크기           |
| number                          | number  | 현재 페이지 번호        |
| first                           | boolean | 첫 페이지 여부         |
| last                            | boolean | 마지막 페이지 여부       |
| empty                           | boolean | 비어있는지 여부         |

**Example Request**

```
GET /api/v1/admin/reports?page=0&size=20
```

---

### 6.3 PATCH /api/v1/admin/reports/{reportId}/status

**설명**: 신고 상태 변경 <br/>
**인증**: 필요 <br/>
**권한**: ADMIN

**Path Parameters**

| 파라미터     | 타입     | 필수 | 설명    |
|----------|--------|----|-------|
| reportId | string | O  | 신고 ID |

**Query Parameters**

| 파라미터   | 타입   | 필수 | 설명     |
|--------|------|----|--------|
| status | enum | O  | 변경할 상태 |

**status**:

- `RECEIVED`: 접수됨
- `PROCESSED`: 처리 완료
- `REJECTED`: 반려됨

**Response**: void

**Example Request**

```
PATCH /api/v1/admin/reports/report-uuid-1/status?status=PROCESSED
```

---

## 7. 차단 (Block)

### 7.1 GET /api/v1/blocks/me

**설명**: 내가 차단한 사용자 목록 조회 <br/>
**인증**: 필요 <br/>
**권한**: USER

**Response**: List<string>

```
차단한 사용자 ID 목록
```

**Example Response**

```json
{
  "code": "SUCCESS",
  "message": "요청에 성공하였습니다.",
  "data": [
    "member-uuid-1",
    "member-uuid-2"
  ]
}
```

---

### 7.2 POST /api/v1/blocks/{blockedUserId}

**설명**: 사용자 차단 <br/>
**인증**: 필요 <br/>
**권한**: USER

**Path Parameters**

| 파라미터          | 타입     | 필수 | 설명         |
|---------------|--------|----|------------|
| blockedUserId | string | O  | 차단할 사용자 ID |

**Response**: void

---

### 7.3 DELETE /api/v1/blocks/{blockedUserId}

**설명**: 사용자 차단 해제 <br/>
**인증**: 필요 <br/>
**권한**: USER

**Path Parameters**

| 파라미터          | 타입     | 필수 | 설명            |
|---------------|--------|----|---------------|
| blockedUserId | string | O  | 차단 해제할 사용자 ID |

**Response**: void

---

## 8. 지도 (Map)

### 8.1 GET /api/v1/map/objects

**설명**: 지도 영역 내 오브젝트 조회 (그레인, 정적/동적 클라우드) <br/>
**인증**: 선택 (인증 시 조회 여부 등 개인화 정보 포함) <br/>
**권한**: 없음

**Query Parameters**: `MapObjectsRequest`

| 파라미터  | 타입     | 필수 | 설명                            |
|-------|--------|----|-------------------------------|
| swLat | number | O  | 남서쪽 위도 (South-West Latitude)  |
| swLng | number | O  | 남서쪽 경도 (South-West Longitude) |
| neLat | number | O  | 북동쪽 위도 (North-East Latitude)  |
| neLng | number | O  | 북동쪽 경도 (North-East Longitude) |

**Response**: `MapObjectsResponse`

| 필드                                 | 타입      | 설명              |
|------------------------------------|---------|-----------------|
| grains                             | array   | 그레인 (개별 게시글) 목록 |
| grains[].postId                    | string  | 게시글 ID          |
| grains[].latitude                  | number  | 위도              |
| grains[].longitude                 | number  | 경도              |
| grains[].author                    | object  | 작성자 정보          |
| grains[].author.id                 | string  | 작성자 ID          |
| grains[].author.nickname           | string  | 작성자 닉네임         |
| grains[].author.profileImageUrl    | string  | 작성자 프로필 이미지 URL |
| grains[].isViewed                  | boolean | 조회 여부           |
| grains[].isRecent                  | boolean | 최근 게시물 여부       |
| grains[].infoText                  | string  | 정보 텍스트          |
| staticClouds                       | array   | 정적 클라우드 목록      |
| staticClouds[].placeId             | string  | 장소 ID           |
| staticClouds[].name                | string  | 장소명             |
| staticClouds[].centerLatitude      | number  | 중심 위도           |
| staticClouds[].centerLongitude     | number  | 중심 경도           |
| staticClouds[].postCount           | number  | 게시글 수           |
| staticClouds[].polygon             | array   | 폴리곤 좌표          |
| staticClouds[].polygon[].latitude  | number  | 위도              |
| staticClouds[].polygon[].longitude | number  | 경도              |
| dynamicClouds                      | array   | 동적 클라우드 목록      |
| dynamicClouds[].cloudId            | string  | 클라우드 ID         |
| dynamicClouds[].postCount          | number  | 게시글 수           |
| dynamicClouds[].polygon            | array   | 폴리곤 좌표          |

**Example Request**

```
GET /api/v1/map/objects?swLat=37.5&swLng=126.9&neLat=37.6&neLng=127.0
```

---

## 9. 부스 (Booth)

### 9.1 POST /api/v1/booths

**설명**: 부스 계정 등록 (관리자 전용) <br/>
**인증**: 필요 <br/>
**권한**: ADMIN

**Request Body**: `BoothRegistrationRequest`

| 필드        | 타입     | 필수 | 설명    | Validation |
|-----------|--------|----|-------|------------|
| boothName | string | O  | 부스 이름 | @NotBlank  |
| password  | string | O  | 비밀번호  | @NotBlank  |

**Response**: `BoothRegistrationResponse`

| 필드           | 타입     | 설명      |
|--------------|--------|---------|
| email        | string | 이메일     |
| id           | string | 회원 ID   |
| nickname     | string | 닉네임     |
| profileImage | string | 프로필 이미지 |

**Example Request**

```json
{
  "boothName": "축제부스1",
  "password": "booth-password-123"
}
```

---

## 10. 파일 (File/PresignedUrl)

### 10.1 POST /api/v1/files/upload-urls

**설명**: 파일 업로드를 위한 Presigned URL 발급 <br/>
**인증**: 선택 <br/>
**권한**: 없음

**Request Body**: `UploadUrlRequest`

| 필드               | 타입     | 필수 | 설명            | Validation        |
|------------------|--------|----|---------------|-------------------|
| fileType         | enum   | O  | 파일 타입         | @NotNull          |
| files            | array  | O  | 파일 목록         | -                 |
| files[].fileName | string | O  | 파일명           | @NotBlank         |
| files[].fileSize | number | O  | 파일 크기 (bytes) | @NotNull, @Min(1) |

**fileType**:

- `POST_FILE`: 게시글 파일
- `PROFILE_IMAGE`: 프로필 이미지

**Response**: `UploadUrlResponse`

| 필드                     | 타입     | 설명               |
|------------------------|--------|------------------|
| issuedUrls             | array  | 발급된 URL 목록       |
| issuedUrls[].fileKey   | string | 파일 키             |
| issuedUrls[].url       | string | Presigned URL    |
| issuedUrls[].expiresAt | string | 만료 일시 (ISO-8601) |

**Example Request**

```json
{
  "fileType": "POST_FILE",
  "files": [
    {
      "fileName": "photo1.jpg",
      "fileSize": 1024000
    },
    {
      "fileName": "photo2.jpg",
      "fileSize": 2048000
    }
  ]
}
```

---

### 10.2 POST /api/v1/files/view-urls

**설명**: 파일 조회를 위한 Presigned URL 발급 (테스트용 API) <br/>
**인증**: 선택 <br/>
**권한**: 없음

**참고**: 실제로는 게시물 조회 응답에 파일 URL이 포함되어야 함

**Request Body**: `ViewUrlRequest`

| 필드          | 타입       | 필수 | 설명      | Validation |
|-------------|----------|----|---------|------------|
| fileKeyList | string[] | O  | 파일 키 목록 | @NotNull   |

**Response**: `ViewUrlResponse`

| 필드                     | 타입     | 설명               |
|------------------------|--------|------------------|
| issuedUrls             | array  | 발급된 URL 목록       |
| issuedUrls[].fileKey   | string | 파일 키             |
| issuedUrls[].url       | string | Presigned URL    |
| issuedUrls[].expiresAt | string | 만료 일시 (ISO-8601) |

---

## 부록: Enum 타입 정의

### ReactionType

| 값       | 설명  |
|---------|-----|
| LIKE    | 좋아요 |
| DISLIKE | 싫어요 |

### PostSort

| 값             | 설명          |
|---------------|-------------|
| ranking_score | 랭킹 점수 (기본값) |
| createdAt     | 생성 일시       |

### CommentSort

| 값      | 설명         |
|--------|------------|
| LATEST | 최신순        |
| LIKES  | 좋아요순 (기본값) |

### ReportReason

| 값             | 설명             |
|---------------|----------------|
| SPAM          | 스팸/홍보성 콘텐츠     |
| ABUSE         | 욕설/비방 등 불쾌한 표현 |
| PORNOGRAPHY   | 음란물/성희롱        |
| ILLEGAL       | 불법 정보          |
| INAPPROPRIATE | 기타 부적절한 콘텐츠    |

### ReportedTargetType

| 값       | 설명  |
|---------|-----|
| POST    | 게시글 |
| COMMENT | 댓글  |

### ReportStatus

| 값         | 설명    |
|-----------|-------|
| RECEIVED  | 접수됨   |
| PROCESSED | 처리 완료 |
| REJECTED  | 반려됨   |

### FileType

| 값             | 설명      |
|---------------|---------|
| POST_FILE     | 게시글 파일  |
| PROFILE_IMAGE | 프로필 이미지 |

---

## API 요약

### 도메인별 엔드포인트 수

- 인증/회원가입: 8개
- 사용자: 5개
- 게시글: 10개 (일반 6개, 관리자 4개)
- 댓글: 5개
- 반응: 1개
- 신고: 3개 (일반 1개, 관리자 2개)
- 차단: 3개
- 지도: 1개
- 부스: 1개
- 파일: 2개

**총 39개 REST API 엔드포인트**

### 주요 특징

1. **자동 응답 래핑**: 모든 API는 `ApiResponseAdvice`에 의해 자동으로 `ApiResponse`로 래핑됨
2. **인증 방식**: JWT 기반 (Bearer Token)
3. **페이징**:
    - 게시글/댓글: 커서 기반 페이징
    - 관리자 신고 목록: 페이지 기반 페이징 (Spring Pageable)
4. **익명 기능**: 게시글과 댓글 작성 시 익명 설정 가능
5. **파일 업로드**: Presigned URL 방식 (S3 직접 업로드)
6. **위치 기반**: 게시글은 위치 정보를 포함하며, 지도 영역별 조회 지원

---

**작성일**: 2025-11-15
**프로젝트**: MONGLE_SERVER
**버전**: 1.0.0
