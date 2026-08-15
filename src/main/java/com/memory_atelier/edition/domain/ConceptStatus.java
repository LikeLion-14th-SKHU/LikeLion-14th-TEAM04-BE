package com.memory_atelier.edition.domain;

/**
 * 콘셉트 한 장이 이 배치(Edition Generation) 안에서 어디까지 진행됐는지.
 * 3D 변환({@code MODEL_READY} 상당)은 최종 확정 이후(Certificate 이슈) 단계라 여기 없다.
 */
public enum ConceptStatus {

    /** 행만 만들어진 상태. FastAPI 파이프라인이 아직 후보 이미지를 안 준 상태. */
    PENDING,

    /** 콘셉트 이미지까지 완성(FastAPI의 awaiting_selection 상당). 후보 카드로 제시 가능. */
    IMAGE_READY,

    /** 파이프라인이 실패했거나 job을 못 받음. 사용자에게 재생성을 안내한다. */
    FAILED;

    public boolean hasImage() {
        return this == IMAGE_READY;
    }
}
