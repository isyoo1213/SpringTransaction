package spring.transaction.exception;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SpringBootTest
class RollbackTest {

    @Autowired RollbackService rollbackService;

    @Test
    void runtimeException() {
        Assertions.assertThatThrownBy(() -> rollbackService.runtimeException())
                .isInstanceOf(RuntimeException.class);
        // Trnasaction 생성 로그
        //  [    Test worker] o.s.orm.jpa.JpaTransactionManager
        //  : Creating new transaction with name [spring.transaction.exception.RollbackTest$RollbackService.runtimeException]
        //  : PROPAGATION_REQUIRED,ISOLATION_DEFAULT
        //  - Transaction의 이름은 className.methodName으로 정해짐 - RollbackTest$RollbackService.runtimeException
        //  - 뒤에는 생성할 Transaction에 관련된 옵션들을 보여줌
        // Transaction 처리 로그
        //  o.s.orm.jpa.JpaTransactionManager
        //  : Initiating transaction rollback - Rollback 수행
    }

    @Test
    void checkedException() {
        Assertions.assertThatThrownBy(() -> rollbackService.checkedException())
                .isInstanceOf(MyException.class);
        // Transaction 처리 로그
        //  o.s.orm.jpa.JpaTransactionManager
        //  : Initiating transaction commit
    }

    @Test
    void rollbackFor() {
        Assertions.assertThatThrownBy(() -> rollbackService.rollbackFor())
                .isInstanceOf(MyException.class);
        // Transaction 처리 로그
        //  o.s.orm.jpa.JpaTransactionManager
        //  : Initiating transaction rollback
    }


    @TestConfiguration
    static class RollbackTestConfig {
        @Bean
        RollbackService rollbackService() {
            return new RollbackService();
        }
    }

    static class RollbackService {

        // RuntimeException : Rollback
        @Transactional
        public void runtimeException() {
            log.info("call runtimeException");
            throw new RuntimeException();
        }

        // CheckedException : Commit
        // * 예외가 발생했는데 왜 커밋하지?
        @Transactional
        public void checkedException() throws MyException {
            log.info("call checkedException");
            throw new MyException(); //checkedException이므로 catch or throws
        }

        // CheckedException - rollbackFor 지정 : Rollback
        @Transactional(rollbackFor = MyException.class)
        public void rollbackFor() throws MyException {
            log.info("call rollbackFor");
            throw new MyException();
        }

    }

    static class MyException extends Exception {
    }
}
