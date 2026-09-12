import pymupdf


class PdfTextExtractor:

    def extract(self, file_bytes: bytes) -> dict:
        try:
            document = pymupdf.open(
                stream=file_bytes,
                filetype="pdf",
            )
        except Exception as exception:
            raise ValueError(
                "Не удалось открыть PDF-файл"
            ) from exception

        try:
            pages_text: list[str] = []

            for page in document:
                page_text = page.get_text("text").strip()

                if page_text:
                    pages_text.append(page_text)

            text = "\n\n".join(pages_text).strip()

            return {
                "page_count": document.page_count,
                "text": text,
                "character_count": len(text),
                "has_text": bool(text),
            }

        finally:
            document.close()