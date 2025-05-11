CREATE TABLE historial_precios (
                                   id_historial INT AUTO_INCREMENT PRIMARY KEY,
                                   id_cripto_fk INT NOT NULL,
                                   precio DECIMAL(20, 8) NOT NULL,
                                   moneda_cotizacion VARCHAR(10) NOT NULL DEFAULT 'usd',
                                   timestamp_precio BIGINT NOT NULL COMMENT 'Timestamp UNIX en milisegundos del precio',
                                   fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   FOREIGN KEY (id_cripto_fk) REFERENCES criptomonedas(id_cripto) ON DELETE CASCADE,
                                   INDEX idx_timestamp_precio (timestamp_precio),
                                   INDEX idx_cripto_timestamp (id_cripto_fk, timestamp_precio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;