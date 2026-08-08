import json
import re
import urllib.request
from collections import defaultdict

# URL of the Unicode emoji test file (change version if needed)
EMOJI_TEST_URL = "https://unicode.org/Public/emoji/15.0/emoji-test.txt"
SKIN_TONE_MODIFIERS = ["🏻", "🏼", "🏽", "🏾", "🏿"]
SKIN_TONE_NAMES = ["light skin tone", "medium-light skin tone", "medium skin tone",
                   "medium-dark skin tone", "dark skin tone"]

# Mapping from Unicode group names to your categories
CATEGORY_MAP = {
    "Smileys": "SMILEYS",
    "People & Body": "PEOPLE",
    "Animals & Nature": "ANIMALS",
    "Food & Drink": "FOOD",
    "Travel & Places": "TRAVEL",
    "Activities": "ACTIVITIES",
    "Objects": "OBJECTS",
    "Symbols": "SYMBOLS",
    "Flags": "FLAGS",
}

def generate_keywords(name):
    """Generate a list of keywords from the emoji name."""
    words = re.findall(r"[a-z0-9]+", name.lower())
    # Keep only meaningful tokens; you can customise this further
    return words

def parse_emoji_test(lines):
    """
    Parses the emoji-test.txt format.
    Returns a list of dicts with unicode, name, group, and status.
    Only fully-qualified emojis are kept.
    """
    emojis = []
    current_group = "Unknown"
    for line in lines:
        line = line.strip()
        if not line:
            continue
        if line.startswith("# group:"):
            current_group = line.split(":")[1].strip()
            continue
        if "#" in line and "fully-qualified" in line:
            parts = line.split("#")
            if len(parts) != 2:
                continue
            code_and_status = parts[0].strip()
            if "fully-qualified" in code_and_status:
                code_part = code_and_status.split(";")[0].strip()
                codepoints = code_part.split()
                try:
                    chars = "".join(chr(int(cp, 16)) for cp in codepoints)
                except ValueError:
                    continue
                name = parts[1].strip()
                # Remove version info like "E1.0" or "1.0"
                name_parts = name.split(" ")
                if len(name_parts) > 1 and name_parts[0].replace(".", "").isdigit():
                    name = " ".join(name_parts[1:])
                name = re.sub(r"^[\d.]+ ", "", name)
                emojis.append({
                    "unicode": chars,
                    "name": name,
                    "group": CATEGORY_MAP.get(current_group, "OTHER"),
                    "status": "fully-qualified",
                    "codepoints": codepoints,
                })
    return emojis

def is_skin_tone_modifiable(emoji):
    """
    Heuristic: if the emoji is in the PEOPLE category and is not already
    a skin-tone modifier and does not contain a ZWJ (complex sequence),
    we consider it skin‑tone‑modifiable.
    """
    if emoji["group"] != "PEOPLE":
        return False
    # Exclude if it already contains a skin tone modifier
    if any(mod in emoji["unicode"] for mod in SKIN_TONE_MODIFIERS):
        return False
    # Skip emojis with ZWJ (most family/gender sequences)
    if "200D" in " ".join(emoji["codepoints"]):
        return False
    # Also skip if it has more than 2 codepoints (like flags)
    if len(emoji["codepoints"]) > 2:
        return False
    return True

def generate_skin_tone_variants(base_entry):
    """
    For a base emoji entry that supports skin tones, create 5 variants.
    `base_entry` must have keys: unicode, name, category.
    """
    variants = []
    base_unicode = base_entry["unicode"]
    for i, modifier in enumerate(SKIN_TONE_MODIFIERS):
        new_unicode = base_unicode + modifier
        new_name = f"{base_entry['name']} ({SKIN_TONE_NAMES[i]})"
        keywords = generate_keywords(base_entry["name"]) + [SKIN_TONE_NAMES[i].replace(" skin tone", "")]
        variants.append({
            "unicode": new_unicode,
            "name": new_name,
            "keywords": keywords,
            "category": base_entry["category"],
            "skinToneSupport": False   # variants do not support further skin tones
        })
    return variants

def main():
    print("Downloading Unicode emoji test data...")
    with urllib.request.urlopen(EMOJI_TEST_URL) as response:
        content = response.read().decode("utf-8")
    lines = content.splitlines()

    print("Parsing emojis...")
    all_emojis = parse_emoji_test(lines)
    print(f"Found {len(all_emojis)} fully-qualified emojis.")

    # Load original file (if present)
    try:
        with open("emojis.json", "r", encoding="utf-8") as f:
            original = json.load(f)
        print(f"Loaded {len(original)} original emojis.")
    except FileNotFoundError:
        original = []
        print("No original file found, starting fresh.")

    # Start with the original emojis
    extended = list(original)
    existing_unicodes = {e["unicode"] for e in extended}

    # Process each parsed emoji
    for emoji in all_emojis:
        if emoji["unicode"] in existing_unicodes:
            continue

        # Create a base entry for this emoji
        entry = {
            "unicode": emoji["unicode"],
            "name": emoji["name"],
            "keywords": generate_keywords(emoji["name"]),
            "category": emoji["group"],
        }

        # Check if it supports skin tones
        if is_skin_tone_modifiable(emoji):
            entry["skinToneSupport"] = True
            extended.append(entry)
            existing_unicodes.add(emoji["unicode"])

            # Generate and add skin‑tone variants (using the entry we just created)
            variants = generate_skin_tone_variants(entry)
            for v in variants:
                if v["unicode"] not in existing_unicodes:
                    extended.append(v)
                    existing_unicodes.add(v["unicode"])
        else:
            # No skin tone support
            extended.append(entry)
            existing_unicodes.add(emoji["unicode"])

    print(f"Total emojis after extension: {len(extended)}")

    # Write to JSON file
    with open("emojis_extended.json", "w", encoding="utf-8") as f:
        json.dump(extended, f, ensure_ascii=False, indent=2)
    print("Extended file written to emojis_extended.json")

if __name__ == "__main__":
    main()