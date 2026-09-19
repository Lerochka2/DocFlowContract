import re
from dataclasses import dataclass
from typing import Pattern


ANALYZER_VERSION = "contract-risk-rules-v2"


@dataclass(frozen=True)
class RiskRule:
    code: str
    title: str
    severity: str
    weight: int
    patterns: tuple[Pattern[str], ...]
    explanation: str
    recommendation: str


RISK_RULES = (
    RiskRule(
        code="PENALTY",
        title="Штрафы и неустойка",
        severity="MEDIUM",
        weight=20,
        patterns=(
            re.compile(
                r"\bнеустойк\w*\b",
                re.IGNORECASE,
            ),
            re.compile(
                r"\bштраф\w*\b",
                re.IGNORECASE,
            ),
            re.compile(
                r"\bпен\w*\b",
                re.IGNORECASE,
            ),
        ),
        explanation=(
            "В договоре обнаружены условия о штрафах, "
            "пене или неустойке. Такие положения могут "
            "увеличивать финансовые последствия нарушения обязательств."
        ),
        recommendation=(
            "Проверить размер санкций, условия их начисления "
            "и соразмерность возможному нарушению."
        ),
    ),
    RiskRule(
        code="UNILATERAL_TERMINATION",
        title="Одностороннее расторжение",
        severity="HIGH",
        weight=30,
        patterns=(
            re.compile(
                r"\bодносторонн\w*\b.{0,100}\b"
                r"(расторж\w*|расторг\w*|отказ\w*)\b",
                re.IGNORECASE | re.DOTALL,
            ),
            re.compile(
                r"\b(расторж\w*|отказ\w*)\b.{0,100}\b"
                r"односторонн\w*\b",
                re.IGNORECASE | re.DOTALL,
            ),
        ),
        explanation=(
            "Обнаружена возможность одностороннего прекращения "
            "или расторжения договора."
        ),
        recommendation=(
            "Проверить, для какой стороны предусмотрено это право, "
            "какие основания применяются и установлен ли срок уведомления."
        ),
    ),
    RiskRule(
        code="AUTO_RENEWAL",
        title="Автоматическая пролонгация",
        severity="MEDIUM",
        weight=15,
        patterns=(
            re.compile(
                r"\bавтоматическ\w*\b.{0,80}\b"
                r"(пролонг\w*|продлен\w*|продлева\w*)\b",
                re.IGNORECASE | re.DOTALL,
            ),
            re.compile(
                r"\b(пролонг\w*|продлен\w*|продлева\w*)\b"
                r".{0,80}\bавтоматическ\w*\b",
                re.IGNORECASE | re.DOTALL,
            ),
        ),
        explanation=(
            "В договоре присутствует условие об автоматическом "
            "продлении срока действия."
        ),
        recommendation=(
            "Проверить порядок отказа от продления и срок, "
            "за который необходимо уведомить другую сторону."
        ),
    ),
    RiskRule(
        code="ADVANCE_PAYMENT",
        title="Предоплата или аванс",
        severity="MEDIUM",
        weight=15,
        patterns=(
            re.compile(
                r"\bпредоплат\w*\b",
                re.IGNORECASE,
            ),
            re.compile(
                r"\bаванс\w*\b",
                re.IGNORECASE,
            ),
        ),
        explanation=(
            "Обнаружено условие о предварительной оплате "
            "или авансовом платеже."
        ),
        recommendation=(
            "Проверить размер предоплаты, условия возврата средств "
            "и момент возникновения встречных обязательств."
        ),
    ),
    RiskRule(
        code="LIABILITY_LIMITATION",
        title="Ограничение ответственности",
        severity="HIGH",
        weight=25,
        patterns=(
            re.compile(
                r"\bне\s+нес[её]т\s+ответственност\w*\b",
                re.IGNORECASE,
            ),
            re.compile(
                r"\bосвобожда\w*\s+от\s+ответственност\w*\b",
                re.IGNORECASE,
            ),
            re.compile(
                r"\bответственност\w*\b.{0,70}\b"
                r"(ограничива\w*|ограничен\w*)\b",
                re.IGNORECASE | re.DOTALL,
            ),
            re.compile(
                r"\b(ограничить|ограничивается|ограничена|"
                r"ограничен|ограничены|ограничение)\b"
                r".{0,70}\bответственност\w*\b",
                re.IGNORECASE | re.DOTALL,
            ),
        ),
        explanation=(
            "Найдены положения, которые могут исключать "
            "или ограничивать ответственность одной из сторон."
        ),
        recommendation=(
            "Проверить баланс ответственности сторон и перечень случаев, "
            "в которых сторона освобождается от ответственности."
        ),
    ),
    RiskRule(
        code="SHORT_NOTICE",
        title="Короткий срок уведомления",
        severity="MEDIUM",
        weight=10,
        patterns=(
            re.compile(
                r"\bуведом\w*.{0,100}\b"
                r"(?:1|2|3|4|5)\s+"
                r"(?:календарн\w+\s+|рабоч\w+\s+)?"
                r"дн",
                re.IGNORECASE | re.DOTALL,
            ),
        ),
        explanation=(
            "Обнаружен короткий срок для направления уведомления."
        ),
        recommendation=(
            "Проверить, достаточно ли указанного срока "
            "для выполнения соответствующего обязательства."
        ),
    ),
)


