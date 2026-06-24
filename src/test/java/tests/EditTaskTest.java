package tests;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.testng.Assert;
import org.testng.annotations.Test;

import pages.EditTaskPage;
import utils.TaskCreationHelper;
import utils.BaseTest;

public class EditTaskTest extends BaseTest {

    private static final DateTimeFormatter NAME_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Test
    public void shouldEditTaskSuccessfully() {
        String originalName = uniqueTaskName("EditTask");
        String updatedName = uniqueTaskName("EditedTask");

        EditTaskPage taskPage = createSeedTaskAndOpenTasks(originalName);
        taskPage.openEditTask(originalName);
        taskPage.enterTaskName(updatedName);
        taskPage.clickSave();

        Assert.assertTrue(taskPage.isSuccessMessageVisible(),
                "Expected success message after updating task.");
        taskPage.closeSuccessMessageIfPresent();
        taskPage.searchTask(updatedName);
        Assert.assertTrue(taskPage.isTaskVisible(updatedName),
                "Updated task should be visible in the listing/search result.");
        taskPage.searchTask(originalName);
        Assert.assertFalse(taskPage.isTaskVisible(originalName),
                "Old task name should not be returned after the update.");
    }

    @Test
    public void shouldShowValidationWhenEditedTaskNameIsEmpty() {
        String taskName = uniqueTaskName("EmptyEditTask");

        EditTaskPage taskPage = createSeedTaskAndOpenTasks(taskName);
        taskPage.openEditTask(taskName);
        taskPage.clearTaskName();
        taskPage.clickSave();

        Assert.assertTrue(taskPage.isValidationMessageVisible("Please enter task name"),
                "Expected required validation message for an empty task name.");
        Assert.assertTrue(taskPage.isEditFormOpen(),
                "Edit form should remain open after name validation fails.");
    }

    @Test
    public void shouldCancelEditTask() {
        String originalName = uniqueTaskName("CancelEditTask");
        String changedName = uniqueTaskName("CancelledChange");

        EditTaskPage taskPage = createSeedTaskAndOpenTasks(originalName);

        taskPage.openEditTask(originalName);
        taskPage.enterTaskName(changedName);
        taskPage.clickCancel();

        Assert.assertFalse(taskPage.isEditFormOpen(), "Edit form should close after Cancel.");
        taskPage.searchTask(originalName);
        Assert.assertTrue(taskPage.isTaskVisible(originalName),
                "Original task should remain after cancelling its edit.");
        taskPage.searchTask(changedName);
        Assert.assertFalse(taskPage.isTaskVisible(changedName),
                "Cancelled task name must not appear in search results.");
    }

    private EditTaskPage createSeedTaskAndOpenTasks(String taskName) {
        loginWithValidCredentials();
        String successMessage = TaskCreationHelper.createTaskUsingDefaultData(page, taskName);
        Assert.assertTrue(successMessage.toLowerCase().contains("success"),
                "Expected success message while creating seed task: " + taskName);
        TaskCreationHelper.closeSuccessMessage(page);

        EditTaskPage taskPage = new EditTaskPage(page);
        taskPage.openTaskModule();
        taskPage.searchTask(taskName);
        Assert.assertTrue(taskPage.isTaskVisible(taskName),
                "Seed task should be available before it is edited: " + taskName);
        return taskPage;
    }

    private String uniqueTaskName(String prefix) {
        return prefix + "_" + NAME_STAMP.format(LocalDateTime.now());
    }
}
