# Git Convention

## Commit Message

### 구조

```text
{gitmoji} {제목} ({issue number})

- 필요한 경우 상세 설명
```

### 예시

```text
🔨 백엔드 프로젝트 초기 구성 (#1)

- Gradle 멀티모듈 구조를 추가했습니다.
- 공통 빌드 설정을 추가했습니다.
```

---

## Branch

### 구조

```text
{도메인}/{issue number}-{작업 내용}
```

### 예시

```text
configs/1-init
accounts/3-social-login
pays/16-portone
utils/2-decorator
```

---

## Pull Request

### 제목

이슈 제목 + 추가/수정/삭제 등 직관적으로 작성한다.

### 본문

```markdown
## 🔎 What is this PR?
- 관련 API 명세서 링크

## ✨ Changes

## 📷 Result

## 💬 To. Reviewer
```