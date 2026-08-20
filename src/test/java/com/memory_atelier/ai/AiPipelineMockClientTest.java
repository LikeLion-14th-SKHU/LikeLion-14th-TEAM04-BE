package com.memory_atelier.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.memory_atelier.ai.dto.request.PipelineRunRequestDto;
import com.memory_atelier.ai.dto.request.UserInputDto;
import com.memory_atelier.ai.dto.response.JobAcceptedResponseDto;
import com.memory_atelier.ai.dto.response.JobInfoResponseDto;
import com.memory_atelier.global.exception.CustomException;
import org.junit.jupiter.api.Test;

class AiPipelineMockClientTest {

    private final AiPipelineMockClient client = new AiPipelineMockClient(2);

    @Test
    void 파이프라인_실행부터_완료까지_상태가_순서대로_전이한다() {
        UserInputDto.ClothingCategoryDto category = new UserInputDto.ClothingCategoryDto("상의", "니트", null);
        UserInputDto userInput = new UserInputDto(category, "울", java.util.List.of("해짐"), "아빠가 물려준 니트예요.");
        PipelineRunRequestDto request = PipelineRunRequestDto.of("base64-image", userInput, "니트");

        JobAcceptedResponseDto accepted = client.runPipeline(request);
        assertThat(accepted.jobId()).isNotBlank();

        // pollsPerPhase(2)번은 running
        assertThat(client.getJob(accepted.jobId()).status()).isEqualTo(JobInfoResponseDto.STATUS_RUNNING);
        assertThat(client.getJob(accepted.jobId()).status()).isEqualTo(JobInfoResponseDto.STATUS_RUNNING);

        // 그 다음부터는 awaiting_selection, 후보 3개 포함
        JobInfoResponseDto awaiting = client.getJob(accepted.jobId());
        assertThat(awaiting.isAwaitingSelection()).isTrue();
        assertThat(awaiting.result()).containsKey("candidates");

        // 선택하면 다시 running으로 돌아갔다가
        client.selectCandidate(accepted.jobId(), 0);
        assertThat(client.getJob(accepted.jobId()).status()).isEqualTo(JobInfoResponseDto.STATUS_RUNNING);
        assertThat(client.getJob(accepted.jobId()).status()).isEqualTo(JobInfoResponseDto.STATUS_RUNNING);

        // pollsPerPhase번 더 지나면 done, 산출물 URL 포함
        JobInfoResponseDto done = client.getJob(accepted.jobId());
        assertThat(done.isDone()).isTrue();
        assertThat(done.result()).containsKeys("glb_url", "front_image_url");
    }

    @Test
    void 존재하지_않는_job을_조회하면_예외가_발생한다() {
        assertThatThrownBy(() -> client.getJob("no-such-job"))
                .isInstanceOf(CustomException.class);
    }
}
