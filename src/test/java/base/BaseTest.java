package base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.Locator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import pages.LoginPage;
import com.microsoft.playwright.Page.GetByRoleOptions;
import com.microsoft.playwright.options.LoadState;

public class BaseTest {
    protected static final String DEFAULT_URL = "https://www.smartclean.io/matrix/sso/#/login";
    private static final int NAVIGATION_RETRIES = 2;

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;
    protected LoginPage loginPage;

    @BeforeMethod
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(Boolean.parseBoolean(System.getProperty("headless", "true"))));
        context = browser.newContext();
        page = context.newPage();
        page.setDefaultTimeout(15000);
        page.setDefaultNavigationTimeout(30000);
        loginPage = new LoginPage(page);
        navigateWithRetry(System.getProperty("matrix.base.url", DEFAULT_URL));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

   
    private void navigateWithRetry(String url) {
        PlaywrightException lastException = null;

        for (int attempt = 1; attempt <= NAVIGATION_RETRIES; attempt++) {
            try {
                loginPage.navigate(url);
                return;
            } catch (PlaywrightException ex) {
                lastException = ex;
                if (attempt == NAVIGATION_RETRIES) {
                    throw ex;
                }
                page.waitForTimeout(2000L * attempt);
            }
        }

        if (lastException != null) {
            throw lastException;
        }
    }
}