class ContractRiskAnalyzer:

    def analyze(self, text: str) -> dict:
        cleaned_text = self._normalize_text(text)

        if not cleaned_text:
            raise ValueError(
                "Текст договора отсутствует"
            )

        findings = []

        for rule in RISK_RULES:
            finding = self._find_risk(
                cleaned_text,
                rule,
            )

            if finding is not None:
                findings.append(finding)

        risk_score = min(
            100,
            sum(
                finding["weight"]
                for finding in findings
            ),
        )

        risk_level = self._risk_level(
            risk_score,
            findings,
        )

        return {
            "risk_level": risk_level,
            "risk_score": risk_score,
            "risk_count": len(findings),
            "risks": findings,
            "summary": self._build_summary(
                risk_level,
                len(findings),
            ),
            "analyzer_version": ANALYZER_VERSION,
        }

    def _find_risk(
        self,
        text: str,
        rule: RiskRule,
    ) -> dict | None:
        matches = []

        for pattern in rule.patterns:
            match = pattern.search(text)

            if match is not None:
                matches.append(match)

        if not matches:
            return None

        first_match = min(
            matches,
            key=lambda item: item.start(),
        )

        return {
            "code": rule.code,
            "title": rule.title,
            "severity": rule.severity,
            "weight": rule.weight,
            "evidence": self._extract_evidence(
                text,
                first_match.start(),
                first_match.end(),
            ),
            "explanation": rule.explanation,
            "recommendation": rule.recommendation,
        }

    def _extract_evidence(
        self,
        text: str,
        start: int,
        end: int,
    ) -> str:
        left = max(
            0,
            start - 100,
        )

        right = min(
            len(text),
            end + 140,
        )

        fragment = text[left:right].strip()

        fragment = re.sub(
            r"\s+",
            " ",
            fragment,
        )

        if left > 0:
            fragment = "… " + fragment

        if right < len(text):
            fragment = fragment + " …"

        return fragment

    def _normalize_text(
        self,
        text: str,
    ) -> str:
        return re.sub(
            r"\s+",
            " ",
            text,
        ).strip()

    def _risk_level(
        self,
        score: int,
        findings: list[dict],
    ) -> str:
        severities = {
            str(
                finding.get(
                    "severity",
                    "",
                )
            ).upper()
            for finding in findings
        }

        if (
            "HIGH" in severities
            or score >= 55
        ):
            return "HIGH"

        if (
            "MEDIUM" in severities
            or score >= 25
        ):
            return "MEDIUM"

        return "LOW"

    def _build_summary(
        self,
        level: str,
        risk_count: int,
    ) -> str:
        if risk_count == 0:
            return (
                "Явные рискованные условия по заданным "
                "правилам не обнаружены."
            )

        if level == "HIGH":
            return (
                "Обнаружены условия, требующие повышенного "
                "внимания при юридической проверке договора."
            )

        if level == "MEDIUM":
            return (
                "Обнаружены отдельные условия, которые "
                "рекомендуется дополнительно проверить."
            )

        return (
            "Обнаружены отдельные условия с низким "
            "уровнем совокупного риска."
        )