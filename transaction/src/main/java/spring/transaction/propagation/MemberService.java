package spring.transaction.propagation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service에서 메서드 단위에 @Transaction을 적용하는 것이 아닌, Repository의 메서드 단위에 @Transactional이 적용된 상황
 * -> 즉 이상황은 각각의 repository 로직이 각각의 트랜잭션을 사용
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final LogRepository logRepository;

    /**
     * 1. 각각의 repository의 내부 메서드에 @Transactionl 적용
     * 2. repository들을 호출하는 메서드에 @Transactional 적용
     */
    @Transactional
    public void joinV1(String username) {
        Member member = new Member(username);
        Log logMessage = new Log(username); //편의상 log 메시지는 username으로

        log.info("=== memberRepository 호출 시작 ===");
        memberRepository.save(member);
        log.info("=== memberRepository 호출 종료 ===");

        log.info("=== logRepository 호출 시작 ===");
        logRepository.save(logMessage);
        log.info("=== logRepository 호출 종료 ===");
    }

    /**
     * logRepository에서 발생한 Exception이 memberRepository에 영향을 주는 것이 싫은 상황
     *  -> logRepository에 try-catch를 적용해 method 단위에서 정상 흐름을 반환하도록 복구
     * 각각의 repository의 내부 메서드에 @Transactionl 적용 중
     */
    @Transactional
    public void joinV2(String username) {
        Member member = new Member(username);
        Log logMessage = new Log(username); //편의상 log 메시지는 username으로

        log.info("=== memberRepository 호출 시작 ===");
        memberRepository.save(member);
        log.info("=== memberRepository 호출 종료 ===");


        log.info("=== logRepository 호출 시작 ===");
        try {
            logRepository.save(logMessage);
        } catch (RuntimeException e) {
            log.info("log 저장에 실패했습니다.", logMessage.getMessage());
            log.info("정상 흐름 반환");
        }
        log.info("=== logRepository 호출 종료 ===");
    }
}
