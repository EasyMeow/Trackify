#!/bin/bash
set -e

DOMAIN=${1:-$DOMAIN}
EMAIL=${2:-$LETSENCRYPT_EMAIL}

if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
  echo "Usage: ./init-ssl.sh <domain> <email>"
  echo "  or:  DOMAIN=example.com LETSENCRYPT_EMAIL=you@example.com ./init-ssl.sh"
  exit 1
fi

echo ">>> Stopping frontend to free port 80..."
docker compose -f docker-compose.prod.yml stop frontend 2>/dev/null || true

echo ">>> Obtaining Let's Encrypt certificate for $DOMAIN..."
docker run --rm \
  -p 80:80 \
  -v trackify-letsencrypt:/etc/letsencrypt \
  -v trackify-certbot-www:/var/www/certbot \
  certbot/certbot certonly \
  --standalone \
  --non-interactive \
  --agree-tos \
  --email "$EMAIL" \
  -d "$DOMAIN"

echo ">>> Certificate obtained. Starting full stack..."
DOMAIN="$DOMAIN" docker compose -f docker-compose.prod.yml up -d

echo ""
echo "Done! App is now available at https://$DOMAIN"
echo ""
echo "Automatic renewal runs every 12 hours via the certbot container."
echo "After each renewal, reload nginx to pick up the new certificate:"
echo "  docker exec trackify-frontend nginx -s reload"
echo ""
echo "To automate this, add the following to the server's crontab (crontab -e):"
echo "  0 */12 * * * docker exec trackify-frontend nginx -s reload"
