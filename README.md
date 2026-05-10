# InnControl Backend

InnControl es una solución integral para la gestión hotelera, diseñada para optimizar las operaciones diarias, la gestión de tareas y el control de inventario. Este repositorio contiene el **Backend** desarrollado con Spring Boot.

## Tecnologías Utilizadas

*   **Java 17**
*   **Spring Boot 3.x** (Web, Data JPA, Security)
*   **MySQL** (Base de Datos)
*   **JWT** (Autenticación y Autorización)
*   **Lombok** (Reducción de código boilerplate)
*   **Maven** (Gestión de dependencias)

## Características Principales

*   **Gestión de Usuarios**: Autenticación segura mediante JWT con roles de Gerente y Empleado.
*   **Control de Habitaciones**: CRUD completo de habitaciones con estados (Libre, Ocupada, Limpieza, Mantenimiento) y asignación por pisos.
*   **Sistema de Tareas**: Gestión de labores diarias con prioridades y asignación dinámica.
*   **Control de Inventario**: Monitoreo de stock en tiempo real con alertas de niveles críticos.
*   **Mensajería Interna**: Sistema de chat en tiempo real para la coordinación del equipo.
*   **Asignación Asistida**: Algoritmo de sugerencia para la asignación de tareas según carga de trabajo.

## Configuración e Instalación

1.  **Clonar el repositorio**:
    ```bash
    git clone https://github.com/tu-usuario/inncontrol-backend.git
    ```

2.  **Configurar Base de Datos**:
    Crea una base de datos en MySQL llamada `inncontrol`.

3.  **Variables de Entorno**:
    El proyecto utiliza variables de entorno para la configuración sensible. Puedes configurarlas en tu IDE o sistema:
    *   `DB_HOST`: Host de la base de datos (default: localhost)
    *   `DB_USER`: Usuario de MySQL (default: root)
    *   `DB_PASSWORD`: Contraseña de MySQL
    *   `JWT_SECRET`: Clave secreta para los tokens JWT

4.  **Ejecutar la aplicación**:
    ```bash
    ./mvnw spring-boot:run
    ```

## Seguridad

El sistema implementa **Spring Security** para proteger los endpoints. 
*   Los endpoints de gestión (Crear habitaciones, eliminar usuarios, etc.) están restringidos para el rol `GERENTE`.
*   El acceso a tareas e inventario es compartido pero con permisos específicos según el rol.

---
Desarrollado para la eficiencia y modernización de la gestión hotelera.
