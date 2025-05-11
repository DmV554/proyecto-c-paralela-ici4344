CREATE TABLE criptomonedas (
                               id_cripto INT AUTO_INCREMENT PRIMARY KEY,
                               simbolo VARCHAR(20) NOT NULL UNIQUE COMMENT 'Ej: BTC, ETH',
                               coingecko_id VARCHAR(100) NOT NULL UNIQUE COMMENT 'Ej: bitcoin, ethereum',
                               nombre_cripto VARCHAR(100) NULL COMMENT 'Nombre completo opcional, ej: Bitcoin'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;