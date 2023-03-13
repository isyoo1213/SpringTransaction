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
}
