package spring.transaction.order;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * JPA 사용 시, 별다른 DataSource 설정이 없다면 스프링 내부적으로 '메모리 DB를' 사용할 수있도록 테이블을 생성한다
 *    - spring.jpa.hibernate.ddl-auto 설정에서 Entity정보를 참고하는 '테이블 자동 생성'에 관한 옵션 설정 가능
 *    - 흔히 JPA 사용시 application.properties에서 설정하던 none / create 옵션
 * 이는 Test 메서드/클래스 단위에서 @Transactional 사용이 commit을 rollback으로 변경하는 것과는 다른 주제
 */

@Slf4j
@SpringBootTest
class OrderServiceTest {

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;

    @Test
    void complete() throws NotEnoughMoneyException {
        //given
        Order order = new Order();
        order.setUnsername("정상");

        //when
        orderService.order(order);

        //then
        // * 정상 흐름이므로 transaction의 Commit을 검증
        Order findOrder = orderRepository.findById(order.getId()).get(); //Optional이므로 바로 꺼내서 사용해보기

        assertThat(findOrder.getPayStatus()).isEqualTo("완료"); //문자열보다 Enum 클래스로 사용하는 것이 일반적임
    }

    @Test
    void runtimeException() throws NotEnoughMoneyException {
        //given
        Order order = new Order();
        order.setUnsername("예외");

        //when
        assertThatThrownBy(() -> orderService.order(order))
                .isInstanceOf(RuntimeException.class);

        //then
        // *** RuntimeException 발생으로 인한 Rollback 수행이므로 order.getStatus()에 어떠한 세팅도 되지 않은 상태를 검증
        // -> *** Service 계층에서 시작된 Transaction 자체가 order() 메서드 또한 타고 들어가므로 save() 또한 롤백됨
        // -> *** 로그에서 insert 관련된 쿼리가 생성되지 않는 것도 JPA의 commit 시점 sql 수행과 관련
        Optional<Order> orderOptional = orderRepository.findById(order.getId());
        assertThat(orderOptional).isEmpty();
    }

    @Test
    void bizException() {
        //given
        Order order = new Order();
        order.setUnsername("잔고부족");

        //when
        // * 이번에는 CheckedException을 잡아서 대응하는 로직을 위해 try-catch 상황 설정
        try {
            orderService.order(order);
        } catch (NotEnoughMoneyException e) {
            //실제로는 Service를 호출하는 Controller 계층에서 thorws된 Exception을 잡아서 처리하는 로직
            log.info("고객에게 잔고 부족을 알리고 별도의 계좌로 입금하도록 안내");
        }

        //then
        // *** 예외가 터지더라도 transaction이 Commit되는 것을 검증
        Order findOrder = orderRepository.findById(order.getId()).get();
        // * Optional에 get()을 직접 수행하는 것은 좋지 않은 패턴
        assertThat(findOrder.getPayStatus()).isEqualTo("대기");
    }

    /**
     * * 즉, CheckedException이 commit되게끔 설정된 것은, 예외를 메서드의 'return' 처럼 사용가능하게 하는 것
     *   cf) CheckedException을 사용하지않고 설계하는 방식
     *      - 실제로 Enum 클래스로 문자열들을 정의한 후, Exception이 아닌 '상태'를 return하고 Controller에서 처리하는 구조도 가능
     *      *** 어쩄든 두 방식 모두 Trnasaction의 commit 정상 수행에 기반함
     * * 여튼 CheckedException이 '비즈니스적 의미'를 가지는 맥락과 commit 수행을 기본으로 제공하는 정책인 점이 포인트
     */

}