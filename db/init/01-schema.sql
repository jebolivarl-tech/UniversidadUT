-- Esquema de la base `universidad` (generado a partir de los dumps originales y endurecido con las
-- reglas académicas). Instala de cero: hace DROP de todas las tablas.
-- Si ya tiene datos de un curso anterior, NO ejecute este archivo: aplique db/migrations/002_reglas_academicas.sql.
CREATE DATABASE IF NOT EXISTS universidad CHARACTER SET utf8mb4;
USE universidad;

DROP TABLE IF EXISTS `asistencias`;
DROP TABLE IF EXISTS `calificaciones`;
DROP TABLE IF EXISTS `matriculas`;
DROP TABLE IF EXISTS `secciones`;
DROP TABLE IF EXISTS `usuarios`;
DROP TABLE IF EXISTS `cursos`;
DROP TABLE IF EXISTS `estudiantes`;
DROP TABLE IF EXISTS `carreras`;
DROP TABLE IF EXISTS `profesores`;
DROP TABLE IF EXISTS `periodos_academicos`;
DROP TABLE IF EXISTS `facultades`;

CREATE TABLE `facultades` (
  `id_facultad` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `decano` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id_facultad`),
  UNIQUE KEY `uq_facultad_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `periodos_academicos` (
  `id_periodo` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_fin` date NOT NULL,
  `estado` enum('ACTIVO','INACTIVO','CERRADO') NOT NULL DEFAULT 'INACTIVO',
  PRIMARY KEY (`id_periodo`),
  UNIQUE KEY `uq_periodo_nombre` (`nombre`),
  CONSTRAINT `chk_periodo_fechas` CHECK (`fecha_fin` >= `fecha_inicio`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `profesores` (
  `id_profesor` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(60) NOT NULL,
  `apellido` varchar(60) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `id_facultad` int NOT NULL,
  PRIMARY KEY (`id_profesor`),
  UNIQUE KEY `email` (`email`),
  KEY `fk_profesor_facultad` (`id_facultad`),
  CONSTRAINT `fk_profesor_facultad` FOREIGN KEY (`id_facultad`) REFERENCES `facultades` (`id_facultad`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `carreras` (
  `id_carrera` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `id_facultad` int NOT NULL,
  PRIMARY KEY (`id_carrera`),
  UNIQUE KEY `uq_carrera_nombre` (`nombre`),
  KEY `fk_carrera_facultad` (`id_facultad`),
  CONSTRAINT `fk_carrera_facultad` FOREIGN KEY (`id_facultad`) REFERENCES `facultades` (`id_facultad`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `estudiantes` (
  `id_estudiante` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(60) NOT NULL,
  `apellido` varchar(60) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `fecha_nacimiento` date DEFAULT NULL,
  `id_carrera` int NOT NULL,
  PRIMARY KEY (`id_estudiante`),
  UNIQUE KEY `email` (`email`),
  KEY `fk_estudiante_carrera` (`id_carrera`),
  CONSTRAINT `fk_estudiante_carrera` FOREIGN KEY (`id_carrera`) REFERENCES `carreras` (`id_carrera`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cursos` (
  `id_curso` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `creditos` int NOT NULL DEFAULT 3,
  `id_carrera` int NOT NULL,
  `id_profesor` int NOT NULL,
  PRIMARY KEY (`id_curso`),
  KEY `fk_curso_carrera` (`id_carrera`),
  KEY `fk_curso_profesor` (`id_profesor`),
  CONSTRAINT `chk_curso_creditos` CHECK (`creditos` between 1 and 10),
  CONSTRAINT `fk_curso_carrera` FOREIGN KEY (`id_carrera`) REFERENCES `carreras` (`id_carrera`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_curso_profesor` FOREIGN KEY (`id_profesor`) REFERENCES `profesores` (`id_profesor`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Un grupo es un curso dictado por un profesor en un periodo, en un aula y con un cupo.
-- La clave única evita los grupos duplicados que antes solo controlaba la aplicación.
CREATE TABLE `secciones` (
  `id_seccion` int NOT NULL AUTO_INCREMENT,
  `id_curso` int NOT NULL,
  `id_profesor` int NOT NULL,
  `id_periodo` int NOT NULL,
  `aula` varchar(50) DEFAULT NULL,
  `cupo_maximo` int NOT NULL DEFAULT 30,
  PRIMARY KEY (`id_seccion`),
  UNIQUE KEY `uq_seccion_grupo` (`id_curso`,`id_periodo`,`aula`),
  KEY `id_profesor` (`id_profesor`),
  KEY `id_periodo` (`id_periodo`),
  CONSTRAINT `chk_seccion_cupo` CHECK (`cupo_maximo` between 1 and 200),
  CONSTRAINT `secciones_ibfk_1` FOREIGN KEY (`id_curso`) REFERENCES `cursos` (`id_curso`) ON DELETE CASCADE,
  CONSTRAINT `secciones_ibfk_2` FOREIGN KEY (`id_profesor`) REFERENCES `profesores` (`id_profesor`) ON DELETE CASCADE,
  CONSTRAINT `secciones_ibfk_3` FOREIGN KEY (`id_periodo`) REFERENCES `periodos_academicos` (`id_periodo`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- La matrícula se inscribe en una sección: el curso y el periodo se derivan de ella.
-- RESTRICT: un grupo con matriculados no se puede borrar sin retirar antes las matrículas.
CREATE TABLE `matriculas` (
  `id_matricula` int NOT NULL AUTO_INCREMENT,
  `id_estudiante` int NOT NULL,
  `id_seccion` int NOT NULL,
  `id_curso` int NOT NULL,
  `periodo` varchar(50) NOT NULL,
  `nota` decimal(4,2) DEFAULT NULL,
  `estado` enum('EN_CURSO','APROBADO','REPROBADO','PERDIDO_POR_FALTA_DE_ASISTENCIA') NOT NULL DEFAULT 'EN_CURSO',
  `fecha_matricula` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_matricula`),
  UNIQUE KEY `uq_matricula_seccion` (`id_estudiante`,`id_seccion`),
  UNIQUE KEY `uq_matricula` (`id_estudiante`,`id_curso`,`periodo`),
  KEY `fk_matricula_curso` (`id_curso`),
  KEY `fk_matricula_seccion` (`id_seccion`),
  CONSTRAINT `chk_matricula_nota` CHECK (`nota` is null or (`nota` between 0 and 5)),
  CONSTRAINT `fk_matricula_seccion` FOREIGN KEY (`id_seccion`) REFERENCES `secciones` (`id_seccion`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_matricula_curso` FOREIGN KEY (`id_curso`) REFERENCES `cursos` (`id_curso`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_matricula_estudiante` FOREIGN KEY (`id_estudiante`) REFERENCES `estudiantes` (`id_estudiante`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- MySQL no permite un CHECK sobre columnas que participan en una FK (error 3823), así que
-- "toda cuenta no administrativa tiene su ficha" la garantiza la aplicación: Validaciones.esCuentaValida
-- en la API y CrearUsuario en la línea de comandos.
CREATE TABLE `usuarios` (
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `rol` enum('ADMINISTRATIVO','ESTUDIANTE','PROFESOR') NOT NULL,
  `id_estudiante` int DEFAULT NULL,
  `id_profesor` int DEFAULT NULL,
  `estado` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `idx_username` (`username`),
  KEY `fk_usuario_estudiante` (`id_estudiante`),
  KEY `fk_usuario_profesor` (`id_profesor`),
  CONSTRAINT `fk_usuario_estudiante` FOREIGN KEY (`id_estudiante`) REFERENCES `estudiantes` (`id_estudiante`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_usuario_profesor` FOREIGN KEY (`id_profesor`) REFERENCES `profesores` (`id_profesor`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Un control por estudiante, sección y día: la regla que antes solo vivía en Java.
CREATE TABLE `asistencias` (
  `id_asistencia` int NOT NULL AUTO_INCREMENT,
  `id_estudiante` int NOT NULL,
  `id_seccion` int NOT NULL,
  `fecha` date NOT NULL,
  `estado` enum('ASISTIO','FALTO','TARDANZA','JUSTIFICADO') NOT NULL,
  PRIMARY KEY (`id_asistencia`),
  UNIQUE KEY `uq_asistencia_dia` (`id_estudiante`,`id_seccion`,`fecha`),
  KEY `id_seccion` (`id_seccion`),
  CONSTRAINT `asistencias_ibfk_1` FOREIGN KEY (`id_estudiante`) REFERENCES `estudiantes` (`id_estudiante`) ON DELETE CASCADE,
  CONSTRAINT `asistencias_ibfk_2` FOREIGN KEY (`id_seccion`) REFERENCES `secciones` (`id_seccion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Las notas empiezan vacías: un cero es una nota presentada, no una nota sin digitar.
CREATE TABLE `calificaciones` (
  `id_calificacion` int NOT NULL AUTO_INCREMENT,
  `id_estudiante` int NOT NULL,
  `id_seccion` int NOT NULL,
  `nota_parcial1` decimal(4,2) DEFAULT NULL,
  `nota_parcial2` decimal(4,2) DEFAULT NULL,
  `nota_examen_final` decimal(4,2) DEFAULT NULL,
  `promedio_final` decimal(4,2) GENERATED ALWAYS AS (round(coalesce(`nota_parcial1`, 0) * 0.3 + coalesce(`nota_parcial2`, 0) * 0.3 + coalesce(`nota_examen_final`, 0) * 0.4, 2)) STORED,
  `estado` enum('APROBADO','REPROBADO','EN_CURSO','PERDIDO_POR_FALTA_DE_ASISTENCIA') NOT NULL DEFAULT 'EN_CURSO',
  PRIMARY KEY (`id_calificacion`),
  UNIQUE KEY `unica_inscripcion` (`id_estudiante`,`id_seccion`),
  KEY `id_seccion` (`id_seccion`),
  CONSTRAINT `chk_calificacion_escala` CHECK ((`nota_parcial1` is null or (`nota_parcial1` between 0 and 5)) and (`nota_parcial2` is null or (`nota_parcial2` between 0 and 5)) and (`nota_examen_final` is null or (`nota_examen_final` between 0 and 5))),
  CONSTRAINT `calificaciones_ibfk_1` FOREIGN KEY (`id_estudiante`) REFERENCES `estudiantes` (`id_estudiante`) ON DELETE CASCADE,
  CONSTRAINT `calificaciones_ibfk_2` FOREIGN KEY (`id_seccion`) REFERENCES `secciones` (`id_seccion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
