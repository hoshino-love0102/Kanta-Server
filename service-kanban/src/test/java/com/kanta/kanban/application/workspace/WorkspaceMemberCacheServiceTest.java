package com.kanta.kanban.application.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanta.kanban.domain.workspace.entity.WorkspaceMemberCache;
import com.kanta.kanban.domain.workspace.repository.WorkspaceMemberCacheRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkspaceMemberCacheServiceTest {

    private final WorkspaceMemberCacheRepository repository = mock(WorkspaceMemberCacheRepository.class);
    private final WorkspaceMemberCacheService service = new WorkspaceMemberCacheService(repository);

    @Test
    void memberIds가_비어있으면_빈_맵을_반환한다() {
        var result = service.findDisplayNames(Set.of());

        assertThat(result).isEmpty();
        verify(repository, never()).findByIdInAndActiveTrue(any());
    }

    @Test
    void memberIds가_null이면_빈_맵을_반환한다() {
        var result = service.findDisplayNames(null);

        assertThat(result).isEmpty();
    }

    @Test
    void 활성_멤버의_displayName을_조회한다() {
        var memberId = UUID.randomUUID();
        var member = new WorkspaceMemberCache(memberId, UUID.randomUUID(), "user-1", "홍길동", "MEMBER", true);
        when(repository.findByIdInAndActiveTrue(Set.of(memberId))).thenReturn(List.of(member));

        var result = service.findDisplayNames(Set.of(memberId));

        assertThat(result).containsEntry(memberId, "홍길동");
    }

    @Test
    void upsert는_신규_멤버일_경우_새로_생성하여_저장한다() {
        var memberId = UUID.randomUUID();
        var workspaceId = UUID.randomUUID();
        when(repository.findById(memberId)).thenReturn(Optional.empty());

        service.upsert(memberId, workspaceId, "user-1", "홍길동", "MEMBER", true);

        verify(repository, times(1)).save(any(WorkspaceMemberCache.class));
    }

    @Test
    void upsert는_기존_멤버가_있으면_정보를_갱신하여_저장한다() {
        var memberId = UUID.randomUUID();
        var workspaceId = UUID.randomUUID();
        var existing = new WorkspaceMemberCache(memberId, workspaceId, "user-1", "old-name", "MEMBER", true);
        when(repository.findById(memberId)).thenReturn(Optional.of(existing));

        service.upsert(memberId, workspaceId, "user-1", "새이름", "ADMIN", true);

        assertThat(existing.getDisplayName()).isEqualTo("새이름");
        assertThat(existing.getRole()).isEqualTo("ADMIN");
        verify(repository).save(existing);
    }

    @Test
    void remove는_존재하는_멤버를_비활성화하여_저장한다() {
        var memberId = UUID.randomUUID();
        var existing = new WorkspaceMemberCache(memberId, UUID.randomUUID(), "user-1", "홍길동", "MEMBER", true);
        when(repository.findById(memberId)).thenReturn(Optional.of(existing));

        service.remove(memberId);

        assertThat(existing.isActive()).isFalse();
        verify(repository).save(existing);
    }

    @Test
    void remove는_존재하지_않는_멤버이면_아무것도_하지_않는다() {
        var memberId = UUID.randomUUID();
        when(repository.findById(memberId)).thenReturn(Optional.empty());

        service.remove(memberId);

        verify(repository, never()).save(any(WorkspaceMemberCache.class));
    }
}
