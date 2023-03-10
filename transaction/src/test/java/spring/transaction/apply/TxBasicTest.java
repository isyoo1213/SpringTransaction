package spring.transaction.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.*;

/**
 * 1. Transaction Proxy 등록 과정
 * 2. Transaction Proxy 동작 과정
 * 3. AOP Proxy가 적용된 클래스 확인 방법
 */

@Slf4j
@SpringBootTest //AOP 동작 확인을 위해 서버 자체를 띄워야 함
class TxBasicTest {

    @Autowired BasicService basicService;

    /**
     * AOP proxy가 적용된 클래스인지 확인하는 방법
     */
    @Test
    void proxyCheck() {
        log.info("AOP class = {}", basicService.getClass());
        assertThat(AopUtils.isAopProxy(basicService)).isTrue();
    }

    @Test
    void txTest() {
        basicService.tx();
        basicService.nonTx();
    }

    @TestConfiguration
    static class TxApplyBasicConfig {
        @Bean
        BasicService basicService() {
            return new BasicService();
        }
    }

    static class BasicService {

        @Transactional
        // *** Transaction Proxy 등록하는 과정
        //   - *** @Transactional 어노테이션이 특정 '클래스'나 '메서드'에 '하나라도' 존재하면, 즉 클래스 내부에 어느 하나라도 존재한다면
        //   -> '트랜잭션 AOP'는 Proxy를 만들어서 스프링 컨테이너에 등록
        //   -> *** 실제 BasicService 클래스의 인스턴스가 아닌, basicService$$CGLIB를 Bean 등록함
        //   -> 이 프록시 bean은 실제 BasicService를 참조
        //   -> *** 즉 테스트 클래스에서 주입받는 @Autowired BasicService basicService;에는 Proxy Bean이 주입됨
        //      * Proxy는 실제 BasicService를 '상속' -> 다형성 활용 가능
        // *** Transaction Proxy 동작 과정
        //   1. 클라이언트인 txBasicTest는 transaction Proxy의 메서드를 호출 -> tx() or nonTx()
        //   2. * transaction Proxy에서 @Transactional 어노테이션의 등록 여부 확인
        //      - @Transactional이 적용된 경우
        //      -> * Transaction을 시작 -> 실제 basicService.tx()실행 -> Transaction 종료 (Commit or Rollback)
        //      - @Transactionl이 적용되지 않은 경우
        //      -> 실제 basicService.nonTx() 실행
        public void tx() {
            log.info("call tx");
            // *** Thread가 호출한 코드 내에서 transaction이 활성화 되어있는지 확인하는 방법
            //   - 트랜잭션 동기화 매니저에서 con을 체크하고 가져오고 하는 일련의 과정이 있었는지 확인하는 방식
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active = {}", txActive);
        }

        public void nonTx() {
            log.info("call nonTx");
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active = {}", txActive);
        }
    }

}


