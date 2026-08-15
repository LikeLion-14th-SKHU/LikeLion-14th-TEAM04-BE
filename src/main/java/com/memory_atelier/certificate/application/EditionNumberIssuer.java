package com.memory_atelier.certificate.application;

import com.memory_atelier.certificate.domain.EditionSequence;
import com.memory_atelier.certificate.domain.repository.EditionSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 에디션 번호(시리얼) 채번. 형식은 [카테고리 코드 1글자][전역 순번 8자리, 36진수] — 예: B00000009
@Component
@RequiredArgsConstructor
public class EditionNumberIssuer {

    private static final int SEQUENCE_LENGTH = 8;
    private static final int RADIX = 36;

    private final EditionSequenceRepository editionSequenceRepository;

    // 행 하나를 저장해 DB가 내주는 자동증가 id를 순번으로 그대로 쓴다 — "다음 값을 읽어서 +1 하고
    // 다시 쓰는" 방식과 달리 동시 요청이 와도 AUTO_INCREMENT가 겹치지 않는 값을 보장한다
    public String issue(String category) {
        long sequence = editionSequenceRepository.save(new EditionSequence()).getId();
        return EditionCategoryCode.resolve(category) + pad(sequence);
    }

    private static String pad(long sequence) {
        String encoded = Long.toString(sequence, RADIX).toUpperCase();
        return "0".repeat(Math.max(0, SEQUENCE_LENGTH - encoded.length())) + encoded;
    }
}
