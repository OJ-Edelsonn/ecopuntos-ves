# EcoPuntos VES

Aplicación web desarrollada en **Java 17** y **Spring Boot 3.5.16** para la gestión de acopio de materiales reciclables, seguimiento de puntos críticos de acumulación de residuos con cuadrillas municipales y administración de incentivos vecinales en Villa El Salvador (Lima, Perú).

---

## Stack tecnológico

* **Backend:** Java 17, Spring Boot 3.5.16, Spring MVC, Spring Data JPA (Hibernate), Spring Security (BCrypt, CSRF y autorización por rol/propietario).
* **Frontend:** Thymeleaf + `thymeleaf-extras-springsecurity6`, Bootstrap 5.3.3, Bootstrap Icons y hoja de estilos unificada en modo oscuro (`#4ADE80`).
* **Base de datos:** MariaDB 10.4 (XAMPP) con 14 tablas InnoDB (`utf8mb4_unicode_ci`), columnas generadas `PERSISTENT`, restricciones `CHECK`/`UNIQUE` y bloqueo pesimista (`PESSIMISTIC_WRITE`).
* **Pruebas:** JUnit 5, Spring Boot Test y Spring Security Test (`MockMvc`).

---

## Arquitectura del proyecto

```text
src/
├── main/
│   ├── java/pe/ecopuntosves/
│   │   ├── config/          # SecurityConfig, AutenticacionUsuariosService, CambioClavePendienteFilter
│   │   ├── model/           # 14 entidades JPA mapeadas a MariaDB
│   │   ├── repository/      # 14 interfaces JpaRepository (JPQL, paginación y bloqueos)
│   │   ├── service/         # 11 servicios transaccionales de negocio
│   │   └── web/             # 12 controladores MVC, GlobalExceptionHandler y formularios
│   └── resources/
│       ├── static/css/      # estilos.css (diseño responsivo oscuro #4ADE80)
│       ├── templates/       # 32 plantillas Thymeleaf (incluye fragmentos/menu.html y vistas 403/404)
│       └── application.properties
└── test/
    └── java/pe/ecopuntosves/
        ├── EcopuntosVesApplicationTests.java
        ├── SeguridadAccesoTests.java
        └── ReglasNegocioIntegracionTests.java
sql/
├── 01_ecopuntos_esquema.sql
├── 02_ecopuntos_datos.sql
└── consolidacion/
    ├── 03_01_crear_catalogo_cuadrillas.sql
    ├── 03_02_datos_cuadrillas.sql
    └── 03_03_verificar_cuadrillas.sql
```

---

## Requisitos previos

* **JDK 17** (Eclipse Adoptium u Oracle).
* **XAMPP** con **MariaDB 10.4** activo en el puerto `3306`.
* **Spring Tools Suite (STS / Eclipse)** o Maven (`mvnw`).

---

## Instalación y ejecución desde cero

### 1. Clonar el repositorio

```bash
git clone [https://github.com/OJ-Edelsonn/ecopuntos-ves.git](https://github.com/OJ-Edelsonn/ecopuntos-ves.git)
cd ecopuntos-ves
```

### 2. Crear y poblar la base de datos en MariaDB

Con el servicio **MySQL/MariaDB** encendido en XAMPP, ejecuta los scripts SQL en este orden exacto (desde phpMyAdmin, MySQL Workbench o consola):

1. `sql/01_ecopuntos_esquema.sql` — Crea la base `ecopuntos_ves` y las tablas principales.
2. `sql/02_ecopuntos_datos.sql` — Carga catálogos, tarifas iniciales y datos de demostración (350 vecinos, entregas, reportes, canjes y movimientos).
3. `sql/consolidacion/03_01_crear_catalogo_cuadrillas.sql` — Añade las tablas `cuadrilla` y `miembro_cuadrilla` con integridad referencial compuesta hacia `atencion_punto_critico`.
4. `sql/consolidacion/03_02_datos_cuadrillas.sql` — Registra 4 cuadrillas y 12 integrantes iniciales.
5. `sql/consolidacion/03_03_verificar_cuadrillas.sql` *(opcional)* — Verifica las 14 tablas y conteos esperados.

### 3. Configurar credenciales locales

Revisa `src/main/resources/application.properties` y ajusta usuario/clave de MariaDB si tu instancia local difiere de la configuración por defecto de XAMPP (`root` sin contraseña):

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/ecopuntos_ves
spring.datasource.username=root
spring.datasource.password=
```

### 4. Arrancar la aplicación

* **Desde STS:** Importar como *Existing Maven Project* → clic derecho en el proyecto → **Maven → Update Project** → ejecutar `EcopuntosVesApplication` como **Spring Boot App**.
* **Desde terminal:**

```bash
./mvnw spring-boot:run
```

Accede en el navegador a `http://localhost:8080/`. Las fotografías privadas adjuntadas en reportes se almacenan automáticamente en `%USERPROFILE%\EcoPuntos_VES\fotos_reportes`.

---

## Manual breve por rol

Contraseña común de las cuentas de demostración locales: `Demo1234!`

| Rol | Usuario demo | Módulos y operaciones principales |
| :--- | :--- | :--- |
| **Vecino** | `vecino001` | Consultar perfil con saldo (`/mi-perfil`), estado de cuenta de movimientos (`/mis-puntos`), historial y detalle por material de entregas (`/mis-entregas`, `/mis-entregas/{id}`), registrar y seguir puntos críticos (`/puntos-criticos/mis-reportes`) y emitir constancias imprimibles de canje (`/canjes`). |
| **Operador** | `operador01` | Buscar, registrar y editar vecinos (`/vecinos`), generando automáticamente cuenta `v+DNI` con clave temporal; registrar entregas multimaterial con cálculo automático de puntos (`/entregas/nueva`). |
| **Funcionario** | `funcionario01` | Filtrar puntos críticos por sector, estado y fechas con tarjetas de totales (`/puntos-criticos`), validar o rechazar reportes, asignar cuadrillas/integrantes y cerrar resoluciones; anular canjes con reverso único (`/canjes/administrar`); consultar reportes y exportar CSV (`/reportes`). |
| **Administrador** | `admin` | Administrar materiales reciclables y versiones de reglas de puntos sin alterar historial (`/reglas`), gestionar personal con alta/edición/estado (`/usuarios`), administrar cuadrillas e integrantes (`/cuadrillas`) y supervisar vecinos, entregas, incidencias y reportes. |

---

## Pruebas automatizadas

Con MariaDB encendido en XAMPP, ejecuta la suite completa (11 pruebas JUnit 5):

* **Desde STS:** clic derecho en `src/test/java` → **Run As → JUnit Test**.
* **Desde terminal:**

```bash
./mvnw test
```

Las pruebas de escritura usan `@Transactional` con reversión automática (*rollback*) para preservar intacta la base de datos.

---

## Equipo de desarrollo

* **Curso:** Lenguajes de Programación II (2026-2) · Universidad Científica del Sur
* **Docente:** Julio Alfredo Córdova Forero
* **Integrantes:**
  * Chávez Bada, Franco Akira
  * Chochoca Yaranga, Rosalinda Lizbet
  * Montes Velasquez, Jhosselyn Keysi
  * Orihuela Jara, Edelson Anghuelo

> Proyecto académico: los datos de vecinos, DNI, operaciones y constancias de canje utilizados en demostración son ficticios y no tienen validez tributaria ni municipal real.