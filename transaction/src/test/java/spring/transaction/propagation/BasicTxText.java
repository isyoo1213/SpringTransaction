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

@Slf4j
@SpringBootTest
class BasicTxText {

    // * Spring은 트랜잭션 매니저도 자동으로 등록해주지만, 이렇게 직접 등록한 Bean을 주입한 경우 구체적인 구현체 주입 가능
    @Autowired
    PlatformTransactionManager txManager;

    @TestConfiguration
    static class Config {
        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Test
    void commit() {
        log.info("트랜잭션 시작");
        //트랜잭션 매니저를 통해 status를 가져오는 것이 트랜잭션의 시작
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

        //로그 - 트랜잭션 생성
        //o.s.j.d.DataSourceTransactionManager
        //: Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
        //로그 - con 획득
        //o.s.j.d.DataSourceTransactionManager
        // : Acquired Connection [HikariProxyConnection@348228202 wrapping conn0
        // : url=jdbc:h2:mem:2c60cd4a-bdae-492f-88f4-f30900e3eb9f user=SA] for JDBC transaction
        //로그 - Switching JDBC Connection to manual commit

        log.info("트랜잭션 커밋 시작");
        txManager.commit(status);
        //로그 - 커밋 시작
        //o.s.j.d.DataSourceTransactionManager     : Initiating transaction commit
        //로그 - 커밋 진행
        //o.s.j.d.DataSourceTransactionManager
        // : Committing JDBC transaction on Connection [HikariProxyConnection@348228202 wrapping conn0
        // : url=jdbc:h2:mem:2c60cd4a-bdae-492f-88f4-f30900e3eb9f user=SA]
        //로그 - Resource release - con을 가져왔던 ConnectionPool
        //o.s.j.d.DataSourceTransactionManager
        // : Releasing JDBC Connection [HikariProxyConnection@348228202 wrapping conn0
        // : url=jdbc:h2:mem:2c60cd4a-bdae-492f-88f4-f30900e3eb9f user=SA] after transaction

        log.info("트랜잭션 커밋 완료");
    }

    @Test
    void rollback() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

        log.info("트랜잭션 롤백 시작");
        txManager.rollback(status);
        //로그 - 롤백 시작
        //o.s.j.d.DataSourceTransactionManager     : Initiating transaction rollback
        //로그 - 롤백 진행
        //o.s.j.d.DataSourceTransactionManager
        // : Rolling back JDBC transaction on Connection [HikariProxyConnection@1476159321 wrapping conn0
        // : url=jdbc:h2:mem:52f29e67-682f-4c42-9ca7-8c612297a08a user=SA]
        //로그 - Release

        log.info("트랜잭션 롤백 완료");
    }

    /**
     * *** 히카리 커넥션 풀로의 con 요청 및 반환
     *      실제 물리 con0을 HikariProxyConnection 이라는 Proxy로 감싼 객체를 반환
     *      -> 획득/반환되어 재사용되는 실제 물리 con0이 아닌, con을 감싸고 있는 Proxy의 인스턴스 정보
     *      -> con0의 반납이 완료되면 이를 감싼 Proxy 인스턴스는 파괴되고, 새로운 요청에 새롭게 생성되어 con0을 재사용
     */
    @Test
    void double_commit() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionDefinition());
        //로그 - Con 정보
        //HikariProxyConnection@54162225 wrapping conn0
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionDefinition());
        //로그 - Con 정보
        //HikariProxyConnection@1799598337 wrapping conn0
        log.info("트랜잭션2 커밋");
        txManager.commit(tx2);
    }

    /**
     * 각각의 트랜잭션이 커넥션 풀에서 획득한 다른 con을 사용하는 상황
     * 트랜잭션 1은 commit / 트랜젹션 2는 rollback
     * 즉, 여기까지는 트랜잭션이 꼬이지 않고 서로 다른 con을 활용한 서로 다른 트랜잭션이 이루어지는 단순한 상황
     */
    @Test
    void double_commit_rollback() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionDefinition());
        //로그 - Con 정보
        //HikariProxyConnection@54162225 wrapping conn0
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionDefinition());
        //로그 - Con 정보
        //HikariProxyConnection@1799598337 wrapping conn0
        log.info("트랜잭션2 롤백");
        txManager.rollback(tx2);
    }
}
