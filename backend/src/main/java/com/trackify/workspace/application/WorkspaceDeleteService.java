package com.trackify.workspace.application;

import com.trackify.board.infrastructure.BoardColumnRepository;
import com.trackify.comment.infrastructure.CommentRepository;
import com.trackify.common.exception.ConflictException;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.project.domain.Project;
import com.trackify.project.infrastructure.ProjectMemberRepository;
import com.trackify.project.infrastructure.ProjectRepository;
import com.trackify.task.domain.Task;
import com.trackify.task.infrastructure.TaskDependencyRepository;
import com.trackify.task.infrastructure.TaskRepository;
import com.trackify.workspace.domain.Workspace;
import com.trackify.workspace.domain.WorkspaceRole;
import com.trackify.workspace.infrastructure.WorkspaceMemberRepository;
import com.trackify.workspace.infrastructure.WorkspaceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Deletes a workspace and all its dependent rows in a single transaction (TASK-110).
 *
 * <p>Cascade order:
 * <ol>
 *   <li>For each project: task dependencies, comments, tasks, board columns, project members
 *   <li>All projects in the workspace
 *   <li>All workspace members
 *   <li>The workspace row itself
 * </ol>
 *
 * <p>Guards: only the workspace OWNER may delete; the user's last owned workspace cannot be deleted.
 */
@Service
public class WorkspaceDeleteService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final CommentRepository commentRepository;
    private final BoardColumnRepository boardColumnRepository;

    public WorkspaceDeleteService(WorkspaceRepository workspaceRepository,
                                  WorkspaceMemberRepository memberRepository,
                                  ProjectRepository projectRepository,
                                  ProjectMemberRepository projectMemberRepository,
                                  TaskRepository taskRepository,
                                  TaskDependencyRepository taskDependencyRepository,
                                  CommentRepository commentRepository,
                                  BoardColumnRepository boardColumnRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskRepository = taskRepository;
        this.taskDependencyRepository = taskDependencyRepository;
        this.commentRepository = commentRepository;
        this.boardColumnRepository = boardColumnRepository;
    }

    @Transactional
    public void delete(UUID workspaceId, UUID userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found: " + workspaceId));

        if (!memberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, userId, WorkspaceRole.OWNER)) {
            throw new ForbiddenException("Only workspace owners may delete the workspace.");
        }

        long ownedCount = memberRepository.countByUserIdAndRole(userId, WorkspaceRole.OWNER);
        if (ownedCount <= 1) {
            throw new ConflictException("Cannot delete the last workspace. Create another workspace first.");
        }

        List<Project> projects = projectRepository.findAllByWorkspaceId(workspaceId);
        for (Project project : projects) {
            List<UUID> taskIds = taskRepository.findByProjectId(project.getId())
                    .stream()
                    .map(Task::getId)
                    .toList();
            if (!taskIds.isEmpty()) {
                taskDependencyRepository.deleteByPredecessorTaskIdIn(taskIds);
                taskDependencyRepository.deleteBySuccessorTaskIdIn(taskIds);
                commentRepository.deleteByTaskIdIn(taskIds);
            }
            taskRepository.deleteByProjectId(project.getId());
            boardColumnRepository.deleteByProjectId(project.getId());
            projectMemberRepository.deleteByProjectId(project.getId());
        }

        projectRepository.deleteAll(projects);
        memberRepository.deleteByWorkspaceId(workspaceId);
        workspaceRepository.delete(workspace);
    }
}
