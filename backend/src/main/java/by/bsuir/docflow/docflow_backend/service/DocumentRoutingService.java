package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentRoute;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.entity.enums.NotificationType;
import by.bsuir.docflow.docflow_backend.entity.enums.RoutingStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.RoutingTarget;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentRoutingService {

    private final DocumentRouteRepository documentRouteRepository;
    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;

    public DocumentRoutingService(
            DocumentRouteRepository documentRouteRepository,
            AppUserRepository appUserRepository,
            NotificationService notificationService
    ) {
        this.documentRouteRepository =
                documentRouteRepository;

        this.appUserRepository =
                appUserRepository;

        this.notificationService =
                notificationService;
    }

    @Transactional
    public DocumentRoute route(
            Document document
    ) {

        if (document == null) {
            throw new IllegalArgumentException(
                    "Документ для маршрутизации отсутствует"
            );
        }

        if (document.getDocumentType() == null) {
            throw new IllegalStateException(
                    "Невозможно маршрутизировать документ без определенного типа"
            );
        }

        DocumentType documentType =
                document.getDocumentType();

        RoutingTarget routingTarget =
                determineTarget(documentType);

        String routingReason =
                determineReason(documentType);

        Optional<DocumentRoute> activeRoute =
                documentRouteRepository
                        .findFirstByDocumentIdAndRoutingStatusOrderByCreatedAtDesc(
                                document.getId(),
                                RoutingStatus.ROUTED
                        );

        if (activeRoute.isPresent()) {

            DocumentRoute currentRoute =
                    activeRoute.get();

            boolean sameDocumentType =
                    currentRoute.getDocumentType()
                            == documentType;

            boolean sameTarget =
                    currentRoute.getRoutingTarget()
                            == routingTarget;

            if (sameDocumentType && sameTarget) {
                return currentRoute;
            }

            currentRoute.setRoutingStatus(
                    RoutingStatus.CANCELLED
            );

            documentRouteRepository.save(
                    currentRoute
            );
        }

        DocumentRoute newRoute =
                new DocumentRoute();

        newRoute.setDocument(document);

        newRoute.setDocumentType(
                documentType
        );

        newRoute.setRoutingTarget(
                routingTarget
        );

        newRoute.setRoutingStatus(
                RoutingStatus.ROUTED
        );

        newRoute.setRoutingReason(
                routingReason
        );

        DocumentRoute savedRoute =
                documentRouteRepository.save(
                        newRoute
                );

        createAssignmentNotifications(
                document,
                savedRoute
        );

        return savedRoute;
    }

    private void createAssignmentNotifications(
            Document document,
            DocumentRoute route
    ) {

        if (
                route.getRoutingTarget()
                == null
        ) {
            return;
        }

        String roleCode =
                route.getRoutingTarget().name();

        List<AppUser> recipients =
                appUserRepository
                        .findByRoleCodeAndEnabledTrue(
                                roleCode
                        );

        String documentLabel =
                buildDocumentLabel(
                        document
                );

        for (AppUser recipient : recipients) {

            String dedupKey =
                    "DOCUMENT_ASSIGNED:"
                            + route.getId();

            notificationService
                    .createNotification(
                            recipient,
                            document,
                            NotificationType.DOCUMENT_ASSIGNED,
                            "Новый документ назначен вам",
                            "Вам назначен документ: " + documentLabel + ".",
                            "/documents/"
                                    + document.getId(),
                            "Открыть документ",
                            dedupKey
                    );
        }
    }

    private String buildDocumentLabel(
            Document document
    ) {

        String typeLabel =
                switch (
                        document.getDocumentType()
                ) {
                    case CONTRACT ->
                            "Договор";

                    case AMENDMENT_CHANGE ->
                            "Дополнительное соглашение";

                    case AMENDMENT_PROLONGATION ->
                            "Дополнительное соглашение";

                    case APPENDIX ->
                            "Приложение";
                };

        if (
                document.getDocumentNumber()
                        != null
                && !document
                .getDocumentNumber()
                .isBlank()
        ) {
            return typeLabel
                    + " №"
                    + document
                    .getDocumentNumber()
                    .trim();
        }

        return typeLabel;
    }

    private RoutingTarget determineTarget(
            DocumentType documentType
    ) {

        return switch (documentType) {
            case CONTRACT ->
                    RoutingTarget.CONTRACT_MANAGER;

            case AMENDMENT_CHANGE ->
                    RoutingTarget.LAWYER;

            case AMENDMENT_PROLONGATION ->
                    RoutingTarget.CONTRACT_MANAGER;

            case APPENDIX ->
                    RoutingTarget.CONTRACT_MANAGER;
        };
    }

    private String determineReason(
            DocumentType documentType
    ) {

        return switch (documentType) {
            case CONTRACT ->
                    "Основной договор направлен менеджеру по договорам";

            case AMENDMENT_CHANGE ->
                    "Дополнительное соглашение с изменениями направлено юристу";

            case AMENDMENT_PROLONGATION ->
                    "Дополнительное соглашение о пролонгации направлено менеджеру по договорам";

            case APPENDIX ->
                    "Приложение к договору направлено менеджеру по договорам";
        };
    }
}