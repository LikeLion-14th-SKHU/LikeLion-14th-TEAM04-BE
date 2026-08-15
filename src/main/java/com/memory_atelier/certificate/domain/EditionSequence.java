package com.memory_atelier.certificate.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 에디션 번호(시리얼) 채번용 전역 카운터. 행 하나를 저장해 DB가 내주는 자동증가 id를 그대로 순번으로 쓴다
// (EditionNumberIssuer 참고) — 컬럼이 id뿐이라 다른 엔티티처럼 생성자를 protected + @Builder로 감쌀 이유가 없다
@Entity
@Table(name = "edition_sequences")
@Getter
@NoArgsConstructor
public class EditionSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
