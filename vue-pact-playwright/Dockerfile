FROM node:22.15-slim AS base
ENV PNPM_HOME="/pnpm"
ENV PATH="$PNPM_HOME:$PATH"
RUN corepack enable
COPY . /app
WORKDIR /app

FROM base AS prod-deps
RUN --mount=type=cache,id=pnpm,target=/pnpm/store pnpm install --prod --frozen-lockfile

FROM base AS build
RUN --mount=type=cache,id=pnpm,target=/pnpm/store pnpm install --frozen-lockfile
RUN pnpm run build

FROM pactfoundation/pact-stub-server:0.6.0 AS pact
COPY --from=build /app/pacts /pacts
EXPOSE 8080
CMD ["-p", "8080", "-d", "/pacts", "--provider-state-header-name", "X-Pact-Provider-State", "-o"]


FROM nginx:stable-alpine AS web
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
