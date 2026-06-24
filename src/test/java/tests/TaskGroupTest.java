package tests;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.testng.Assert;
import org.testng.annotations.Test;

import pages.TaskGroupPage;
import utils.BaseTest;
import utils.TaskGroupCreationHelper;

public class TaskGroupTest extends BaseTest {

    private static final DateTimeFormatter NAME_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Test
    public void shouldCreateTaskGroupSuccessfully() {
        TaskGroupPage taskGroupPage = openTaskGroups();
        String taskGroupName = uniqueTaskGroupName("AutoTaskGroup");

        TaskGroupCreationHelper.createTaskGroup(page, taskGroupPage, taskGroupName,
                "Created by automation: " + taskGroupName, 1);
    }

    @Test
    public void shouldShowValidationWhenTaskGroupNameIsMissing() {
        TaskGroupPage taskGroupPage = openTaskGroups();

        taskGroupPage.clickCreateTaskGroup();
        Assert.assertTrue(taskGroupPage.isCreateFormOpen(), "Create Task Group form should open.");

        taskGroupPage.enterDescription("Validation test without task group name");
        taskGroupPage.clickSave();

        Assert.assertTrue(taskGroupPage.hasValidationMessageContaining("Task Group"),
                "Expected validation message for missing task group name.");
        Assert.assertTrue(taskGroupPage.isCreateFormOpen(),
                "Create Task Group form should remain open after validation.");
    }

    @Test
    public void shouldShowValidationWhenNoTaskIsAdded() {
        TaskGroupPage taskGroupPage = openTaskGroups();

        taskGroupPage.clickCreateTaskGroup();
        Assert.assertTrue(taskGroupPage.isCreateFormOpen(), "Create Task Group form should open.");

        taskGroupPage.enterTaskGroupName(uniqueTaskGroupName("NoTaskGroup"));
        taskGroupPage.enterDescription("Validation test without tasks");
        taskGroupPage.fillRequiredDefaults();
        taskGroupPage.clickSave();

        Assert.assertTrue(taskGroupPage.hasValidationMessageContaining("Task"),
                "Expected validation message for missing task selection.");
        Assert.assertTrue(taskGroupPage.isCreateFormOpen(),
                "Create Task Group form should remain open after validation.");
    }

    @Test
    public void shouldCancelCreateTaskGroup() {
        TaskGroupPage taskGroupPage = openTaskGroups();
        String taskGroupName = uniqueTaskGroupName("CancelTaskGroup");

        taskGroupPage.clickCreateTaskGroup();
        Assert.assertTrue(taskGroupPage.isCreateFormOpen(), "Create Task Group form should open.");

        taskGroupPage.enterTaskGroupName(taskGroupName);
        taskGroupPage.enterDescription("This task group should not be saved");
        taskGroupPage.clickCancel();

        Assert.assertFalse(taskGroupPage.isCreateFormOpen(),
                "Create Task Group form should close after cancel.");
        Assert.assertFalse(taskGroupPage.isTaskGroupVisible(taskGroupName),
                "Cancelled task group should not appear in the listing.");
    }

    @Test
    public void shouldCreateTaskGroupWithMultipleTasks() {
        TaskGroupPage taskGroupPage = openTaskGroups();
        String taskGroupName = uniqueTaskGroupName("MultiTaskGroup");

        TaskGroupCreationHelper.createTaskGroup(page, taskGroupPage, taskGroupName,
                "Created with multiple tasks by automation", 2);
    }

    private TaskGroupPage openTaskGroups() {
        loginWithValidCredentials();

        TaskGroupPage taskGroupPage = new TaskGroupPage(page);
        taskGroupPage.openTaskGroupModule();

        return taskGroupPage;
    }

    private String uniqueTaskGroupName(String prefix) {
        return prefix + "_" + NAME_STAMP.format(LocalDateTime.now());
    }

}
