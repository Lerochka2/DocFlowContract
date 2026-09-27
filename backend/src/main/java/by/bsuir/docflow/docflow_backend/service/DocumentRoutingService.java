package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.entity.AppUser;
import by.bsuir.docflow.docflow_backend.entity.ContractRiskAnalysis;
import by.bsuir.docflow.docflow_backend.entity.Document;
import by.bsuir.docflow.docflow_backend.entity.DocumentLink;
import by.bsuir.docflow.docflow_backend.entity.DocumentRoute;
import by.bsuir.docflow.docflow_backend.entity.enums.DocumentType;
import by.bsuir.docflow.docflow_backend.entity.enums.NotificationType;
import by.bsuir.docflow.docflow_backend.entity.enums.RoutingStatus;
import by.bsuir.docflow.docflow_backend.entity.enums.RoutingTarget;
import by.bsuir.docflow.docflow_backend.repository.AppUserRepository;
import by.bsuir.docflow.docflow_backend.repository.ContractRiskAnalysisRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentLinkRepository;
import by.bsuir.docflow.docflow_backend.repository.DocumentRouteRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DocumentRoutingService {

    private static final int LAWYER_RISK_SCORE_THRESHOLD =
            60;

    private static final int MAX_REASON_LENGTH =
            500;


    private final DocumentRouteRepository
            documentRouteRepository;

    private final ContractRiskAnalysisRepository
            contractRiskAnalysisRepository;

    private final DocumentLinkRepository
            documentLinkRepository;

    private final AppUserRepository
            appUserRepository;

    private final NotificationService
            notificationService;

    private final ApprovalWorkflowService
            approvalWorkflowService;


    public DocumentRoutingService(
            DocumentRouteRepository documentRouteRepository,
            ContractRiskAnalysisRepository contractRiskAnalysisRepository,
            DocumentLinkRepository documentLinkRepository,
            AppUserRepository appUserRepository,
            NotificationService notificationService,
            ApprovalWorkflowService approvalWorkflowService
    ) {

        this.documentRouteRepository =
                documentRouteRepository;

        this.contractRiskAnalysisRepository =
                contractRiskAnalysisRepository;

        this.documentLinkRepository =
                documentLinkRepository;

        this.appUserRepository =
                appUserRepository;

        this.notificationService =
                notificationService;

        this.approvalWorkflowService =
                approvalWorkflowService;
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

        RoutingDecision decision =
                determineDecision(
                        document
                );

        RoutingTarget routingTarget =
                decision.target();

        String routingReason =
                limitReason(
                        decision.reason()
                );

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
                    currentRoute
                            .getDocumentType()
                            == documentType;

            boolean sameTarget =
                    currentRoute
                            .getRoutingTarget()
                            == routingTarget;

            if (
                    sameDocumentType
                            &&
                    sameTarget
            ) {

                if (
                        !Objects.equals(
                                currentRoute
                                        .getRoutingReason(),
                                routingReason
                        )
                ) {

                    currentRoute.setRoutingReason(
                            routingReason
                    );

                    currentRoute =
                            documentRouteRepository
                                    .save(
                                            currentRoute
                                    );
                }

                approvalWorkflowService
                        .ensureForRoute(
                                document,
                                currentRoute
                        );

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

        newRoute.setDocument(
                document
        );

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

        approvalWorkflowService
                .ensureForRoute(
                        document,
                        savedRoute
                );

        createAssignmentNotifications(
                document,
                savedRoute
        );

        return savedRoute;
    }


    private RoutingDecision determineDecision(
            Document document
    ) {

        return switch (
                document.getDocumentType()
        ) {

            case CONTRACT ->
                    determineContractDecision(
                            document
                    );

            case AMENDMENT_CHANGE ->
                    determineAmendmentChangeDecision(
                            document
                    );

            case AMENDMENT_PROLONGATION ->
                    determineProlongationDecision(
                            document
                    );

            case APPENDIX ->
                    new RoutingDecision(
                            RoutingTarget.CONTRACT_MANAGER,
                            "Приложение к договору направлено "
                                    + "менеджеру по договорам: "
                                    + "юридически значимые изменения "
                                    + "основных условий не предполагаются."
                    );
        };
    }


    private RoutingDecision determineContractDecision(
            Document document
    ) {

        Optional<ContractRiskAnalysis> optionalAnalysis =
                contractRiskAnalysisRepository
                        .findFirstByDocumentIdOrderByAnalyzedAtDescIdDesc(
                                document.getId()
                        );

        if (optionalAnalysis.isEmpty()) {

            return new RoutingDecision(
                    RoutingTarget.CONTRACT_MANAGER,
                    "Основной договор направлен менеджеру "
                            + "по договорам. AI-анализ риска "
                            + "ещё не выполнен; после анализа "
                            + "маршрут будет автоматически уточнён."
            );
        }

        ContractRiskAnalysis analysis =
                optionalAnalysis.get();

        String riskLevel =
                normalizeRiskLevel(
                        analysis.getRiskLevel()
                );

        Integer riskScore =
                analysis.getRiskScore();

        if (
                requiresLawyer(
                        riskLevel,
                        riskScore
                )
        ) {

            return new RoutingDecision(
                    RoutingTarget.LAWYER,
                    "AI-анализ выявил "
                            + riskLevelLabel(riskLevel)
                            + " уровень риска"
                            + formatRiskScore(riskScore)
                            + ". Договор направлен юристу "
                            + "для дополнительной юридической проверки."
            );
        }

        return new RoutingDecision(
                RoutingTarget.CONTRACT_MANAGER,
                "AI-анализ: "
                        + riskLevelLabel(riskLevel)
                        + " уровень риска"
                        + formatRiskScore(riskScore)
                        + ". Договор направлен менеджеру "
                        + "по договорам."
        );
    }


    private RoutingDecision
    determineAmendmentChangeDecision(
            Document document
    ) {

        Optional<Document> contract =
                resolveLinkedContract(
                        document
                );

        if (contract.isEmpty()) {

            return new RoutingDecision(
                    RoutingTarget.LAWYER,
                    "Дополнительное соглашение изменяет "
                            + "условия основного договора "
                            + "и требует юридической проверки."
            );
        }

        if (
                referencedContractMismatch(
                        contract.get(),
                        document
                )
        ) {

            return new RoutingDecision(
                    RoutingTarget.LAWYER,
                    "Реквизиты указанного основного договора "
                            + "не совпадают со связанным договором. "
                            + "Документ направлен юристу."
            );
        }

        int changedFields =
                countChangedFields(
                        contract.get(),
                        document,
                        true
                );

        if (changedFields > 0) {

            return new RoutingDecision(
                    RoutingTarget.LAWYER,
                    "Сравнение с основным договором выявило "
                            + changedFields
                            + " изменённых реквизит(а/ов). "
                            + "Дополнительное соглашение "
                            + "направлено юристу."
            );
        }

        return new RoutingDecision(
                RoutingTarget.LAWYER,
                "Документ классифицирован как дополнительное "
                        + "соглашение с изменением условий. "
                        + "Требуется юридическая проверка."
        );
    }


    private RoutingDecision
    determineProlongationDecision(
            Document document
    ) {

        Optional<Document> contract =
                resolveLinkedContract(
                        document
                );

        if (contract.isEmpty()) {

            return new RoutingDecision(
                    RoutingTarget.CONTRACT_MANAGER,
                    "Дополнительное соглашение о пролонгации "
                            + "направлено менеджеру по договорам."
            );
        }

        Document baseContract =
                contract.get();

        if (
                referencedContractMismatch(
                        baseContract,
                        document
                )
        ) {

            return new RoutingDecision(
                    RoutingTarget.LAWYER,
                    "При пролонгации обнаружено несоответствие "
                            + "реквизитов основного договора. "
                            + "Документ направлен юристу."
            );
        }

        int substantiveChanges =
                countChangedFields(
                        baseContract,
                        document,
                        false
                );

        if (substantiveChanges > 0) {

            return new RoutingDecision(
                    RoutingTarget.LAWYER,
                    "Помимо продления срока обнаружено "
                            + substantiveChanges
                            + (substantiveChanges == 1 ? " изменение существенных условий. " : " изменения существенных условий. ")
                            + "Документ направлен юристу."
            );
        }

        boolean validToChanged =
                changedValue(
                        baseContract.getValidTo(),
                        document.getValidTo()
                );

        if (validToChanged) {

            return new RoutingDecision(
                    RoutingTarget.CONTRACT_MANAGER,
                    "Сравнение с основным договором показало, "
                            + "что изменён только срок действия. "
                            + "Пролонгация направлена менеджеру "
                            + "по договорам."
            );
        }

        return new RoutingDecision(
                RoutingTarget.CONTRACT_MANAGER,
                "Дополнительное соглашение определено "
                        + "как пролонгация без иных существенных "
                        + "изменений. Направлено менеджеру "
                        + "по договорам."
        );
    }


    private boolean requiresLawyer(
            String riskLevel,
            Integer riskScore
    ) {

        if (
                "HIGH".equals(riskLevel)
                        ||
                "CRITICAL".equals(riskLevel)
                        ||
                "VERY_HIGH".equals(riskLevel)
        ) {

            return true;
        }

        return riskScore != null
                &&
                riskScore
                        >= LAWYER_RISK_SCORE_THRESHOLD;
    }


    private String normalizeRiskLevel(
            String value
    ) {

        if (
                value == null
                        ||
                value.isBlank()
        ) {

            return "UNKNOWN";
        }

        return value
                .trim()
                .toUpperCase();
    }


    private String riskLevelLabel(
            String riskLevel
    ) {

        return switch (riskLevel) {

            case "LOW" ->
                    "низкий";

            case "MEDIUM" ->
                    "средний";

            case "HIGH" ->
                    "высокий";

            case "CRITICAL",
                 "VERY_HIGH" ->
                    "критический";

            default ->
                    "неопределённый";
        };
    }


    private String formatRiskScore(
            Integer score
    ) {

        if (score == null) {
            return "";
        }

        return " (" + score + "/100)";
    }


    private Optional<Document> resolveLinkedContract(
            Document document
    ) {

        if (
                document == null
                        ||
                document.getId() == null
        ) {

            return Optional.empty();
        }

        return documentLinkRepository
                .findByChildDocumentId(
                        document.getId()
                )
                .map(
                        DocumentLink::getContractDocument
                )
                .filter(
                        Objects::nonNull
                );
    }


    private int countChangedFields(
            Document contract,
            Document additionalDocument,
            boolean includeValidTo
    ) {

        int changed =
                0;

        if (
                changedAmount(
                        contract.getAmount(),
                        additionalDocument.getAmount()
                )
        ) {
            changed++;
        }

        if (
                changedString(
                        contract.getCurrency(),
                        additionalDocument.getCurrency()
                )
        ) {
            changed++;
        }

        if (
                changedString(
                        contract.getSubject(),
                        additionalDocument.getSubject()
                )
        ) {
            changed++;
        }

        if (
                changedValue(
                        contract.getValidFrom(),
                        additionalDocument.getValidFrom()
                )
        ) {
            changed++;
        }

        if (
                includeValidTo
                        &&
                changedValue(
                        contract.getValidTo(),
                        additionalDocument.getValidTo()
                )
        ) {
            changed++;
        }

        return changed;
    }


    private boolean changedAmount(
            BigDecimal contractValue,
            BigDecimal additionalValue
    ) {

        if (additionalValue == null) {
            return false;
        }

        return contractValue == null
                ||
                contractValue.compareTo(
                        additionalValue
                ) != 0;
    }


    private boolean changedString(
            String contractValue,
            String additionalValue
    ) {

        if (
                additionalValue == null
                        ||
                additionalValue.isBlank()
        ) {

            return false;
        }

        return contractValue == null
                ||
                !contractValue
                        .trim()
                        .equalsIgnoreCase(
                                additionalValue.trim()
                        );
    }


    private boolean changedValue(
            Object contractValue,
            Object additionalValue
    ) {

        if (additionalValue == null) {
            return false;
        }

        return !Objects.equals(
                contractValue,
                additionalValue
        );
    }


    private boolean referencedContractMismatch(
            Document contract,
            Document additionalDocument
    ) {

        String referencedNumber =
                additionalDocument
                        .getReferencedContractNumber();

        if (
                referencedNumber != null
                        &&
                !referencedNumber.isBlank()
                        &&
                (
                        contract.getDocumentNumber() == null
                                ||
                        !contract.getDocumentNumber()
                                .trim()
                                .equalsIgnoreCase(
                                        referencedNumber.trim()
                                )
                )
        ) {

            return true;
        }

        return additionalDocument
                .getReferencedContractDate()
                != null
                &&
                !Objects.equals(
                        contract.getDocumentDate(),
                        additionalDocument
                                .getReferencedContractDate()
                );
    }


    private String limitReason(
            String reason
    ) {

        if (reason == null) {
            return null;
        }

        if (
                reason.length()
                        <= MAX_REASON_LENGTH
        ) {

            return reason;
        }

        return reason.substring(
                0,
                MAX_REASON_LENGTH
        );
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
                route.getRoutingTarget()
                        .name();

        List<AppUser> recipients =
                appUserRepository
                        .findByRoleCodeAndEnabledTrue(
                                roleCode
                        );

        String documentLabel =
                buildDocumentLabel(
                        document
                );

        for (
                AppUser recipient
                : recipients
        ) {

            String dedupKey =
                    "DOCUMENT_ASSIGNED:"
                            + route.getId();

            notificationService
                    .createNotification(
                            recipient,
                            document,
                            NotificationType.DOCUMENT_ASSIGNED,
                            "Новый документ назначен вам",
                            "Вам назначен документ: "
                                    + documentLabel
                                    + ".",
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
                        &&
                !document
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


    private record RoutingDecision(
            RoutingTarget target,
            String reason
    ) {
    }
}