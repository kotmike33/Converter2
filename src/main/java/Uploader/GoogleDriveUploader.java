package Uploader;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class GoogleDriveUploader {

    private static final String APPLICATION_NAME = "Converter";

    private static final GsonFactory JSON_FACTORY =
            GsonFactory.getDefaultInstance();

    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private final Drive driveService;

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
                createGoogleDriveFolder(folderName);

        System.out.println(
                "Created Google Drive folder. ID: "
                        + googleFolderId
        );

        // Загружаем файлы
        try (var files = Files.list(folder)) {

            files
                    .filter(Files::isRegularFile)
                    .forEach(file -> {

                        try {

                            uploadFile(
                                    file,
                                    googleFolderId
                            );

                        } catch (Exception e) {

                            System.err.println(
                                    "Failed to upload: "
                                            + file
                            );

                            e.printStackTrace();
                        }
                    });
        }

        System.out.println(
                "Upload completed!"
        );
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
}