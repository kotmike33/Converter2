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
import java.time.format.DateTimeFormatter;
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
                        new FileReader("C:/Users/kotmi/Documents/GitHub/credentials.json")
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
     * Uploads all files from the specified folder
     * to a new folder on Google Drive.
     *
     * @param folderPath local folder path
     */
    public void uploadFolder(String folderPath) throws Exception {

        Path folder = Path.of(folderPath);

        // Check that the folder exists.
        if (!Files.exists(folder)) {
            throw new IllegalArgumentException(
                    "Folder does not exist: " + folderPath
            );
        }

        // Check that the path points to a folder.
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException(
                    "Path is not a folder: " + folderPath
            );
        }

        if(getFolderSizeBytes(folder)>getAvailableDriveBytes()){
            throw new Exception("Not enough space on Google Drive!");
        }

        // Get the folder name.
        String folderName = folder.getFileName().toString();

        System.out.println(
                "Local folder: " + folder
        );

        System.out.println(
                "Google Drive folder: " + folderName
        );

        // Create a folder on Google Drive.
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

        // Upload the files.

        int filesUploaded = 0;
        LocalDateTime startTime = LocalDateTime.now();

        try (var files = Files.walk(folder)) {
            var fileList = files
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();

            int filesCount = fileList.size();

            System.out.println(
                    "Files count for upload: " + filesCount
            );

            int filesProcessed = 0;
            java.util.List<String> failures = new java.util.ArrayList<>();
            for (var file : fileList) {
                try {
                    String driveName = folder.relativize(file).toString().replace('\\', '/');
                    uploadFile(file, googleFolderId, driveName);
                    filesUploaded++;
                } catch (Exception e) {
                    failures.add(folder.relativize(file) + ": " + (e.getMessage() == null ? "upload failed" : e.getMessage()));
                    e.printStackTrace();
                } finally {
                    filesProcessed++;
                    updateProgressBar(startTime, filesProcessed, filesCount);
                }
            }
            if (!failures.isEmpty()) {
                StringBuilder message = new StringBuilder("Uploaded ")
                        .append(filesUploaded).append(" of ").append(filesCount)
                        .append(" files. Failed uploads: ").append(failures.size());
                for (int index = 0; index < Math.min(5, failures.size()); index++) {
                    message.append("\n\n• ").append(failures.get(index));
                }
                PopupFrame.createPopup(message.toString());
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
     * Creates a folder on Google Drive.
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
     * Uploads one file to the specified Google Drive folder.
     */
    private void uploadFile(
            Path path,
            String googleFolderId,
            String driveName
    ) throws Exception {

        String mimeType =
                Files.probeContentType(path);

        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        com.google.api.services.drive.model.File metadata =
                new com.google.api.services.drive.model.File();

        metadata.setName(driveName);

        // Place the file in the created folder.
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
            String estimatedFinish = "Estimated finish " + LocalDateTime.now().plus(d)
                    .format(DateTimeFormatter.ofPattern("HH:mm"));
            AppPage.updateProgress(filesUploaded,filesCount,estimatedFinish,"Uploading");
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

    private long getAvailableDriveBytes() throws Exception {
        var about = driveService.about()
                .get()
                .setFields("storageQuota(limit,usage)")
                .execute();

        Long limit = about.getStorageQuota().getLimit();
        Long usage = about.getStorageQuota().getUsage();

        if (limit == null) {
            return Long.MAX_VALUE;
        }

        return limit - usage;
    }

    private long getFolderSizeBytes(Path folder) throws Exception {
        try (var files = Files.walk(folder)) {
            return files
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .sum();
        }
    }
}
