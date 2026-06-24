package utils;

import java.util.List;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import models.TaskData;
import pages.CreateTaskPage;
import pages.WorkorderPage;

/** Shared task-seeding workflow for tests that need an independently-created task. */
public final class TaskCreationHelper {

    private TaskCreationHelper() {
    }

    public static String createTask(Page page, String taskName, TaskData taskData) {
        WorkorderPage workorderPage = new WorkorderPage(page);
        CreateTaskPage createTaskPage = new CreateTaskPage(page);

        workorderPage.navigateToCreateTask();
        createTaskPage.createTask(taskName,
                taskData.category,
                valueOrFallback(taskData.subCategory, taskData.categoryList),
                taskData.skills,
                valueOrFallback(taskData.unitOfMeasure, taskData.typeOfMeasure),
                valueOrFallback(taskData.measure, taskData.triggerPoint),
                valueOrFallback(taskData.timeInMinutes, taskData.timeToResolve));

        Locator validationError = page.locator(".ant-form-item-explain-error:visible").first();
        if (validationError.count() > 0) {
            throw new IllegalStateException("Task validation failed: " + validationError.innerText());
        }

        return createTaskPage.getSuccessMessage();
    }

    public static String createTaskUsingDefaultData(Page page, String taskName) {
        List<TaskData> tasks = TaskDataLoader.loadTaskData();
        if (tasks.isEmpty()) {
            throw new IllegalStateException("No task data is configured for task creation.");
        }
        return createTask(page, taskName, tasks.get(0));
    }

    public static void closeSuccessMessage(Page page) {
        new CreateTaskPage(page).closeSuccessMessage();
    }

    private static String valueOrFallback(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
