package by.bsuir.docflow.docflow_backend.service;

import by.bsuir.docflow.docflow_backend.entity.Document;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentMetadataExtractionService {

    private static final String RUSSIAN_MONTHS =
            "января|февраля|марта|апреля|мая|июня|июля|августа|"
                    + "сентября|октября|ноября|декабря";

    private static final String DATE_FRAGMENT =
            "(?:\\d{1,2}[./-]\\d{1,2}[./-]\\d{4}"
                    + "|\\d{1,2}\\s+(?:"
                    + RUSSIAN_MONTHS
                    + ")\\s+\\d{4})";

    private static final Pattern DOCUMENT_NUMBER_PATTERN =
            Pattern.compile(
                    "(?iu)(?:договор|дополнительное\\s+соглашение|"
                            + "соглашение|приложение)"
                            + ".{0,160}?"
                            + "(?:№|N\\s*[OoОоº°]?|#)"
                            + "\\s*"
                            + "([A-ZА-ЯЁ0-9][A-ZА-ЯЁ0-9./_-]{0,99})"
            );

    private static final Pattern DOCUMENT_NUMBER_SLASH_PATTERN =
            Pattern.compile(
                    "(?iu)(?:договор|дополнительное\\s+соглашение|"
                            + "соглашение|приложение)"
                            + ".{0,160}?"
                            + "\\b(\\d{1,10}\\s*/\\s*\\d{1,10})\\b"
            );

    private static final Pattern GENERIC_DOCUMENT_NUMBER_PATTERN =
            Pattern.compile(
                    "(?iu)(?:№|N\\s*[OoОоº°]?|#)"
                            + "\\s*"
                            + "([A-ZА-ЯЁ0-9][A-ZА-ЯЁ0-9./_-]{0,99})"
            );

    private static final Pattern DOCUMENT_DATE_PATTERN =
            Pattern.compile(
                    "(?iu)\\b(?:договор|дополнительное\\s+соглашение|"
                            + "соглашение|приложение)\\b"
                            + ".{0,180}?\\b("
                            + DATE_FRAGMENT
                            + ")\\b"
            );

    private static final Pattern FIRST_DATE_PATTERN =
            Pattern.compile(
                    "(?iu)\\b("
                            + DATE_FRAGMENT
                            + ")\\b"
            );

    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile(
                    "(?iu)(?:общая\\s+стоимость|стоимость|сумма)"
                            + ".{0,120}?"
                            + "(?:составляет|в\\s+размере|равна|:)"
                            + "\\s*"
                            + "(\\d{1,3}(?:[\\s\\u00A0]\\d{3})*"
                            + "(?:[.,]\\d{1,2})?)"
            );

    private static final Pattern SUBJECT_PATTERN =
            Pattern.compile(
                    "(?iu)(?:оказать\\s+услуги\\s+по"
                            + "|выполнить\\s+работы\\s+по"
                            + "|предметом\\s+(?:настоящего\\s+)?договора"
                            + "\\s+является)"
                            + "\\s+(.+?)"
                            + "(?=,\\s*(?:а\\s+)?заказчик\\b"
                            + "|\\.\\s*\\d+\\.\\d+\\."
                            + "|\\.)"
            );

    private static final Pattern VALID_FROM_PATTERN =
            Pattern.compile(
                    "(?iu)(?:вступает\\s+в\\s+силу\\s+с"
                            + "|действует\\s+с"
                            + "|срок\\s+действия.{0,60}?\\sс)"
                            + "\\s+("
                            + DATE_FRAGMENT
                            + ")"
            );

    private static final Pattern VALID_TO_PATTERN =
            Pattern.compile(
                    "(?iu)(?:действует\\s+до"
                            + "|срок\\s+действия.{0,80}?до)"
                            + "\\s+("
                            + DATE_FRAGMENT
                            + ")"
            );

    private static final Pattern REFERENCED_CONTRACT_PATTERN =
            Pattern.compile(
                    "(?iu)(?:к|по)\\s+"
                            + "(?:основному\\s+)?договору\\s*"
                            + "(?:№|N\\s*[OoОоº°]?|#)?\\s*"
                            + "([A-ZА-ЯЁ0-9][A-ZА-ЯЁ0-9./_-]{0,99})"
                            + "(?:\\s+от\\s+("
                            + DATE_FRAGMENT
                            + "))?"
            );

    private static final Pattern BYN_PATTERN =
            Pattern.compile(
                    "(?iu)\\bBYN\\b"
                            + "|белорусск\\w*\\s+рубл\\w*"
            );

    private static final Pattern USD_PATTERN =
            Pattern.compile(
                    "(?iu)\\bUSD\\b"
                            + "|доллар\\w*\\s+США"
            );

    private static final Pattern EUR_PATTERN =
            Pattern.compile(
                    "(?iu)\\bEUR\\b"
                            + "|\\bевро\\b"
            );

    private static final Map<String, Integer> MONTHS =
            Map.ofEntries(
                    Map.entry("января", 1),
                    Map.entry("февраля", 2),
                    Map.entry("марта", 3),
                    Map.entry("апреля", 4),
                    Map.entry("мая", 5),
                    Map.entry("июня", 6),
                    Map.entry("июля", 7),
                    Map.entry("августа", 8),
                    Map.entry("сентября", 9),
                    Map.entry("октября", 10),
                    Map.entry("ноября", 11),
                    Map.entry("декабря", 12)
            );

    public void enrichFromRecognizedText(
            Document document,
            String recognizedText
    ) {

        if (
                document == null
                        || recognizedText == null
                        || recognizedText.isBlank()
        ) {
            return;
        }

        String text =
                normalizeText(
                        recognizedText
                );

        fillDocumentNumber(
                document,
                text
        );

        fillDocumentDate(
                document,
                text
        );

        fillAmountAndCurrency(
                document,
                text
        );

        fillSubject(
                document,
                text
        );

        fillValidity(
                document,
                text
        );

        fillReferencedContract(
                document,
                text
        );
    }

    private void fillDocumentNumber(
            Document document,
            String text
    ) {

        if (!isBlank(
                document.getDocumentNumber()
        )) {
            return;
        }

        String number =
                findValue(
                        DOCUMENT_NUMBER_PATTERN,
                        text,
                        1
                );

        if (number == null) {

            number =
                    findValue(
                            DOCUMENT_NUMBER_SLASH_PATTERN,
                            text,
                            1
                    );
        }

        if (number == null) {

            number =
                    findValue(
                            GENERIC_DOCUMENT_NUMBER_PATTERN,
                            text,
                            1
                    );
        }

        if (number == null) {
            return;
        }

        number =
                number.replaceAll(
                        "\\s*/\\s*",
                        "/"
                );

        document.setDocumentNumber(
                cleanValue(number)
        );
    }

    private void fillDocumentDate(
            Document document,
            String text
    ) {

        if (document.getDocumentDate() != null) {
            return;
        }

        LocalDate date =
                findDate(
                        DOCUMENT_DATE_PATTERN,
                        text,
                        1
                );

        if (date == null) {

            date =
                    findDate(
                            FIRST_DATE_PATTERN,
                            text,
                            1
                    );
        }

        if (date != null) {

            document.setDocumentDate(
                    date
            );
        }
    }

    private void fillAmountAndCurrency(
            Document document,
            String text
    ) {

        if (document.getAmount() == null) {

            Matcher matcher =
                    AMOUNT_PATTERN
                            .matcher(text);

            if (matcher.find()) {

                BigDecimal amount =
                        parseAmount(
                                matcher.group(1)
                        );

                if (amount != null) {

                    document.setAmount(
                            amount
                    );
                }
            }
        }

        if (isBlank(
                document.getCurrency()
        )) {

            String currency =
                    detectCurrency(
                            text
                    );

            if (currency != null) {

                document.setCurrency(
                        currency
                );
            }
        }
    }

    private void fillSubject(
            Document document,
            String text
    ) {

        if (!isBlank(
                document.getSubject()
        )) {
            return;
        }

        Matcher matcher =
                SUBJECT_PATTERN
                        .matcher(text);

        if (!matcher.find()) {
            return;
        }

        String subject =
                cleanValue(
                        matcher.group(1)
                );

        if (subject == null) {
            return;
        }

        if (subject.length() > 1000) {

            subject =
                    subject.substring(
                            0,
                            1000
                    );
        }

        document.setSubject(
                subject
        );
    }

    private void fillValidity(
            Document document,
            String text
    ) {

        if (document.getValidFrom() == null) {

            LocalDate validFrom =
                    findDate(
                            VALID_FROM_PATTERN,
                            text,
                            1
                    );

            if (validFrom != null) {

                document.setValidFrom(
                        validFrom
                );
            }
        }

        if (document.getValidTo() == null) {

            LocalDate validTo =
                    findDate(
                            VALID_TO_PATTERN,
                            text,
                            1
                    );

            if (validTo != null) {

                document.setValidTo(
                        validTo
                );
            }
        }
    }

    private void fillReferencedContract(
            Document document,
            String text
    ) {

        if (
                !isBlank(
                        document.getReferencedContractNumber()
                )
                        && document.getReferencedContractDate() != null
        ) {
            return;
        }

        Matcher matcher =
                REFERENCED_CONTRACT_PATTERN
                        .matcher(text);

        if (!matcher.find()) {
            return;
        }

        if (isBlank(
                document.getReferencedContractNumber()
        )) {

            document.setReferencedContractNumber(
                    cleanValue(
                            matcher.group(1)
                    )
            );
        }

        if (
                document.getReferencedContractDate() == null
                        && matcher.group(2) != null
        ) {

            LocalDate referencedDate =
                    parseDate(
                            matcher.group(2)
                    );

            if (referencedDate != null) {

                document.setReferencedContractDate(
                        referencedDate
                );
            }
        }
    }

    private String findValue(
            Pattern pattern,
            String text,
            int group
    ) {

        Matcher matcher =
                pattern.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(group);
    }

    private LocalDate findDate(
            Pattern pattern,
            String text,
            int group
    ) {

        String value =
                findValue(
                        pattern,
                        text,
                        group
                );

        if (value == null) {
            return null;
        }

        return parseDate(value);
    }

    private LocalDate parseDate(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        String normalized =
                value
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replace('/', '.')
                        .replace('-', '.');

        if (normalized.matches(
                "\\d{1,2}\\.\\d{1,2}\\.\\d{4}"
        )) {

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "d.M.uuuu"
                    );

            try {

                return LocalDate.parse(
                        normalized,
                        formatter
                );

            } catch (
                    DateTimeParseException exception
            ) {
                return null;
            }
        }

        Matcher russianDate =
                Pattern.compile(
                        "(?iu)(\\d{1,2})\\s+("
                                + RUSSIAN_MONTHS
                                + ")\\s+(\\d{4})"
                )
                .matcher(normalized);

        if (!russianDate.find()) {
            return null;
        }

        int day =
                Integer.parseInt(
                        russianDate.group(1)
                );

        Integer month =
                MONTHS.get(
                        russianDate.group(2)
                                .toLowerCase(
                                        Locale.ROOT
                                )
                );

        int year =
                Integer.parseInt(
                        russianDate.group(3)
                );

        if (month == null) {
            return null;
        }

        try {

            return LocalDate.of(
                    year,
                    month,
                    day
            );

        } catch (RuntimeException exception) {
            return null;
        }
    }

    private BigDecimal parseAmount(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        String normalized =
                value
                        .replace("\u00A0", "")
                        .replaceAll("\\s+", "")
                        .replace(',', '.');

        try {

            return new BigDecimal(
                    normalized
            );

        } catch (
                NumberFormatException exception
        ) {
            return null;
        }
    }

    private String detectCurrency(
            String text
    ) {

        if (
                BYN_PATTERN
                        .matcher(text)
                        .find()
        ) {
            return "BYN";
        }

        if (
                USD_PATTERN
                        .matcher(text)
                        .find()
        ) {
            return "USD";
        }

        if (
                EUR_PATTERN
                        .matcher(text)
                        .find()
        ) {
            return "EUR";
        }

        return null;
    }

    private String normalizeText(
            String value
    ) {

        return value
                .replace(
                        '\u00A0',
                        ' '
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private String cleanValue(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value
                        .replace(
                                '\u00A0',
                                ' '
                        )
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        while (
                normalized.endsWith(".")
                        || normalized.endsWith(",")
                        || normalized.endsWith(";")
        ) {

            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1
                    )
                    .trim();
        }

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.isBlank();
    }
}