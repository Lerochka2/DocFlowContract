package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentRoute;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.entity.enums.RoutingStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.RoutingTarget;
import by.bsuir.docflow.docflow_backend.repository.DocumentRouteRepository;
import org.springframework.stereotype.Service;

@Service
public class DocumentRoutingService {

    private final DocumentRouteRepository documentRouteRepository;

    public DocumentRoutingService(
            DocumentRouteRepository documentRouteRepository
    ) {
        this.documentRouteRepository = documentRouteRepository;
    }

    public DocumentRoute route(Document document) {

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

        DocumentRoute route =
                new DocumentRoute();

        route.setDocument(document);
        route.setDocumentType(documentType);
        route.setRoutingTarget(routingTarget);
        route.setRoutingStatus(
                RoutingStatus.ROUTED
        );
        route.setRoutingReason(
                routingReason
        );

        return documentRouteRepository.save(route);
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