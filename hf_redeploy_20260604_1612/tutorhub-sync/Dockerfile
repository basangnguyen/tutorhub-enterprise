FROM node:18-alpine

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --omit=dev

COPY server.js ./
COPY update.jar ./
COPY version.json ./

ENV PORT=7860
EXPOSE 7860

CMD ["node", "server.js"]
