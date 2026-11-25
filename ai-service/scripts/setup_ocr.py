"""Install the two official Tesseract language models before service startup."""
import hashlib
from pathlib import Path
import urllib.request


DESTINATION = Path(__file__).resolve().parents[1] / "models" / "tessdata"
SOURCE = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/4.1.0"
SHA256 = {
    "rus": "e16e5e036cce1d9ec2b00063cf8b54472625b9e14d893a169e2b0dedeb4df225",
    "eng": "7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2",
}


def main():
    DESTINATION.mkdir(parents=True, exist_ok=True)
    for language in ("rus", "eng"):
        name = f"{language}.traineddata"
        target = DESTINATION / name
        if target.is_file() and hashlib.sha256(target.read_bytes()).hexdigest() == SHA256[language]:
            print(f"Verified {name}")
            continue
        with urllib.request.urlopen(f"{SOURCE}/{name}", timeout=30) as response:
            model = response.read()
        if hashlib.sha256(model).hexdigest() != SHA256[language]:
            raise RuntimeError(f"Invalid language model: {name}")
        temporary = target.with_suffix(".download")
        temporary.write_bytes(model)
        temporary.replace(target)
        print(f"Installed {name}; SHA256={hashlib.sha256(model).hexdigest()}")


if __name__ == "__main__":
    main()
