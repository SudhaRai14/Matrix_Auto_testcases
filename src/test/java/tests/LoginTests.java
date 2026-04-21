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

        Assert.assertTrue(
                loginPage.isPropertySelectionDisplayed(),
                "Property selection list should appear after valid credentials are submitted.");

        loginPage.selectFirstPropertyAndLogin();

        Assert.assertTrue(
                loginPage.isLoginSuccessful(),
                "User should land on a post-login page after selecting a property and completing login.");
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

        Assert.assertTrue(
                loginPage.isUsernameValidationDisplayed(),
                "Username required validation should be displayed.");
        Assert.assertTrue(
                loginPage.isPasswordValidationDisplayed(),
                "Password required validation should be displayed.");
    }

    @Test(description = "Verify auditor tab is accessible and uses the login validation flow")
    public void loginAsAuditorValidation() {
        Assert.assertTrue(
                loginPage.isAuditorTabDisplayed(),
                "Auditor tab should be visible on the login page.");

        loginPage.openAuditorTab();
        loginPage.clickLoginWithoutCredentials();

        Assert.assertTrue(
                loginPage.isUsernameValidationDisplayed(),
                "Auditor login should show username validation when credentials are empty.");
        Assert.assertTrue(
                loginPage.isPasswordValidationDisplayed(),
                "Auditor login should show password validation when credentials are empty.");
    }

    @Test(description = "Verify SSO tab opens the SSO page and validates empty email")
    public void loginWithSsoValidation() {
        loginPage.openSsoTab();

        Assert.assertTrue(
                loginPage.isSsoPageDisplayed(),
                "SSO tab should navigate to the SSO login page.");

        loginPage.clickSsoLoginWithoutEmail();

        Assert.assertTrue(
                loginPage.isSsoEmailValidationDisplayed(),
                "SSO login should show email validation when email is empty.");
    }
}
