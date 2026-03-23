# traveldesk-api

Backend Spring Boot para TravelDesk.

## Email en produccion con Brevo

La aplicacion usa `spring-boot-starter-mail` y envia correos via `JavaMailSender`.
Con esta implementacion, el perfil `prod` queda preparado para Brevo API (HTTPS 443) por defecto,
y mantiene SMTP como fallback opcional.

### 1) Variables de entorno requeridas (Brevo API - recomendado)

Usa como base el archivo `/.env.prod.brevo.example`.

Variables clave:

- `MAIL_PROVIDER=brevo-api`
- `BREVO_API_URL=https://api.brevo.com/v3`
- `BREVO_API_KEY=<api-key-brevo>`
- `BREVO_API_TIMEOUT_SECONDS=15`
- `MAIL_FROM=<sender-verified@tu-dominio-o-brevo>`

### 2) Fallback SMTP (opcional)

Si tu entorno permite salida SMTP, puedes usar:

- `MAIL_PROVIDER=smtp`
- `MAIL_HOST=smtp-relay.brevo.com`
- `MAIL_PORT=587` (o `2525`)
- `MAIL_USERNAME=<smtp-login-brevo>`
- `MAIL_PASSWORD=<smtp-key-brevo>`
- `MAIL_SMTP_AUTH=true`
- `MAIL_SMTP_STARTTLS_ENABLE=true`
- `MAIL_SMTP_STARTTLS_REQUIRED=true`
- `MAIL_SMTP_SSL_ENABLE=false`

### 3) URLs del frontend para links de reset/invitacion

Define en prod URLs publicas (no `localhost`):

- `RESET_PASSWORD_BASE_URL=https://traveldesk-pi.vercel.app/reset-password`
- `ACCEPT_INVITE_BASE_URL=https://traveldesk-pi.vercel.app/accept-invite`

### 4) Verificacion local rapida

```bash
./mvnw -q test
```

### 5) Run en prod profile (local)

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

### 6) Troubleshooting SMTP

Si hay timeout en prod:

```bash
nc -vz smtp-relay.brevo.com 587
```

Si el puerto no abre, el problema suele ser de egress/firewall del hosting.

