package spring.transaction.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class MemberServiceTest {

    @Autowired
    MemberService memberService;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    LogRepository logRepository;

    /**
     * memberService     @Transactional:OFF
     * memberRepository  @Transactional:ON
     * logRepository     @Transactional:ON
     */
    @Test
    void outerTxOff_success() {
        //given
        String username = "outerTxOff_success";

        //when
        memberService.joinV1(username);

        //then : 모든 데이터가 정상 저장된다

        //Assertions - junit의 Assertions를 사용
        // -> Optional 처리 편리하게 가능
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent()); //Log의 message는 username과 동일하게 맞추기로
    }

    /**
     * memberService     @Transactional:OFF
     * memberRepository  @Transactional:ON
     * logRepository     @Transactional:ON + Exception(RuntimeException)
     */
    @Test
    void outerTxOff_fail() {
        //given
        String username = "로그예외_outerTxOff_fail"; // -> contains "로그예외" -> logRepository에서의 RuntimeException

        //when
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        //then : log 데이터는 롤백된다
        //Assertions - junit의 Assertions를 사용
        // -> Optional 처리 편리하게 가능
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty()); //Log의 message는 username과 동일하게 맞추기로
    }

    /**
     * memberService     @Transactional:ON
     * memberRepository  @Transactional:OFF
     * logRepository     @Transactional:OFF
     */
    @Test
    void singleTx() {
        //given
        String username = "onlyOuterTxOn_success";

        //when
        memberService.joinV1(username);

        //then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService     @Transactional:ON
     * memberRepository  @Transactional:ON
     * logRepository     @Transactional:ON
     * 즉, Service 계층에서 물리 outerTx를 생성하고 Repository 계층에서 각각의 논리 innerTx 를 생성하는 경우
     * -> '신규 트랜잭션 여부' 확인이 중요
     * -> 기존의 물리 outerTx 로 participating
     * -> innerTx commit일 경우 실제 commit이 아닌 논리 commit
     */
    @Test
    void outerTxOn_success() {
        //given
        String username = "outerTxOn_success";

        //when
        memberService.joinV1(username);

        //then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService     @Transactional:ON
     * memberRepository  @Transactional:ON
     * logRepository     @Transactional:ON + Exception(RuntimeException / try-catch X)
     * 즉, Service 계층에서 물리 outerTx를 생성하고 Repository 계층에서 각각의 논리 innerTx 를 생성하는 경우
     * -> '신규 트랜잭션 여부' 확인이 중요
     * -> 기존의 물리 outerTx 로 participating
     * -> innerTx Rollback 시, 참여하고 있는 물리 outerTx에 marking 여부 - rollback-only
     */
    @Test
    void outerTxOn_fail() {
        //given
        String username = "로그예외_outerTxOn_fail";

        //when
        // * memberService.joinV1() 은 try-catch로 Exception처리를 하지 않음
        assertThatThrownBy(()->memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        //then : 모든 데이터가 롤백된다
        assertTrue(memberRepository.find(username).isEmpty()); //이제 물리 outerTx 또한 rollback 되는 것을 예상
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * memberService     @Transactional:ON
     * memberRepository  @Transactional:ON
     * logRepository     @Transactional:ON + Exception(RuntimeException / try-catch X)
     * 즉, Service 계층에서 물리 outerTx를 생성하고 Repository 계층에서 각각의 논리 innerTx 를 생성하는 경우
     * -> '신규 트랜잭션 여부' 확인이 중요
     * -> 기존의 물리 outerTx 로 participating
     * -> innerTx Rollback 시, 참여하고 있는 물리 outerTx에 marking 여부 - rollback-only
     */
    @Test
    void recoverException_fail() {
        //given
        String username = "로그예외_recoverException_fail";

        //when
        // * 트랜잭션 상황 가정을 위해 memberService.joinV2() 에도 @Transactional 추가 필요
        // *** 논리 innerTx의 신규 트랜잭션 여부 + rollback-only marking 을 통해 기존 물리 outerTx 자체에 영향
        //     -> try-catch 예외 처리만으로는 물리 outerTx 의 commit 유도할 수 있는 것은 아님
        // -> 정상 흐름으로 Service계층의 로직이 끝나므로 AOP Proxy는 트랜잭션 매니저에 commit 요청
        // -> 트랜잭션 메니저가 정상 흐름으로 반환 + 신규 트랜잭션 여부 확인됐지만, TSM에 저장된 con의 rollback-only 여부 체크
        // -> 트랜잭션 매니저는 UnexpectedRollbackException 을 발생 + AOP Proxy로 던짐
        // -> AOP Proxy는 상위로 해당 Exception을 전달함
        assertThatThrownBy(()->memberService.joinV2(username))
                .isInstanceOf(UnexpectedRollbackException.class);

        //then : 모든 데이터가 롤백된다
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }

}