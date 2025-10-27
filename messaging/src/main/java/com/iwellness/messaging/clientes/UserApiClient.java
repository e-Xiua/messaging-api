package com.iwellness.messaging.clientes;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.iwellness.messaging.config.FeignClientInterceptor;
import com.iwellness.messaging.dto.UsuarioDTO;

// 'admin-users-service' es el nombre de la aplicación de destino.
// La URL es para desarrollo local. En producción, esto se resolvería con un Discovery Server.
// El 'configuration' agrega el interceptor que propaga los headers de autenticación
@FeignClient(
    name = "admin-users-service", 
    url = "${feign.client.turista.url:http://localhost:8082}/usuarios",
    configuration = FeignClientInterceptor.class
)
public interface UserApiClient {

    @GetMapping("/perfil-publico/{id}")
    UsuarioDTO findById(@PathVariable("id") Long id);

    // --- NUEVO MÉTODO ---
    // Llama a GET http://localhost:8082/api/usuarios/{userId}/contacts
    @GetMapping("/{userId}/contacts")
    List<UsuarioDTO> getContactsForUser(@PathVariable("userId") Long userId);
}
