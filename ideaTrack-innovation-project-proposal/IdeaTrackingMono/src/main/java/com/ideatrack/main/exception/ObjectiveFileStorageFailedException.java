package com.ideatrack.main.exception;
import java.nio.file.Path;

public class ObjectiveFileStorageFailedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final transient Path targetPath;

    public ObjectiveFileStorageFailedException(Path targetPath) {
        super(buildMessage(targetPath, null));
        this.targetPath = targetPath;
    }

    public ObjectiveFileStorageFailedException(Path targetPath, Throwable cause) {
        super(buildMessage(targetPath, cause), cause);
        this.targetPath = targetPath;
    }

    private static String buildMessage(Path targetPath, Throwable cause) {
        String base = "File storage failed";
        String pathPart = (targetPath != null) ? " (target=" + targetPath.toString() + ")" : "";
        String causePart = (cause != null && cause.getMessage() != null)
                ? " - cause: " + cause.getMessage()
                : "";
        return base + pathPart + causePart + ".";
    }

    public Path getTargetPath() {
        return targetPath;
    }
}
