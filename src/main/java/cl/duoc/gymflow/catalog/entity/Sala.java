package cl.duoc.gymflow.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Sala de un gimnasio de la red. Su capacidad es el aforo físico: ninguna clase dictada en ella
 * puede ofrecer más cupos que esto.
 */
@Entity
@Table(name = "SALA")
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String sede;

    @Column(nullable = false)
    private int capacidad;

    protected Sala() {
        // requerido por JPA
    }

    public Sala(String nombre, String sede, int capacidad) {
        this.nombre = nombre;
        this.sede = sede;
        this.capacidad = capacidad;
    }

    public void actualizar(String nombre, String sede, int capacidad) {
        this.nombre = nombre;
        this.sede = sede;
        this.capacidad = capacidad;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSede() {
        return sede;
    }

    public int getCapacidad() {
        return capacidad;
    }
}
