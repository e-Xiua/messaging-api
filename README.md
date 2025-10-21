# Messaging API - e-Xiua

Microservicio de mensajería en tiempo real para la plataforma e-Xiua. Permite chat entre usuarios (turistas, proveedores, admin) vía WebSocket/STOMP y HTTP REST.

## 🚀 Características

- **WebSocket/STOMP**: Chat en tiempo real
- **Autenticación JWT**: Validación en handshake
- **Historial de conversaciones**: Persistencia en PostgreSQL/H2
- **Marcado de mensajes leídos**: Notificaciones push
- **Indicador "escribiendo..."**: UX mejorada
- **RabbitMQ**: Publicación de eventos (message.sent, message.read)
- **REST API**: Listar conversaciones, mensajes, no leídos
- **Paginación**: Mensajes paginados
- **Fallback SockJS**: Compatibilidad con navegadores legacy

## 📋 Requisitos

- Java 21
- Maven 3.9+
- RabbitMQ
- PostgreSQL (producción) / H2 (desarrollo)
- Docker (opcional)

## 🛠️ Configuración

### Variables de Entorno

```bash
JWT_SECRET=tu-clave-secreta-muy-segura
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres123
MESSAGING_DB_NAME=messaging_db
RABBITMQ_USER=admin
RABBITMQ_PASSWORD=admin123
```

## 🌐 Endpoints

### WebSocket

- **Endpoint**: `/ws` (STOMP/SockJS)
- **Enviar mensaje**: `/app/chat.send`
- **Marcar como leído**: `/app/chat.read`
- **Indicador escribiendo**: `/app/chat.typing`
- **Recibir mensajes**: `/user/queue/messages`
- **Recibir typing**: `/user/queue/typing`

### REST

- `GET /api/conversations` - Listar conversaciones del usuario
- `GET /api/conversations/{id}/messages` - Mensajes paginados
- `GET /api/conversations/unread` - Mensajes no leídos

## 📨 Eventos RabbitMQ

| Evento | Routing Key | Descripción |
|--------|-------------|-------------|
| MessageSent | `message.sent` | Mensaje enviado |
| MessageRead | `message.read` | Mensaje leído |
| ConversationCreated | `conversation.created` | Nueva conversación |

## 🧪 Testing

### WebSocket (JavaScript)

```javascript
const socket = new SockJS('http://localhost:8765/ws?token=YOUR_JWT_TOKEN');
const stompClient = Stomp.over(socket);
stompClient.connect({}, function(frame) {
    stompClient.subscribe('/user/queue/messages', function(msg) {
        console.log('Mensaje:', JSON.parse(msg.body));
    });
    stompClient.send('/app/chat.send', {}, JSON.stringify({
        receiverId: 2,
        content: 'Hola!'
    }));
});
```

### REST

```bash
curl -X GET http://localhost:8765/api/conversations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

curl -X GET http://localhost:8765/api/conversations/1/messages?page=0&size=50 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🔐 Seguridad

- **JWT**: Validado en handshake y en endpoints REST
- **Header X-User-Id**: Inyectado por el API Gateway
- **Solo participantes pueden acceder a conversaciones/mensajes**

## 📦 Dependencias

- Spring Boot 3.2.0
- Spring WebSocket
- Spring Data JPA
- RabbitMQ
- PostgreSQL/H2
- JJWT
- Lombok

## 📝 Arquitectura

```
┌─────────────────────────────┐
│ Messaging API (8083)        │
│ ┌─────────────────────────┐ │
│ │ WebSocketConfig         │ │
│ │ JwtHandshakeInterceptor │ │
│ │ MessagingService        │ │
│ │ ConversationController  │ │
│ │ ChatController          │ │
│ │ MessageEventPublisher   │ │
│ └─────────────────────────┘ │
└─────────────┬───────────────┘
              │
        ┌─────▼─────┐
        │ PostgreSQL│
        └───────────┘
        ┌─────▼─────┐
        │ RabbitMQ  │
        └───────────┘
```

## 🤝 Contribución

1. Fork el repositorio
2. Crea una rama para tu feature
3. Commit tus cambios
4. Push a la rama
5. Abre un Pull Request

## 📄 Licencia

Proyecto e-Xiua - Todos los derechos reservados
