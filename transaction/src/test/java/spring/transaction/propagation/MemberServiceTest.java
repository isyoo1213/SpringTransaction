package spring.transaction.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

        //then : 모든 데이터가 정상 저장된다

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
        String username = "outerTxOff_success";

        //when
        memberService.joinV1(username);

        //then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

}