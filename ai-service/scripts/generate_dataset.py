import csv
import random
from pathlib import Path


RANDOM_SEED = 42
SAMPLES_PER_CLASS = 120
EXTRA_CHANGE_SAMPLES = 40

OUTPUT_PATH = Path("data") / "documents_dataset.csv"

random.seed(RANDOM_SEED)


ORGANIZATIONS = [
    'ООО "Альфа"',
    'ООО "БелТех"',
    'ЗАО "Вектор"',
    'ОАО "СтройИнвест"',
    'ООО "ИнфоСистемы"',
    'УП "БелСервис"',
    'ООО "ПромЛогистика"',
    'ЗАО "ТехноПлюс"',
]

SUBJECTS = [
    "поставка компьютерного оборудования",
    "оказание консультационных услуг",
    "разработка программного обеспечения",
    "техническое обслуживание оборудования",
    "поставка офисной мебели",
    "оказание транспортных услуг",
    "выполнение проектных работ",
    "информационное сопровождение",
]

CURRENCIES = [
    "белорусских рублей",
    "долларов США",
    "евро",
]

APPENDIX_TYPES = [
    "спецификация",
    "график поставки",
    "перечень оборудования",
    "техническое задание",
    "калькуляция стоимости",
    "перечень оказываемых услуг",
]


def random_number() -> str:
    return f"{random.randint(1, 999)}/{random.randint(1, 99)}"


def random_date() -> str:
    day = random.randint(1, 28)
    month = random.randint(1, 12)
    year = random.choice([2024, 2025, 2026])

    return f"{day:02d}.{month:02d}.{year}"


def random_amount() -> str:
    return f"{random.randint(500, 150000):,}".replace(",", " ")


def contract_sample() -> str:
    seller = random.choice(ORGANIZATIONS)
    buyer = random.choice(
        [item for item in ORGANIZATIONS if item != seller]
    )
    subject = random.choice(SUBJECTS)

    variants = [
        (
            f"ДОГОВОР № {random_number()} от {random_date()}. "
            f"{seller}, именуемое в дальнейшем Исполнитель, и {buyer}, "
            f"именуемое Заказчик, заключили настоящий договор. "
            f"Предметом договора является {subject}. "
            f"Стоимость договора составляет {random_amount()} "
            f"{random.choice(CURRENCIES)}. "
            f"Стороны определили порядок расчетов, права и обязанности, "
            f"ответственность и срок действия договора."
        ),
        (
            f"Договор поставки № {random_number()}. "
            f"Поставщик {seller} обязуется передать товар, а покупатель "
            f"{buyer} принять и оплатить его. "
            f"Предмет договора: {subject}. "
            f"Цена договора составляет {random_amount()} "
            f"{random.choice(CURRENCIES)}. "
            f"Настоящий договор вступает в силу с момента подписания сторонами."
        ),
        (
            f"ДОГОВОР оказания услуг № {random_number()} от {random_date()}. "
            f"Заказчик {buyer} поручает, а Исполнитель {seller} принимает "
            f"на себя обязательства по выполнению работ: {subject}. "
            f"Сроки выполнения работ, стоимость, порядок приемки и "
            f"ответственность сторон определяются настоящим договором."
        ),
    ]

    return random.choice(variants)


def amendment_change_sample() -> str:
    organization = random.choice(ORGANIZATIONS)
    point = random.randint(1, 9)

    variants = [
        (
            f"ДОПОЛНИТЕЛЬНОЕ СОГЛАШЕНИЕ № {random_number()} "
            f"к договору № {random_number()} от {random_date()}. "
            f"Стороны договорились внести изменения в условия договора. "
            f"Пункт {point}.1 договора изложить в новой редакции. "
            f"Стоимость договора изменить и установить в размере "
            f"{random_amount()} {random.choice(CURRENCIES)}. "
            f"Остальные условия договора остаются без изменений."
        ),
        (
            f"Дополнительное соглашение к договору № {random_number()}. "
            f"{organization} и вторая сторона пришли к соглашению изменить "
            f"отдельные положения ранее заключенного договора. "
            f"Пункт {point}.2 считать утратившим силу и изложить "
            f"соответствующий раздел в новой редакции. "
            f"Настоящее дополнительное соглашение является неотъемлемой "
            f"частью договора."
        ),
        (
            f"ДОПОЛНИТЕЛЬНОЕ СОГЛАШЕНИЕ от {random_date()}. "
            f"В связи с изменением условий сотрудничества стороны решили "
            f"внести изменения в договор № {random_number()}. "
            f"Изменить реквизиты стороны, порядок расчетов и цену договора. "
            f"Все остальные положения договора сохраняют свою силу."
        ),
    ]

    return random.choice(variants)


