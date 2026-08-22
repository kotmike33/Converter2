package Uploader;

import UI.AppPage;
import UI.PopupFrame;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;

public class GoogleDriveUploader {

    private static final String APPLICATION_NAME = "Converter";

    private static final GsonFactory JSON_FACTORY =
            GsonFactory.getDefaultInstance();

    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private final Drive driveService;

    private static String driveFolderId;

    public GoogleDriveUploader() throws Exception {
        driveService = createDriveService();
    }

    private Drive createDriveService() throws Exception {

        var httpTransport =
                GoogleNetHttpTransport.newTrustedTransport();

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(
                        JSON_FACTORY,
                        new FileReader("credentials.json")
                );

        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport,
                        JSON_FACTORY,
                        clientSecrets,
                        Collections.singletonList(
                                DriveScopes.DRIVE_FILE
                        )
                )
                        .setDataStoreFactory(
                                new FileDataStoreFactory(
                                        new File(TOKENS_DIRECTORY_PATH)
                                )
                        )
                        .setAccessType("offline")
                        .build();

        LocalServerReceiver receiver =
                new LocalServerReceiver.Builder()
                        .setPort(8888)
                        .build();

        Credential credential =
                new AuthorizationCodeInstalledApp(
                        flow,
                        receiver
                ).authorize("user");

        return new Drive.Builder(
                httpTransport,
                JSON_FACTORY,
                credential
        )
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Загружает все файлы из указанной папки
     * в новую папку на Google Drive.
     *
     * @param folderPath путь к папке на компьютере
     */
    public void uploadFolder(String folderPath) throws Exception {

        Path folder = Path.of(folderPath);

        // Проверяем существование
        if (!Files.exists(folder)) {
            throw new IllegalArgumentException(
                    "Folder does not exist: " + folderPath
            );
        }

        // Проверяем, что это папка
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException(
                    "Path is not a folder: " + folderPath
            );
        }

        // Получаем название папки
        String folderName = folder.getFileName().toString();

        System.out.println(
                "Local folder: " + folder
        );

        System.out.println(
                "Google Drive folder: " + folderName
        );

        // Создаём папку на Google Drive
        String googleFolderId =
                findGoogleDriveFolder(folderName);

        if (googleFolderId == null) {

            googleFolderId =
                    createGoogleDriveFolder(folderName);

            System.out.println(
                    "Created Google Drive folder. ID: "
                            + googleFolderId
            );

        } else {

            System.out.println(
                    "Google Drive folder already exists. ID: "
                            + googleFolderId
            );
            throw new Exception("Drive Folder already exists");
        }

        driveFolderId = googleFolderId;

        System.out.println(
                "Created Google Drive folder. ID: "
                        + googleFolderId
        );

        // Загружаем файлы

        int filesUploaded = 0;
        LocalDateTime startTime = LocalDateTime.now();

        try (var files = Files.list(folder)) {
            var fileList = files
                    .filter(Files::isRegularFile)
                    .toList();

            int filesCount = fileList.size();

            System.out.println(
                    "Files count for upload: " + filesCount
            );

            for (var file : fileList) {
                try {
                    uploadFile(file, googleFolderId);
                    filesUploaded++;

                    updateProgressBar(
                            startTime,
                            filesUploaded,
                            filesCount
                    );

                } catch (Exception e) {
                    PopupFrame.createPopup(
                            "Failed to upload: " + file
                    );
                    e.printStackTrace();
                }
            }
        }

        if(filesUploaded>0 && driveFolderId!=null){
            String driveLink =
                    makeFolderPublic(driveFolderId);
            PopupFrame.createLinkPopup(driveLink);
        }

        System.out.println(
                "Upload completed!"
        );
    }

    private String findGoogleDriveFolder(String folderName) throws Exception {

        String query =
                "name = '" + folderName.replace("'", "\\'") + "'" +
                        " and mimeType = 'application/vnd.google-apps.folder'" +
                        " and trashed = false";

        var result = driveService.files()
                .list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id,name)")
                .execute();

        if (result.getFiles().isEmpty()) {
            return null;
        }

        return result.getFiles().get(0).getId();
    }

    /**
     * Создаёт папку на Google Drive.
     */
    private String createGoogleDriveFolder(
            String folderName
    ) throws Exception {

        com.google.api.services.drive.model.File folder =
                new com.google.api.services.drive.model.File();

        folder.setName(folderName);

        folder.setMimeType(
                "application/vnd.google-apps.folder"
        );

        com.google.api.services.drive.model.File createdFolder =
                driveService.files()
                        .create(folder)
                        .setFields("id,name")
                        .execute();

        return createdFolder.getId();
    }

    /**
     * Загружает один файл в указанную папку Google Drive.
     */
    private void uploadFile(
            Path path,
            String googleFolderId
    ) throws Exception {

        String mimeType =
                Files.probeContentType(path);

        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        com.google.api.services.drive.model.File metadata =
                new com.google.api.services.drive.model.File();

        metadata.setName(
                path.getFileName().toString()
        );

        // Помещаем файл в созданную папку
        metadata.setParents(
                Collections.singletonList(
                        googleFolderId
                )
        );

        FileContent mediaContent =
                new FileContent(
                        mimeType,
                        path.toFile()
                );

        com.google.api.services.drive.model.File uploadedFile =
                driveService.files()
                        .create(
                                metadata,
                                mediaContent
                        )
                        .setFields(
                                "id,name,webViewLink"
                        )
                        .execute();

        System.out.println(
                "Uploaded: "
                        + uploadedFile.getName()
        );

        System.out.println(
                "Link: "
                        + uploadedFile.getWebViewLink()
        );
    }

    private void updateProgressBar(LocalDateTime startTime,int filesUploaded,int filesCount){

        if(filesUploaded>0){
            long secondsPassed = Duration.between(startTime, LocalDateTime.now()).getSeconds();
            BigDecimal secondsPerFile = new BigDecimal(secondsPassed)
                    .divide(new BigDecimal(filesUploaded), 10, RoundingMode.HALF_UP);
            BigDecimal secondsLeft = secondsPerFile.multiply(new BigDecimal(filesCount-filesUploaded));
            Duration d = Duration.ofSeconds(secondsLeft.longValue());
            String timeLeft="";
            long h = d.toHours();
            long m = d.toMinutesPart();
            long s = d.toSecondsPart();
            if(h==0 && m!=0){
                timeLeft = String.format("%dm %ds", m, s);
            } else if (h == 0) {
                timeLeft = String.format("%ds", s);
            }else {
                timeLeft = String.format("%dh %dm %ds", h, m, s);
            }
            AppPage.updateProgress(filesUploaded,filesCount,timeLeft,"Uploading");
        }
    }

    private String makeFolderPublic(String folderId) throws Exception {

        com.google.api.services.drive.model.Permission permission =
                new com.google.api.services.drive.model.Permission();

        permission.setType("anyone");
        permission.setRole("reader");

        driveService.permissions()
                .create(folderId, permission)
                .execute();

        return "https://drive.google.com/drive/folders/" + folderId;
    }
}