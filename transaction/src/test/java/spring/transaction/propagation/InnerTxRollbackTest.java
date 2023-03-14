package spring.transaction.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;

/**
 * * rollback-only marking
 *   - 물리 Tx는 외부 Tx에서만 컨트롤 가능
 *   - 내부 Tx는 물리 Tx에 컨트롤 불가능 -> Marking
 * * 트랜잭션 매니저의 InnerTx '신규 트랜잭션' 여부 확인
 *   - commit -> 그냥 정상 흐름
 *   - rollback -> Tx적용된 con을 저장하고있는 트랜잭션 동기화 매니저에 해당 con이 'rollbackOnly=true' 표시를 해둠
 * * 트랜잭션 매니저의 OuterTx '신규 트랜잭션' 여부 확인
 *   - 트랜잭션 동기화 매니저에 rollbackOnly= 옵션을 확인
 *   -> 여부에 따라 물리 Tx의 commit/rollback 수행
 * * UnexpectedRollbackException
 *   - Outer Tx의 commit은 개발자가 commit을 기대하는 것이 일반적
 *   -> commit()했으나 실제로는 rollback 됐음을 Exception을 통해 강력하게 전달
 */

@Slf4j
@SpringBootTest
public class InnerTxRollbackTest {

    @Autowired PlatformTransactionManager txManager;

    @Test
    void inner_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outerTx = txManager.getTransaction(new DefaultTransactionDefinition());


        log.info("내부 트랜잭션 시작");
        TransactionStatus innerTx = txManager.getTransaction(new DefaultTransactionDefinition());

        // 이제 innerTx를 롤백하는 상황을 가정

        log.info("내부 트랜잭션 롤백");
        txManager.rollback(innerTx);
        //로그
        // * Tx 참여에 실패 + 존재하는 Tx에 'rollback-only'라는 마킹을 남김
        //  o.s.j.d.DataSourceTransactionManager
        //  : Participating transaction failed
        //  - marking existing transaction as rollback-only
        //로그
        // * Tx의 옵션을 'rollback-only'로 세팅
        //  o.s.j.d.DataSourceTransactionManager
        //  : Setting JDBC transaction [HikariProxyConnection@1865869318 wrapping conn0
        //  : url=jdbc:h2:mem:4bb90515-ad77-4666-9a76-acd1cfdf467d user=SA]
        //  rollback-only

        log.info("외부 트랜잭션 커밋");
        Assertions.assertThatThrownBy(()->txManager.commit(outerTx))
                .isInstanceOf(UnexpectedRollbackException.class);
        //로그
        // * Global Tx가 'rollback-only'로 마킹 but + Tx코드가 commit을 요청 -> rollback 수행
        //  o.s.j.d.DataSourceTransactionManager
        //  : Global transaction is marked as rollback-only but transactional code requested commit
        //  o.s.j.d.DataSourceTransactionManager
        //  : Initiating transaction rollback

        // *** Exception을 던져줌
        //로그
        //  org.springframework.transaction.UnexpectedRollbackException
        //  : Transaction rolled back because it has been marked as rollback-only
    }

    @TestConfiguration
    static class TxPropagationTextConfig {

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

}
