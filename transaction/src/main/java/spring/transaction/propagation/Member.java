package spring.transaction.propagation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Member {

    @Id
    @GeneratedValue
    private Long id;

    private String username;

    // *** 기본 생성자는 JPA 스펙상 꼭 존재해야함
    public Member() {
    }

    // 편의를 위한 생성자
    public Member(String username) {
        this.username = username;
    }
}
