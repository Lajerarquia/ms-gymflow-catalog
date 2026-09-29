package cl.duoc.gymflow.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Clase del catálogo (en la API se llama "service", como en el caso: {@code /api/catalog/services}).
 * <p>
 * {@code cuposDisponibles} baja cuando se confirma una reserva y sube cuando se cancela una reserva
 * confirmada. Siempre se cumple {@code 0 <= cuposDisponibles <= capacidad}.
 */
@Entity
@Table(name = "CLASE")
public class Clase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, length = 100)
    private String instructor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @Column(nullable = false)
    private Instant inicio;

    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos;

    @Column(nullable = false)
    private int capacidad;

    @Column(name = "cupos_disponibles", nullable = false)
    private int cuposDisponibles;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_membresia", nullable = false, length = 20)
    private Plan plan;

    protected Clase() {
        // requerido por JPA
    }

    public Clase(String nombre, String descripcion, String instructor, Sala sala, Instant inicio,
                 int duracionMinutos, int capacidad, Plan plan) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.instructor = instructor;
        this.sala = sala;
        this.inicio = inicio;
        this.duracionMinutos = duracionMinutos;
        this.capacidad = capacidad;
        this.cuposDisponibles = capacidad;
        this.plan = plan;
    }

    /**
     * Actualiza los datos de la clase. Si cambia la capacidad, los cupos disponibles se recalculan
     * manteniendo los ya ocupados (el servicio verifica antes que la nueva capacidad los alcance).
     */
    public void actualizar(String nombre, String descripcion, String instructor, Sala sala, Instant inicio,
                           int duracionMinutos, int capacidad, Plan plan) {
        int ocupados = getCuposOcupados();
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.instructor = instructor;
        this.sala = sala;
        this.inicio = inicio;
        this.duracionMinutos = duracionMinutos;
        this.capacidad = capacidad;
        this.cuposDisponibles = capacidad - ocupados;
        this.plan = plan;
    }

    public void tomarCupo() {
        if (cuposDisponibles <= 0) {
            throw new IllegalStateException("La clase no tiene cupos disponibles");
        }
        cuposDisponibles--;
    }

    public void devolverCupo() {
        if (cuposDisponibles < capacidad) {
            cuposDisponibles++;
        }
    }

    public int getCuposOcupados() {
        return capacidad - cuposDisponibles;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getInstructor() {
        return instructor;
    }

    public Sala getSala() {
        return sala;
    }

    public Instant getInicio() {
        return inicio;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public int getCuposDisponibles() {
        return cuposDisponibles;
    }

    public Plan getPlan() {
        return plan;
    }
}
