package spring.transaction.apply;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * * 초기화 코드는 Spring의 초기화 시점에 실행 -> 직접 호출할 필요가 없음
 *     + 초기화 시점의 실행 코드들이므로, Test 결과에 로그가 찍히는 것이 아닌, 서버 띄우는 로그에서 확인 가능
 * * @PostConstruct와 @Transactional을 함께 사용 시, 트랜잭션이 적용되지 않는 이유 - * 메서드 호출 순서 차이
 *     - @PostConstruct 호출 -> @Transactional이 적용되므로 초기화 시점에는 메서드에 대한 트랜잭션을 획득할 수 없다
 * * 대안 - @EventListener
 *     - SpringContainer / AOP 모두 만든 이후에 @Transactional 호출하도록
 */

@Slf4j
@SpringBootTest
public class InitTxTest {

    @Autowired Hello hello;

    @Test
    void go() {
        // * 직접 호출할 경우 - Transaction 적용됨
        //hello.initV1();

        /**
         * initV1() 과 initV2() 사이의 로그
         * - Started InitTxTest in 2.894 seconds (process running for 4.494)
         */
    }

    @TestConfiguration
    static class initTxTestConfig {
        @Bean
        Hello hello() {
            return new Hello();
        }
    }

    static class Hello {

        @PostConstruct
        @Transactional
        public void initV1() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("Hello initV1() @PostConstruct txActive is = {}", isActive);
        }

        @EventListener(ApplicationReadyEvent.class)
        @Transactional
        public void initV2() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("Hello initV2() @EventListener(ApplicationReadyEvent.class) txActive is = {}", isActive);
        }
    }
}
