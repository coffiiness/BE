package com.team.voteland.domain.vote.domain;

import com.team.voteland.core.enums.VoteStatus;
import com.team.voteland.core.enums.VoteType;
import com.team.voteland.domain.vote.api.v1.response.*;
import com.team.voteland.domain.vote.api.v1.request.VoteSubmitRequest;
import com.team.voteland.storage.db.core.BaseEntity;
import com.team.voteland.storage.db.core.vote.*;
import com.team.voteland.support.error.CoreException;
import com.team.voteland.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VoteService {

    private final VoteRepository voteRepository;

    private final VoteOptionRepository voteOptionRepository;

    private final VoteRecordRepository voteRecordRepository;

    @Autowired
    public VoteService(VoteRepository voteRepository, VoteOptionRepository voteOptionRepository,
            VoteRecordRepository voteRecordRepository) {
        this.voteRepository = voteRepository;
        this.voteOptionRepository = voteOptionRepository;
        this.voteRecordRepository = voteRecordRepository;
    }

    /**
     * 투표 생성
     */
    public void createVote(Long userId, String title, String description, VoteType voteType, List<String> options,
            LocalDateTime deadline) {

        // deadline이 현재 시간보다 과거인지 체크
        if (deadline.isBefore(LocalDateTime.now())) {
            throw new CoreException(ErrorType.DEFAULT_ERROR);
        }

        // 옵션이 비었거나 개수가 2개 이하인지 체크
        if (options.isEmpty() || options.size() < 2) {
            throw new CoreException(ErrorType.DEFAULT_ERROR);
        }

        // 제목 빈칸 또는 공백만 있는지 체크
        if (title.isBlank()) {
            throw new CoreException(ErrorType.DEFAULT_ERROR);
        }

        VoteEntity voteEntity = new VoteEntity(userId, title, description, voteType, deadline);
        voteRepository.save(voteEntity);

        List<VoteOptionEntity> voteOptions = new ArrayList<>();
        for (int sequence = 0; sequence < options.size(); sequence++) {
            VoteOptionEntity voteOptionEntity = new VoteOptionEntity(voteEntity.getId(), options.get(sequence),
                    sequence);
            voteOptions.add(voteOptionEntity);
        }
        voteOptionRepository.saveAll(voteOptions);
    }

    /**
     * 투표 조회
     */
    public List<VoteInfo> getVoteInfos() {
        List<Vote> votes = voteRepository.findAll().stream().map(Vote::from).toList();

        List<VoteInfo> voteInfos = new ArrayList<>();
        for (Vote vote : votes) {
            long optionCount = voteOptionRepository.countByVoteId(vote.id());
            long voterCount = voteRecordRepository.countDistinctUserIdByVoteId(vote.id());

            VoteInfo voteInfo = VoteInfo.of(vote, (int) optionCount, (int) voterCount);
            voteInfos.add(voteInfo);
        }

        return voteInfos;
    }

    /**
     * 투표 상세 조회
     */
    // 투표 정보 조회 및 도메인 객체(Vote)로 변환
    public VoteDetailResponse getVoteDetail(Long voteId) {
        VoteEntity voteEntity = voteRepository.findById(voteId)
            .orElseThrow(() -> new IllegalArgumentException("Vote not found"));
        Vote vote = Vote.from(voteEntity);

        // 투표 옵션 조회
        List<VoteOptionEntity> voteOptions = voteOptionRepository.findAllByVoteId(vote.id());
        long voterCount = voteRecordRepository.countDistinctUserIdByVoteId(voteId);

        // 마감까지 남은 시간 계산
        LocalDateTime now = LocalDateTime.now();
        String remainingTime = calculateRemainingTime(now, vote.deadline());

        // 마감 시간이 지났으면 VoteStatus를 '종료(CLOSED)'로 설정
        VoteStatus currentStatus = vote.voteStatus();
        if (now.isAfter(vote.deadline())) {
            currentStatus = VoteStatus.CLOSED;
        }

        // VoteOptionEntity를 DTO로 변환
        List<VoteOptionResponse> items = voteOptions.stream()
            .map(option -> new VoteOptionResponse(option.getId(), option.getContent()))
            .toList();

        // 최종 상세 조회 응답 객체 생성 및 반환
        return new VoteDetailResponse(vote.id(), currentStatus, vote.title(), vote.description(), vote.createdAt(),
                vote.deadline(), remainingTime, vote.voteType(), (int) voterCount, items);
    }

    /**
     * 투표 참여했는지 상태 조회
     */
    public VoteStatusResponse getVoteStatus(Long voteId, Long userId) {
        boolean exists = voteRecordRepository.existsByVoteIdAndUserId(voteId, userId);
        return new VoteStatusResponse(exists);
    }

    // 마감 기한까지 남은 시간을 계산해 문자열로 반환
    private String calculateRemainingTime(LocalDateTime now, LocalDateTime deadline) {
        // 1. 이미 마감된 경우
        if (now.isAfter(deadline)) {
            return "투표 종료";
        }
        // 2. 시간 차이 계산
        java.time.Duration duration = java.time.Duration.between(now, deadline);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        // 3. formating: 1시간 이상 남았으면 '분'까지만, 1시간 미만이면 '초'까지 표시
        if (hours > 0) {
            return String.format("%d시간 %d분 남음", hours, minutes);
        }
        else {
            return String.format("%d분 %d초 남음", minutes, seconds);
        }
    }

    /**
     * 투표 결과 조회
     */
    @Transactional(readOnly = true)
    public VoteResultResponse getVoteResult(Long voteId) {
        VoteEntity voteEntity = voteRepository.findById(voteId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 투표입니다."));
        Vote vote = Vote.from(voteEntity);

        // 옵션 목록 조회
        List<VoteOptionEntity> options = voteOptionRepository.findAllByVoteId(voteId);

        // 총 참여자 수 (응답용)
        Long totalParticipants = voteRecordRepository.countDistinctUserIdByVoteId(voteId);

        // 총 투표 수 (비율 계산용 - 옵션별 득표수의 합)
        Long totalVoteCount = options.stream().mapToLong(VoteOptionEntity::getVoteCount).sum();

        // 결과 리스트 생성 (비율 및 순위 계산)
        List<VoteOptionResultResponse> resultItems = new ArrayList<>();
        for (VoteOptionEntity option : options) {
            double ratio = 0.0;
            if (totalVoteCount > 0) {
                ratio = (double) option.getVoteCount() / totalVoteCount * 100.0;
            }

            // 일단 순위는 나중에 정렬 후 매김 (0으로 초기화)
            resultItems.add(new VoteOptionResultResponse(option.getId(), option.getContent(), option.getVoteCount(),
                    Math.round(ratio * 10.0) / 10.0, // 소수점 첫째 자리 반올림
                    0));
        }

        // 득표수 내림차순 정렬
        resultItems.sort((o1, o2) -> Integer.compare(o2.voteCount(), o1.voteCount()));

        // 순위 매기기
        List<VoteOptionResultResponse> rankedItems = new ArrayList<>();
        int currentRank = 1;
        for (VoteOptionResultResponse item : resultItems) {
            rankedItems.add(new VoteOptionResultResponse(item.id(), item.content(), item.voteCount(), item.voteRatio(),
                    currentRank++));
        }

        // 마지막 업데이트 시간
        LocalDateTime lastUpdatedAt = voteRecordRepository.findTopByVoteIdOrderByCreatedAtDesc(voteId)
            .map(BaseEntity::getCreatedAt)
            .orElse(vote.createdAt());

        // 현재 상태 계산
        LocalDateTime now = LocalDateTime.now();
        VoteStatus currentStatus = now.isBefore(vote.deadline()) ? VoteStatus.OPEN : VoteStatus.CLOSED;

        return new VoteResultResponse(vote.id(), vote.title(), vote.description(), currentStatus,
                totalParticipants.intValue(), vote.deadline(), lastUpdatedAt, rankedItems);
    }

    @Transactional
    public VoteSubmitResponse submitVote(Long voteId, Long userId, VoteSubmitRequest request) {
        // 1. 투표 조회 및 유효성 검사
        VoteEntity voteEntity = voteRepository.findById(voteId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 투표입니다."));
        Vote vote = Vote.from(voteEntity);

        if (LocalDateTime.now().isAfter(vote.deadline())) {
            throw new IllegalArgumentException("이미 종료된 투표입니다.");
        }

        // 2. 이미 참여했는지 확인 (기존 투표 삭제 후 재투표 방식)
        List<VoteRecordEntity> existingRecords = voteRecordRepository.findAllByVoteIdAndUserId(voteId, userId);
        if (!existingRecords.isEmpty()) {
            for (VoteRecordEntity record : existingRecords) {
                // 기존 옵션 득표수 감소 (옵션이 삭제되었을 수도 있으므로 안전하게 처리)
                voteOptionRepository.findById(record.getVoteOptionId()).ifPresent(VoteOptionEntity::decreaseVoteCount);
            }
            // 기존 기록 삭제
            voteRecordRepository.deleteAll(existingRecords);
            // 삭제가 DB에 즉시 반영되도록 flush (같은 옵션을 다시 선택했을 때 Unique Index 충돌 방지)
            voteRecordRepository.flush();
        }

        // 3. 선택한 옵션 유효성 검사
        List<Long> itemIds = request.itemIds();
        if (itemIds == null || itemIds.isEmpty()) {
            throw new IllegalArgumentException("최소 하나 이상의 항목을 선택해야 합니다.");
        }

        // 다중 투표 여부 확인
        if (vote.voteType() == VoteType.SINGLE && itemIds.size() > 1) {
            throw new IllegalArgumentException("중복 선택이 허용되지 않는 투표입니다.");
        }

        // 4. 새로운 투표 기록 저장 및 득표수 증가
        for (Long itemId : itemIds) {
            VoteOptionEntity option = voteOptionRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옵션입니다: " + itemId));

            if (!option.getVoteId().equals(voteId)) {
                throw new IllegalArgumentException("해당 투표의 옵션이 아닙니다.");
            }

            option.increaseVoteCount();
            voteRecordRepository.save(new VoteRecordEntity(voteId, userId, itemId));
        }

        // 5. 응답 생성
        List<VoteOptionResultResponse> optionResponses = new ArrayList<>();

        return new VoteSubmitResponse("투표가 완료되었습니다.", itemIds);
    }

}
