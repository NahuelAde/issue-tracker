# issue-tracker — Gestor de incidencias

Aplicación local y personal para gestionar incidencias de trabajo por proyecto, con seguimiento
cronológico, horas y sprints. Interfaz en español; UI en Java con Vaadin Flow.

## Stack

- Java 21
- Spring Boot 4.1
- Vaadin 25 Flow
- Maven
- Spring Data JPA + Hibernate
- H2 (base de datos en fichero)
- JUnit 5 + AssertJ (tests)

## Requisitos

- **JDK 21** (imprescindible). En esta máquina hay un JDK 21 en `C:\Program Files\Java\jdk-21`;
  el `java` del PATH puede ser otra versión, así que conviene fijar `JAVA_HOME` a JDK 21.
- No hace falta instalar Maven: se usa el wrapper (`mvnw.cmd` en Windows).

## Cómo ejecutar

### Desde IntelliJ IDEA
1. Abrir el proyecto e indicar el **SDK del proyecto = 21** (File → Project Structure → Project SDK).
2. Ejecutar la clase `com.nahuel.issuetracker.IssueTrackerApplication`.
3. Para recarga en caliente de cambios Java: instalar el plugin **Vaadin** y arrancar con
   **"Debug using HotswapAgent"**.

### Con Maven (línea de comandos, PowerShell)
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
.\mvnw.cmd spring-boot:run
```
La aplicación abre en **http://localhost:5555**.

### Generar el JAR ejecutable
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
.\mvnw.cmd clean package
```
Genera `target\issue-tracker-<versión>.jar` (versión tomada de `pom.xml`; incluye el frontend
compilado en modo producción).

### Ejecutar el JAR
```powershell
& "C:\Program Files\Java\jdk-21\bin\java.exe" -jar target\issue-tracker-<versión>.jar
```
Abre en **http://localhost:5555**.

### Acceso directo (Windows)
Hay un lanzador `arrancar-gestor.cmd` en la raíz del proyecto que ejecuta el JAR con JDK 21 y abre el
navegador. Existe además un acceso directo **"Gestor de incidencias"** en el Escritorio que lo lanza.
Al cerrar la ventana del lanzador se detiene la aplicación. (Requiere haber generado el JAR con
`mvnw.cmd clean package`.)

## Base de datos

- H2 en fichero, en la carpeta **`./data`** del proyecto (`jdbc:h2:file:./data/issue-tracker`).
- Los datos persisten entre reinicios. El esquema se crea/actualiza solo (`ddl-auto=update`).
- Consola web de H2 en **/h2-console** (usuario `nahuel`, sin contraseña).
- La aplicación arranca **sin datos**: no se crea ningún proyecto de ejemplo. Crea tus proyectos desde la propia interfaz ("Añadir proyecto").

### Copia de seguridad
1. **Detener** la aplicación.
2. Copiar la carpeta **`./data`** completa a un lugar seguro.

### Restaurar
1. Detener la aplicación.
2. Sustituir la carpeta **`./data`** por la copia guardada.
3. Arrancar de nuevo.

## Estructura funcional

- **Proyectos**: crear/editar/activar/desactivar (sin borrado físico). Selector en la cabecera;
  gestión en **Proyectos**.
- **Sprints por proyecto**: crear con fechas; el "sprint actual" es el que incluye la fecha de hoy.
  Gestión en **Sprints**.
- **Incidencias** (vista principal): listado con filtros (búsqueda, estado, prioridad, categoría,
  sprint, tipo, asignada, "sólo abiertas"), columnas por bloques (Desarrollo / Ubicación / Pruebas),
  coloreado por estado y sprint, y tooltip (icono ℹ) con el histórico de entradas.
- **Detalle de incidencia**: datos principales (código, título, categoría, tipo, asignada, estado,
  prioridad, referencia, sprint y planificación), evolución (checkboxes), resumen (horas totales,
  fechas, últimos despliegues PRE/PRO), seguimiento cronológico (entradas con horas) y
  resolución/pruebas. Cerrar/reabrir incidencia.

## Tests

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
.\mvnw.cmd "-Dvaadin.skip=true" test
```
Tests de la lógica de negocio (servicios) con H2 en memoria, aislados de `./data`.

## Limitaciones actuales (fuera de alcance)

Sin autenticación/usuarios, adjuntos, exportación de documentos, integraciones (Jira/GitLab),
notificaciones, API REST, Docker ni despliegue en servidor. Es una herramienta de escritorio local
optimizada para tema claro.
