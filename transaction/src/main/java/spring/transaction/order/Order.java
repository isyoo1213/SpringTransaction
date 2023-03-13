package spring.transaction.order;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orders") //DB 예약어 중에 order by 등이 있으므로 order 단어로 테이블 생성하는 것은 좋지 않음
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    private String unsername; // 정상, 예외, 잔고부족
    private String payStatus; //대기, 완료
}
