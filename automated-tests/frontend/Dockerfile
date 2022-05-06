FROM node:16-alpine AS development
ENV NODE_ENV development

WORKDIR /app

COPY . .

RUN npm ci

EXPOSE 3000

CMD [ "npm", "start" ]

FROM node:16-alpine AS builder
ENV NODE_ENV production

WORKDIR /app

COPY . .

RUN npm ci
RUN npm run-script build

FROM nginx:1-alpine AS production
ENV NODE_ENV production

COPY --from=builder /app/build /usr/share/nginx/html

COPY ./nginx/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD [ "nginx", "-g", "daemon off;" ]