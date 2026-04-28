package com.trackify.project.application;

import com.trackify.board.infrastructure.BoardColumnRepository;
import com.trackify.comment.infrastructure.CommentRepository;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.project.domain.Project;
import com.trackify.project.infrastructure.ProjectMemberRepository;
import com.trackify.project.infrastructure.ProjectRepository;
import com.trackify.task.domain.Task;
import com.trackify.task.infrastructure.TaskDependencyRepository;
import com.trackify.task.infrastructure.TaskRepository;
import com.trackify.workspace.infrastructure.WorkspaceMemberRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Deletes a project and all its dependent rows in a single transaction (TASK-095).
 *
 * <p>Cascade order is:
 * <ol>
 *   <li>Dependencies (both outgoing and incoming edges for every task in the project)
 *   <li>Comments for every task in the project
 *   <li>Tasks in the project
 *   <li>Board columns for the project
 *   <li>Project members
 *   <li>The project row itself
 * </ol>
 */
@Service
public class ProjectDeleteService {

    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final CommentRepository commentRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectDeleteService(ProjectRepository projectRepository,
                                WorkspaceMemberRepository workspaceMemberRepository,
                                TaskRepository taskRepository,
                                TaskDependencyRepository taskDependencyRepository,
                                CommentRepository commentRepository,
                                BoardColumnRepository boardColumnRepository,
                                ProjectMemberRepository projectMemberRepository) {
        this.projectRepository = projectRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.taskRepository = taskRepository;
        this.taskDependencyRepository = taskDependencyRepository;
        this.commentRepository = commentRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional
    public void delete(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(project.getWorkspaceId(), userId)) {
            throw new ForbiddenException("Project not accessible");
        }

        List<UUID> taskIds = taskRepository.findByProjectId(projectId)
                .stream()
                .map(Task::getId)
                .toList();

        if (!taskIds.isEmpty()) {
            taskDependencyRepository.deleteByPredecessorTaskIdIn(taskIds);
            taskDependencyRepository.deleteBySuccessorTaskIdIn(taskIds);
            commentRepository.deleteByTaskIdIn(taskIds);
        }

        taskRepository.deleteByProjectId(projectId);
        boardColumnRepository.deleteByProjectId(projectId);
        projectMemberRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
    }
}
