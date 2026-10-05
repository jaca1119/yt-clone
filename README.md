# YT-clone

## Architecture

- Separate frontend, REST API, and authentication service.
- Java 25, Spring Boot 4, Spring MVC, and Spring Data JPA/Hibernate.
- Layered backend: `domain`, `application`, and `infrastructure`.
- Separate PostgreSQL databases for application data and user accounts; local storage for media files.

## File Uploads

- Two-step upload: metadata creation followed by multipart MP4 upload.
- `max-file-size: 1GB` and `max-request-size: 1GB` to cap file and request sizes.
- `file-size-threshold: 2MB` to write larger uploads to disk and reduce memory usage.
- `resolve-lazily: true` to defer multipart parsing until accessed.

## Video Processing & Streaming

- FFmpeg: H.264/AAC encoding, thumbnails, and HLS VOD with a single quality variant.
- HLS: `.m3u8` playlist and `.ts` segments with a 6-second target duration for segmented playback.
- ffprobe: duration extraction after upload, during local video import, and before preview generation.
- Spring `@Async`: preview thumbnail sprites every 5 seconds with WebVTT timing. (When you hover over video timeline you see thumbnails as on the YT)

## Frontend

- React 19, TypeScript, React Router 7 with SSR, and Vite.
- HeroUI and Tailwind CSS 4.
- Video.js HLS player; Fetch and Axios for API requests and upload progress.

## Authentication

- Dedicated Spring OAuth2 Authorization Server with OpenID Connect.
- Authorization Code with PKCE for the public frontend client.
- Spring OAuth2 Resource Server with JWT verification through JWKS.
- BCrypt password hashing; frontend integration via `oidc-client-ts` and `react-oidc-context`.

