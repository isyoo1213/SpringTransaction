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
 *
 */

@Slf4j
@SpringBootTest
public class OuterTxRollbackTest {

    @Autowired PlatformTransactionManager txManager;

    @Test
    void outer_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerTx = txManager.getTransaction(new DefaultTransactionDefinition());

        //if Outer Logic - DB INSERT A

        log.info("내부 트랜잭션 시작");
        TransactionStatus innerTx = txManager.getTransaction(new DefaultTransactionDefinition());

        //if Inner Logic - DB INSERT A

        log.info("내부 트랜잭션 커밋");
        txManager.commit(innerTx);

        // 이제 outerTx를 롤백하는 상황을 가정
        log.info("외부 트랜잭션 롤백");
        txManager.rollback(outerTx);

        // DB INSERT A, B - 둘 모두 Rollback
    }

    @TestConfiguration
    static class TxPropagationTextConfig {

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

}