def amendment_change_full_document_sample() -> str:
    """A longer agreement with the contract vocabulary found in PDF forms."""
    base = random.choice([
        "договору поставки",
        "договору оказания услуг",
        "договору подряда",
        "основному договору",
    ])
    change = random.choice([
        "изменить цену",
        "уточнить предмет договора",
        "изменить объем работ",
        "скорректировать порядок оплаты",
        "изменить условия поставки",
        "дополнить перечень услуг",
        "заменить реквизиты стороны",
    ])
    return (
        f"ДОПОЛНИТЕЛЬНОЕ СОГЛАШЕНИЕ № {random.randint(1, 99)}\n"
        f"к {base} № DF-{random.randint(1, 999)}/25\n"
        "г. Минск\n"
        "Стороны заключили настоящее дополнительное соглашение "
        f"к ранее заключенному {base}.\n"
        "1. Изменения в условиях договора. "
        f"Стороны согласовали {change}. "
        f"Пункт {random.randint(1, 12)}.{random.randint(1, 9)} "
        "договора изложить в новой редакции. "
        f"Новая стоимость составляет {random.randint(1000, 99000)} "
        "белорусских рублей. "
        "2. Права и обязанности сторон, порядок расчетов, срок "
        "действия договора и остальные условия, не затронутые настоящим "
        "соглашением, сохраняют силу. Настоящее дополнительное "
        "соглашение является неотъемлемой частью договора. "
        "Реквизиты и подписи сторон."
    )


def amendment_prolongation_sample() -> str:
    variants = [
        (
            f"ДОПОЛНИТЕЛЬНОЕ СОГЛАШЕНИЕ № {random_number()} "
            f"к договору № {random_number()}. "
            f"Стороны договорились продлить срок действия договора. "
            f"Договор действует до 31 декабря "
            f"{random.choice([2026, 2027, 2028])} года. "
            f"В части, не затронутой настоящим соглашением, "
            f"условия договора остаются без изменений."
        ),
        (
            f"Дополнительное соглашение о пролонгации договора "
            f"№ {random_number()} от {random_date()}. "
            f"Стороны пришли к соглашению пролонгировать действие договора "
            f"на один календарный год. "
            f"Срок действия договора считать продленным. "
            f"Остальные условия договора сохраняют юридическую силу."
        ),
        (
            f"ДОПОЛНИТЕЛЬНОЕ СОГЛАШЕНИЕ. "
            f"Стороны договорились изменить срок действия договора "
            f"№ {random_number()} и установить новую дату окончания "
            f"{random_date()}. "
            f"Продление срока не изменяет остальные существенные условия "
            f"заключенного договора."
        ),
    ]

    return random.choice(variants)


def appendix_sample() -> str:
    appendix_type = random.choice(APPENDIX_TYPES)

    variants = [
        (
            f"ПРИЛОЖЕНИЕ № {random.randint(1, 10)} "
            f"к договору № {random_number()} от {random_date()}. "
            f"{appendix_type.capitalize()}. "
            f"Настоящее приложение является неотъемлемой частью договора. "
            f"В приложении определяется перечень позиций, количество, "
            f"стоимость и иные характеристики."
        ),
        (
            f"Приложение к договору № {random_number()}. "
            f"{appendix_type.capitalize()} по предмету договора: "
            f"{random.choice(SUBJECTS)}. "
            f"Стороны согласовали настоящий документ как составную "
            f"и неотъемлемую часть основного договора."
        ),
        (
            f"ПРИЛОЖЕНИЕ № {random.randint(1, 8)}. "
            f"К договору от {random_date()} № {random_number()}. "
            f"Документ содержит {appendix_type}, сроки выполнения, "
            f"объем и стоимость работ. "
            f"Условия настоящего приложения применяются совместно "
            f"с условиями основного договора."
        ),
    ]

    return random.choice(variants)


GENERATORS = {
    "CONTRACT": contract_sample,
    "AMENDMENT_CHANGE": amendment_change_sample,
    "AMENDMENT_PROLONGATION": amendment_prolongation_sample,
    "APPENDIX": appendix_sample,
}


def main() -> None:
    OUTPUT_PATH.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    rows: list[dict[str, str]] = []

    for label, generator in GENERATORS.items():
        for _ in range(SAMPLES_PER_CLASS):
            rows.append(
                {
                    "text": generator(),
                    "label": label,
                }
            )

    # Keep the original 480 examples in their established order, then add
    # the new full-document examples. This keeps the training set auditable.
    random.shuffle(rows)

    for _ in range(EXTRA_CHANGE_SAMPLES):
        rows.append({
            "text": amendment_change_full_document_sample(),
            "label": "AMENDMENT_CHANGE",
        })

    with OUTPUT_PATH.open(
        "w",
        encoding="utf-8",
        newline="",
    ) as csv_file:
        writer = csv.DictWriter(
            csv_file,
            fieldnames=["text", "label"],
            lineterminator="\n",
        )

        writer.writeheader()
        writer.writerows(rows)

    print(
        f"Dataset created: {OUTPUT_PATH}"
    )
    print(
        f"Total samples: {len(rows)}"
    )

    for label in GENERATORS:
        count = sum(
            1
            for row in rows
            if row["label"] == label
        )

        print(
            f"{label}: {count}"
        )


if __name__ == "__main__":
    main()
