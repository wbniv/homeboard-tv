#!/usr/bin/env python3
"""Offline OCR oracle for the Homeboard TV air-quality smoke test."""
import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

from PIL import Image


def normalized(text):
    return re.sub(r"[^a-z0-9]+", " ", text.lower()).strip()


def read_card(image_path):
    """OCR the upper dashboard area; grid placement can put a widget anywhere."""
    with Image.open(image_path) as image:
        width, height = image.size
        crop = image.crop((int(width * .03), int(height * .08),
                           int(width * .97), int(height * .60)))
        crop = crop.resize((crop.width * 2, crop.height * 2))
        result = subprocess.run(
            ["tesseract", "stdin", "stdout", "--psm", "6"],
            input=_png_bytes(crop), check=True, capture_output=True, text=False)
    return normalized(result.stdout.decode("utf-8", errors="replace"))


def _png_bytes(image):
    import io
    output = io.BytesIO()
    image.save(output, format="PNG")
    return output.getvalue()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("evidence", type=Path)
    parser.add_argument("--expect-text", help="require text in the card OCR (negative test supported)")
    args = parser.parse_args()
    screenshot = args.evidence / "screenshot.png"
    if not screenshot.is_file():
        print(json.dumps({"pass": False, "failure": "missing screenshot.png"}))
        return 1
    text = read_card(screenshot)
    required = [normalized(args.expect_text)] if args.expect_text else [
        "pm2 5", "bangkok", "airbkk"]
    failures = [token for token in required if token not in text]
    print(json.dumps({"pass": not failures, "required": required,
                      "missing": failures, "card_ocr": text}, indent=2))
    return 0 if not failures else 1


if __name__ == "__main__":
    sys.exit(main())
