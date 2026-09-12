package by.bsuir.docflow.docflow_backend.client;

import by.bsuir.docflow.docflow_backend.dto.ai.AiAnalyzeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Component
public class AiClient {

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final String aiBaseUrl;

    public AiClient(
            JsonMapper jsonMapper,
            @Value("${app.ai.base-url}") String aiBaseUrl
    ) {
        this.jsonMapper = jsonMapper;
        this.aiBaseUrl = aiBaseUrl;

        this.httpClient = HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(
                        Duration.ofSeconds(10)
                )
                .build();
    }

    public AiAnalyzeResponse analyze(Path filePath) {

        if (filePath == null) {
            throw new IllegalArgumentException(
                    "Путь к файлу отсутствует"
            );
        }

        Path normalizedPath = filePath
                .toAbsolutePath()
                .normalize();

        if (!Files.exists(normalizedPath)) {
            throw new IllegalStateException(
                    "Файл для AI-анализа не найден: "
                            + normalizedPath
            );
        }

        if (!Files.isRegularFile(normalizedPath)) {
            throw new IllegalStateException(
                    "Путь не является файлом: "
                            + normalizedPath
            );
        }

        byte[] fileBytes;

        try {
            fileBytes = Files.readAllBytes(
                    normalizedPath
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Не удалось прочитать файл для AI-анализа: "
                            + normalizedPath,
                    exception
            );
        }

        if (fileBytes.length == 0) {
            throw new IllegalStateException(
                    "Файл для AI-анализа пуст"
            );
        }

        String filename = normalizedPath
                .getFileName()
                .toString();

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(
                        URI.create(
                                aiBaseUrl
                                        + "/internal/analyze"
                        )
                )
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(
                        Duration.ofSeconds(60)
                )
                .header(
                        "Content-Type",
                        "application/pdf"
                )
                .header(
                        "Accept",
                        "application/json"
                )
                .header(
                        "X-Filename",
                        filename
                )
                .POST(
                        HttpRequest.BodyPublishers
                                .ofByteArray(
                                        fileBytes
                                )
                )
                .build();

        HttpResponse<String> response;

        try {
            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers
                            .ofString()
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Ошибка соединения с AI-сервисом",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Запрос к AI-сервису был прерван",
                    exception
            );
        }

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new IllegalStateException(
                    "AI-сервис вернул HTTP "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }

        try {
            return jsonMapper.readValue(
                    response.body(),
                    AiAnalyzeResponse.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Не удалось разобрать ответ AI-сервиса",
                    exception
            );
        }
    }
}