package spring.transaction.apply;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * * 내부호출 -> 외부호출로 전환
 *   - target 인스턴스가 주입받은 @Transactional이 붙은 proxy 인스턴스를 호출하는 방식
 * *** 클래스 레벨에 선언된 @Transactional은 클래스 내부의 public/default 접근제어자가 붙은 메서드에서만 적용된다
 *   - 만약 다른 접근제어자의 메서드에서 @Transactional 사용 시, 다른 오류 메시지는 나타나지 않고 Transaction도 적용되지 않으므로 주의
 */

@Slf4j
@SpringBootTest
class InternalCallV2Test {

    @Autowired CallService callService;

    @Test
    void printProxy() {
        log.info("callService class = {}", callService.getClass());
    }

    @Test
    void externalCallV2() {
        callService.external();
    }

    @TestConfiguration
    static class InternalCallV1TestConfig {

        //InternalService.class Bean등록 및 CallService에 주입
        // *** InternalService.class의 internal()에 @Transactional 존재하므로 실제 주입되는 인스턴스는 Proxy 인스턴스
        // *** CallService는 이제 내부에 external()과 printTxInfo()만 존재하므로 Proxy 인스턴스가 아닌 실제 객체의 인스턴스
        @Bean
        CallService callService() {
            return new CallService(internalService());
        }

        @Bean
        InternalService internalService() {
            return new InternalService();
        }
    }

    @RequiredArgsConstructor
    static class CallService {

        // 2. internal()을 담고있는 클래스를 주입
        private final InternalService internalService;

        public void external() {
            log.info("call external");
            printTxInfo();
            // 3. this.internal()을 대체 -> 내부호출에서 외부호출로
            internalService.internal();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isSynchronizationActive();
            log.info("txActive = {}", txActive);
        }
    }

    // 1. 기존의 internal() 메서드를 새로운 클래스에서 정의
    static class InternalService {
        @Transactional
        void internal() {
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isSynchronizationActive();
            log.info("txActive = {}", txActive);
        }

    }

}
