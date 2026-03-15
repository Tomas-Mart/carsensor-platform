# Этап 1: Сборка frontend приложения
FROM node:20-alpine AS builder

WORKDIR /app

# Копирование файлов зависимостей
COPY package.json package-lock.json ./

# Установка зависимостей
RUN npm ci --frozen-lockfile

# Копирование исходного кода
COPY . .

# Сборка приложения
RUN npm run build

# Этап 2: Запуск в production режиме
FROM node:20-alpine AS runner

WORKDIR /app

# Установка curl ДО создания пользователя
RUN apk --no-cache add curl

# Создание непривилегированного пользователя
RUN addgroup -g 1001 -S nodejs
RUN adduser -S nextjs -u 1001

# Копирование собранного приложения из этапа сборки
COPY --from=builder --chown=nextjs:nodejs /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

# Переключение на непривилегированного пользователя
USER nextjs

# Порт, на котором работает приложение
EXPOSE 3000

# Установка переменных окружения
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"
ENV NODE_ENV=production

# Метки для информации
LABEL maintainer="CarSensor Team"
LABEL version="1.0.0"
LABEL description="Frontend for CarSensor Platform"

# Healthcheck для проверки работоспособности
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD node -e "require('http').get('http://localhost:3000', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})"

# Запуск приложения
CMD ["node", "server.js"]