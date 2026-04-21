package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;

import java.util.regex.Pattern;

public class LoginPage {
    private static final double DEFAULT_TIMEOUT_MS = 15000;

    private final Page page;
    private final Locator usernameInput;
    private final Locator passwordInput;
    private final Locator loginButton;
    private final Locator auditorTab;
    private final Locator ssoTab;
    private final Locator ssoEmailInput;
    private final Locator ssoLoginButton;
    private final Locator propertySelect;
    private final Locator propertyOptions;
    private final Locator continueButton;
    private final Locator errorMessage;
    private final Locator homeLoadingIndicator;

    public LoginPage(Page page) {
        this.page = page;
        this.usernameInput = page.locator("input[placeholder='Your Username'], input[type='text'], input[name='username']").first();
        this.passwordInput = page.locator("input[placeholder='Your password'], input[type='password'], input[name='password']").first();
        this.loginButton = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("^login$", Pattern.CASE_INSENSITIVE))).first();
        this.auditorTab = page.locator("#nz-tabs-0-tab-1").or(page.getByText("Auditor", new Page.GetByTextOptions().setExact(true))).first();
        this.ssoTab = page.locator("#nz-tabs-0-tab-2").or(page.getByText("SSO", new Page.GetByTextOptions().setExact(false))).first();
        this.ssoEmailInput = page.locator("input[placeholder='name@example.com'], input[type='email']").first();
        this.ssoLoginButton = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("sso|login", Pattern.CASE_INSENSITIVE))).first();
        this.propertySelect = page.locator("nz-select.ant-select, [data-testid='property-select'], .property-selection nz-select").first();
        this.propertyOptions = page.locator(".ant-select-item-option, [role='option']");
        this.continueButton = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("continue|login", Pattern.CASE_INSENSITIVE))).first();
        this.errorMessage = page.locator(".err_msg, .ant-form-item-explain-error, [role='alert']");
        this.homeLoadingIndicator = page.locator(
                ".ant-spin-spinning, .spinner, .loading, ngx-spinner, [role='progressbar']");
    }

    public void navigate(String url) {
        page.navigate(url);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        waitForLoginForm();
    }

    public void login(String username, String password) {
        waitForLoginForm();
        usernameInput.fill(username);
        passwordInput.fill(password);
        loginButton.click();
    }

    public void loginAndSelectFirstProperty(String username, String password) {
        login(username, password);
        if (isPropertySelectionDisplayed()) {
            selectFirstPropertyAndLogin();
        }
        waitForHomePage();
    }

    public void loginAndSelectProperty(String username, String password, String propertyName) {
        login(username, password);
        if (isPropertySelectionDisplayed()) {
            selectPropertyAndLogin(propertyName);
        }
        waitForHomePage();
    }

    public void clickLoginWithoutCredentials() {
        waitForLoginForm();
        loginButton.click();
    }

    public void openAuditorTab() {
        auditorTab.click();
        waitForLoginForm();
    }

    public boolean isAuditorTabDisplayed() {
        return auditorTab.isVisible();
    }

    public void openSsoTab() {
        ssoTab.click();
        waitForSsoForm();
    }

    public boolean isSsoPageDisplayed() {
        return page.url().contains("#/sso-login") && ssoEmailInput.isVisible();
    }

    public void clickSsoLoginWithoutEmail() {
        waitForSsoForm();
        ssoLoginButton.click();
    }

    public boolean isSsoEmailValidationDisplayed() {
        return errorMessage.first().isVisible()
                && errorMessage.first().innerText().toLowerCase().contains("email");
    }

    public boolean isPropertySelectionDisplayed() {
        try {
            propertySelect.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
            continueButton.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
            return propertySelect.isVisible() && continueButton.isVisible();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public void selectFirstPropertyAndLogin() {
        propertySelect.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        propertySelect.click();

        Locator firstProperty = propertyOptions.first();
        firstProperty.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        selectPropertyOption(firstProperty, null);
        continueButton.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        continueButton.click();
        waitForHomePage();
    }

    public void selectPropertyAndLogin(String propertyName) {
        propertySelect.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        propertySelect.click();

        Locator matchingProperty = propertyOptions
                .filter(new Locator.FilterOptions().setHasText(propertyName))
                .first();
        matchingProperty.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        selectPropertyOption(matchingProperty, propertyName);

        continueButton.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        continueButton.click();
        waitForHomePage();
    }

    public boolean isLoginSuccessful() {
        try {
            waitForHomePage();
            return true;
        } catch (RuntimeException ex) {
            return page.url().contains("#/home");
        }
    }

    public void waitForHomePage() {
        page.waitForURL("**/#/home**", new Page.WaitForURLOptions().setTimeout(DEFAULT_TIMEOUT_MS * 2));
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        waitForHomeContentToRender();
    }

    public void waitForHomeContentToRender() {
        try {
            if (homeLoadingIndicator.count() > 0) {
                homeLoadingIndicator.first().waitFor(new Locator.WaitForOptions()
                        .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                        .setTimeout(DEFAULT_TIMEOUT_MS * 2));
            }
        } catch (RuntimeException ignored) {
            // Some environments keep a detached loader node around; the home URL is the primary guard.
        }
    }

    public String getErrorMessage() {
        errorMessage.first().waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        return errorMessage.first().innerText().trim();
    }

    public boolean isUsernameValidationDisplayed() {
        return errorMessage.first().isVisible()
                && errorMessage.first().innerText().toLowerCase().contains("username");
    }

    public boolean isPasswordValidationDisplayed() {
        String classes = passwordInput.getAttribute("class");
        return classes != null && classes.contains("ng-invalid");
    }

    public void assertLoginFormIsReady() {
        waitForLoginForm();
        PlaywrightAssertions.assertThat(usernameInput).isVisible();
        PlaywrightAssertions.assertThat(passwordInput).isVisible();
        PlaywrightAssertions.assertThat(loginButton).isVisible();
    }

    private void waitForLoginForm() {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        usernameInput.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        passwordInput.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        loginButton.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
    }

    private void waitForSsoForm() {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        ssoEmailInput.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
        ssoLoginButton.waitFor(new Locator.WaitForOptions().setTimeout(DEFAULT_TIMEOUT_MS));
    }

    private void selectPropertyOption(Locator option, String propertyName) {
        Locator searchInput = propertySelect.locator("input").first();
        if (propertyName != null && searchInput.count() > 0) {
            searchInput.fill(propertyName);
        }

        try {
            option.scrollIntoViewIfNeeded();
            option.click();
        } catch (RuntimeException ex) {
            option.click(new Locator.ClickOptions().setForce(true));
        }
    }
}
