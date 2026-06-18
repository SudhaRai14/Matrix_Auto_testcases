package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class WorkorderPage {

    private final Page page;

    public WorkorderPage(Page page) {
        this.page = page;
    }

    private Locator workorderModule() {
        return page.getByText("Workorders")
                .first();
    }

    private Locator buildingDropdown() {
        return page.locator(".ant-select-selector").first();
    }

    private Locator activitiesTab() {
        return page.locator("span.sidenav-element")
            .filter(new Locator.FilterOptions().setHasText("Activities"));
    }


    private Locator createTaskButton() {
        return page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Create Task"));
    }

    public void openWorkorderModule() {

        workorderModule().click();

        page.waitForURL("**/workorders/**");

        // Allow dashboard to fully render
        page.waitForTimeout(5000);
}

    public void selectFirstBuildingFromTopBar() {

        try {

            buildingDropdown().click();

            page.locator(".ant-select-item-option")
                    .first()
                    .click();

            page.waitForTimeout(1000);

        } catch (Exception e) {

            System.out.println("Building already selected");
        }
    }

    public void openActivitiesTab() {

        page.waitForLoadState();

        for (int i = 0; i < 5; i++) {

            try {

                if (activitiesTab().isVisible()) {

                    activitiesTab().click();

                    return;
                }

            } catch (Exception ignored) {
            }

            page.waitForTimeout(3000);
        }

        throw new RuntimeException(
            "Activities tab not visible after dashboard load");
    }

    public void clickCreateTaskButton() {

        createTaskButton().waitFor();

        createTaskButton().click();
    }

    public void navigateToCreateTask() {

        openWorkorderModule();

        //selectFirstBuildingFromTopBar();

        openActivitiesTab();

        clickCreateTaskButton();
    }
}