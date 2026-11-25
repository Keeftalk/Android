#!/usr/bin/env python3
import os
import re
import sys
import subprocess
import datetime

def run_cmd(cmd):
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.returncode, result.stdout.strip(), result.stderr.strip()

def get_gradle_version_info(app_build_file):
    version_name = "1.2"
    version_code = "13"
    if os.path.exists(app_build_file):
        with open(app_build_file, 'r') as f:
            content = f.read()
            m_code = re.search(r'versionCode\s*=\s*(\d+)', content)
            if m_code:
                version_code = m_code.group(1)
            m_name = re.search(r'versionName\s*=\s*["\']([^"\']+)["\']', content)
            if m_name:
                version_name = m_name.group(1)
    return version_name, version_code

def get_git_tags():
    run_cmd("git fetch --tags --force")
    code, stdout, _ = run_cmd("git tag -l")
    if code == 0 and stdout:
        return [t.strip() for t in stdout.split('\n') if t.strip()]
    return []

def get_latest_tag(tags):
    release_tags = [t for t in tags if re.match(r'^(v\d+|Alpha)', t, re.IGNORECASE)]
    if not release_tags:
        return None

    tag_dates = []
    for t in release_tags:
        code, out, _ = run_cmd(f"git log -1 --format=%ct '{t}'")
        if code == 0 and out.isdigit():
            tag_dates.append((int(out), t))
    if tag_dates:
        tag_dates.sort()
        return tag_dates[-1][1]
    return release_tags[-1]

def increment_version(version_str):
    m = re.search(r'(\d+)\.(\d+)', version_str)
    if m:
        major = int(m.group(1))
        minor = int(m.group(2))
        return f"{major}.{minor + 1}"
    return version_str

def calculate_next_tag(gradle_version_name, last_tag, tags):
    base_version = gradle_version_name
    if last_tag:
        m = re.search(r'v?(\d+\.\d+)', last_tag)
        if m:
            last_version = m.group(1)
            try:
                g_parts = [int(x) for x in gradle_version_name.split('.')]
                l_parts = [int(x) for x in last_version.split('.')]
                if l_parts >= g_parts:
                    base_version = last_version
            except ValueError:
                pass

    next_version = increment_version(base_version)
    base_prefix = f"v{next_version}-pre."

    max_num = 0
    for t in tags:
        if t.startswith(base_prefix):
            try:
                num = int(t[len(base_prefix):])
                if num > max_num:
                    max_num = num
            except ValueError:
                pass
    next_num = max_num + 1
    return f"{base_prefix}{next_num}"

def generate_release_notes(last_tag, next_tag, version_name, version_code):
    commit_sha = os.environ.get("GITHUB_SHA", "")
    if not commit_sha:
        _, commit_sha, _ = run_cmd("git rev-parse --short HEAD")
    else:
        commit_sha = commit_sha[:7]

    now_utc = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")

    if last_tag:
        git_range = f"'{last_tag}'..HEAD"
        _, commits, _ = run_cmd(f"git log {git_range} --oneline --no-merges -n 30")
    else:
        _, commits, _ = run_cmd("git log -n 30 --oneline --no-merges")

    commit_list = "\n".join([f"- {c}" for c in commits.split("\n") if c.strip()]) if commits else "- Continuous integration and build improvements."

    repo = os.environ.get("GITHUB_REPOSITORY", "Keeftalk/Android")
    base_url = f"https://github.com/{repo}/releases/download/{next_tag}"

    notes = f"""## 🚀 Keeftalk Pre-Release {next_tag}

### 📋 Build Information
- **Version Name:** `{version_name}`
- **Version Code:** `{version_code}`
- **Commit SHA:** `{commit_sha}`
- **Build Date:** `{now_utc}`

### 📦 Architecture-Specific Downloads
Download the APK corresponding to your device CPU architecture:

- 📱 [**ARM64 64-bit (arm64-v8a)**]({base_url}/app-arm64-v8a-release.apk)
- 📱 [**ARM 32-bit (armeabi-v7a)**]({base_url}/app-armeabi-v7a-release.apk)
- 💻 [**x86_64 64-bit (x86_64)**]({base_url}/app-x86_64-release.apk)
- 💻 [**x86 32-bit (x86)**]({base_url}/app-x86-release.apk)

### 📝 Key Changes Since Last Release ({last_tag or 'Initial'})
{commit_list}
"""
    return notes

def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else "check"
    app_build_file = sys.argv[2] if len(sys.argv) > 2 else "app/build.gradle.kts"

    version_name, version_code = get_gradle_version_info(app_build_file)
    tags = get_git_tags()
    last_tag = get_latest_tag(tags)

    if mode == "check":
        force_release = os.environ.get("FORCE_RELEASE", "false").lower() == "true"
        event_name = os.environ.get("GITHUB_EVENT_NAME", "")

        merge_count = 0
        days_elapsed = 0

        if last_tag:
            _, out_merges, _ = run_cmd(f"git rev-list --min-parents=2 --count '{last_tag}..HEAD'")
            _, out_commits, _ = run_cmd(f"git rev-list --count '{last_tag}..HEAD'")

            merges_num = int(out_merges) if out_merges.isdigit() else 0
            commits_num = int(out_commits) if out_commits.isdigit() else 0

            merge_count = merges_num if merges_num > 0 else commits_num

            _, out_ts, _ = run_cmd(f"git log -1 --format=%ct '{last_tag}'")
            if out_ts.isdigit():
                tag_ts = int(out_ts)
                now_ts = int(datetime.datetime.now(datetime.timezone.utc).timestamp())
                days_elapsed = max(0, (now_ts - tag_ts) // 86400)
        else:
            merge_count = 5
            days_elapsed = 5

        should_release = (merge_count >= 5) or (days_elapsed >= 5) or force_release or (event_name == "workflow_dispatch")
        next_tag = calculate_next_tag(version_name, last_tag, tags)

        print(f"Version Name: {version_name}")
        print(f"Version Code: {version_code}")
        print(f"Last Tag: {last_tag}")
        print(f"Merges/Commits since last tag: {merge_count}")
        print(f"Days elapsed since last tag: {days_elapsed}")
        print(f"Should Release: {should_release}")
        print(f"Next Tag: {next_tag}")

        github_output = os.environ.get("GITHUB_OUTPUT")
        if github_output:
            with open(github_output, "a") as f:
                f.write(f"should_release={'true' if should_release else 'false'}\n")
                f.write(f"version_name={version_name}\n")
                f.write(f"version_code={version_code}\n")
                f.write(f"last_tag={last_tag or ''}\n")
                f.write(f"next_tag={next_tag}\n")
                f.write(f"merge_count={merge_count}\n")
                f.write(f"days_elapsed={days_elapsed}\n")

    elif mode == "notes":
        next_tag = calculate_next_tag(version_name, last_tag, tags)
        notes = generate_release_notes(last_tag, next_tag, version_name, version_code)
        out_file = sys.argv[3] if len(sys.argv) > 3 else "release_notes.md"
        with open(out_file, "w") as f:
            f.write(notes)
        print(f"Release notes written to {out_file}")

if __name__ == "__main__":
    main()
