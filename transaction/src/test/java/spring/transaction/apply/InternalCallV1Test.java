package spring.transaction.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * *** Proxy 방식의 AOP의 한계
 * * 구조 : 클라이언트 - Transaction Proxy(프록시 클래스) - target(실제 클래스)
 * * 기본
 *   1. @Transactional 어노테이션이 내부에 하나라도 존재하면 우선적으로 Transaction AOP가 target 클래스의 Proxy를 생성/Bean등록/주입
 *   2. @Transactional 어노테이션의 여부 확인 전에, 호출되는 것은 주입된 Proxy
 *   3. Proxy에서 @Transactional 어노테이션 여부 확인을 통해 Transaction 생성 판단 후 target 클래스 인스턴스의 실제 로직 호출
 * * 핵심은 this
 *   - 메서드의 호출에 특정한 참조가 없다면 자기 자신의 내부 메서드를 호출하는 것
 */

@Slf4j
@SpringBootTest
class InternalCallV1Test {

    @Autowired CallService callService;
    // *** CallService.class 내의 internal()에 @Transactional 어노테이션이 있으므로 Transaction AOP 적용
    // -> *** Proxy 객체가 주입됨

    //CallService의 인스턴스가 Proxy가 적용된 인스턴스인지 확인하는 테스트
    @Test
    void printProxy() {
        log.info("callService class = {}", callService.getClass());
    }

    //단순히 internalCall()을 호출해서 Transaction이 적용되어있는지 확인하는 테스트
    @Test
    void internalCall() {
        callService.internal();
    }

    //externalCall()의 로직 수행 후 -> internalCall()의 트랜잭션이 적용되는지 확인하는 테스트
    @Test
    void externalCall() {
        callService.external();
    }

    @TestConfiguration
    static class InternalCallV1TestConfig {

        @Bean
        CallService callService() {
            return new CallService();
        }
    }

    static class CallService {

        //외부에서 호출하는 메서드를 가정
        // *** @Transactional 미적용 -> Proxy는 Transaction을 생성하지 않고 target 클래스 인스턴스의 로직 호출
        public void external() {
            log.info("call external");
            printTxInfo();

            //Tx관련 info를 출력한 후 internal()을 호출하도록 가정
            // *** Transaction이 필요한 부분
            // *** but, Proxy를 거치지 않고 target 클래스 인스턴스가 직접 호출하는 것이 문제 상황
            // *** 모든 메서드의 호출에서 앞에 별도의 '참조'가 없으면 this.메서드()의 형식으로 호출된다
            // -> 즉, 자기 자신의 내부 메서드를 호출
            // *** 즉 this.internal(), 참조(callService 인스턴스)를 거치지 않고 나 자신(실제 target 클래스)의 인스턴스를 호출하는 것
            // *** -> '내부호출'은 Proxy를 거치지 않는다 -> Transaction을 적용할 수 없다(Transaction은 AOP를 통해 Proxy에서 처리하므로)
            internal();
        }

        //내부에서 호출하는 메서드를 가정
        // *** @Transactional 적용 -> Proxy는 Transaction 생성 후 실제 target 클래스 인스턴스의 로직 호출
        @Transactional
        public void internal() {
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isSynchronizationActive();
            log.info("txActive = {}", txActive);
        }
    }

}
