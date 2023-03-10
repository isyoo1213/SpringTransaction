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
 * * Transaction 적용 우선순위 원칙
 *   - 구체적이고 자세한 것이 항상 우선순위를 가진다
 *   - 클래스의 메서드 -> 클래스의 타입 -> 인터페이스의 메서드 -> 인터페이스의 타입
 *   -> 이는 Spring의 @Transactional 어노테이션의 탐색 순서와도 같음
 * * 인터페이스에 @Transactional 적용은 추천하지 않는다
 *   - AOP가 적용되지 않을 경우도 발생할 가능성 내재 by CGLIB의 구체 클래스 기반 Proxy 생성
 *   -> 되도록 구현체 중심, 클래스와 메서드 중심으로 작성
 */

@Slf4j
@SpringBootTest
class TxLevelTest {

    @Autowired LevelService levelService;

    @Test
    void orderTest() {
        levelService.write();
        levelService.read();
    }

    @TestConfiguration
    static class TxLevelTestConfig {
        @Bean
        LevelService levelService() {
            return new LevelService();
        }
    }

    // 클래스 Level
    @Transactional(readOnly = true) //트랜잭션의 읽기/쓰기 작업 중 읽기 작업만 가능하도록
    static class LevelService {

        // 메서드 Level
        @Transactional(readOnly = false) //write기능은 쓰기이므로 readOnly=false로 지정
        public void write() {
            log.info("call write");
            printTxInfo();
        }

        public void read() {
            log.info("call read");
            printTxInfo();
        }

        // 트랜잭션 적용/옵션 여부
        private void printTxInfo() {
            //트랜잭션 적용 여부
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx Active = {}", txActive);

            //트랜잭션 옵션 여부
            boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("isReadOnly = {}", isReadOnly);
        }
    }

}
