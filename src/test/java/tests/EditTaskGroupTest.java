package tests;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import pages.TaskGroupPage;
import utils.BaseTest;
import utils.TaskGroupCreationHelper;

public class EditTaskGroupTest extends BaseTest {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Test
    public void shouldEditTaskGroupSuccessfully() {
        String originalName = uniqueName("EditTaskGroup");
        Seed seed = createSeedTaskGroup(originalName, 1);
        TaskGroupPage taskGroupPage = seed.page();
        String updatedName = uniqueName("EditedTaskGroup");

        taskGroupPage.openEditTaskGroup(originalName);
        taskGroupPage.enterTaskGroupName(updatedName);
        taskGroupPage.enterDescription("Updated description for " + updatedName);
        taskGroupPage.clickSave();

        Assert.assertTrue(taskGroupPage.isSuccessMessageVisible(), "Expected task group update success message.");
        taskGroupPage.closeSuccessMessageIfPresent();
        taskGroupPage.searchTaskGroup(updatedName);
        Assert.assertTrue(taskGroupPage.isTaskGroupVisible(updatedName), "Updated task group should be listed.");
        taskGroupPage.searchTaskGroup(originalName);
        Assert.assertFalse(taskGroupPage.isTaskGroupVisible(originalName), "Old task group name must not be listed.");
    }

    @Test
    public void shouldShowValidationWhenEditedTaskGroupNameIsEmpty() {
        String name = uniqueName("EmptyEditTaskGroup");
        TaskGroupPage taskGroupPage = createSeedTaskGroup(name, 1).page();

        taskGroupPage.openEditTaskGroup(name);
        taskGroupPage.clearTaskGroupName();
        taskGroupPage.clickSave();

        Assert.assertTrue(taskGroupPage.hasValidationMessageContaining("Task Group"),
                "Expected inline required validation for task group name.");
        Assert.assertTrue(taskGroupPage.isEditFormOpen(), "Edit form should remain open after validation.");
    }

    @Test
    public void shouldCancelEditTaskGroup() {
        String originalName = uniqueName("CancelEditTaskGroup");
        String changedName = uniqueName("CancelledTaskGroup");
        TaskGroupPage taskGroupPage = createSeedTaskGroup(originalName, 1).page();

        taskGroupPage.openEditTaskGroup(originalName);
        taskGroupPage.enterTaskGroupName(changedName);
        taskGroupPage.enterDescription("This description must not be saved");
        taskGroupPage.clickCancel();

        Assert.assertFalse(taskGroupPage.isEditFormOpen(), "Edit form should close after Cancel.");
        taskGroupPage.searchTaskGroup(originalName);
        Assert.assertTrue(taskGroupPage.isTaskGroupVisible(originalName), "Original task group should remain.");
        taskGroupPage.searchTaskGroup(changedName);
        Assert.assertFalse(taskGroupPage.isTaskGroupVisible(changedName), "Cancelled name must not be saved.");
    }

    @Test
    public void shouldAddTaskWhileEditingTaskGroup() {
        String name = uniqueName("AddTaskEditGroup");
        Seed seed = createSeedTaskGroup(name, 1);
        TaskGroupPage taskGroupPage = seed.page();
        String secondTask = seed.created().availableTasks().get(1);

        taskGroupPage.openEditTaskGroup(name);
        taskGroupPage.addTask(secondTask);
        taskGroupPage.clickSave();

        Assert.assertTrue(taskGroupPage.isSuccessMessageVisible(), "Expected success after adding a task.");
        taskGroupPage.closeSuccessMessageIfPresent();
        taskGroupPage.openEditTaskGroup(name);
        Assert.assertTrue(taskGroupPage.isTaskAssociatedWithGroup(secondTask),
                "Second task should be associated with the updated task group.");
    }

    @Test
    public void shouldRemoveTaskWhileEditingTaskGroup() {
        String name = uniqueName("RemoveTaskEditGroup");
        Seed seed = createSeedTaskGroup(name, 2);
        TaskGroupPage taskGroupPage = seed.page();
        List<String> associatedTasks = seed.created().associatedTasks();
        String removedTask = associatedTasks.get(0);
        String remainingTask = associatedTasks.get(1);

        taskGroupPage.openEditTaskGroup(name);
        taskGroupPage.removeTask(removedTask);
        taskGroupPage.clickSave();

        Assert.assertTrue(taskGroupPage.isSuccessMessageVisible(), "Expected success after removing a task.");
        taskGroupPage.closeSuccessMessageIfPresent();
        taskGroupPage.openEditTaskGroup(name);
        Assert.assertFalse(taskGroupPage.isTaskAssociatedWithGroup(removedTask),
                "Removed task must no longer be associated.");
        Assert.assertTrue(taskGroupPage.isTaskAssociatedWithGroup(remainingTask),
                "Remaining task should still be associated.");
    }

    private Seed createSeedTaskGroup(String name, int taskCount) {
        loginWithValidCredentials();
        TaskGroupPage taskGroupPage = new TaskGroupPage(page);
        taskGroupPage.openTaskGroupModule();
        TaskGroupCreationHelper.CreatedTaskGroup created = TaskGroupCreationHelper.createTaskGroup(page, taskGroupPage, name,
                "Created by edit-task-group automation: " + name, taskCount);
        return new Seed(taskGroupPage, created);
    }

    private String uniqueName(String prefix) {
        return prefix + "_" + STAMP.format(LocalDateTime.now());
    }

    private record Seed(TaskGroupPage page, TaskGroupCreationHelper.CreatedTaskGroup created) {
    }
}
