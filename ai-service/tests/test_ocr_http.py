"""Exercise real HTTP endpoints with a real OCR process, without extra dependencies."""
import json
from pathlib import Path
import socket
import subprocess
import sys
import time
import unittest
import urllib.error
import urllib.request

from test_pdf_ocr import make_pdf


class OcrHttpTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with socket.socket() as listener:
            listener.bind(("127.0.0.1", 0))
            port = listener.getsockname()[1]
        cls.base = f"http://127.0.0.1:{port}"
        cls.process = subprocess.Popen(
            [sys.executable, "-B", "-m", "uvicorn", "app.main:app", "--host", "127.0.0.1",
             "--port", str(port)], cwd=Path(__file__).resolve().parents[1],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
            creationflags=subprocess.CREATE_NO_WINDOW if sys.platform == "win32" else 0,
        )
        cls.addClassCleanup(cls.stop_server)
        for _ in range(100):
            try:
                with urllib.request.urlopen(cls.base + "/health", timeout=1):
                    return
            except (OSError, urllib.error.URLError):
                if cls.process.poll() is not None:
                    raise RuntimeError("Test AI server failed to start")
                time.sleep(0.1)
        raise RuntimeError("Test AI server startup timeout")

    @classmethod
    def stop_server(cls):
        cls.process.terminate()
        try:
            cls.process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            cls.process.kill()
            cls.process.wait(timeout=5)

    def post(self, path, payload, content_type):
        request = urllib.request.Request(self.base + path, data=payload,
                                         headers={"Content-Type": content_type}, method="POST")
        with urllib.request.urlopen(request, timeout=60) as response:
            self.assertEqual(200, response.status)
            return json.load(response)

    def test_backend_internal_endpoint_recognizes_scan(self):
        result = self.post("/internal/analyze", make_pdf("scan"), "application/pdf")
        self.assertTrue(result["ocrUsed"])
        self.assertFalse(result["requiresOcr"])
        self.assertEqual("CONTRACT", result["predictedType"])
        self.assertIn("договор", result["text"].lower())

    def test_public_upload_endpoints_recognize_scan(self):
        boundary = "DocFlowOcrTestBoundary"
        body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; "
                "filename=\"scan.pdf\"\r\nContent-Type: application/pdf\r\n\r\n").encode()
        body += make_pdf("scan") + f"\r\n--{boundary}--\r\n".encode()
        for endpoint in ("/extract-text", "/analyze"):
            with self.subTest(endpoint=endpoint):
                result = self.post(endpoint, body, f"multipart/form-data; boundary={boundary}")
                self.assertTrue(result["ocrUsed"])
                self.assertTrue(result["hasText"])
                self.assertFalse(result["requiresOcr"])


if __name__ == "__main__":
    unittest.main()
