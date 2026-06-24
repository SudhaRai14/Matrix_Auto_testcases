package tests;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.Assert;

import com.microsoft.playwright.Page;
import utils.BaseTest;
import utils.TaskDataLoader;
import utils.TaskCreationHelper;
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

        String successMessage = TaskCreationHelper.createTask(page, taskName, taskData);

        page.screenshot(
                new Page.ScreenshotOptions()
                        .setPath(Paths.get("create-task-result.png")));

        Assert.assertTrue(successMessage.toLowerCase().contains("success"),
                "Expected task creation success message but saw: " + successMessage);
        TaskCreationHelper.closeSuccessMessage(page);
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

}
