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
RUN pnpm run build

FROM base AS production
ENV NODE_ENV=production

COPY --from=production-dependencies-env /app/node_modules ./node_modules
COPY --from=build-env /app/build ./build

EXPOSE 3000
CMD ["pnpm", "start"]
