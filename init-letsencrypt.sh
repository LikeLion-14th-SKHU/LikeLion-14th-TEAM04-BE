#!/bin/bash
# Let's Encrypt 인증서 최초 발급용 — 홈서버에서 딱 한 번만 실행한다.
# nginx.conf가 처음부터 ssl_certificate를 참조하고 있어서, 진짜 인증서가 없으면 nginx
# 자체가 못 뜬다. 그래서 임시 자체서명 인증서로 nginx를 먼저 띄우고, certbot으로 진짜
# 인증서를 받은 뒤 nginx를 리로드하는 순서를 탄다. 이후 갱신은 certbot 서비스가 알아서 한다.
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo ".env가 없습니다. cp .env.example .env 로 만들고 채워주세요." >&2
  exit 1
fi

# .env를 source하지 않는다 — DB_PASSWORD 등 다른 값에 백틱/$ 같은 셸 특수문자가 섞여
# 있으면 source가 그걸 셸 코드로 해석하려다 깨진다(실제로 겪음). 여기서 진짜 필요한
# 두 값만 텍스트로 그대로 뽑아 쓴다
env_value() {
  grep -E "^$1=" .env | tail -n1 | cut -d '=' -f2-
}

DOMAIN="$(env_value DOMAIN)"
CERTBOT_EMAIL="$(env_value CERTBOT_EMAIL)"

: "${DOMAIN:?".env에 DOMAIN을 설정하세요 (예: api.memory-atelier.store)"}"
: "${CERTBOT_EMAIL:?".env에 CERTBOT_EMAIL을 설정하세요"}"

echo "### 1/4 임시 자체서명 인증서 생성 ###"
docker compose run --rm --entrypoint "\
  sh -c 'mkdir -p /etc/letsencrypt/live/$DOMAIN && \
    openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
      -keyout /etc/letsencrypt/live/$DOMAIN/privkey.pem \
      -out /etc/letsencrypt/live/$DOMAIN/fullchain.pem \
      -subj /CN=localhost'" certbot

echo "### 2/4 nginx 기동(임시 인증서로) ###"
docker compose up -d nginx

echo "### 3/4 임시 인증서 삭제 후 실제 인증서 요청 ###"
docker compose run --rm --entrypoint "\
  sh -c 'rm -rf /etc/letsencrypt/live/$DOMAIN /etc/letsencrypt/archive/$DOMAIN /etc/letsencrypt/renewal/$DOMAIN.conf'" certbot

docker compose run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
    -d $DOMAIN \
    --email $CERTBOT_EMAIL --agree-tos --no-eff-email" certbot

echo "### 4/4 nginx에 실제 인증서 반영 ###"
docker compose exec nginx nginx -s reload

echo "완료. https://$DOMAIN 으로 접속해서 확인하세요."
echo "참고: 인증서 자동 갱신은 certbot 서비스가 계속 돌면서 처리합니다."
echo "      단, nginx는 갱신된 인증서를 스스로 다시 읽지 않으니 90일 안에 한 번씩"
echo "      'docker compose exec nginx nginx -s reload'를 실행해 주세요(호스트 cron 권장)."
