package spring.transaction.propagation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

/**
 * Server가 아닌 DB에 남기기 위한 로그
 */
@Entity
@Getter
@Setter
public class Log {

    @Id
    @GeneratedValue
    private Long id;

    private String message;

    //JPA를 위한 기본 생성자
    public Log() {
    }

    //편의를 위한 생성자
    public Log(String message) {
        this.message = message;
    }
}
