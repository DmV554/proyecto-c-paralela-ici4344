
CREATE TABLE alertas (
                         id_alerta INT AUTO_INCREMENT PRIMARY KEY,
                         id_usuario_fk INT NOT NULL,
                         id_cripto_fk INT NOT NULL,
                         precio_umbral DECIMAL(20, 8) NOT NULL,
                         tipo_condicion VARCHAR(10) NOT NULL COMMENT 'MAYOR_QUE, MENOR_QUE',
                         activa BOOLEAN NOT NULL DEFAULT TRUE,
                         fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         FOREIGN KEY (id_usuario_fk) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                         FOREIGN KEY (id_cripto_fk) REFERENCES criptomonedas(id_cripto) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;