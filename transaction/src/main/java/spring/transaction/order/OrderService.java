package spring.transaction.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    //JPA는 트랜잭션 Commit 시점에 스냅샷과 비교한 Order 데이터를 DB에 반영한다
    @Transactional
    public void order(Order order) throws NotEnoughMoneyException {
        log.info("order 호출");
        orderRepository.save(order);

        log.info("결제 프로세스 진입");
        if (order.getUnsername().equals("예외")) {
        //시스템 예외
            log.info("시스템 예외 발생");
            throw new RuntimeException("시스템 예외");
        } else if (order.getUnsername().equals("잔고부족")) {
        //비즈니스 예외 - *** CheckedException 발생하지만 data는 Commit 되길 원하는 상황
            log.info("잔고부족 비즈니스 예외 발생");
            order.setPayStatus("대기");
            //Eitntiy 필드 세팅만 해줘도 commit 시점에 update 수행
            throw new NotEnoughMoneyException("잔고가 부족합니다");
        } else {
        //정상 승인
            log.info("정상 승인");
            order.setPayStatus("완료");
        }
        log.info("결제 프로세스 완료");
    }
}
