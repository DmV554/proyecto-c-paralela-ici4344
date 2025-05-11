CREATE TABLE usuarios (
                          id_usuario INT AUTO_INCREMENT PRIMARY KEY,
                          nombre_usuario VARCHAR(255) NOT NULL UNIQUE COMMENT 'Identificador simple del usuario, ej. "consola_user_1"'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;