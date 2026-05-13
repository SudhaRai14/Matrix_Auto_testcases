package tests;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import utils.BaseTest;

public class LoginTests extends BaseTest {

    @Test(description = "Verify user can login successfully with valid credentials and property selection")
    public void successfulLoginWithValidCredentials() {
        String username = System.getProperty("matrix.valid.username", System.getenv("MATRIX_VALID_USERNAME"));
        String password = System.getProperty("matrix.valid.password", System.getenv("MATRIX_VALID_PASSWORD"));

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new SkipException("Valid login credentials are not configured for this run.");
        }

        loginPage.login(username, password);

        loginPage.assertPropertySelectionDisplayed();

        loginPage.selectFirstPropertyAndLogin();

        loginPage.assertLoginSuccessful();
    }

    @Test(description = "Verify error message is displayed for invalid credentials")
    public void errorMessageOnInvalidCredentials() {
        loginPage.login("invalid.user@smartclean.io", "InvalidPassword123");

        String errorMessage = loginPage.getErrorMessage();
        Assert.assertFalse(errorMessage.isBlank(), "An error message should be displayed for invalid credentials.");
        Assert.assertTrue(
                errorMessage.toLowerCase().matches(".*(invalid|incorrect|failed|error).*"),
                "Unexpected invalid login message: " + errorMessage);
    }

    @Test(description = "Verify validation messages are displayed when username and password are empty")
    public void validationForEmptyUsernameAndPassword() {
        loginPage.clickLoginWithoutCredentials();

        loginPage.assertUsernameValidationDisplayed();
        loginPage.assertPasswordValidationDisplayed();
    }

    @Test(description = "Verify auditor tab is accessible and uses the login validation flow")
    public void loginAsAuditorValidation() {
        loginPage.assertAuditorTabVisible();

        loginPage.openAuditorTab();
        loginPage.clickLoginWithoutCredentials();

        loginPage.assertUsernameValidationDisplayed();
        loginPage.assertPasswordValidationDisplayed();
    }

    @Test(description = "Verify SSO tab opens the SSO page and validates empty email")
    public void loginWithSsoValidation() {
        loginPage.openSsoTab();

        loginPage.assertSsoPageDisplayed();

        loginPage.clickSsoLoginWithoutEmail();

        loginPage.assertSsoEmailValidationDisplayed();
    }
}
