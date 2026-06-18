package tests;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.Assert;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import utils.BaseTest;
import utils.TaskDataLoader;
import pages.CreateTaskPage;
import pages.WorkorderPage;
import models.TaskData;
//import utils.TaskDataProvider;

public class CreateTaskTest extends BaseTest {

    private static final DateTimeFormatter TASK_NAME_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Test(dataProvider = "taskData")
    public void shouldCreateTask(TaskData taskData) {

        loginWithValidCredentials();

        String taskName =taskData.taskName + "_"
        + TASK_NAME_STAMP.format(LocalDateTime.now());

        WorkorderPage workorderPage = new WorkorderPage(page);
        CreateTaskPage createTaskPage = new CreateTaskPage(page);

        // Navigate to Create Task popup
        workorderPage.navigateToCreateTask();

        // Create Task
        createTaskPage.createTask(taskName,
                taskData.category,
                valueOrFallback(taskData.subCategory, taskData.categoryList),
                taskData.skills,
                valueOrFallback(taskData.unitOfMeasure, taskData.typeOfMeasure),
                valueOrFallback(taskData.measure, taskData.triggerPoint),
                valueOrFallback(taskData.timeInMinutes, taskData.timeToResolve));

        page.screenshot(
                new Page.ScreenshotOptions()
                        .setPath(Paths.get("create-task-result.png")));

        // Validation Errors
        Locator validationError =
                page.locator(".ant-form-item-explain-error:visible");

        if (validationError.count() > 0) {

            throw new IllegalStateException(
                    "Task validation failed: "
                            + validationError.first().innerText());
        }

        String successMessage = createTaskPage.getSuccessMessage();
        Assert.assertTrue(successMessage.toLowerCase().contains("success"),
                "Expected task creation success message but saw: " + successMessage);
        createTaskPage.closeSuccessMessage();
    }

    @DataProvider(name = "taskData")
    public Object[][] taskData() {

    List<TaskData> tasks =
            TaskDataLoader.loadTaskData();

    Object[][] data = new Object[tasks.size()][1];

    for (int i = 0; i < tasks.size(); i++) {
        data[i][0] = tasks.get(i);
    }

    return data;
}

    private String valueOrFallback(String value, String fallback) {

        if (value != null && !value.isBlank()) {
            return value;
        }

        return fallback;
    }
}
