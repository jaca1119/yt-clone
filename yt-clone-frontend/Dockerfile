FROM node:20-alpine AS base
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN corepack enable && pnpm --version

FROM base AS development-dependencies-env
RUN pnpm install --frozen-lockfile

FROM base AS production-dependencies-env
RUN pnpm install --prod --frozen-lockfile

FROM base AS build-env
COPY --from=development-dependencies-env /app/node_modules ./node_modules
COPY . .
RUN pnpm build

FROM nginx:alpine
COPY --from=build-env /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
