# traveldesk-api

Backend Spring Boot para TravelDesk.

## Email en produccion con Brevo (SMTP)

La aplicacion usa `spring-boot-starter-mail` y envia correos via `JavaMailSender`.
Con esta implementacion, el perfil `prod` queda preparado para Brevo por defecto.

### 1) Variables de entorno requeridas

Usa como base el archivo `/.env.prod.brevo.example`.

Variables clave:

- `MAIL_HOST=smtp-relay.brevo.com`
- `MAIL_PORT=587`
- `MAIL_USERNAME=<smtp-login-brevo>`
- `MAIL_PASSWORD=<smtp-key-brevo>`
- `MAIL_SMTP_AUTH=true`
- `MAIL_SMTP_STARTTLS_ENABLE=true`
- `MAIL_SMTP_STARTTLS_REQUIRED=true`
- `MAIL_SMTP_SSL_ENABLE=false`
- `MAIL_FROM=<sender-verified@tu-dominio-o-brevo>`

> Nota: en Brevo, `MAIL_USERNAME` suele ser el login SMTP de Brevo, no siempre tu email.

### 2) URLs del frontend para links de reset/invitacion

Define en prod URLs publicas (no `localhost`):

- `RESET_PASSWORD_BASE_URL=https://traveldesk-pi.vercel.app/reset-password`
- `ACCEPT_INVITE_BASE_URL=https://traveldesk-pi.vercel.app/accept-invite`

### 3) Verificacion local rapida

```bash
./mvnw -q test
```

### 4) Run en prod profile (local)

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

### 5) Troubleshooting SMTP

Si hay timeout en prod:

```bash
nc -vz smtp-relay.brevo.com 587
```

Si el puerto no abre, el problema suele ser de egress/firewall del hosting.

