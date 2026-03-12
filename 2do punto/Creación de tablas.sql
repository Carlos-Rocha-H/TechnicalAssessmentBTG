IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'BTG_DB')
BEGIN
    CREATE DATABASE [BTG_DB];
END
GO

-- Tablas
CREATE TABLE Cliente (
    id INT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    ciudad VARCHAR(100) NOT NULL
);

CREATE TABLE Sucursal (
    id INT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    ciudad VARCHAR(100) NOT NULL
);

CREATE TABLE Producto (
    id INT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    tipoProducto VARCHAR(100) NOT NULL
);

CREATE TABLE Inscripcion (
    idProducto INT,
    idCliente INT,
    PRIMARY KEY (idProducto, idCliente),
    FOREIGN KEY (idProducto) REFERENCES producto(id),
    FOREIGN KEY (idCliente) REFERENCES cliente(id)
);

CREATE TABLE Disponibilidad (
    idSucursal INT,
    idProducto INT,
    PRIMARY KEY (idSucursal, idProducto),
    FOREIGN KEY (idSucursal) REFERENCES sucursal(id),
    FOREIGN KEY (idProducto) REFERENCES producto(id)
);

CREATE TABLE Visitan (
    idSucursal INT,
    idCliente INT,
    fechaVisita DATE NOT NULL,
    PRIMARY KEY (idSucursal, idCliente),
    FOREIGN KEY (idSucursal) REFERENCES sucursal(id),
    FOREIGN KEY (idCliente) REFERENCES cliente(id)
);

--Datos
INSERT INTO Sucursal VALUES (101, 'Sucursal Norte', 'Bogotá');
INSERT INTO Sucursal VALUES (102, 'Sucursal Centro', 'Bogotá');
INSERT INTO Sucursal VALUES (103, 'Sucursal Sur', 'Medellín');

INSERT INTO Producto VALUES (1, 'Fondo Acciones', 'FIC');
INSERT INTO Producto VALUES (2, 'CDT Pro', 'Ahorro');

INSERT INTO Cliente VALUES (1, 'Juan', 'Perez', 'Bogotá');
INSERT INTO Cliente VALUES (2, 'Maria', 'Gomez', 'Bogotá');

INSERT INTO Disponibilidad VALUES (101, 1);
INSERT INTO Disponibilidad VALUES (102, 1);
INSERT INTO Disponibilidad VALUES (103, 2);

INSERT INTO Inscripcion VALUES (1, 1);
INSERT INTO Inscripcion VALUES (1, 2);

INSERT INTO Visitan VALUES (101, 1, '2023-10-01');
INSERT INTO Visitan VALUES (102, 1, '2023-10-02');
INSERT INTO Visitan VALUES (101, 2, '2023-10-01');


--drop table Visitan
--drop table Disponibilidad
--drop table Inscripcion
--drop table Producto
--drop table Sucursal
--drop table Cliente