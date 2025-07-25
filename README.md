Sistema de Monitoreo Remoto de Dispositivos
Este repositorio contiene el código fuente de una aplicación Android desarrollada como parte del "Desafío de Desarrollo de Apps Móviles". La aplicación convierte un dispositivo móvil en un nodo de recolección de datos GPS y de estado, con la capacidad de ser consultado de forma remota a través de una API REST segura.

✨ Características Principales
Recolección de Datos GPS: Captura la latitud, longitud y marca de tiempo del dispositivo.

Dos Modos de Recolección Flexibles:

Modo Continuo: Recolecta datos 24/7 a intervalos regulares (cada 30 segundos).

Modo Programado: Permite configurar días de la semana y un rango de horas para una recolección eficiente y optimizada para el consumo de batería.

Almacenamiento Local Persistente: Utiliza una base de datos Room (SQLite) para guardar de forma segura todos los datos recolectados.

Servidor API Integrado: Implementa un servidor HTTP (NanoHTTPD) directamente en la aplicación, permitiendo recibir peticiones remotas.

Endpoints de API:

/api/sensor_data: Devuelve los datos GPS, con la opción de filtrar por un rango de fechas.

/api/device_status: Devuelve el estado actual del dispositivo (batería, modelo, red, almacenamiento, etc.).

Seguridad: Todas las peticiones a la API requieren autenticación mediante un Bearer Token.

Interfaz de Usuario Intuitiva: Permite al usuario configurar el modo de operación, visualizar el estado del sistema, la IP local y la información recolectada.

🛠️ Tecnologías y Librerías Utilizadas
Lenguaje: Kotlin

Arquitectura: Basada en componentes (Activity, Service, BroadcastReceiver)

Base de Datos: Room Persistence Library (abstracción de SQLite)

Concurrencia: Corutinas de Kotlin para operaciones asíncronas.

Servidor HTTP: NanoHTTPD, una librería ligera para embeber un servidor web.

Interfaz de Usuario: Android XML Layouts.

🚀 Cómo Configurar y Ejecutar el Proyecto
Sigue estos pasos para poner en marcha la aplicación.

Prerrequisitos
Android Studio: Asegúrate de tener instalada una versión reciente (ej. Flamingo o superior).

Dispositivo o Emulador Android: Con Android 8.0 (Oreo) o superior.

Pasos de Instalación
Clonar el Repositorio
Abre tu terminal y ejecuta el siguiente comando:

git clone [URL_DE_TU_REPOSITORIO]

Abrir en Android Studio

Inicia Android Studio.

Selecciona "Open" y navega hasta la carpeta del proyecto que acabas de clonar.

Espera a que Gradle sincronice y construya el proyecto. Esto puede tardar unos minutos la primera vez.

Ejecutar la Aplicación

Conecta tu dispositivo Android a tu computadora o inicia un emulador.

Asegúrate de que Android Studio detecte tu dispositivo.

Haz clic en el botón "Run 'app'" (el ícono de play verde ▶️) en la barra de herramientas.

La aplicación se instalará y se iniciará en tu dispositivo.

Otorgar Permisos
La primera vez que se ejecute, la aplicación te solicitará permisos de ubicación. Es necesario aceptarlos para que la recolección de datos funcione.

📱 Cómo Usar la Aplicación
1. Seleccionar el Modo de Operación
Al abrir la app, puedes elegir entre dos modos:

Continuo 24/7: Ideal para un seguimiento constante.

Programado: Ideal para ahorrar batería, recolectando solo en momentos específicos.

2. Activar la Recolección
Si elegiste "Continuo": Presiona el botón "Iniciar Recolección Continua".

Si elegiste "Programado": Configura los días y las horas deseadas y luego presiona "Activar Horario".

En ambos casos, el texto de estado principal te confirmará que el modo está ACTIVO.

3. Visualizar Información
La interfaz principal te mostrará en tiempo real:

La IP Local de tu dispositivo (necesaria para las pruebas de API).

La última ubicación registrada.

El total de registros guardados en la base de datos.

Puedes presionar "Mostrar Estado del Dispositivo" para ver detalles como el nivel de batería, modelo, etc.

📡 Cómo Probar la API Remota
Para consultar los datos de forma remota, necesitarás una herramienta como Postman y asegurarte de que tu PC y tu celular estén en la misma red Wi-Fi.

1. Obtener el Token de API
Dentro de la aplicación, presiona el botón "Ver Token de API".

Se mostrará una ventana con tu token único. Cópialo.

2. Realizar Peticiones con Postman
Abre Postman y crea una nueva petición.

Configura el Encabezado de Autenticación:

Ve a la pestaña Headers.

En KEY, escribe: Authorization

En VALUE, escribe: Bearer <TU_TOKEN_COPIADO_AQUI> (reemplaza el texto con tu token real).

Realiza las Peticiones:

Para obtener el estado del dispositivo:

Método: GET

URL: http://<IP_LOCAL_DEL_CELULAR>:8080/api/device_status

Para obtener todos los datos del GPS:

Método: GET

URL: http://<IP_LOCAL_DEL_CELULAR>:8080/api/sensor_data

Para obtener datos GPS en un rango de tiempo:

Método: GET

URL: http://<IP_LOCAL_DEL_CELULAR>:8080/api/sensor_data?start_time=1672531200000&end_time=1675209600000
(Nota: los timestamps deben estar en milisegundos)

Si la autenticación es correcta, recibirás una respuesta JSON con los datos solicitados. Si no, recibirás un error 401 Unauthorized.
