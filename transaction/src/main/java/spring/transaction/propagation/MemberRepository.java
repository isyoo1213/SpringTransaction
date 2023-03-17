package spring.transaction.propagation;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * SpringDataJPA의 repository를 사용하지 않고, 직접 JPA를 사용하게끔 구성
 */

@Slf4j
@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final EntityManager em;

    //@Transactional(propagation = Propagation.REQUIRED)
    public void save(Member member) {
        log.info("member 저장");
        em.persist(member);
    }

    // *** PK를 통한 조회가 아니므로 jpql 사용
    public Optional<Member> find(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList().stream().findAny();
        //findAny() - 가장 먼저 찾은 결과 하나만 반환
        //getSingleResult()로 받을 시 - 결과거 없을 경우 Exception을 반환해버리므로 getResultList()로 받음
    }
}
