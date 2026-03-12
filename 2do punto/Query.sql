SELECT DISTINCT c.nombre, c.apellidos
FROM Cliente c
JOIN Inscripcion i ON c.id = i.idCliente
WHERE NOT EXISTS (
    SELECT 1 
    FROM Disponibilidad d
    WHERE d.idProducto = i.idProducto
    AND NOT EXISTS (
        SELECT 1 
        FROM Visitan v 
        WHERE v.idCliente = c.id 
        AND v.idSucursal = d.idSucursal
    )
);