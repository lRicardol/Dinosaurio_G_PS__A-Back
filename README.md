# Dinosaurio_G_PS__A-Back

## Descripción General

**Dinosaurio_G_PS__A-Back** es el **servidor backend** del proyecto académico *Vampire Survivor Multiplayer Edition*, desarrollado como parte del curso de **Arquitectura de Software**.  
Su función principal es gestionar toda la **lógica de negocio**, la **persistencia de datos**, y la **comunicación en tiempo real** entre los jugadores del juego.

Este servicio está construido con **Spring Boot (Java 17+)**, siguiendo el patrón **MVC extendido (Model-View-Controller)** y aplicando las mejores prácticas de **arquitectura limpia y desacoplamiento** mediante capas **Controller, Service, Repository, Model y DTO**.

---

## Objetivo del Repositorio

Implementar la **API REST** y los **servicios WebSocket** necesarios para soportar:
- Creación y gestión de partidas (modo multijugador y en tiempo real).
- Control de sesio
- nes y jugadores conectados.
- Sincronización de eventos dentro del mapa del juego.
- Persistencia de datos de usuarios, estadísticas y progresos.
- Comunicación fluida con el frontend [`Dinosaurio_G_PS__A-From`](https://github.com/...).

---

## Arquitectura y Diseño

src/main/java/com/dinosaurio_G/Back

├── controller/ → Maneja las solicitudes HTTP o WebSocket (entrada del sistema)

├── service/ → Lógica de negocio y reglas del juego

├── repository/ → Acceso a la base de datos (JPA / Mongo / Redis)

├── model/ → Entidades del dominio (Player, GameRoom, Enemy, etc.)

├── dto/ → Objetos de transferencia de datos (seguridad y eficiencia)

└── BackApplication.java → Clase principal que inicia el servidor Spring Boot

### Estructura de Paquetes

### Tecnologías Principales

| Tecnología | Descripción |
|-------------|--------------|
| **Java 17+** | Lenguaje principal de desarrollo |
| **Spring Boot 3.x** | Framework base del backend |
| **Spring WebSocket** | Comunicación en tiempo real entre jugadores |
| **Spring Data JPA** | Gestión de persistencia de datos |
| **H2 / MySQL / MongoDB** | Bases de datos utilizadas (según entorno) |
| **Maven** | Sistema de gestión de dependencias |
| **AWS / Azure DevOps** | Infraestructura de despliegue y CI/CD |

---

## Funcionalidades Principales (MVP Release 1)

-  **Autenticación y login de usuarios** (pantalla de login conectada al backend).
-  **Creación de partidas multijugador** (privadas o públicas).
-  **Sincronización en tiempo real** mediante WebSockets.
-  **Gestión de combate y eventos del mapa** (ataques, cofres, experiencia, daño).
-  **Actualización del estado de la partida** (vida, experiencia, enemigos activos).
-  **Persistencia de jugadores y estadísticas** en la base de datos.

---

##  Patrón de Diseño Aplicado

El backend aplica una versión extendida del patrón **MVC (Model-View-Controller)**, donde:

- **Controller:** recibe las peticiones REST o WebSocket y las deriva a la capa de servicio.
- **Service:** contiene la lógica del juego, validaciones, sincronización y reglas de negocio.
- **Repository:** abstrae la capa de persistencia (JPA / Mongo).
- **Model:** representa las entidades del dominio.
- **DTO:** asegura el intercambio seguro de datos entre frontend y backend.

Esta estructura mejora la **modularidad, mantenibilidad y escalabilidad** del sistema.

---

##  Integración con el Frontend

Este backend se comunica con el proyecto **`Dinosaurio_G_PS__A-From`**, encargado del rendering visual del juego y la interacción del usuario.  
Toda la comunicación entre ambos se realiza mediante:
- **HTTP (REST)** → autenticación, configuración y estadísticas.
- **WebSocket (STOMP)** → sincronización de eventos en tiempo real dentro de las partidas.

---

## Ejecución Local

### Requisitos Previos
- Java 17 o superior
- Maven 3.9+
- IDE recomendado: IntelliJ IDEA o VSCode con extensión Java

### Ejecutar la Aplicación

```bash
# Clonar el repositorio
git clone https://github.com/tuusuario/Dinosaurio_G_PS__A-Back.git
cd Dinosaurio_G_PS__A-Back

# Compilar el proyecto
mvn clean install

# Ejecutar el servidor
mvn spring-boot:run
