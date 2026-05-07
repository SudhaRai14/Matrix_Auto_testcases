package utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.awt.Dimension;
import java.awt.Toolkit;

public final class PlaywrightFactory {
    private static final Path DEFAULT_DRIVER_TMP_DIR = Paths.get("target", "playwright-driver");
    private static final Path BUNDLED_NODE_DIR = Paths.get("target", "playwright-node");
    private static final Path DEFAULT_BROWSERS_DIR = Paths.get("target", "ms-playwright");
    private static final Path HOME_PLAYWRIGHT_CACHE = Paths.get(System.getProperty("user.home"), "Library", "Caches", "ms-playwright");
    private static final Path MAC_CHROME_PATH = Paths.get("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
    private static final Path CACHED_WEBKIT_PATH = HOME_PLAYWRIGHT_CACHE.resolve(Paths.get("webkit-2272", "pw_run.sh"));

    private PlaywrightFactory() {
    }

    public static Playwright createPlaywright() {
        try {
            Files.createDirectories(DEFAULT_DRIVER_TMP_DIR);
            Files.createDirectories(DEFAULT_BROWSERS_DIR);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to create Playwright runtime directories.", ex);
        }

        String resolvedNodePath = System.getProperty("playwright.nodejs.path");
        if (resolvedNodePath == null || resolvedNodePath.isBlank()) {
            resolvedNodePath = extractBundledNode().toAbsolutePath().toString();
        }

        System.setProperty("playwright.nodejs.path", resolvedNodePath);
        System.setProperty("playwright.driver.tmpdir",
                System.getProperty("playwright.driver.tmpdir", DEFAULT_DRIVER_TMP_DIR.toAbsolutePath().toString()));
        System.setProperty("playwright.browsers.path",
                System.getProperty("playwright.browsers.path", DEFAULT_BROWSERS_DIR.toAbsolutePath().toString()));
        System.setProperty("playwright.skipBrowserDownload",
                System.getProperty("playwright.skipBrowserDownload", "true"));

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("PLAYWRIGHT_NODEJS_PATH", System.getProperty("playwright.nodejs.path"));
        env.put("PLAYWRIGHT_BROWSERS_PATH", System.getProperty("playwright.browsers.path"));
        env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", System.getProperty("playwright.skipBrowserDownload"));
        return Playwright.create(new Playwright.CreateOptions().setEnv(env));
    }

    private static Path extractBundledNode() {
        String resourcePath = bundledNodeResourcePath();
        Path extractedNode = BUNDLED_NODE_DIR.resolve(resourcePath);

        if (Files.exists(extractedNode) && Files.isExecutable(extractedNode)) {
            return extractedNode;
        }

        try {
            Files.createDirectories(extractedNode.getParent());
            try (InputStream inputStream = PlaywrightFactory.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    throw new IllegalStateException("Unable to locate bundled Playwright node: " + resourcePath);
                }
                Files.copy(inputStream, extractedNode, StandardCopyOption.REPLACE_EXISTING);
            }
            extractedNode.toFile().setExecutable(true, true);
            return extractedNode;
        } catch (IOException ex) {
            throw new RuntimeException("Unable to extract bundled Playwright node.", ex);
        }
    }

    private static String bundledNodeResourcePath() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String osArch = System.getProperty("os.arch", "").toLowerCase();

