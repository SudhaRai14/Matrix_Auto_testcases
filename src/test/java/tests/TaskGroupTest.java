package tests;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import pages.TaskGroupPage;
import utils.BaseTest;

public class TaskGroupTest extends BaseTest {

    private static final DateTimeFormatter NAME_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Test
    public void shouldCreateTaskGroupSuccessfully() {
        TaskGroupPage taskGroupPage = openTaskGroups();
        String taskGroupName = uniqueTaskGroupName("AutoTaskGroup");

        taskGroupPage.clickCreateTaskGroup();
        Assert.assertTrue(taskGroupPage.isCreateFormOpen(), "Create Task Group form should open.");

        taskGroupPage.enterTaskGroupName(taskGroupName);
        taskGroupPage.enterDescription("Created by automation: " + taskGroupName);
        taskGroupPage.addTask(getExistingTaskNameFromDropdown(taskGroupPage));
        taskGroupPage.fillRequiredDefaults();
        taskGroupPage.clickSave();

        Assert.assertTrue(taskGroupPage.isSuccessMessageVisible(),
                "Expected success message after creating task group.");

        taskGroupPage.closeSuccessMessageIfPresent();
        taskGroupPage.searchTaskGroup(taskGroupName);
        Assert.assertTrue(taskGroupPage.isTaskGroupVisible(taskGroupName),
                "Created task group should be visible in listing/search results.");
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

        taskGroupPage.clickCreateTaskGroup();
        Assert.assertTrue(taskGroupPage.isCreateFormOpen(), "Create Task Group form should open.");

        taskGroupPage.enterTaskGroupName(taskGroupName);
        taskGroupPage.enterDescription("Created with multiple tasks by automation");
        taskGroupPage.addMultipleTasks(getExistingTaskNamesFromDropdown(taskGroupPage, 2));
        taskGroupPage.fillRequiredDefaults();
        taskGroupPage.clickSave();

        Assert.assertTrue(taskGroupPage.isSuccessMessageVisible(),
                "Expected success message after creating task group with multiple tasks.");

        taskGroupPage.closeSuccessMessageIfPresent();
        taskGroupPage.searchTaskGroup(taskGroupName);
        Assert.assertTrue(taskGroupPage.isTaskGroupVisible(taskGroupName),
                "Created multi-task group should be visible in listing/search results.");
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

    private String getExistingTaskNameFromDropdown(TaskGroupPage taskGroupPage) {
        taskGroupPage.openTaskDropdown();
        return taskGroupPage.getFirstAvailableTaskName();
    }

    private List<String> getExistingTaskNamesFromDropdown(TaskGroupPage taskGroupPage, int count) {
        taskGroupPage.openTaskDropdown();
        List<String> taskNames = taskGroupPage.getAvailableTaskNamesFromDropdown(count);

        Assert.assertTrue(taskNames.size() >= count,
                "Expected at least " + count + " available tasks but found: " + taskNames);

        return taskNames;
    }
}
