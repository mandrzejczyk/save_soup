import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SaveSoupApp {

    private static String soupPath;
    private static String downloadFolder;
    private static String fileNumber;
    private static boolean downloadImages;
    private static boolean downloadVideos;
    private static WebDriver driver;
    private static boolean isFinished = false;
    private static String lastPagePath;
    private static boolean saveDescription;
    private static String currentPageAddress;

    public static void main(String[] args) {
        System.out.println(String.format("OPERATION SAVE %s IN PROGRESS!", soupPath));
        setArguments(args);
        if (!downloadImages && !downloadVideos) {
            System.out.println("Nothing to download.");
            System.out.println("Set at least one content type download to true.");
            return;
        }
        createDownloadFolder();
        try {
            setupDriver();
            loadPage(soupPath);
            while (!isFinished) {
                List<WebElement> posts = driver.findElements(By.cssSelector(".post"));
                for (WebElement element : posts) {
                    if (downloadImage(element)) continue;
                    downloadVideo(element);
                }
                loadNextPage();
            }
        } catch (Exception e) {
            System.out.println("Error occurred when downloading your content.");
            System.out.println(String.format("See lastPage.txt in %s for link to resume your download.", lastPagePath));
            System.out.println("Remember to set proper file number to avoid overriding your existing files.");
            driver.quit();
            throw new RuntimeException(e);
        }
        driver.quit();
    }

    private static void setArguments(String[] args) {
        try {
            soupPath = args[0];
            downloadFolder = String.format("%s\\%s", System.getProperty("user.dir"), args[1]);
            fileNumber = args[2];
            downloadImages = Boolean.parseBoolean(args[3]);
            downloadVideos = Boolean.parseBoolean(args[4]);
            saveDescription = Boolean.parseBoolean(args[5]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Please set all 6 parameters.");
            throw new RuntimeException(e);
        }
    }

    private static void setupDriver() throws IOException {
        System.setProperty("webdriver.chrome.driver", new ResourceExtractor().getByName("chromedriver.exe"));
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--allow-insecure-localhost");
        options.addArguments("headless");
        options.addArguments("disable-gpu");
        DesiredCapabilities caps = DesiredCapabilities.chrome();
        caps.setCapability(ChromeOptions.CAPABILITY, options);
        caps.setCapability("acceptInsecureCerts", true);
        driver = new ChromeDriver(caps);
    }

    private static void loadPage(String pageAddress) throws IOException, InterruptedException {
        boolean isLoaded = false;
        int retry = 0;
        while (!isLoaded) {
            if (retry == 5) {
                System.out.println(driver.getCurrentUrl());
                System.out.println(driver.getTitle());
                System.out.println(driver.getPageSource());
                throw new RuntimeException(String.format("Failed to load page %s after 5 attempts", pageAddress));
            }
            driver.get(pageAddress);
            try {
                driver.findElement(By.xpath("//*[.='503 – Hang on a second']"));
                System.out.println("Error 503");
                System.out.println("Waiting for 20s");
                Thread.sleep(20000);
                System.out.println(String.format("Retrying to load page %s", pageAddress));
            } catch (NoSuchElementException e1) {
                try {
                    driver.findElement(By.xpath("//*[.='429 Too Many Requests']"));
                    System.out.println("Error 429");
                    System.out.println("Waiting for 20s");
                    Thread.sleep(20000);
                    System.out.println(String.format("Retrying to load page %s", pageAddress));
                } catch (NoSuchElementException e2) {
                    isLoaded = true;
                    currentPageAddress = pageAddress;
                    lastPagePath = String.format("%s\\lastPage.txt", downloadFolder);
                    Files.write(Paths.get(lastPagePath), pageAddress.getBytes());
                    driver.findElement(By.cssSelector("#avatarcontainer"));
                    System.out.println(String.format("Loaded %s", pageAddress));
                }
            }
            retry++;
        }
    }

    private static void loadNextPage() throws IOException, InterruptedException {
        try {
            String nextPage = driver.findElement(By.cssSelector(".endlessnotice a[onclick='SOUP.Endless.getMoreBelow(); return false;']")).getAttribute("href");
            loadPage(nextPage);
        } catch (NoSuchElementException ex) {
            System.out.println(driver.getCurrentUrl());
            System.out.println(driver.getPageSource());
            System.out.println(String.format("Finished downloading. Last loaded page was %s", currentPageAddress));
            isFinished = true;
        }
    }

    private static void createDownloadFolder() {
        File file = new File(downloadFolder);
        if (!file.exists()) file.mkdir();
    }

    private static boolean downloadImage(WebElement element) throws IOException {
        if (downloadImages) {
            try {
                String downloadPath = element.findElement(By.cssSelector(".content .imagecontainer .lightbox")).getAttribute("href");
                downloadPath = downloadPath.replaceAll("_\\d{3}\\.", ".");
                downloadFile(downloadPath);
                saveDescription(element);
                return true;
            } catch (NoSuchElementException ignored) {
            }
            try {
                String downloadPath = element.findElement(By.cssSelector(".content .imagecontainer img")).getAttribute("src");
                downloadPath = downloadPath.replaceAll("_\\d{3}\\.", ".");
                downloadFile(downloadPath);
                saveDescription(element);
                return true;
            } catch (NoSuchElementException ignored) {
            }
        }
        return false;
    }

    private static void downloadVideo(WebElement element) throws IOException {
        if (downloadVideos) {
            try {
                String downloadPath = element.findElement(By.cssSelector(".content video")).getAttribute("src");
                downloadFile(downloadPath);
                saveDescription(element);
            } catch (NoSuchElementException ignored) {
            }
        }
    }

    private static void saveDescription(WebElement element) throws IOException {
        if (saveDescription) {
            try {
                WebElement description = element.findElement(By.cssSelector(".description"));
                Files.write(Paths.get(String.format("%s\\%s.txt", downloadFolder, fileNumber)), description.getText().getBytes());
            } catch (NoSuchElementException ignore) {
            }
        }
    }

    private static void downloadFile(String downloadPath) {
        try (InputStream in = new URL(downloadPath).openStream()) {
            Files.copy(in, Paths.get(String.format("%s\\%s", downloadFolder, getFileName(downloadPath))));
        } catch (IOException e) {
            System.out.println(String.format("Failed to download file %s", downloadPath));
        }
    }

    private static String getFileName(String downloadPath) {
        Pattern p = Pattern.compile("([^.]+$)");
        Matcher m = p.matcher(downloadPath);
        m.find();
        String fileSuffix = m.group();
        fileNumber = String.format(
                "%0" + fileNumber.length() + "d",
                Integer.parseInt(fileNumber) + 1);
        return String.format("%s.%s", fileNumber, fileSuffix);
    }

    public static class ResourceExtractor {
        public String getByName(String name) throws IOException {
            URL inputUrl = getClass().getResource("/" + name);
            File dest = new File(String.format("%s\\%s", System.getProperty("user.dir"), name));
            if (!dest.exists()) FileUtils.copyURLToFile(inputUrl, dest);
            return dest.getAbsolutePath();
        }
    }
}