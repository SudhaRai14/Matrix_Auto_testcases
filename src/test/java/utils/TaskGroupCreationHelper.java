package utils;

import java.util.List;
import java.util.ArrayList;

import com.microsoft.playwright.Page;

import pages.TaskGroupPage;

/** Shared task-group creation workflow for tests that require their own seed group. */
public final class TaskGroupCreationHelper {

    public record CreatedTaskGroup(List<String> associatedTasks, List<String> availableTasks) {
    }

    private TaskGroupCreationHelper() {
    }

    public static CreatedTaskGroup createTaskGroup(
            Page page, TaskGroupPage taskGroupPage, String name, String description, int taskCount) {
        taskGroupPage.clickCreateTaskGroup();
        taskGroupPage.enterTaskGroupName(name);
        taskGroupPage.enterDescription(description);

        taskGroupPage.openTaskDropdown();
        List<String> availableTasks = taskGroupPage.getAvailableTaskNamesFromDropdown(Math.max(taskCount, 2));
        if (availableTasks.size() < taskCount) {
            throw new IllegalStateException("Expected " + taskCount + " available tasks but found: " + availableTasks);
        }
        List<String> tasks = new ArrayList<>(availableTasks.subList(0, taskCount));

        if (taskCount == 1) {
            taskGroupPage.addTask(tasks.get(0));
        } else {
            taskGroupPage.addMultipleTasks(tasks);
        }

        taskGroupPage.fillRequiredDefaults();
        taskGroupPage.clickSave();
        if (!taskGroupPage.isSuccessMessageVisible()) {
            throw new IllegalStateException("Task group creation did not show a success message: " + name);
        }
        taskGroupPage.closeSuccessMessageIfPresent();
        taskGroupPage.searchTaskGroup(name);
        if (!taskGroupPage.isTaskGroupVisible(name)) {
            throw new IllegalStateException("Created task group was not visible: " + name);
        }
        return new CreatedTaskGroup(List.copyOf(tasks), List.copyOf(availableTasks));
    }
}
