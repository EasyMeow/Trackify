package com.trackify.board.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackify.board.domain.BoardColumn;
import com.trackify.board.infrastructure.BoardColumnRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

/**
 * Proves the TASK-045 default-column seeding contract:
 *
 * <ul>
 *   <li>Exactly 3 columns are persisted for the new project.</li>
 *   <li>Their names are "Todo", "In Progress", "Done" in that order.</li>
 *   <li>Their positions are 0, 1, 2 respectively.</li>
 *   <li>All rows reference the supplied projectId.</li>
 * </ul>
 *
 * <p>Uses Mockito (not a Spring context) to stay fast and consistent with
 * {@code WorkspaceBootstrapServiceTest} and {@code UserRegistrationServiceTest}.
 *
 * <p>Note: {@link BoardColumnService#createDefaultColumns(UUID)} carries
 * {@code @Transactional(propagation = MANDATORY)}. The annotation is a
 * runtime Spring AOP concern and has no effect in a plain Mockito unit test,
 * which is correct — the transactional guarantee is exercised by integration
 * tests that boot a real Spring context with a DataSource.
 */
@ExtendWith(MockitoExtension.class)
class BoardColumnServiceTest {

    @Mock
    private BoardColumnRepository boardColumnRepository;

    private BoardColumnService service;

    @BeforeEach
    void setUp() {
        service = new BoardColumnService(boardColumnRepository);
    }

    @Test
    void createDefaultColumnsPersiststhreeColumnsInOrder() {
        UUID projectId = UUID.randomUUID();

        // Return the argument unchanged so we can capture what was saved.
        when(boardColumnRepository.save(any(BoardColumn.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createDefaultColumns(projectId);

        ArgumentCaptor<BoardColumn> captor = ArgumentCaptor.forClass(BoardColumn.class);
        verify(boardColumnRepository, times(3)).save(captor.capture());

        List<BoardColumn> saved = captor.getAllValues();

        // Exactly 3 columns.
        assertThat(saved).hasSize(3);

        // Names in order.
        assertThat(saved.get(0).getName()).isEqualTo("Todo");
        assertThat(saved.get(1).getName()).isEqualTo("In Progress");
        assertThat(saved.get(2).getName()).isEqualTo("Done");

        // Positions 0, 1, 2.
        assertThat(saved.get(0).getPosition()).isEqualTo(0);
        assertThat(saved.get(1).getPosition()).isEqualTo(1);
        assertThat(saved.get(2).getPosition()).isEqualTo(2);

        // All rows belong to the given project.
        assertThat(saved).allMatch(col -> projectId.equals(col.getProjectId()));
    }

    @Test
    void defaultColumnNamesConstantMatchesExpectedValues() {
        assertThat(BoardColumnService.DEFAULT_COLUMN_NAMES)
                .containsExactly("Todo", "In Progress", "Done");
    }
}
