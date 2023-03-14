package spring.transaction.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;

/**
 * 물리 - 실제 DB con
 * 논리 - 트랜잭션 매니저/트랜잭션 동기화 매니저 를 통한 Transaction 생성 및 con의 획득
 * 물리 commit - 실제 DB con에 commit 요청
 * 논리 commit - 트랜잭션 매니저를 통한 commit 요청 - 신규 트랜잭션 여부 확인 후 물리 commit 으로 연결할지의 여부 확인
 */

@Slf4j
@SpringBootTest
public class TxPropagationTest {

    @Autowired PlatformTransactionManager txManager;

    @Test
    void inner_commit() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerTx = txManager.getTransaction(new DefaultTransactionDefinition());
        //1. DataSource를 통한 con 생성(현재는 Pool에서 획득)
        //2. 획득한 DB 커넥션의 setAutoCommit(false) - 물리 트랜잭션의 생성
        //   로그 - setAutoCommit(false)
        //   o.s.j.d.DataSourceTransactionManager
        //   : Switching JDBC Connection [HikariProxyConnection@689393150 wrapping conn0
        //   : url=jdbc:h2:mem:08dd2356-b358-4d2c-a8db-90dcb10fd8e9 user=SA]
        //   to manual commit -> 수동 커밋 모드
        //3. 트랜잭션 매니저는 트랜잭션이 적용된 con을 트랜잭션 동기화 매니저에 보관

        log.info("outerTx.isNewTransaction() = {}", outerTx.isNewTransaction());
        //4. 트랜잭션 매니저가 트랜잭션 생성 결과를 status에 담아서 반환 - 신규 트랜잭션의 여부가 담김
        //5. 트랜잭션 적용된 con이 필요한 로직에서 트랜잭션 동기화 매니저에 저장된 con을 꺼내서 사용

        //6. 이미 outTx가 적용된 con을 사용하는 상황에서 새로운 트랜잭션을 생성하려는 시도
        log.info("내부 트랜잭션 시작");
        TransactionStatus innerTx = txManager.getTransaction(new DefaultTransactionDefinition());
        //7. *** 트랜잭션 매니저는 트랜잭션 동기화 매니저를 통해 기존 트랜잭션이 존재하는지 확인
        //8. 기존 트랜잭션 존재 -> 참여 -> 이는 아무 것도 처리하지 않음을 의미
        //   로그 - 트랜잭션 참여
        //   o.s.j.d.DataSourceTransactionManager     : Participating in existing transaction

        log.info("innerTx.isNewTransaction() = {}", innerTx.isNewTransaction());
        //9. 새로운 con 및 tx를 생성하지 않음 - 새로운 status 인스턴스를 획득하는 것처럼 보이지만, 이는 기존 con에 대한 정보에 불과
        //10. 이후 로직에서 tx적용된 con이 필요할 경우 트랜잭션 동기화 매니저에 저장된 기존 con 사용

        log.info("내부 트랜잭션 커밋");
        txManager.commit(innerTx);// *** 새로운 물리 트랜잭션에 참여한 논리 트랜잭션의 commit을 수행했지만, 어떤 로그도 나타나지 않음
        //11. inner로직이 끝나고 트랜잭션 매니저를 통해 innerTx의 commit을 시도
        //12. *** 신규 트랜잭션 여부 확인 - false이므로 commit 호출하지 않음
        //   + *** 실제 물리 commit 이 발생하기 위해서는 이 innerTx의 rollback이 아니어야함
        //         -> 해당 코드 자체는 불필요하지만, rollback이 아님을 인지해야함

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outerTx);
        //13. outer로직 끝난 후 트랜잭션 매니저를 통해 outerTx의 commit을 시도
        //14. 신규 트랜잭션 여부 확인 - true
        //15. DB con에 실제 commit 호출
    }

    @TestConfiguration
    static class TxPropagationTextConfig {

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

}
