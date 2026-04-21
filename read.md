# Matrix Login Automation

This project uses Playwright Java with TestNG and Page Object Model (POM) to automate the SmartClean Matrix login flow.

## Covered Scenarios

1. Successful login with valid credentials
2. Error message for invalid credentials
3. Validation for empty username and password
4. Auditor tab validation flow
5. SSO tab navigation and empty email validation

## Project Structure

- `src/main/java/pages/LoginPage.java`
  Page object for the login screen and property selection flow.
- `src/test/java/base/BaseTest.java`
  Shared Playwright setup and teardown.
- `src/test/java/tests/LoginTests.java`
  TestNG test cases for login validation.
- `pom.xml`
  Maven dependencies and test execution setup.
- `testng.xml`
  Test suite definition.

## Run Command

```bash
/opt/homebrew/opt/maven/bin/mvn test -Dmatrix.valid.username='your_username' -Dmatrix.valid.password='your_password'
```

## Notes

- The login page is a client-rendered SPA, so the automation uses explicit waits for the rendered form.
- After valid login, the script selects the first available property and continues to the home page.
- The Auditor tab uses the same username/password style login flow.
- The SSO tab redirects to `#/sso-login` and validates an email field.
- The setup includes a small retry for navigation to handle transient network issues.
