"""Regression examples independent from the generated training templates."""

import unittest

from app.services.document_classifier import DocumentClassifier


class ClassifierQualityTest(unittest.TestCase):
    def test_change_agreement_with_contract_vocabulary(self):
        text = (
            "Дополнительное соглашение № 15 к договору оказания услуг. "
            "Стороны согласовали изменение цены договора и порядка оплаты. "
            "Договор, заключенный между сторонами, действует в остальной "
            "части без изменений. Объем услуг уточняется в приложении. "
            "Новая цена составляет 25000 рублей. "
            "Стороны подписали документ."
        )

        result = DocumentClassifier().classify(text)

        self.assertEqual("AMENDMENT_CHANGE", result["document_type"])

    def test_unrelated_office_note_requires_review(self):
        text = (
            "СЛУЖЕБНАЯ ЗАПИСКА. Прошу предоставить доступ к корпоративной "
            "системе и согласовать учетную запись сотрудника. Руководитель "
            "отдела визирует заявку, администратор создает пользователя."
        )

        result = DocumentClassifier().classify(text)

        self.assertTrue(result["review_required"])


if __name__ == "__main__":
    unittest.main()