        if (osName.contains("mac")) {
            return osArch.contains("aarch64") || osArch.contains("arm64")
                    ? "driver/mac-arm64/node"
                    : "driver/mac/node";
        }
        if (osName.contains("win")) {
            return "driver/win32_x64/node.exe";
        }
        return osArch.contains("aarch64") || osArch.contains("arm64")
                ? "driver/linux-arm64/node"
                : "driver/linux/node";
    }

    public static Browser launchBrowser(Playwright playwright) {
        String browserName = System.getProperty("matrix.browser", defaultBrowserName());
        boolean headless = isHeadlessEnabled();
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(headless);


        Path chromiumExecutable = resolveChromiumExecutable(headless);
        if (chromiumExecutable != null) {
            launchOptions.setExecutablePath(chromiumExecutable);
        } else if (Files.isExecutable(MAC_CHROME_PATH)) {
            launchOptions.setExecutablePath(MAC_CHROME_PATH);
        }

        if ("webkit".equalsIgnoreCase(browserName) && Files.isExecutable(CACHED_WEBKIT_PATH)) {
            try {
                return playwright.webkit().launch(new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setExecutablePath(CACHED_WEBKIT_PATH));
            } catch (PlaywrightException ex) {
                System.err.println("Requested WebKit browser failed to launch; falling back to Chromium. Cause: "
                        + ex.getMessage());
            }
        }

        return playwright.chromium().launch(launchOptions);
    }

    private static String defaultBrowserName() {
        return "chromium";
    }

    private static Path resolveChromiumExecutable(boolean headless) {
        if (headless) {
            Path cachedHeadlessShell = findLatestCachedHeadlessShellExecutable();
            if (cachedHeadlessShell != null) {
                return cachedHeadlessShell;
            }
        }

        Path cachedChromium = findLatestCachedChromiumExecutable();
        if (cachedChromium != null) {
            return cachedChromium;
        }

        return null;
    }

    private static Path findLatestCachedChromiumExecutable() {
        if (!Files.isDirectory(HOME_PLAYWRIGHT_CACHE)) {
            return null;
        }

        try (Stream<Path> directories = Files.list(HOME_PLAYWRIGHT_CACHE)) {
            return directories
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("chromium-"))
                    .sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed())
                    .map(path -> path.resolve(Paths.get(
                            "chrome-mac-arm64",
                            "Google Chrome for Testing.app",
                            "Contents",
                            "MacOS",
                            "Google Chrome for Testing")))
                    .filter(Files::isExecutable)
                    .findFirst()
                    .orElse(null);
        } catch (IOException ex) {
            return null;
        }
    }

    private static Path findLatestCachedHeadlessShellExecutable() {
        if (!Files.isDirectory(HOME_PLAYWRIGHT_CACHE)) {
            return null;
        }

        try (Stream<Path> directories = Files.list(HOME_PLAYWRIGHT_CACHE)) {
            return directories
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("chromium_headless_shell-"))
                    .sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed())
                    .map(path -> path.resolve(Paths.get(
                            "chrome-headless-shell-mac-arm64",
                            "chrome-headless-shell")))
                    .filter(Files::isExecutable)
                    .findFirst()
                    .orElse(null);
        } catch (IOException ex) {
            return null;
        }
    }

    // public static BrowserContext createContext(Browser browser) {
    //     return browser.newContext(new Browser.NewContextOptions().setViewportSize(1920,1080));
    // }

    public static BrowserContext createContext(Browser browser) {

    boolean headless = isHeadlessEnabled();

    Browser.NewContextOptions options = new Browser.NewContextOptions();

    if (!headless) {
        try {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            options.setViewportSize(
                    (int) screenSize.getWidth(),
                    (int) screenSize.getHeight()
            );

            System.out.println("Screen size applied: "
                    + screenSize.getWidth() + "x" + screenSize.getHeight());

        } catch (Exception e) {
            // fallback
            options.setViewportSize(1920, 1080);
        }
    } else {
        // headless fallback
        options.setViewportSize(1920, 1080);
    }

    return browser.newContext(options);
}

    public static BrowserContext createContext(Browser browser, Path storageStatePath) {
        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setViewportSize(1920, 1080);

        if (storageStatePath != null && Files.exists(storageStatePath)) {
            options.setStorageStatePath(storageStatePath);
        }

        return browser.newContext(options);
    }

    public static Page createPage(BrowserContext context) {
        Page page = context.newPage();
        page.setDefaultTimeout(15000);
        page.setDefaultNavigationTimeout(30000);
        return page;
    }

    public static boolean isHeadlessEnabled() {
        String headless = System.getProperty("headless");
        if (headless == null || headless.isBlank() || headless.startsWith("${")) {
            return true;
        }
        return Boolean.parseBoolean(headless);
    }
}
