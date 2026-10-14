# Despliegue

La aplicación es un servidor Java (Javalin) que sirve el front-end y la API, y necesita **MySQL 8**. Por eso
**no sirve un hosting compartido solo de PHP/HTML**: necesita un hosting que ejecute Java/Docker.

## Opción A — VPS con Docker (todo incluido: app + MySQL)
1. Contrate un VPS (Ubuntu) con Docker instalado y un dominio apuntando a su IP.
2. Suba el proyecto (git o `scp`) y cree el archivo `.env` a partir de `.env.example` con contraseñas largas.
3. `docker compose up -d --build`
4. Ponga HTTPS delante, por ejemplo con Caddy (obtiene el certificado solo). `Caddyfile`:
   ```
   su-dominio.com {
       reverse_proxy localhost:8080
   }
   ```
5. Cree el primer administrador (la BD nace sin usuarios):
   ```
   docker compose run --rm --entrypoint "" app sh -c 'java -cp app.jar universidad.seguridad.CrearUsuario admin "UnaClaveLarga!2026" ADMINISTRATIVO'
   ```
   (con `DB_URL`, `DB_USER`, `DB_PASSWORD` ya definidos por el compose).

## Opción B — PaaS con Docker (Railway, Render, Fly.io…)
1. Cree un servicio desde este repositorio (detectan el `Dockerfile`).
2. Cree una base MySQL (el propio servicio o uno externo) y ejecute `db/init/01-schema.sql` en ella.
3. Variables de entorno del servicio: `DB_URL` (`jdbc:mysql://HOST:PUERTO/universidad`, añada `?sslMode=REQUIRED`
   si el proveedor lo exige), `DB_USER`, `DB_PASSWORD`. `PORT` lo suele definir el proveedor; `COOKIE_SECURE=true`
   y `TRUST_PROXY=true` ya vienen por defecto en la imagen.
4. Cree el administrador con `CrearUsuario` (ver arriba) desde su máquina apuntando a esa BD, o inserte la fila
   con un hash generado por esa misma herramienta.

## Verificación después de desplegar
- `https://su-dominio/api/health` debe responder `{"status":"UP","database":"UP"}`.
- Inicie sesión en `https://su-dominio/login.html`.
- Un usuario `ESTUDIANTE` no debe poder abrir Estudiantes/Carreras ni llamar a sus APIs de escritura (403).

## Base de datos existente (con datos)
Si ya tiene datos, no use `01-schema.sql`: ejecute `db/001_seguridad_y_fk.sql` y luego
`universidad.seguridad.MigrarPasswords` para convertir las contraseñas a hash.
