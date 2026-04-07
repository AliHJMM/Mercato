FROM nginx:alpine

# Generate a self-signed certificate valid for 365 days
RUN apk add --no-cache openssl && \
    mkdir -p /etc/ssl/private && \
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
      -keyout /etc/ssl/private/buy01.key \
      -out    /etc/ssl/certs/buy01.crt \
      -subj "/C=US/ST=Local/L=Dev/O=Mercato/OU=Platform/CN=localhost"

COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]
