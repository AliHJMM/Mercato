FROM nginx:alpine

# Generate a self-signed certificate with Subject Alternative Names (SAN).
# SAN is required by Chrome/Firefox since 2017 — without it the browser
# shows "NET::ERR_CERT_COMMON_NAME_INVALID" and refuses to add a permanent
# exception. With SAN for localhost/127.0.0.1, you only need to trust the
# cert once in the OS store and the warning disappears.
RUN apk add --no-cache openssl && \
    mkdir -p /etc/ssl/private && \
    printf '[req]\ndefault_bits=2048\nprompt=no\ndefault_md=sha256\n\
distinguished_name=dn\nx509_extensions=v3_req\n\
[dn]\nC=US\nST=Local\nL=Dev\nO=Mercato\nOU=Platform\nCN=localhost\n\
[v3_req]\nbasicConstraints=critical,CA:TRUE\n\
keyUsage=critical,digitalSignature,keyEncipherment,keyCertSign\n\
extendedKeyUsage=serverAuth\nsubjectAltName=@alt_names\n\
[alt_names]\nDNS.1=localhost\nIP.1=127.0.0.1\n' > /tmp/openssl.cnf && \
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
      -keyout /etc/ssl/private/buy01.key \
      -out    /etc/ssl/certs/buy01.crt \
      -config /tmp/openssl.cnf

COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]
