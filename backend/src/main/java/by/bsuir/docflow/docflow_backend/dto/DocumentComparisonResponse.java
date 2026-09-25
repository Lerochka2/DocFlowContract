package by.bsuir.docflow.docflow_backend.dto;

import java.time.LocalDate;
import java.util.List;

public record DocumentComparisonResponse(

        Long additionalDocumentId,
        String additionalDocumentType,
        String additionalDocumentNumber,
        LocalDate additionalDocumentDate,

        Long contractDocumentId,
        String contractDocumentNumber,
        LocalDate contractDocumentDate,

        Boolean referencedContractMatches,

        int changedFieldsCount,

        List<FieldComparison> comparisons
) {

    public record FieldComparison(
            String field,
            String label,
            String contractValue,
            String additionalValue,
            String status
    ) {
    }
}