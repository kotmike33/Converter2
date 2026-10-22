# Converter

A small Swing application for converting images with ImageMagick.

## Run

1. Install ImageMagick and make sure `magick` is available on `PATH` (`magick -version`).
2. Run the application from your IDE, or build it with `mvn package` and run the assembled JAR in `target`.
3. Choose a folder, select the input and output formats, and click **Start**.

The converter searches the selected folder and its subfolders. Converted images are written to a sibling folder named `<selected-folder>-converted`; source files are kept. Existing output files are replaced only after a successful conversion. Each image is processed independently, so one bad or unsupported file does not stop the rest. The completion dialog lists failures.

Settings are stored in `%USERPROFILE%\\.converter2\\config.properties`.

## Notes

- ImageMagick must support the selected input format. RAW formats such as CR2 may require additional ImageMagick delegates.
- Google Drive upload is optional and disabled by default.
- After a file is converted successfully and the output is verified, its original is deleted. Files that fail conversion are kept.
- Conversion progress shows an estimated finish time. You can stop a batch; completed files stay converted, while the active and remaining originals are kept.
- Each batch is saved in a sibling folder named `<source>-converted-<format>`. If that folder exists, the app uses the next free numbered name, such as `-1` or `-2`.
- The results-folder path in the window is clickable. It opens the latest results folder, or creates and opens the next destination so the next batch can use it.
- Google Drive uploads include every file under the results folder, including nested subfolders. Nested paths are retained in each uploaded file name.
