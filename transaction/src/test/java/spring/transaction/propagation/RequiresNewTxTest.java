package spring.transaction.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;

/**
 * *** definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW)
 *     - 기존 물리 Tx를 무시하고 새로운 물리 Tx를 생성하도록 옵션 변경
 *     - 옵션의 Default - PROPAGATION_REQUIRED
 */

@Slf4j
@SpringBootTest
public class RequiresNewTxTest {

    @Autowired PlatformTransactionManager txManager;

    @Test
    void inner_rollback_requires_new() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerTx = txManager.getTransaction(new DefaultTransactionDefinition());
        log.info("outer.isNewTransaction() = {}", outerTx.isNewTransaction()); //True

        log.info("내부 트랜잭션 시작");

        // *** 트랜잭션 생성 시, definition에 옵션 설정
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        TransactionStatus innerTx = txManager.getTransaction(definition);
        //로그 - Outter Tx Suspending - 기존 물리 Tx이자 외부 논리Tx를 유보
        //o.s.j.d.DataSourceTransactionManager
        // : Suspending current transaction, creating new transaction with name [null]

        //로그 - 새로운 con 획득 - con1
        //o.s.j.d.DataSourceTransactionManager
        // : Acquired Connection [HikariProxyConnection@4266291 wrapping conn1
        // : url=jdbc:h2:mem:4f44302b-93f8-49db-a8aa-7debaeedd7f2 user=SA] for JDBC transaction

        //로그 - Tx를 위한 commit 모드 전환
        //Switching JDBC Connection [HikariProxyConnection@4266291 wrapping conn1
        // : url=jdbc:h2:mem:4f44302b-93f8-49db-a8aa-7debaeedd7f2 user=SA]
        // to manual commit

        log.info("inner.isNewTransaction() = {}", innerTx.isNewTransaction()); //True
        //로그 - 새로 획득한 '물리' Tx임을 확인
        //s.t.propagation.RequiresNewTxTest
        // : inner.isNewTransaction() = true

        log.info("내부 트랜잭션 롤백");
        txManager.rollback(innerTx);
        //로그 - rollback 시작 및 수행
        //o.s.j.d.DataSourceTransactionManager
        // : Initiating transaction rollback

        //로그 - Release - 물리 Tx인 con1을 커넥션 풀에 반납
        //Releasing JDBC Connection [HikariProxyConnection@4266291 wrapping conn1
        // : url=jdbc:h2:mem:4f44302b-93f8-49db-a8aa-7debaeedd7f2 user=SA] after transaction

        //로그 - Resuming suspended transaction - 기존 물리 Tx이자 외부 Tx인 con0으로 복귀
        //o.s.j.d.DataSourceTransactionManager
        // : Resuming suspended transaction after completion of inner transaction



        log.info("외부 트랜잭션 커밋");
        txManager.commit(outerTx);
    }

    @TestConfiguration
    static class TxPropagationTextConfig {

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

}
