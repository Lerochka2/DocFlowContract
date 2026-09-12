package by.bsuir.docflow.docflow_backend.exception;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String message) {
        super(message);
    }
}