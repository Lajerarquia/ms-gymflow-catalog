package cl.duoc.gymflow.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Registro de qué reserva ocupa un cupo de qué clase.
 * <p>
 * Existe para que tomar y devolver cupo sean <b>idempotentes</b>: si ms-gymflow-reservations reintenta
 * "tomar cupo" para la misma reserva (por un timeout, por ejemplo), no se descuenta dos veces; y si pide
 * "devolver cupo" de una reserva que nunca lo tomó, no se suma uno de más. La restricción única sobre
 * {@code reserva_id} garantiza que una reserva ocupe como máximo un cupo.
 */
@Entity
@Table(name = "CUPO_RESERVADO", uniqueConstraints = @UniqueConstraint(name = "UK_CUPO_RESERVA", columnNames = "reserva_id"))
public class CupoReservado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clase_id", nullable = false)
    private Clase clase;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(name = "tomado_en", nullable = false)
    private Instant tomadoEn;

    protected CupoReservado() {
        // requerido por JPA
    }

    public CupoReservado(Clase clase, Long reservaId) {
        this.clase = clase;
        this.reservaId = reservaId;
        this.tomadoEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Clase getClase() {
        return clase;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public Instant getTomadoEn() {
        return tomadoEn;
    }
}
